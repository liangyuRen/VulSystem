package com.nju.backend.service.project.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nju.backend.repository.mapper.ProjectMapper;
import com.nju.backend.repository.po.Project;
import com.nju.backend.service.project.ProjectService;
import com.nju.backend.service.project.util.ProjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectUtil projectUtil;

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
        project.setVulnerability("{}");

        projectMapper.insert(project);

        //TODO: save data to company
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
