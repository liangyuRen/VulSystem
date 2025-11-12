package com.nju.backend.service.project.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nju.backend.config.vo.ProjectAnalysisResult;
import com.nju.backend.config.vo.ProjectVO;
import com.nju.backend.config.vo.VulnerabilityVO;
import com.nju.backend.repository.mapper.*;
import com.nju.backend.repository.po.*;
import com.nju.backend.service.project.ProjectService;
import com.nju.backend.service.project.util.ProjectUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

@Component
public class ProjectServiceImpl implements ProjectService, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectUtil projectUtil;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private ProjectVulnerabilityMapper projectVulnerabilityMapper;

    @Autowired
    private VulnerabilityMapper vulnerabilityMapper;
    @Autowired
    private String getOpenscaToolPath;

    @org.springframework.beans.factory.annotation.Value("${flask.crawler.url}")
    private String flaskCrawlerUrl;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Autowired
    private WhiteListMapper whiteListMapper;

    @Autowired
    private VulnerabilityReportMapper vulnerabilityReportMapper;

    @Override
    @Transactional
    public Object uploadAndAnalyzeProject(String name, String description, int riskThreshold, int companyId, MultipartFile file) throws IOException {
        System.out.println("======== 开始统一上传和分析项目 ========");
        System.out.println("项目名称: " + name);
        System.out.println("公司ID: " + companyId);

        // 验证公司存在
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new RuntimeException("Company does not exist.");
        }

        // 解压文件
        String filePath = projectUtil.unzipAndSaveFile(file);
        System.out.println("文件解压完成，路径: " + filePath);

        // 创建项目记录，初始状态为 "pending"
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setRiskThreshold(riskThreshold);
        project.setIsDelete(0);
        project.setRoadmapFile("");
        project.setCreateTime(new Date());
        project.setFile(filePath);
        project.setLanguage("detecting");  // 初始状态：检测中
        project.setAnalysisStatus("pending");  // 分析状态：待分析
        project.setComponentCount(0);
        project.setVulnerabilityCount(0);
        project.setLastAnalysisTime(System.currentTimeMillis());

        projectMapper.insert(project);
        System.out.println("项目记录已创建，ID: " + project.getId());

        // 更新公司的项目列表
        if (company.getProjectId() == null || company.getProjectId().isEmpty()) {
            company.setProjectId("{}");
        }

        // 修复JSON处理 - 使用ObjectMapper而非字符串操作
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> projectMap = mapper.readValue(company.getProjectId(), new TypeReference<Map<String, Object>>() {});
            projectMap.put(String.valueOf(project.getId()), "detecting");
            company.setProjectId(mapper.writeValueAsString(projectMap));
        } catch (Exception e) {
            // 如果JSON解析失败，重新初始化为包含当前项目的JSON对象
            company.setProjectId("{\"" + project.getId() + "\":\"detecting\"}");
        }

        companyMapper.updateById(company);

        // 启动异步分析任务
        ProjectService projectService = applicationContext.getBean(ProjectService.class);
        projectService.asyncAnalyzeProject(project.getId(), filePath);

        // 返回项目ID和初始状态
        Map<String, Object> response = new HashMap<>();
        response.put("projectId", project.getId());
        response.put("status", "pending");
        response.put("message", "Project uploaded, analysis started...");
        return response;
    }

    /**
     * 异步分析项目的核心方法
     * 完整的分析流程：
     * 1. 自动检测所有项目语言（支持多语言）
     * 2. 使用parse_and_save_v2.py脚本解析依赖并保存到数据库
     * 3. 获取保存的组件信息
     * 4. 匹配组件与漏洞
     * 5. 计算风险级别
     * 6. 更新项目状态
     */
    @Async("projectAnalysisExecutor")
    public void asyncAnalyzeProject(Integer projectId, String filePath) {
        System.out.println("======== 开始异步分析项目 " + projectId + " ========");
        Project project = projectMapper.selectById(projectId);

        if (project == null) {
            System.err.println("项目不存在: " + projectId);
            return;
        }

        try {
            // 更新状态为"分析中"
            updateProjectStatus(projectId, "analyzing", null);

            // 步骤1-3: 自动检测语言、解析依赖、保存到数据库
            System.out.println("步骤1-3: 使用parse_and_save_v2.py自动检测语言并解析依赖...");
            int savedCount = runPythonParseScript(projectId, filePath);
            System.out.println("Python脚本保存的组件数: " + savedCount);

            if (savedCount < 0) {
                updateProjectStatus(projectId, "failed", "Failed to run Python parse script");
                return;
            }

            // 从数据库查询已保存的组件，获取语言和详细信息
            System.out.println("步骤3.5: 从数据库查询项目信息...");
            QueryWrapper<WhiteList> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("project_id", projectId);
            List<WhiteList> components = whiteListMapper.selectList(queryWrapper);
            System.out.println("从数据库查询到的组件数: " + components.size());

            // 从数据库更新项目的语言和组件数
            Project updatedProject = projectMapper.selectById(projectId);
            if (updatedProject != null) {
                project.setLanguage(updatedProject.getLanguage());
                project.setComponentCount(updatedProject.getComponentCount());
            }

            // 步骤4: 匹配组件与漏洞
            System.out.println("步骤4: 匹配组件与漏洞...");
            int vulnerabilityCount = matchComponentWithVulnerabilities(projectId, components);
            System.out.println("匹配到的漏洞数: " + vulnerabilityCount);

            // 步骤5: 计算风险级别
            System.out.println("步骤5: 计算风险级别...");
            String riskLevel = analyzeVulnerabilities(projectId, project.getRiskThreshold());
            System.out.println("项目风险级别: " + riskLevel);

            // 步骤6: 更新项目状态为"已完成"
            project.setVulnerabilityCount(vulnerabilityCount);
            project.setRiskLevel(riskLevel);
            project.setLastAnalysisTime(System.currentTimeMillis());
            updateProjectStatus(projectId, "completed", "Analysis completed successfully");

        } catch (Exception e) {
            System.err.println("异步分析项目失败: " + e.getMessage());
            e.printStackTrace();
            updateProjectStatus(projectId, "failed", "Analysis failed: " + e.getMessage());
        }

        System.out.println("======== 异步分析项目 " + projectId + " 完成 ========");
    }

    /**
     * 运行Python parse_and_save_v2.py脚本进行自动语言检测和依赖解析
     * 脚本会：
     * 1. 自动检测项目中的所有编程语言
     * 2. 为每种语言调用相应的解析器
     * 3. 将依赖结果保存到数据库white_list表
     * 4. 更新项目元数据（语言、组件数等）
     *
     * @param projectId 项目ID
     * @param filePath 项目文件路径
     * @return 保存的组件数，-1表示执行失败
     */
    private int runPythonParseScript(Integer projectId, String filePath) {
        try {
            System.out.println("执行Python脚本: parse_and_save_v2.py");
            System.out.println("项目ID: " + projectId + ", 项目路径: " + filePath);

            // 构建Python脚本命令
            String pythonScriptPath = "/root/VulSystem/parse_and_save_v2.py";
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python3",
                    pythonScriptPath,
                    String.valueOf(projectId)
            );

            // 重定向错误输出到标准输出（便于日志记录）
            processBuilder.redirectErrorStream(true);

            // 启动进程
            Process process = processBuilder.start();

            // 读取脚本的输出（实时打印日志）
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Python] " + line);
                    output.append(line).append("\n");
                }
            }

            // 等待脚本执行完成（设置超时时间：60秒）
            boolean completed = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
            int exitCode = process.exitValue();

            if (!completed) {
                System.err.println("Python脚本执行超时 (60秒)");
                process.destroy();
                return -1;
            }

            if (exitCode != 0) {
                System.err.println("Python脚本执行失败，退出码: " + exitCode);
                return -1;
            }

            System.out.println("Python脚本执行成功");

            // 查询数据库获取保存的组件数
            QueryWrapper<WhiteList> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("project_id", projectId);
            long count = whiteListMapper.selectCount(queryWrapper);
            int componentCount = (int) count;
            System.out.println("从数据库查询得到保存的组件数: " + componentCount);

            return componentCount;

        } catch (Exception e) {
            System.err.println("执行Python脚本异常: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 调用Flask Parse API获取项目依赖
     * 对于Odoo项目，直接解析__manifest__.py文件的"depends"字段
     * 对于其他语言，调用对应的Flask API
     */
    private List<WhiteList> callFlaskParseAPI(String filePath, String language) {
        System.out.println("调用Flask API: 语言=" + language + ", 路径=" + filePath);

        try {
            // 特殊处理Odoo项目 - 直接解析__manifest__.py文件
            if ("odoo".equals(language)) {
                System.out.println("检测到Odoo项目，直接解析__manifest__.py文件");
                List<String> odooModules = projectUtil.extractOdooDependencies(filePath);

                // 将Odoo模块名称转换为WhiteList对象
                List<WhiteList> components = new ArrayList<>();
                for (String moduleName : odooModules) {
                    WhiteList component = new WhiteList();
                    component.setName(moduleName);
                    component.setDescription("Odoo module dependency");
                    components.add(component);
                }

                System.out.println("提取到 " + components.size() + " 个Odoo模块");
                return components;
            }

            // 对于非Odoo项目，调用Flask API
            RestTemplate restTemplate = new RestTemplate();
            String apiPath;

            // 根据语言选择对应的API端点
            switch (language) {
                case "java":
                    apiPath = "/parse/pom_parse";
                    break;
                case "javascript":
                    apiPath = "/parse/javascript_parse";
                    break;
                case "python":
                    apiPath = "/parse/python_parse";
                    break;
                case "php":
                    apiPath = "/parse/php_parse";
                    break;
                case "go":
                    apiPath = "/parse/go_parse";
                    break;
                case "rust":
                    apiPath = "/parse/rust_parse";
                    break;
                case "ruby":
                    apiPath = "/parse/ruby_parse";
                    break;
                case "erlang":
                    apiPath = "/parse/erlang_parse";
                    break;
                case "c":
                case "c++":
                    apiPath = "/parse/c_parse";
                    break;
                default:
                    System.err.println("不支持的语言: " + language);
                    return new ArrayList<>();
            }

            String url = UriComponentsBuilder.fromHttpUrl(flaskCrawlerUrl + apiPath)
                    .queryParam("project_folder", filePath)
                    .encode()
                    .build()
                    .toUriString();

            System.out.println("请求URL: " + url);
            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.trim().isEmpty()) {
                System.err.println("Flask API返回空响应");
                return new ArrayList<>();
            }

            if (response.contains("<!doctype html>") || response.contains("<html")) {
                System.err.println("Flask API返回错误页面");
                return new ArrayList<>();
            }

            List<WhiteList> components = projectUtil.parseJsonData(response);
            System.out.println("解析到 " + components.size() + " 个组件");
            return components;

        } catch (Exception e) {
            System.err.println("调用Flask API失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 保存组件到数据库
     */
    private int saveComponentsToDB(Integer projectId, String filePath, String language, List<WhiteList> components) {
        int insertCount = 0;

        for (WhiteList component : components) {
            try {
                // 设置新增字段
                component.setProjectId(projectId);  // 关联项目
                component.setFilePath(filePath);
                component.setLanguage(language);
                component.setCreatedTime(System.currentTimeMillis());  // 设置创建时间

                // 如果没有版本号，设置为 "unknown"
                if (component.getVersion() == null || component.getVersion().isEmpty()) {
                    component.setVersion("unknown");
                }

                // 如果没有包管理器信息，根据语言推断
                if (component.getPackageManager() == null || component.getPackageManager().isEmpty()) {
                    component.setPackageManager(inferPackageManager(language));
                }

                component.setIsdelete(0);

                int result = whiteListMapper.insert(component);
                if (result > 0) {
                    insertCount++;
                    System.out.println("✓ 保存组件: " + component.getName() + " v" + component.getVersion());
                }
            } catch (Exception e) {
                System.err.println("保存组件失败: " + component.getName() + ", 错误: " + e.getMessage());
            }
        }

        System.out.println("成功保存 " + insertCount + " 个组件到数据库 (project_id: " + projectId + ")");
        return insertCount;
    }

    /**
     * 根据编程语言推断包管理器
     */
    private String inferPackageManager(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return "maven";
            case "javascript":
                return "npm";
            case "python":
                return "pip";
            case "php":
                return "composer";
            case "go":
                return "gomod";
            case "rust":
                return "cargo";
            case "ruby":
                return "bundler";
            case "erlang":
                return "rebar";
            case "odoo":
                return "odoo_module";
            default:
                return language;
        }
    }

    /**
     * 匹配组件与漏洞
     * 根据组件名称和语言在漏洞库中查找相关漏洞
     */
    private int matchComponentWithVulnerabilities(Integer projectId, List<WhiteList> components) {
        int vulnerabilityCount = 0;

        try {
            for (WhiteList component : components) {
                // 根据组件名和语言查询漏洞
                QueryWrapper<VulnerabilityReport> queryWrapper = new QueryWrapper<>();
                queryWrapper.like("description", component.getName())
                           .eq("language", component.getLanguage())
                           .eq("isdelete", 0);

                List<VulnerabilityReport> vulnerabilities = vulnerabilityReportMapper.selectList(queryWrapper);

                System.out.println("组件 " + component.getName() + " 匹配到 " + vulnerabilities.size() + " 个漏洞");

                // 为每个漏洞创建项目-漏洞关联
                for (VulnerabilityReport vulnReport : vulnerabilities) {
                    try {
                        // 先检查是否已存在项目-漏洞关联
                        QueryWrapper<ProjectVulnerability> pvWrapper = new QueryWrapper<>();
                        pvWrapper.eq("project_id", projectId)
                                .eq("vulnerability_id", vulnReport.getId());

                        List<ProjectVulnerability> existing = projectVulnerabilityMapper.selectList(pvWrapper);

                        if (existing.isEmpty()) {
                            // 创建关联
                            ProjectVulnerability pv = new ProjectVulnerability();
                            pv.setProjectId(projectId);
                            pv.setVulnerabilityId(vulnReport.getId());
                            pv.setIsDelete(0);
                            projectVulnerabilityMapper.insert(pv);
                            vulnerabilityCount++;
                        }
                    } catch (Exception e) {
                        System.err.println("创建项目-漏洞关联失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("匹配漏洞失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("共关联 " + vulnerabilityCount + " 个漏洞");
        return vulnerabilityCount;
    }

    /**
     * 分析项目的漏洞并计算风险等级
     * 风险等级: 暂无风险/低风险/高风险
     */
    private String analyzeVulnerabilities(Integer projectId, Integer riskThreshold) {
        try {
            // 获取项目的所有漏洞
            QueryWrapper<ProjectVulnerability> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("project_id", projectId).eq("isDelete", 0);
            List<ProjectVulnerability> projectVulnerabilities = projectVulnerabilityMapper.selectList(queryWrapper);

            if (projectVulnerabilities.isEmpty()) {
                return "暂无风险";
            }

            // 统计高风险漏洞数量
            int highRiskCount = 0;
            for (ProjectVulnerability pv : projectVulnerabilities) {
                VulnerabilityReport vulnReport = vulnerabilityReportMapper.selectById(pv.getVulnerabilityId());
                if (vulnReport != null && "High".equalsIgnoreCase(vulnReport.getRiskLevel())) {
                    highRiskCount++;
                }
            }

            System.out.println("项目 " + projectId + " 的高风险漏洞数: " + highRiskCount + ", 阈值: " + riskThreshold);

            if (highRiskCount >= riskThreshold) {
                return "高风险";
            } else if (projectVulnerabilities.size() > 0) {
                return "低风险";
            } else {
                return "暂无风险";
            }

        } catch (Exception e) {
            System.err.println("分析漏洞失败: " + e.getMessage());
            e.printStackTrace();
            return "未知";
        }
    }

    /**
     * 更新项目的分析状态
     */
    @Transactional
    private void updateProjectStatus(Integer projectId, String status, String message) {
        try {
            Project project = projectMapper.selectById(projectId);
            if (project != null) {
                project.setAnalysisStatus(status);
                project.setAnalysisMessage(message);
                project.setLastAnalysisTime(System.currentTimeMillis());
                projectMapper.updateById(project);
                System.out.println("项目 " + projectId + " 状态已更新为: " + status);
            }
        } catch (Exception e) {
            System.err.println("更新项目状态失败: " + e.getMessage());
        }
    }

    @Override
    public ProjectAnalysisResult getProjectAnalysisStatus(int projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("Project not found");
        }

        ProjectAnalysisResult result = new ProjectAnalysisResult();
        result.setProjectId(projectId);
        result.setLanguage(project.getLanguage());
        result.setComponentCount(project.getComponentCount() != null ? project.getComponentCount() : 0);
        result.setVulnerabilityCount(project.getVulnerabilityCount() != null ? project.getVulnerabilityCount() : 0);
        result.setRiskLevel(project.getRiskLevel());
        result.setStatus(project.getAnalysisStatus() != null ? project.getAnalysisStatus() : "pending");
        result.setMessage(project.getAnalysisMessage());
        result.setLastAnalysisTime(project.getLastAnalysisTime());

        return result;
    }

    @Override
    @Transactional
    public void createProject(String name, String description, String language, int risk_threshold, int companyId, String filePath) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        if (projectMapper.selectOne(queryWrapper.eq("name", name)) != null) {
            throw new RuntimeException("Project already exists.");
        }
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setLanguage(language);
        project.setRiskThreshold(risk_threshold);
        project.setIsDelete(0);
        project.setRoadmapFile("");
        project.setCreateTime(new Date());
        project.setFile(filePath);

        projectMapper.insert(project);

        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new RuntimeException("Company does not exist.");
        }
        if (company.getProjectId() == null || company.getProjectId().isEmpty()) {
            company.setProjectId("{}");
        }

        // 修复JSON处理 - 使用JSONObject而非字符串操作
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> projectMap = mapper.readValue(company.getProjectId(), new TypeReference<Map<String, Object>>() {});
            projectMap.put(String.valueOf(project.getId()), project.getLanguage());
            company.setProjectId(mapper.writeValueAsString(projectMap));
        } catch (Exception e) {
            // 如果JSON解析失败，重新初始化为空JSON对象
            company.setProjectId("{\"" + project.getId() + "\":\"" + project.getLanguage() + "\"}");
        }

        companyMapper.updateById(company);
    }

    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParseJavaProject(String filePath) {
        System.out.println("开始解析Java项目: " + filePath);
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = UriComponentsBuilder.fromHttpUrl(flaskCrawlerUrl + "/parse/pom_parse")
                    .queryParam("project_folder", filePath)
                    .encode() // 自动处理 URL 编码
                    .build()
                    .toUriString();

            System.out.println("调用POM解析API: " + url);
            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.trim().isEmpty()) {
                System.err.println("POM解析API返回空响应，项目路径: " + filePath);
                return;
            }

            // 检查是否返回了错误页面
            if (response.contains("<!doctype html>") || response.contains("<html")) {
                System.err.println("POM解析API返回错误页面，项目路径: " + filePath);
                System.err.println("错误详情: " + response.substring(0, Math.min(500, response.length())));
                return;
            }

            System.out.println("POM解析响应长度: " + response.length());
            System.out.println("POM解析响应内容: " + response.substring(0, Math.min(200, response.length())) + "...");

            List<WhiteList> whiteLists = projectUtil.parseJsonData(response);
            System.out.println("解析出依赖库数量: " + whiteLists.size());

            int insertCount = 0;
            for (WhiteList whiteList : whiteLists) {
                whiteList.setFilePath(filePath);
                whiteList.setLanguage("java");
                whiteList.setIsdelete(0);
                int result = whiteListMapper.insert(whiteList);
                if (result > 0) {
                    insertCount++;
                }
            }
            System.out.println("成功插入依赖库数量: " + insertCount);
        } catch (Exception e) {
            System.err.println("解析Java项目失败，路径: " + filePath + "，错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParseCProject(String filePath){
        System.out.println("开始解析C/C++项目: " + filePath);
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = UriComponentsBuilder.fromHttpUrl(flaskCrawlerUrl + "/parse/c_parse")
                    .queryParam("project_folder", filePath)
                    .encode() // 自动处理 URL 编码
                    .build()
                    .toUriString();

            System.out.println("调用C项目解析API: " + url);
            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.trim().isEmpty()) {
                System.err.println("C项目解析API返回空响应，项目路径: " + filePath);
                return;
            }

            // 检查是否返回了错误页面
            if (response.contains("<!doctype html>") || response.contains("<html")) {
                System.err.println("C项目解析API返回错误页面，项目路径: " + filePath);
                System.err.println("错误详情: " + response.substring(0, Math.min(500, response.length())));
                return;
            }

            System.out.println("C项目解析响应长度: " + response.length());
            System.out.println("C项目解析响应内容: " + response.substring(0, Math.min(200, response.length())) + "...");

            List<WhiteList> whiteLists = projectUtil.parseJsonData(response);
            System.out.println("解析出依赖库数量: " + whiteLists.size());

            int insertCount = 0;
            for (WhiteList whiteList : whiteLists) {
                whiteList.setFilePath(filePath);
                whiteList.setLanguage("c/c++");
                whiteList.setIsdelete(0);
                int result = whiteListMapper.insert(whiteList);
                if (result > 0) {
                    insertCount++;
                }
            }
            System.out.println("成功插入C依赖库数量: " + insertCount);
        } catch (Exception e) {
            System.err.println("解析C/C++项目失败，路径: " + filePath + "，错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParseGoProject(String filePath){
        parseProject(filePath, "go", "/parse/go_parse");
    }

    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParseJavascriptProject(String filePath){
        parseProject(filePath, "javascript", "/parse/javascript_parse");
    }

    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParsePythonProject(String filePath){
        parseProject(filePath, "python", "/parse/python_parse");
    }

    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParsePhpProject(String filePath){
        parseProject(filePath, "php", "/parse/php_parse");
    }

    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParseRubyProject(String filePath){
        parseProject(filePath, "ruby", "/parse/ruby_parse");
    }

    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParseRustProject(String filePath){
        parseProject(filePath, "rust", "/parse/rust_parse");
    }

    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParseErlangProject(String filePath){
        parseProject(filePath, "erlang", "/parse/erlang_parse");
    }

    // 通用解析方法
    private void parseProject(String filePath, String language, String apiPath){
        System.out.println("开始解析" + language + "项目: " + filePath);
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = UriComponentsBuilder.fromHttpUrl(flaskCrawlerUrl + apiPath)
                    .queryParam("project_folder", filePath)
                    .encode()
                    .build()
                    .toUriString();

            System.out.println("调用" + language + "项目解析API: " + url);
            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.trim().isEmpty()) {
                System.err.println(language + "项目解析API返回空响应，项目路径: " + filePath);
                return;
            }

            if (response.contains("<!doctype html>") || response.contains("<html")) {
                System.err.println(language + "项目解析API返回错误页面，项目路径: " + filePath);
                System.err.println("错误详情: " + response.substring(0, Math.min(500, response.length())));
                return;
            }

            System.out.println(language + "项目解析响应长度: " + response.length());
            System.out.println(language + "项目解析响应内容: " + response.substring(0, Math.min(200, response.length())) + "...");

            List<WhiteList> whiteLists = projectUtil.parseJsonData(response);
            System.out.println("解析出依赖库数量: " + whiteLists.size());

            int insertCount = 0;
            for (WhiteList whiteList : whiteLists) {
                whiteList.setFilePath(filePath);
                whiteList.setLanguage(language);
                whiteList.setIsdelete(0);
                int result = whiteListMapper.insert(whiteList);
                if (result > 0) {
                    insertCount++;
                }
            }
            System.out.println("成功插入" + language + "依赖库数量: " + insertCount);
        } catch (Exception e) {
            System.err.println("解析" + language + "项目失败，路径: " + filePath + "，错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        String filePath = projectUtil.unzipAndSaveFile(file);
        System.out.println("文件解压完成，路径: " + filePath);

        // 注：项目语言检测现已由Python脚本自动处理，此处不再需要手动检测
        String projectType = "unknown";

        ProjectService projectService = applicationContext.getBean(ProjectService.class);

        switch(projectType) {
            case "java":
                System.out.println("启动Java项目解析任务");
                projectService.asyncParseJavaProject(filePath);
                break;
            case "c":
                System.out.println("启动C/C++项目解析任务");
                projectService.asyncParseCProject(filePath);
                break;
            case "go":
                System.out.println("启动Go项目解析任务");
                projectService.asyncParseGoProject(filePath);
                break;
            case "javascript":
                System.out.println("启动JavaScript/Node.js项目解析任务");
                projectService.asyncParseJavascriptProject(filePath);
                break;
            case "python":
                System.out.println("启动Python项目解析任务");
                projectService.asyncParsePythonProject(filePath);
                break;
            case "php":
                System.out.println("启动PHP项目解析任务");
                projectService.asyncParsePhpProject(filePath);
                break;
            case "ruby":
                System.out.println("启动Ruby项目解析任务");
                projectService.asyncParseRubyProject(filePath);
                break;
            case "rust":
                System.out.println("启动Rust项目解析任务");
                projectService.asyncParseRustProject(filePath);
                break;
            case "erlang":
                System.out.println("启动Erlang项目解析任务");
                projectService.asyncParseErlangProject(filePath);
                break;
            default:
                System.out.println("未知项目类型，跳过解析: " + projectType);
        }

        return filePath;
    }

    @Override
    public List<VulnerabilityVO> getVulnerabilities(int id) {
        Project project = projectMapper.selectById(id);
        if (project == null || project.getIsDelete() == 1) {
            throw new RuntimeException("Project not found or has been deleted");
        }

        QueryWrapper<ProjectVulnerability> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", id)
                .eq("isDelete", 0);

        List<ProjectVulnerability> relations = projectVulnerabilityMapper.selectList(wrapper);

        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> vulnerabilityIds = relations.stream()
                .map(ProjectVulnerability::getVulnerabilityId)
                .collect(Collectors.toList());

        List<Vulnerability> vulnerabilities = vulnerabilityMapper.selectBatchIds(vulnerabilityIds);

        return vulnerabilities.stream()
                .filter(v -> v.getIsDelete() == 0) // 排除已删除漏洞
                .map(Vulnerability::toVulnerabilityVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> getProjectList(int companyId, int page, int size) throws JsonProcessingException {
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new RuntimeException("Company does not exist.");
        }

        String projectJson = company.getProjectId();

        // Handle null projectJson
        if (projectJson == null || projectJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> projectMap = objectMapper.readValue(projectJson, new TypeReference<Map<String, String>>() {
        });

        if (projectMap == null || projectMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Project> projects = new ArrayList<>();
        for (String projectId : projectMap.keySet()) {
            Project project = projectMapper.selectById(Integer.parseInt(projectId));
            if (project != null&&project.getIsDelete()==0) {
                projects.add(project);
            }
        }

        int start = (page - 1) * size;
        int end = Math.min(start + size, projects.size());
        List<Project> paginatedProjects = projects.subList(start, end);

        return paginatedProjects.stream().map(p -> {
            Map<String, String> map = new HashMap<>();
            map.put("id", String.valueOf(p.getId()));
            map.put("name", p.getName());
            map.put("description", p.getDescription());
            map.put("risk_level", projectUtil.getRiskLevel(p.getId(), p.getRiskThreshold())); // 计算风险级别
            map.put("risk_threshold", String.valueOf(p.getRiskThreshold()));
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Object getProjectStatistics(int companyId) {
        int highRiskCount = 0;
        int lowRiskCount = 0;
        int noRiskCount = 0;
        AtomicInteger lowRiskVulNum = new AtomicInteger();
        AtomicInteger highRiskVulNum = new AtomicInteger();
        AtomicInteger mediumRiskVulNum = new AtomicInteger();
        int projectCount;
        long vulnerabilityCount = 0;
        AtomicInteger cVulnerabilityCount = new AtomicInteger();
        AtomicInteger javaVulnerabilityCount = new AtomicInteger();
        Map<String, Integer> highVulnerabilityNumByDay = projectUtil.initRecentSevenDaysMap();
        Map<String, Integer> midVulnerabilityNumByDay = projectUtil.initRecentSevenDaysMap();
        Map<String, Integer> lowVulnerabilityNumByDay = projectUtil.initRecentSevenDaysMap();
        int thirdLibraryCount = 0;

        // 检查公司是否存在
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new RuntimeException("Company does not exist.");
        }

        // 解析公司项目列表
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> projectMap = null;

        String projectJson = company.getProjectId();
        if (projectJson != null && !projectJson.trim().isEmpty()) {
            try {
                projectMap = objectMapper.readValue(projectJson, new TypeReference<Map<String, String>>() {
                });
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        //projectCount = projectMap != null ? projectMap.size() : 0;
        projectCount =0;
        if (projectMap != null) {
            for (String projectId : projectMap.keySet()) {
                Project project = projectMapper.selectById(Integer.parseInt(projectId));
                if (project == null) {
                    throw new RuntimeException("Project does not exist.");
                }
                if(project.getIsDelete()==1)
                {
                    continue;
                }
                projectCount++;

                String filePath = project.getFile();
                QueryWrapper<WhiteList> whiteListQueryWrapper = new QueryWrapper<>();
                whiteListQueryWrapper.eq("file_path", filePath);
                thirdLibraryCount += whiteListMapper.selectList(whiteListQueryWrapper).size();

                // 统计项目风险等级
                String riskLevel = projectUtil.getRiskLevel(project.getId(), project.getRiskThreshold());
                switch (riskLevel) {
                    case "高风险":
                        highRiskCount++;
                        break;
                    case "低风险":
                        lowRiskCount++;
                        break;
                    case "暂无风险":
                        noRiskCount++;
                        break;
                }

                // 统计漏洞总数
                vulnerabilityCount += projectUtil.getVulnerabilityCount(project.getId());

                // 处理每个漏洞
                QueryWrapper<ProjectVulnerability> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("project_id", project.getId());
                List<ProjectVulnerability> pvList = projectVulnerabilityMapper.selectList(queryWrapper);
                pvList.forEach(pv -> {
                    Vulnerability vulnerability = vulnerabilityMapper.selectById(pv.getVulnerabilityId());
                    if (vulnerability == null) return;

                    // 按语言统计
                    String lang = vulnerability.getLanguage();
                    if ("c".equals(lang) || "c++".equals(lang)) {
                        cVulnerabilityCount.incrementAndGet();
                    } else if ("java".equals(lang)) {
                        javaVulnerabilityCount.incrementAndGet();
                    }

                    // 转换为日期字符串并更新按天统计
                    String vulnDateStr = projectUtil.timeToDayOfWeek(vulnerability.getTime());

                    String VulRiskLevel = vulnerability.getRiskLevel();
                    switch (VulRiskLevel) {
                        case "High":
                            updateVulnerabilityCount(vulnDateStr, highVulnerabilityNumByDay, highRiskVulNum);
                            break;
                        case "Medium":
                            updateVulnerabilityCount(vulnDateStr, midVulnerabilityNumByDay, mediumRiskVulNum);
                            break;
                        case "Low":
                            updateVulnerabilityCount(vulnDateStr, lowVulnerabilityNumByDay, lowRiskVulNum);
                            break;
                    }
                });
            }
        }

        // 序列化按天统计结果
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("highVulnerabilityNumByDay", objectMapper.writeValueAsString(highVulnerabilityNumByDay));
            result.put("midVulnerabilityNumByDay", objectMapper.writeValueAsString(midVulnerabilityNumByDay));
            result.put("lowVulnerabilityNumByDay", objectMapper.writeValueAsString(lowVulnerabilityNumByDay));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing vulnerability data", e);
        }

        // 填充其他统计结果
        result.put("highRiskNum", highRiskCount);
        result.put("lowRiskNum", lowRiskCount);
        result.put("noRiskNum", noRiskCount);
        result.put("projectNum", projectCount);
        result.put("thirdLibraryNum", thirdLibraryCount);
        result.put("vulnerabilityNum", vulnerabilityCount);
        result.put("cVulnerabilityNum", cVulnerabilityCount.get());
        result.put("javaVulnerabilityNum", javaVulnerabilityCount.get());
        result.put("lowRiskVulnerabilityNum", lowRiskVulNum.get());
        result.put("highRiskVulnerabilityNum", highRiskVulNum.get());
        result.put("midRiskVulnerabilityNum", mediumRiskVulNum.get());

        return result;
    }

    // 更新指定日期和风险等级的统计
    private void updateVulnerabilityCount(String dateStr, Map<String, Integer> vulnMap, AtomicInteger counter) {
        if (vulnMap.containsKey(dateStr)) {
            vulnMap.put(dateStr, vulnMap.get(dateStr) + 1);
        }
        counter.incrementAndGet();
    }

    @Override
    public ProjectVO getProjectInfo(int id) {
        Project project = projectMapper.selectById(id);
        ProjectVO projectVO = project.toVO();
        projectVO.setHighRiskNum(projectUtil.getRiskNum(id, "High"));
        projectVO.setMidRiskNum(projectUtil.getRiskNum(id, "Medium"));
        projectVO.setLowRiskNum(projectUtil.getRiskNum(id, "Low"));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todaySixAM = now.toLocalDate().atTime(LocalTime.of(6, 0));
        LocalDateTime lastScanTime = now.isBefore(todaySixAM)
                ? todaySixAM.minusDays(1)
                : todaySixAM;

        projectVO.setLastScanTime(lastScanTime.toString());
        return projectVO;

    }

    @Override
    public void deleteProject(Integer id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new RuntimeException("Project does not exist.");
        }
        project.setIsDelete(1);
        projectMapper.updateById(project);
    }

    @Override
    public void updateProject(Integer id, String name, String description, int risk_threshold, String filePath) {

        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new RuntimeException("Project does not exist.");
        }
        project.setName(name);
        project.setDescription(description);
        project.setRiskThreshold(risk_threshold);
        if (filePath != null) {
            project.setFile(filePath);
        }
        projectMapper.updateById(project);

    }

    @Override
    public File getProjectSBOM(int id, String type, String outFileName) throws IOException, InterruptedException {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new RuntimeException("Project does not exist.");
        }

        String projectDir = project.getFile();
        System.out.println("DEBUG: 项目目录: " + projectDir);
        System.out.println("DEBUG: 输出格式: " + type);
        System.out.println("DEBUG: 输出文件名: " + outFileName);

        // 创建SBOM输出目录
        Path sbomDir = Paths.get(projectDir, "sbom");
        if (!Files.exists(sbomDir)) {
            Files.createDirectories(sbomDir);
            System.out.println("DEBUG: 创建SBOM目录: " + sbomDir);
        }

        String sbomFileName = outFileName + "." + type.toLowerCase();
        Path sbomFilePath = sbomDir.resolve(sbomFileName);
        System.out.println("DEBUG: SBOM文件路径: " + sbomFilePath);

        // 如果文件已存在且不为空，直接返回
        if (Files.exists(sbomFilePath) && Files.isRegularFile(sbomFilePath) && Files.size(sbomFilePath) > 0) {
            System.out.println("DEBUG: SBOM文件已存在，直接返回");
            return sbomFilePath.toFile();
        }

        // 检查OpenSCA工具路径
        System.out.println("DEBUG: 原始getOpenscaToolPath值: " + getOpenscaToolPath);

        String openscaToolPath = getOpenscaToolPath;
        if (!openscaToolPath.endsWith(File.separator)) {
            openscaToolPath += File.separator;
        }
        File openscaToolDir = new File(openscaToolPath);
        System.out.println("DEBUG: 处理后的OpenSCA工具目录: " + openscaToolDir.getAbsolutePath());

        if (!openscaToolDir.exists()) {
            throw new RuntimeException("OpenSCA工具目录不存在: " + openscaToolDir.getAbsolutePath());
        }

        // 输出系统信息用于诊断
        System.out.println("DEBUG: 操作系统: " + System.getProperty("os.name"));
        System.out.println("DEBUG: 操作系统版本: " + System.getProperty("os.version"));
        System.out.println("DEBUG: 系统架构: " + System.getProperty("os.arch"));
        System.out.println("DEBUG: Java架构: " + System.getProperty("sun.arch.data.model") + "位");

        // 检查不同可能的OpenSCA文件名（支持Linux和Windows）
        File openscaExe = null;
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isWindows = osName.contains("win");

        String[] possibleNames;
        if (isWindows) {
            possibleNames = new String[]{
                    "opensca-cli-3.0.8-installer.exe",
                    "opensca-cli.exe",
                    "opensca.exe",
                    "opensca-cli-3.0.8.exe"
            };
        } else {
            // Linux/Unix系统
            possibleNames = new String[]{
                    "opensca/opensca-cli",  // 子目录中的CLI
                    "opensca-cli",          // 当前目录的CLI
                    "opensca",              // 脚本
                    "opensca-cli-linux"     // Linux版本
            };
        }

        for (String name : possibleNames) {
            File candidate = new File(openscaToolDir, name);
            System.out.println("DEBUG: 检查文件: " + candidate.getAbsolutePath() + " - 存在: " + candidate.exists() + " - 可执行: " + candidate.canExecute());
            if (candidate.exists()) {
                openscaExe = candidate;
                System.out.println("DEBUG: 找到OpenSCA可执行文件: " + openscaExe.getName());
                break;
            }
        }

        if (openscaExe == null) {
            // 列出目录中的所有文件
            File[] files = openscaToolDir.listFiles();
            System.out.println("DEBUG: OpenSCA目录中的文件:");
            if (files != null) {
                for (File file : files) {
                    System.out.println("DEBUG: - " + file.getName() + " (大小: " + file.length() + " bytes)");
                }
            }
            throw new RuntimeException("OpenSCA目录中没有找到任何可执行文件: " + openscaToolDir.getAbsolutePath());
        }

        // 构建命令
        String[] command = new String[]{
                openscaExe.getAbsolutePath(), // 使用完整路径
                "-path", projectDir,
                "-out", sbomFilePath.toString()
        };

        // 根据格式添加format参数（如果需要）
        if (type != null && !type.isEmpty()) {
            // 只为特定格式添加format参数
            String format = type.toLowerCase();
            if (format.equals("spdx") || format.equals("json") || format.equals("xml")) {
                String[] newCommand = new String[command.length + 2];
                System.arraycopy(command, 0, newCommand, 0, command.length);
                newCommand[command.length] = "-format";
                newCommand[command.length + 1] = format;
                command = newCommand;
            }
        }

        System.out.println("DEBUG: 执行命令: " + String.join(" ", command));

        // 创建ProcessBuilder并确保不设置工作目录
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        System.out.println("DEBUG: ProcessBuilder工作目录: " + processBuilder.directory());

        // 显式设置工作目录为项目目录
        processBuilder.directory(new File(projectDir));
        System.out.println("DEBUG: 设置ProcessBuilder工作目录为: " + projectDir);

        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            // 读取进程输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("DEBUG: OpenSCA输出: " + line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("DEBUG: OpenSCA退出码: " + exitCode);

            if (exitCode != 0) {
                System.err.println("DEBUG: OpenSCA执行失败，完整输出:\n" + output.toString());
                throw new RuntimeException("OpenSCA 执行失败，退出码: " + exitCode + ", 输出: " + output.toString());
            }

            if (!Files.exists(sbomFilePath) || Files.size(sbomFilePath) == 0) {
                throw new IOException("SBOM 文件生成失败或为空，路径: " + sbomFilePath + ", 输出: " + output.toString());
            }

            System.out.println("DEBUG: SBOM文件生成成功，大小: " + Files.size(sbomFilePath) + " bytes");
            return sbomFilePath.toFile();

        } catch (Exception e) {
            System.err.println("DEBUG: OpenSCA工具执行失败: " + e.getMessage());
            System.err.println("DEBUG: 尝试使用备用SBOM生成方案...");

            // 备用方案：基于数据库中的依赖信息生成简化SBOM
            return generateFallbackSBOM(project, sbomFilePath, type);
        }
    }

    /**
     * 备用SBOM生成方案 - 基于数据库中的依赖信息
     */
    private File generateFallbackSBOM(Project project, Path sbomFilePath, String type) throws IOException {
        System.out.println("DEBUG: 开始生成备用SBOM文件");

        String filePath = project.getFile();

        // 从数据库获取白名单依赖信息
        QueryWrapper<WhiteList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_path", filePath).eq("isdelete", 0);
        List<WhiteList> dependencies = whiteListMapper.selectList(queryWrapper);

        System.out.println("DEBUG: 从数据库获取到 " + dependencies.size() + " 个依赖项");

        // 根据格式生成不同的SBOM文件
        String sbomContent;
        if ("json".equalsIgnoreCase(type)) {
            sbomContent = generateJsonSBOM(project, dependencies);
        } else if ("xml".equalsIgnoreCase(type)) {
            sbomContent = generateXmlSBOM(project, dependencies);
        } else {
            // 默认生成JSON格式
            sbomContent = generateJsonSBOM(project, dependencies);
        }

        // 写入文件
        Files.write(sbomFilePath, sbomContent.getBytes("UTF-8"));

        System.out.println("DEBUG: 备用SBOM文件生成完成，大小: " + Files.size(sbomFilePath) + " bytes");
        return sbomFilePath.toFile();
    }

    /**
     * 生成JSON格式的SBOM
     */
    private String generateJsonSBOM(Project project, List<WhiteList> dependencies) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"bomFormat\": \"CycloneDX\",\n");
        json.append("  \"specVersion\": \"1.4\",\n");
        json.append("  \"serialNumber\": \"urn:uuid:").append(UUID.randomUUID()).append("\",\n");
        json.append("  \"version\": 1,\n");
        json.append("  \"metadata\": {\n");
        json.append("    \"timestamp\": \"").append(Instant.now()).append("\",\n");
        json.append("    \"component\": {\n");
        json.append("      \"type\": \"application\",\n");
        json.append("      \"name\": \"").append(escapeJson(project.getName())).append("\",\n");
        json.append("      \"version\": \"1.0.0\"\n");
        json.append("    }\n");
        json.append("  },\n");
        json.append("  \"components\": [\n");

        for (int i = 0; i < dependencies.size(); i++) {
            WhiteList dep = dependencies.get(i);
            json.append("    {\n");
            json.append("      \"type\": \"library\",\n");
            json.append("      \"name\": \"").append(escapeJson(dep.getName())).append("\",\n");
            json.append("      \"version\": \"unknown\",\n");
            json.append("      \"purl\": \"pkg:").append(escapeJson(dep.getLanguage())).append("/").append(escapeJson(dep.getName()));
            json.append("\"\n");
            json.append("    }");
            if (i < dependencies.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        return json.toString();
    }

    /**
     * 生成XML格式的SBOM
     */
    private String generateXmlSBOM(Project project, List<WhiteList> dependencies) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<bom xmlns=\"http://cyclonedx.org/schema/bom/1.4\" version=\"1\">\n");
        xml.append("  <metadata>\n");
        xml.append("    <timestamp>").append(Instant.now()).append("</timestamp>\n");
        xml.append("    <component type=\"application\">\n");
        xml.append("      <name>").append(escapeXml(project.getName())).append("</name>\n");
        xml.append("      <version>1.0.0</version>\n");
        xml.append("    </component>\n");
        xml.append("  </metadata>\n");
        xml.append("  <components>\n");

        for (WhiteList dep : dependencies) {
            xml.append("    <component type=\"library\">\n");
            xml.append("      <name>").append(escapeXml(dep.getName())).append("</name>\n");
            xml.append("      <version>unknown</version>\n");
            xml.append("      <purl>pkg:").append(escapeXml(dep.getLanguage())).append("/").append(escapeXml(dep.getName()));
            xml.append("</purl>\n");
            xml.append("    </component>\n");
        }

        xml.append("  </components>\n");
        xml.append("</bom>\n");

        return xml.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}