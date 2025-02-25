package com.nju.backend.service.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nju.backend.config.vo.ProjectVO;
import com.nju.backend.config.vo.VulnerabilityVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ProjectService {

    void createProject(String name, String description, String language, int risk_threshold, String companyName, String filePath);

    void deleteProject(Integer id);

    void updateProject(Integer id, String name, String description, int risk_threshold);

    String uploadFile(MultipartFile file);

    List<VulnerabilityVO> getVulnerabilities(int id);

    List<Map<String,String>> getProjectList(int companyId, int page, int size) throws JsonProcessingException;

    Object getProjectStatistics(int companyId);

    ProjectVO getProjectInfo(int id);
}
