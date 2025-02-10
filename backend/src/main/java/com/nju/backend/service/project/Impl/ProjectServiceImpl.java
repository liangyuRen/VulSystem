package com.nju.backend.service.project.Impl;

import com.nju.backend.repository.mapper.ProjectMapper;
import com.nju.backend.repository.po.Project;
import com.nju.backend.service.project.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public void createProject(Project project, MultipartFile file, String companyName) {

    }

    @Override
    public void deleteProject(String projectName) {

    }

    @Override
    public void updateProject(Project project) {

    }

    @Override
    public Object getProjectList(Integer page, Integer size) {
        return null;
    }

}
