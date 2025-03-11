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
import com.sun.xml.internal.bind.v2.TODO;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    // 在创建方法中仅触发异步解析
    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParseJavaProject(int companyId, String filePath) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = UriComponentsBuilder.fromHttpUrl("http://localhost:5000/parse/pom_parse")
                    .queryParam("project_folder", filePath)
                    .encode() // 自动处理 URL 编码
                    .build()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            List<WhiteList> whiteLists = projectUtil.parseJsonData(response);
            for (WhiteList whiteList : whiteLists) {
                whiteList.setCompany_id(companyId);
                whiteList.setFilePath(filePath);
                whiteList.setLanguage("java");
                whiteList.setIsdelete(0);
                whiteListMapper.insert(whiteList);
            }
        } catch (Exception e) {
            log.println("Error parsing project " + filePath + ": " + e.getMessage());
        }
    }

    @Override
    public String uploadFile(MultipartFile file, Integer companyId) throws IOException {
        String filePath = projectUtil.unzipAndSaveFile(file);
        applicationContext.getBean(ProjectService.class).asyncParseJavaProject(companyId, filePath);
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
            if (project != null) {
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
        int thirdLibraryCount;

        // 查询第三方库数量
        QueryWrapper<WhiteList> whiteListQueryWrapper = new QueryWrapper<>();
        whiteListQueryWrapper.eq("company_id", companyId);
        thirdLibraryCount = whiteListMapper.selectList(whiteListQueryWrapper).size();

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

        projectCount = projectMap != null ? projectMap.size() : 0;

        if (projectMap != null) {
            for (String projectId : projectMap.keySet()) {
                Project project = projectMapper.selectById(Integer.parseInt(projectId));
                if (project == null) {
                    throw new RuntimeException("Project does not exist.");
                }

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
        String sbomFileName = outFileName + type.toLowerCase();
        Path sbomFilePath = Paths.get(projectDir).resolve(sbomFileName);

        if (Files.exists(sbomFilePath) && Files.isRegularFile(sbomFilePath)) {
            return sbomFilePath.toFile();
        }

        String[] command = {
                "./opensca-cli",
                "-path", projectDir,
                "-out", sbomFilePath.toString(),
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(getOpenscaToolPath));
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("OpenSCA 执行失败，退出码: " + exitCode);
        } else if (!Files.exists(sbomFilePath)) {
            throw new IOException("SBOM 文件生成失败，路径: " + sbomFilePath);
        }

        return sbomFilePath.toFile();
    }
}
