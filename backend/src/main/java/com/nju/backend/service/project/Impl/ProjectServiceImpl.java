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

        //TODO 计算风险级别

        return paginatedProjects.stream().map(p -> {
            Map<String, String> map = new HashMap<>();
            map.put("name", p.getProjectName());
            map.put("description", p.getProjectDescription());
            map.put("risk_level", getRiskLevel(p.getRiskThreshold())); // 计算风险级别
            return map;
        }).collect(Collectors.toList());
    }

    private String getRiskLevel(int riskThreshold) {
        //TODO
        return "low";
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
