package com.nju.backend.service.project.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nju.backend.config.vo.VulnerabilityVO;
import com.nju.backend.repository.mapper.CompanyMapper;
import com.nju.backend.repository.mapper.ProjectMapper;
import com.nju.backend.repository.mapper.ProjectVulnerabilityMapper;
import com.nju.backend.repository.mapper.VulnerabilityMapper;
import com.nju.backend.repository.po.*;
import com.nju.backend.service.project.ProjectService;
import com.nju.backend.service.project.util.ProjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class ProjectServiceImpl implements ProjectService {

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
    public void createProject(String name, String description, String language, int risk_threshold, MultipartFile file, String companyName) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        if(projectMapper.selectOne(queryWrapper.eq("name", name)) != null) {
            throw new RuntimeException("Project already exists.");
        }
        Project project = new Project();
        project.setProjectName(name);
        project.setProjectDescription(description);
        project.setLanguage(language);
        project.setRiskThreshold(risk_threshold);
        if(!file.isEmpty()){
            project.setFile(projectUtil.saveFile(file));
        }
        project.setIsDelete(0);
        project.setRoadmapFile("");

        projectMapper.insert(project);

        Company company = companyMapper.selectOne(new QueryWrapper<Company>().eq("name", companyName));
        if (company == null) {
            throw new RuntimeException("Company does not exist.");
        }
        String projectInfo = project.getId() + ":" + project.getLanguage();
        company.setProjectId(projectInfo);
        companyMapper.updateById(company);
    }

    @Override
    public void uploadFile(Integer id, MultipartFile file) {
        Project project = projectMapper.selectById(id);
        if(project == null) {
            throw new RuntimeException("Project does not exist.");
        }
        if(!file.isEmpty()){
            project.setFile(projectUtil.saveFile(file));
        }
        projectMapper.updateById(project);
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
            map.put("name", p.getProjectName());
            map.put("description", p.getProjectDescription());
            map.put("risk_level", projectUtil.getRiskLevel(p.getId(),p.getRiskThreshold())); // 计算风险级别
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Object getProjectStatistics(int companyId) {
        int highRiskCount = 0;
        int lowRiskCount = 0;
        int noRiskCount = 0;
        int projectCount;
        long vulnerabilityCount = 0;
        AtomicInteger cVulnerabilityCount = new AtomicInteger();
        AtomicInteger javaVulnerabilityCount = new AtomicInteger();
        Map<String,Integer> highVulnerabilityNumByDay = new HashMap<>();
        Map<String,Integer> midVulnerabilityNumByDay = new HashMap<>();
        Map<String,Integer> lowVulnerabilityNumByDay = new HashMap<>();
        int thirdLibraryCount;

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
        Map<String,String> thirdLibraryMap = null;
        try {
            thirdLibraryMap = objectMapper.readValue(company.getWhiteList(), new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        if(projectMap == null) {
            projectCount = 0;
        }else{
            projectCount = projectMap.size();
        }

        if(thirdLibraryMap == null) {
            thirdLibraryCount = 0;
        }else {
            thirdLibraryCount = thirdLibraryMap.size();
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
                                case "high":
                                    highVulnerabilityNumByDay.put(dayOfWeek, highVulnerabilityNumByDay.getOrDefault(dayOfWeek, 0) + 1);
                                    break;
                                case "mid":
                                    midVulnerabilityNumByDay.put(dayOfWeek, midVulnerabilityNumByDay.getOrDefault(dayOfWeek, 0) + 1);
                                    break;
                                case "low":
                                    lowVulnerabilityNumByDay.put(dayOfWeek, lowVulnerabilityNumByDay.getOrDefault(dayOfWeek, 0) + 1);
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

        return result;
    }

    @Override
    public void deleteProject(Integer id) {
        Project project = projectMapper.selectById(id);
        if(project == null) {
            throw new RuntimeException("Project does not exist.");
        }
        project.setIsDelete(1);
        projectMapper.updateById(project);
    }

    @Override
    public void updateProject(Integer id, String name, String description, int risk_threshold) {
        Project project = projectMapper.selectById(id);
        if(project == null) {
            throw new RuntimeException("Project does not exist.");
        }
        project.setProjectName(name);
        project.setProjectDescription(description);
        project.setRiskThreshold(risk_threshold);
        projectMapper.updateById(project);
    }

}
