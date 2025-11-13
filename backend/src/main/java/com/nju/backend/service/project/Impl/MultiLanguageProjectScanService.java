package com.nju.backend.service.project.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nju.backend.repository.po.WhiteList;
import com.nju.backend.repository.po.Project;
import com.nju.backend.repository.mapper.WhiteListMapper;
import com.nju.backend.repository.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 多语言项目扫描服务
 * 支持: Python, PHP, Rust, JavaScript, Java, Go等
 *
 * 功能:
 * 1. 自动检测项目语言
 * 2. 根据语言调用对应的解析器
 * 3. 解析依赖并存储到white-list
 */
@Service
public class MultiLanguageProjectScanService {

    @Autowired
    private WhiteListMapper whiteListMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private RestTemplate restTemplate;

    private static final String FLASK_BASE_URL = "http://127.0.0.1:5000";
    private static final String LANGUAGE_DETECT_URL = FLASK_BASE_URL + "/parse/get_primary_language";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 语言和对应的解析端点
    private static final Map<String, String> LANGUAGE_PARSERS = new HashMap<>();
    static {
        LANGUAGE_PARSERS.put("python", "/parse/python_parse");
        LANGUAGE_PARSERS.put("php", "/parse/php_parse");
        LANGUAGE_PARSERS.put("javascript", "/parse/javascript_parse");
        LANGUAGE_PARSERS.put("go", "/parse/go_parse");
        LANGUAGE_PARSERS.put("rust", "/parse/rust_parse");
        LANGUAGE_PARSERS.put("java", "/parse/pom_parse");
        LANGUAGE_PARSERS.put("ruby", "/parse/ruby_parse");
        LANGUAGE_PARSERS.put("erlang", "/parse/erlang_parse");
    }

    // 包管理器映射
    private static final Map<String, String> PACKAGE_MANAGERS = new HashMap<>();
    static {
        PACKAGE_MANAGERS.put("python", "pip");
        PACKAGE_MANAGERS.put("php", "composer");
        PACKAGE_MANAGERS.put("javascript", "npm");
        PACKAGE_MANAGERS.put("go", "go mod");
        PACKAGE_MANAGERS.put("rust", "cargo");
        PACKAGE_MANAGERS.put("java", "maven");
        PACKAGE_MANAGERS.put("ruby", "gems");
        PACKAGE_MANAGERS.put("erlang", "rebar");
    }

    /**
     * 扫描多语言项目并存储依赖到white-list
     */
    public MultiLangScanResult scanProject(String projectPath, Long projectId) {
        MultiLangScanResult result = new MultiLangScanResult();
        result.setProjectPath(projectPath);
        result.setProjectId(projectId);

        try {
            // Step 1: 检测项目语言
            String detectedLanguage = detectProjectLanguage(projectPath);
            result.setDetectedLanguage(detectedLanguage);

            // Step 2: 更新project表的language字段
            updateProjectLanguage(projectId, detectedLanguage);

            // Step 3: 获取对应的解析器端点
            String parserEndpoint = LANGUAGE_PARSERS.get(detectedLanguage.toLowerCase());
            if (parserEndpoint == null) {
                result.setSuccess(false);
                result.setMessage("Unsupported language: " + detectedLanguage);
                return result;
            }

            // Step 4: 解析依赖
            List<ProjectDependency> dependencies = parseDependencies(projectPath, parserEndpoint);
            result.setDependencies(dependencies);
            result.setDependencyCount(dependencies.size());

            // Step 5: 保存到white-list
            int savedCount = saveToWhiteList(dependencies, projectId, detectedLanguage);
            result.setSavedCount(savedCount);
            result.setSuccess(true);
            result.setMessage("Successfully scanned and saved " + savedCount + " dependencies");

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error during scanning: " + e.getMessage());
            result.setError(e.toString());
        }

        return result;
    }

    /**
     * 检测项目主要语言
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
     * 更新项目的language字段
     */
    private void updateProjectLanguage(Long projectId, String language) {
        try {
            Project project = projectMapper.selectById(projectId);
            if (project != null) {
                project.setLanguage(language);
                projectMapper.updateById(project);
                System.out.println("Updated project " + projectId + " language to: " + language);
            }
        } catch (Exception e) {
            System.err.println("Failed to update project language: " + e.getMessage());
        }
    }

    /**
     * 解析依赖
     */
    private List<ProjectDependency> parseDependencies(String projectPath, String parserEndpoint)
            throws Exception {
        String encodedPath = URLEncoder.encode(projectPath, StandardCharsets.UTF_8.toString());
        String url = FLASK_BASE_URL + parserEndpoint + "?project_folder=" + encodedPath;

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        List<ProjectDependency> dependencies = new ArrayList<>();

        if (response.getStatusCode().is2xxSuccessful()) {
            String responseBody = response.getBody();

            // 尝试解析为数组或对象
            Object parsedResponse = objectMapper.readValue(responseBody, Object.class);
            List<Map<String, Object>> depList = null;

            if (parsedResponse instanceof List) {
                // 直接是数组格式
                depList = (List<Map<String, Object>>) parsedResponse;
            } else if (parsedResponse instanceof Map) {
                // 是对象格式，获取"obj"字段
                Map<String, Object> responseMap = (Map<String, Object>) parsedResponse;
                Object objData = responseMap.get("obj");
                if (objData instanceof List) {
                    depList = (List<Map<String, Object>>) objData;
                }
            }

            // 解析依赖列表
            if (depList != null) {
                for (Map<String, Object> dep : depList) {
                    ProjectDependency dependency = new ProjectDependency();
                    String name = (String) dep.get("name");

                    // 处理版本号：可能在name中，也可能在version字段
                    if (name != null && name.contains(" ")) {
                        String[] parts = name.split(" ", 2);
                        dependency.setName(parts[0]);
                        dependency.setVersion(parts.length > 1 ? parts[1] : "unknown");
                    } else {
                        dependency.setName(name);
                        dependency.setVersion((String) dep.getOrDefault("version", "unknown"));
                    }

                    dependencies.add(dependency);
                }
            }

            System.out.println("Parsed " + dependencies.size() + " dependencies");
            return dependencies;
        }

        throw new Exception("Failed to parse dependencies. HTTP " + response.getStatusCode());
    }

    /**
     * 保存依赖到white-list表
     */
    private int saveToWhiteList(List<ProjectDependency> dependencies, Long projectId, String language) {
        int savedCount = 0;
        String packageManager = PACKAGE_MANAGERS.getOrDefault(language.toLowerCase(), "unknown");

        // 获取项目的file_path
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            System.err.println("项目不存在，ID: " + projectId);
            return 0;
        }
        String filePath = project.getFile();

        for (ProjectDependency dep : dependencies) {
            try {
                WhiteList entry = new WhiteList();
                entry.setFilePath(filePath);
                entry.setComponentName(dep.getName());  // 同时会设置name字段
                entry.setDescription(dep.getName() + " " + dep.getVersion());
                entry.setLanguage(language);
                entry.setIsdelete(0);

                if (!existsInWhiteList(filePath, dep.getName(), dep.getVersion())) {
                    whiteListMapper.insert(entry);
                    savedCount++;
                    System.out.println("保存依赖: " + dep.getName() + " " + dep.getVersion());
                }
            } catch (Exception e) {
                System.err.println("Failed to save: " + dep.getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        return savedCount;
    }

    /**
     * 检查是否已存在（使用file_path）
     */
    private boolean existsInWhiteList(String filePath, String componentName, String version) {
        WhiteList existing = whiteListMapper.selectOne(
            new QueryWrapper<WhiteList>()
                .eq("file_path", filePath)
                .eq("name", componentName)
                .eq("isdelete", 0)
        );
        return existing != null;
    }

    /**
     * 获取项目的white-list记录
     */
    public List<WhiteList> getProjectWhiteList(Long projectId) {
        // 获取项目的file_path
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return new ArrayList<>();
        }
        String filePath = project.getFile();

        return whiteListMapper.selectList(
            new QueryWrapper<WhiteList>()
                .eq("file_path", filePath)
                .eq("isdelete", 0)
        );
    }

    /**
     * 获取特定语言的white-list记录
     */
    public List<WhiteList> getProjectWhiteListByLanguage(Long projectId, String language) {
        // 获取项目的file_path
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return new ArrayList<>();
        }
        String filePath = project.getFile();

        return whiteListMapper.selectList(
            new QueryWrapper<WhiteList>()
                .eq("file_path", filePath)
                .eq("language", language)
                .eq("isdelete", 0)
        );
    }

    // =============== 内部类 ===============

    public static class MultiLangScanResult {
        private boolean success;
        private String message;
        private String error;
        private String projectPath;
        private Long projectId;
        private String detectedLanguage;
        private List<ProjectDependency> dependencies;
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

        public List<ProjectDependency> getDependencies() { return dependencies; }
        public void setDependencies(List<ProjectDependency> dependencies) { this.dependencies = dependencies; }

        public int getDependencyCount() { return dependencyCount; }
        public void setDependencyCount(int dependencyCount) { this.dependencyCount = dependencyCount; }

        public int getSavedCount() { return savedCount; }
        public void setSavedCount(int savedCount) { this.savedCount = savedCount; }
    }

    public static class ProjectDependency {
        private String name;
        private String version;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        @Override
        public String toString() {
            return "ProjectDependency{" +
                    "name='" + name + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }
    }
}
