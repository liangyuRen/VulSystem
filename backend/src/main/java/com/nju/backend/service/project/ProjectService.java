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

    @Async("projectAnalysisExecutor")
    void asyncParseJavaProject(String filePath);

    @Async("projectAnalysisExecutor")
    void asyncParseCProject(String filePath);

    @Async("projectAnalysisExecutor")
    void asyncParsePythonProject(String filePath);

    @Async("projectAnalysisExecutor")
    void asyncParseRustProject(String filePath);

    @Async("projectAnalysisExecutor")
    void asyncParseGoProject(String filePath);

    @Async("projectAnalysisExecutor")
    void asyncParseJavaScriptProject(String filePath);

    @Async("projectAnalysisExecutor")
    void asyncParsePhpProject(String filePath);

    @Async("projectAnalysisExecutor")
    void asyncParseRubyProject(String filePath);

    @Async("projectAnalysisExecutor")
    void asyncParseErlangProject(String filePath);

    String uploadFile(MultipartFile file) throws IOException;

    /**
     * 上传文件并自动检测项目语言
     * @param file 上传的项目文件
     * @return 包含 filePath 和 language 的 Map
     * @throws IOException 文件处理异常
     */
    Map<String, Object> uploadFileWithLanguageDetection(MultipartFile file) throws IOException;

    List<VulnerabilityVO> getVulnerabilities(int id);

    List<Map<String,String>> getProjectList(int companyId, int page, int size) throws JsonProcessingException;

    Object getProjectStatistics(int companyId);

    ProjectVO getProjectInfo(int id);

    File getProjectSBOM(int id,String type,String outFileName) throws IOException, InterruptedException;
}
