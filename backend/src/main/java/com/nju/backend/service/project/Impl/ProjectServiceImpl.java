package com.nju.backend.service.project.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Autowired
    private WhiteListMapper whiteListMapper;

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
        String companyProjectId = company.getProjectId();
        companyProjectId = companyProjectId.substring(0, companyProjectId.length() - 1) + ",\"" + project.getId() + "\":\"" + project.getLanguage() + "\"}";
        company.setProjectId(companyProjectId);

        companyMapper.updateById(company);
    }

    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParseJavaProject(String filePath) {
        System.out.println("开始解析Java项目: " + filePath);
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = UriComponentsBuilder.fromHttpUrl("http://localhost:5000/parse/pom_parse")
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
            String url = UriComponentsBuilder.fromHttpUrl("http://localhost:5000/parse/c_parse")
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

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        String filePath = projectUtil.unzipAndSaveFile(file);
        System.out.println("文件解压完成，路径: " + filePath);
        String projectType = "";
        
        Map<String, Double> languagePercent = ProjectUtil.calcLanguagePercentByFileSize(filePath);
        if (languagePercent.size() == 2) {
            for (Map.Entry<String, Double> entry : languagePercent.entrySet()) {
                if (!entry.getKey().equals("Other")) {
                    projectType = entry.getKey();
                    break;
                }
            }
        }else {
            projectType = ProjectUtil.mapToJson(languagePercent);
        }

        if(projectType.equals("java")) {
            System.out.println("启动Java项目解析任务");
            applicationContext.getBean(ProjectService.class).asyncParseJavaProject(filePath);
        } else if(projectType.equals("c")) {
            System.out.println("启动C/C++项目解析任务");
            applicationContext.getBean(ProjectService.class).asyncParseCProject(filePath);
        } else {
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
        try {
            projectMap = objectMapper.readValue(company.getProjectId(), new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
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

        // 检查不同可能的OpenSCA文件名
        File openscaExe = null;
        String[] possibleNames = {
                "opensca-cli-3.0.8-installer.exe",
                "opensca-cli.exe",
                "opensca.exe",
                "opensca-cli-3.0.8.exe"
        };

        for (String name : possibleNames) {
            File candidate = new File(openscaToolDir, name);
            System.out.println("DEBUG: 检查文件: " + candidate.getAbsolutePath() + " - 存在: " + candidate.exists());
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