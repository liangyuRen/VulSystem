package com.nju.backend.service.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nju.backend.config.vo.ProjectVO;
import com.nju.backend.config.vo.VulnerabilityVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ProjectService {

    void createProject(String name, String description, String language, int risk_threshold, int companyId, String filePath);

    void deleteProject(Integer id);

    void updateProject(Integer id, String name, String description, int risk_threshold,String filePath);

    // 在创建方法中仅触发异步解析
    @Async("projectAnalysisExecutor")
    void asyncParseJavaProject(int companyId,String filePath);

    String uploadFile(MultipartFile file,Integer companyId) throws IOException;

    List<VulnerabilityVO> getVulnerabilities(int id);

    List<Map<String,String>> getProjectList(int companyId, int page, int size) throws JsonProcessingException;

    Object getProjectStatistics(int companyId);

    ProjectVO getProjectInfo(int id);

    File getProjectSBOM(int id,String type,String outFileName) throws IOException, InterruptedException;
}
