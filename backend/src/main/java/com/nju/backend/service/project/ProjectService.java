package com.nju.backend.service.project;

import com.nju.backend.repository.po.Project;
import org.springframework.web.multipart.MultipartFile;

public interface ProjectService {

    void createProject(Project project, MultipartFile file, String companyName);

    void deleteProject(String projectName);

    void updateProject(Project project);

    Object getProjectList(Integer page, Integer size);
}
