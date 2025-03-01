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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Autowired
    private WhiteListMapper whiteListMapper;

    @Override
    @Transactional
    public void createProject(String name, String description, String language, int risk_threshold, int companyId,String filePath) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        if(projectMapper.selectOne(queryWrapper.eq("name", name)) != null) {
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
        if(company.getProjectId() == null || company.getProjectId().isEmpty()) {
            company.setProjectId("{}");
        }
        String companyProjectId = company.getProjectId();
        companyProjectId = companyProjectId.substring(0, companyProjectId.length() - 1) + ",\"" + project.getId() + "\":\"" + project.getLanguage() + "\"}";
        company.setProjectId(companyProjectId);

        companyMapper.updateById(company);

        if ("java".equals(language)) {
            ((ProjectService) applicationContext.getBean(ProjectService.class)).asyncParseJavaProject(project.getId(), filePath, companyId);        }
    }

    // 在创建方法中仅触发异步解析
    @Async("projectAnalysisExecutor")
    @Override
    public void asyncParseJavaProject(int projectId, String filePath, int companyId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = UriComponentsBuilder.fromHttpUrl("http://localhost:5000/parse/pom_parse")
                    .queryParam("project_folder", filePath)
                    .encode() // 自动处理 URL 编码
                    .build()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            List<WhiteList>  whiteLists = projectUtil.parseJsonData(response);
            for(WhiteList whiteList:whiteLists){
                whiteList.setCompany_id(companyId);
                whiteList.setProject_id(projectId);
                whiteList.setLanguage("java");
                whiteList.setIsdelete(0);
                whiteListMapper.insert(whiteList);
            }
        } catch (Exception e) {
            log.println("Error parsing project " + projectId + ": " + e.getMessage());
        }
    }

    @Override
    public String uploadFile(MultipartFile file) {
        return projectUtil.saveFile(file);
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
    public List<Map<String,String>> getProjectList(int companyId, int page, int size) throws JsonProcessingException {
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new RuntimeException("Company does not exist.");
        }

        String projectJson = company.getProjectId();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> projectMap = objectMapper.readValue(projectJson, new TypeReference<Map<String, String>>() {});

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
            map.put("risk_level", projectUtil.getRiskLevel(p.getId(),p.getRiskThreshold())); // 计算风险级别
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
        Map<String,Integer> highVulnerabilityNumByDay = new HashMap<>();
        Map<String,Integer> midVulnerabilityNumByDay = new HashMap<>();
        Map<String,Integer> lowVulnerabilityNumByDay = new HashMap<>();
        int thirdLibraryCount;

        QueryWrapper<WhiteList> whiteListQueryWrapper = new QueryWrapper<>();
        whiteListQueryWrapper.eq("company_id", companyId);
        thirdLibraryCount = whiteListMapper.selectList(whiteListQueryWrapper).size();

        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new RuntimeException("Company does not exist.");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> projectMap = null;
        try {
            projectMap = objectMapper.readValue(company.getProjectId(), new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        if(projectMap == null) {
            projectCount = 0;
        }else{
            projectCount = projectMap.size();
        }


        if(projectMap!=null){
            for (String projectId : projectMap.keySet()) {
                Project project = projectMapper.selectById(Integer.parseInt(projectId));
                if (project == null) {
                    throw new RuntimeException("Project does not exist.");
                }

                switch (projectUtil.getRiskLevel(project.getId(), project.getRiskThreshold())) {
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

                vulnerabilityCount += projectUtil.getVulnerabilityCount(project.getId());

                projectVulnerabilityMapper.selectList(new QueryWrapper<ProjectVulnerability>().eq("project_id", project.getId()))
                        .forEach(pv -> {
                            Vulnerability vulnerability = vulnerabilityMapper.selectById(pv.getVulnerabilityId());
                            if (vulnerability == null) {
                                return;
                            }
                            if (vulnerability.getLanguage().equals("c")||vulnerability.getLanguage().equals("c++")) {
                                cVulnerabilityCount.getAndIncrement();
                            } else if (vulnerability.getLanguage().equals("java")) {
                                javaVulnerabilityCount.getAndIncrement();
                            }

                            String dayOfWeek = projectUtil.timeToDayOfWeek(vulnerability.getTime());
                            switch (vulnerability.getRiskLevel()) {
                                case "High":
                                    highVulnerabilityNumByDay.put(dayOfWeek, highVulnerabilityNumByDay.getOrDefault(dayOfWeek, 0) + 1);
                                    highRiskVulNum.getAndIncrement();
                                    break;
                                case "Medium":
                                    midVulnerabilityNumByDay.put(dayOfWeek, midVulnerabilityNumByDay.getOrDefault(dayOfWeek, 0) + 1);
                                    mediumRiskVulNum.getAndIncrement();
                                    break;
                                case "Low":
                                    lowVulnerabilityNumByDay.put(dayOfWeek, lowVulnerabilityNumByDay.getOrDefault(dayOfWeek, 0) + 1);
                                    lowRiskVulNum.getAndIncrement();
                                    break;
                            }
                        });
            }
        }

        String highVulByDay;
        String midVulByDay;
        String lowVulByDay;
        try {
            highVulByDay = objectMapper.writeValueAsString(highVulnerabilityNumByDay);
            midVulByDay = objectMapper.writeValueAsString(midVulnerabilityNumByDay);
            lowVulByDay = objectMapper.writeValueAsString(lowVulnerabilityNumByDay);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing vulnerability count", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("highRiskNum", highRiskCount);
        result.put("lowRiskNum", lowRiskCount);
        result.put("noRiskNum", noRiskCount);
        result.put("projectNum", projectCount);
        result.put("thirdLibraryNum", thirdLibraryCount);
        result.put("vulnerabilityNum", vulnerabilityCount);
        result.put("highVulnerabilityNumByDay", highVulByDay);
        result.put("midVulnerabilityNumByDay", midVulByDay);
        result.put("lowVulnerabilityNumByDay", lowVulByDay);
        result.put("cVulnerabilityNum", cVulnerabilityCount);
        result.put("javaVulnerabilityNum", javaVulnerabilityCount);
        result.put("lowRiskVulnerabilityNum", lowRiskVulNum);
        result.put("highRiskVulnerabilityNum", highRiskVulNum);
        result.put("midRiskVulnerabilityNum", mediumRiskVulNum);

        return result;
    }

    @Override
    public ProjectVO getProjectInfo(int id) {
        Project project = projectMapper.selectById(id);
        ProjectVO projectVO = project.toVO();
        projectVO.setHighRiskNum(projectUtil.getRiskNum(id,"High"));
        projectVO.setMidRiskNum(projectUtil.getRiskNum(id,"Medium"));
        projectVO.setLowRiskNum(projectUtil.getRiskNum(id,"Low"));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todaySixAM = now.toLocalDate().atTime(LocalTime.of(6, 0));
        LocalDateTime lastScanTime= now.isBefore(todaySixAM)
                ? todaySixAM.minusDays(1)
                : todaySixAM;

        projectVO.setLastScanTime(lastScanTime.toString());
        return projectVO;

    }

    @Override
    public void deleteProject(Integer id) {
        Project project = projectMapper.selectById(id);
        if(project == null) {
            throw new RuntimeException("Project does not exist.");
        }
        project.setIsDelete(1);
        projectMapper.updateById(project);
        whiteListMapper.delete(new QueryWrapper<WhiteList>().eq("project_id", id));
    }

    @Override
    public void updateProject(Integer id, String name, String description, int risk_threshold) {
        Project project = projectMapper.selectById(id);
        if(project == null) {
            throw new RuntimeException("Project does not exist.");
        }
        project.setName(name);
        project.setDescription(description);
        project.setRiskThreshold(risk_threshold);
        projectMapper.updateById(project);
    }

}
