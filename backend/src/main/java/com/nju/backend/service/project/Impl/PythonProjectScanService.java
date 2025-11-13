package com.nju.backend.service.project.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nju.backend.repository.po.Vulnerability;
import com.nju.backend.repository.po.WhiteList;
import com.nju.backend.repository.mapper.WhiteListMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Python项目扫描和White-list管理服务
 *
 * 功能流程:
 * 1. 接收Python项目路径
 * 2. 调用Flask语言检测API确认为Python项目
 * 3. 调用Flask Python解析器获取依赖列表
 * 4. 将依赖信息存储到white-list表
 */
@Service
public class PythonProjectScanService {

    @Autowired
    private WhiteListMapper whiteListMapper;

    @Autowired
    private RestTemplate restTemplate;

    private static final String FLASK_BASE_URL = "http://127.0.0.1:5000";
    private static final String LANGUAGE_DETECT_URL = FLASK_BASE_URL + "/parse/get_primary_language";
    private static final String PYTHON_PARSE_URL = FLASK_BASE_URL + "/parse/python_parse";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 扫描Python项目并存储依赖到white-list
     *
     * @param projectPath Python项目路径
     * @param projectId 项目ID
     * @return 扫描结果
     */
    public PythonScanResult scanPythonProject(String projectPath, Long projectId) {
        PythonScanResult result = new PythonScanResult();
        result.setProjectPath(projectPath);
        result.setProjectId(projectId);

        try {
            // Step 1: 检测项目语言
            String detectedLanguage = detectProjectLanguage(projectPath);
            result.setDetectedLanguage(detectedLanguage);

            if (!"python".equalsIgnoreCase(detectedLanguage)) {
                result.setSuccess(false);
                result.setMessage("Project is not a Python project. Detected: " + detectedLanguage);
                return result;
            }

            // Step 2: 解析Python依赖
            List<PythonDependency> dependencies = parsePythonDependencies(projectPath);
            result.setDependencies(dependencies);
            result.setDependencyCount(dependencies.size());

            // Step 3: 保存到white-list表
            int savedCount = saveToWhiteList(dependencies, projectId);
            result.setSavedCount(savedCount);
            result.setSuccess(true);
            result.setMessage("Successfully scanned and saved " + savedCount + " dependencies to white-list");

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error during scanning: " + e.getMessage());
            result.setError(e.toString());
        }

        return result;
    }

    /**
     * Step 1: 检测项目语言 (调用Flask API)
     */
    private String detectProjectLanguage(String projectPath) throws Exception {
        String encodedPath = URLEncoder.encode(projectPath, StandardCharsets.UTF_8.toString());
        String url = LANGUAGE_DETECT_URL + "?project_folder=" + encodedPath + "&use_optimized=true";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
            String language = (String) responseBody.get("language");
            System.out.println("Detected language: " + language);
            return language;
        }

        throw new Exception("Failed to detect language. HTTP " + response.getStatusCode());
    }

    /**
     * Step 2: 解析Python依赖 (调用Flask API)
     */
    private List<PythonDependency> parsePythonDependencies(String projectPath) throws Exception {
        String encodedPath = URLEncoder.encode(projectPath, StandardCharsets.UTF_8.toString());
        String url = PYTHON_PARSE_URL + "?project_folder=" + encodedPath;

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        List<PythonDependency> dependencies = new ArrayList<>();

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);

            // Flask返回格式: { "obj": [...dependencies...] }
            Object objData = responseBody.get("obj");

            if (objData instanceof List) {
                List<Map<String, Object>> depList = (List<Map<String, Object>>) objData;

                for (Map<String, Object> dep : depList) {
                    PythonDependency pythonDep = new PythonDependency();
                    pythonDep.setName((String) dep.get("name"));
                    pythonDep.setVersion((String) dep.get("version"));
                    pythonDep.setPackageManager((String) dep.getOrDefault("package_manager", "pip"));
                    pythonDep.setLanguage("python");

                    dependencies.add(pythonDep);
                }
            }

            System.out.println("Parsed " + dependencies.size() + " Python dependencies");
            return dependencies;
        }

        throw new Exception("Failed to parse dependencies. HTTP " + response.getStatusCode());
    }

    /**
     * Step 3: 保存依赖到white-list表
     */
    private int saveToWhiteList(List<PythonDependency> dependencies, Long projectId) {
        int savedCount = 0;

        for (PythonDependency dep : dependencies) {
            try {
                WhiteList whiteListEntry = new WhiteList();
                whiteListEntry.setProjectId(projectId);
                whiteListEntry.setComponentName(dep.getName());
                whiteListEntry.setComponentVersion(dep.getVersion());
                whiteListEntry.setLanguage(dep.getLanguage());
                whiteListEntry.setPackageManager(dep.getPackageManager());
                whiteListEntry.setStatus("APPROVED");
                whiteListEntry.setCreatedTime(new Date());
                whiteListEntry.setRemark("Auto-detected from Python project scan");

                // 检查是否已存在
                if (!existsInWhiteList(whiteListEntry)) {
                    whiteListMapper.insert(whiteListEntry);
                    savedCount++;
                }

            } catch (Exception e) {
                System.err.println("Failed to save dependency: " + dep.getName() + " - " + e.getMessage());
            }
        }

        return savedCount;
    }

    /**
     * 检查white-list中是否已存在该条目
     */
    private boolean existsInWhiteList(WhiteList entry) {
        // 查询条件: projectId + componentName + componentVersion
        WhiteList existing = whiteListMapper.selectOne(
            new QueryWrapper<WhiteList>()
                .eq("project_id", entry.getProjectId())
                .eq("component_name", entry.getComponentName())
                .eq("component_version", entry.getComponentVersion())
        );
        return existing != null;
    }

    /**
     * 获取项目的white-list记录
     */
    public List<WhiteList> getProjectWhiteList(Long projectId) {
        return whiteListMapper.selectList(
            new QueryWrapper<WhiteList>()
                .eq("project_id", projectId)
                .eq("language", "python")
                .orderByDesc("created_time")
        );
    }

    // =============== 内部类 ===============

    /**
     * Python扫描结果
     */
    public static class PythonScanResult {
        private boolean success;
        private String message;
        private String error;
        private String projectPath;
        private Long projectId;
        private String detectedLanguage;
        private List<PythonDependency> dependencies;
        private int dependencyCount;
        private int savedCount;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getProjectPath() { return projectPath; }
        public void setProjectPath(String projectPath) { this.projectPath = projectPath; }

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }

        public String getDetectedLanguage() { return detectedLanguage; }
        public void setDetectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; }

        public List<PythonDependency> getDependencies() { return dependencies; }
        public void setDependencies(List<PythonDependency> dependencies) { this.dependencies = dependencies; }

        public int getDependencyCount() { return dependencyCount; }
        public void setDependencyCount(int dependencyCount) { this.dependencyCount = dependencyCount; }

        public int getSavedCount() { return savedCount; }
        public void setSavedCount(int savedCount) { this.savedCount = savedCount; }
    }

    /**
     * Python依赖信息
     */
    public static class PythonDependency {
        private String name;
        private String version;
        private String packageManager;
        private String language;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getPackageManager() { return packageManager; }
        public void setPackageManager(String packageManager) { this.packageManager = packageManager; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        @Override
        public String toString() {
            return "PythonDependency{" +
                    "name='" + name + '\'' +
                    ", version='" + version + '\'' +
                    ", packageManager='" + packageManager + '\'' +
                    ", language='" + language + '\'' +
                    '}';
        }
    }
}
