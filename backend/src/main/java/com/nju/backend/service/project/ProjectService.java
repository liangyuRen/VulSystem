package com.nju.backend.service.project;

import org.springframework.web.multipart.MultipartFile;

public interface ProjectService {

    void createProject(String name, String description, String language, int risk_threshold, MultipartFile file, String companyName);

    void deleteProject(Integer id);

    void updateProject(Integer id, String name, String description, int risk_threshold);

    void uploadFile(Integer id,MultipartFile file);
}
