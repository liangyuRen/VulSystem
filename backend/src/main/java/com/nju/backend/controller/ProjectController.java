package com.nju.backend.controller;

import com.nju.backend.config.RespBean;
import com.nju.backend.config.RespBeanEnum;
import com.nju.backend.repository.mapper.ProjectMapper;
import com.nju.backend.repository.po.Project;
import com.nju.backend.service.project.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/project")
public class ProjectController {
    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectMapper projectMapper;

    //新建项目
    @PostMapping("/create")
    public RespBean createProject(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("language") String language,
            @RequestParam("risk_threshold") int risk_threshold,
            @RequestParam("companyId") int companyId,
            @RequestParam("filePath") String filePath) {
        try {
            // 调用服务层的方法创建项目
            projectService.createProject(name, description, language, risk_threshold, companyId,filePath);
            return RespBean.success();
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    @PostMapping("/uploadFile")
    public RespBean uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            return RespBean.success(projectService.uploadFile(file));
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    /**
     * 统一的项目上传和创建接口
     * 前端调用此接口上传文件并创建项目
     * 服务器自动检测项目语言，不依赖前端的语言参数
     */
    @PostMapping("/uploadProject")
    public RespBean uploadProject(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "riskThreshold", required = false) Integer riskThreshold,
            @RequestParam("companyId") int companyId) {
        try {
            System.out.println("=== uploadProject 接口被调用 ===");
            System.out.println("文件名: " + file.getOriginalFilename());
            System.out.println("项目名: " + name);
            System.out.println("companyId: " + companyId);

            // 验证文件是否为空
            if (file.isEmpty()) {
                return RespBean.error(RespBeanEnum.ERROR, "上传文件为空");
            }

            // 处理参数：默认风险阈值为0
            int riskThresholdValue = (riskThreshold != null && riskThreshold > 0) ? riskThreshold : 0;

            // 【关键改动】上传文件并自动检测项目语言
            System.out.println("步骤1: 开始上传并检测语言...");
            Map<String, Object> uploadResult = projectService.uploadFileWithLanguageDetection(file);
            String filePath = (String) uploadResult.get("filePath");
            String detectedLanguage = (String) uploadResult.get("language");

            System.out.println("步骤2: 文件上传成功");
            System.out.println("  - 文件路径: " + filePath);
            System.out.println("  - 检测语言: " + detectedLanguage);

            // 【关键改动】使用检测到的语言创建项目，而不是前端参数
            System.out.println("步骤3: 开始创建项目，使用检测到的语言: " + detectedLanguage);
            projectService.createProject(name, description, detectedLanguage, riskThresholdValue, companyId, filePath);
            System.out.println("步骤4: 项目创建成功");

            // 【新增】步骤5: 自动触发依赖解析
            System.out.println("步骤5: 自动触发依赖解析...");
            triggerAutoDependencyParsing(detectedLanguage, filePath);

            // 返回成功响应，包含检测结果
            return RespBean.success(new java.util.HashMap<String, Object>() {{
                put("status", "parsing");
                put("message", "项目上传成功，检测到语言: " + detectedLanguage + "，正在后台解析依赖...");
                put("detectedLanguage", detectedLanguage);
                put("filePath", filePath);
            }});
        } catch (Exception e) {
            System.err.println("=== uploadProject 接口异常 ===");
            System.err.println("异常类型: " + e.getClass().getName());
            System.err.println("异常信息: " + e.getMessage());
            e.printStackTrace();
            return RespBean.error(RespBeanEnum.ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    //删除项目
    @PostMapping("/delete")
    public RespBean deleteProject(@RequestParam("id") int id) {
        try {
            projectService.deleteProject(id);
            return RespBean.success();
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    //更新项目
    @PostMapping("/update")
    public RespBean updateProject(@RequestParam("id") int id,@RequestParam("name") String name,@RequestParam("description") String description,@RequestParam("risk_threshold") int risk_threshold,@RequestParam(value = "filePath",required = false) String filePath) {
        try {
            projectService.updateProject(id,name,description,risk_threshold,filePath);
            return RespBean.success();
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    @GetMapping("/getVulnerabilities")
    public RespBean getVulnerabilityInfo(@RequestParam("id") int id) {
        try {
            return RespBean.success(projectService.getVulnerabilities(id));
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    @GetMapping("/list")
    public RespBean getProjectList(@RequestParam("companyId") int companyId, @RequestParam("page") int page, @RequestParam("size") int size) {
        try {
            return RespBean.success(projectService.getProjectList(companyId,page,size));
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public RespBean getProjectStatistics(@RequestParam("companyId") int companyId) {
        try {
            return RespBean.success(projectService.getProjectStatistics(companyId));
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    @GetMapping("/info")
    public RespBean getProjectInfo(@RequestParam("projectid") int id) {
        try {
            return RespBean.success(projectService.getProjectInfo(id));
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    @GetMapping("/sbom")
    public Object getSBOMFile(@RequestParam("projectId") int id, @RequestParam("format") String format,@RequestParam("outFileName") String outFileName) throws IOException {
        try{

            // 1. 获取 SBOM 文件
            File sbomFile = projectService.getProjectSBOM(id, format,outFileName); // 调用你的生成方法

            // 2. 将 File 转换为 Resource（封装文件流）
            Path filePath = sbomFile.toPath();
            Resource resource = new PathResource(filePath);

            // 3. 设置 HTTP 头（关键步骤）
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sbomFile.getName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // 或根据类型指定（如 application/json）
                    .body(resource);
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 手动触发项目依赖重新解析接口
     * 根据指定的语言类型重新解析项目依赖
     *
     * @param projectId 项目ID
     * @param language 语言类型（java, python, go, rust, javascript, php, ruby, erlang, c）
     */
    @PostMapping("/reparse")
    public RespBean reparseProject(
            @RequestParam("projectId") int projectId,
            @RequestParam("language") String language) {
        try {
            // 直接从数据库获取项目信息
            Project project = projectMapper.selectById(projectId);

            if (project == null || project.getIsDelete() == 1) {
                return RespBean.error(RespBeanEnum.ERROR, "项目不存在或已被删除");
            }

            String filePath = project.getFile();
            String languageLower = language.toLowerCase();

            System.out.println("========================================");
            System.out.println("手动触发项目重新解析");
            System.out.println("项目ID: " + projectId);
            System.out.println("项目名称: " + project.getName());
            System.out.println("项目路径: " + filePath);
            System.out.println("目标语言: " + languageLower);
            System.out.println("========================================");

            // 根据语言类型调用相应的解析方法
            switch (languageLower) {
                case "java":
                    projectService.asyncParseJavaProject(filePath);
                    break;
                case "c":
                case "cpp":
                case "c++":
                    projectService.asyncParseCProject(filePath);
                    break;
                case "python":
                    projectService.asyncParsePythonProject(filePath);
                    break;
                case "rust":
                    projectService.asyncParseRustProject(filePath);
                    break;
                case "go":
                case "golang":
                    projectService.asyncParseGoProject(filePath);
                    break;
                case "javascript":
                case "js":
                case "node":
                case "nodejs":
                    projectService.asyncParseJavaScriptProject(filePath);
                    break;
                case "php":
                    projectService.asyncParsePhpProject(filePath);
                    break;
                case "ruby":
                    projectService.asyncParseRubyProject(filePath);
                    break;
                case "erlang":
                    projectService.asyncParseErlangProject(filePath);
                    break;
                default:
                    return RespBean.error(RespBeanEnum.ERROR,
                        "不支持的语言类型: " + language +
                        "\n支持的语言: java, python, go, rust, javascript, php, ruby, erlang, c");
            }

            return RespBean.success(new HashMap<String, Object>() {{
                put("status", "parsing");
                put("message", "已触发" + languageLower + "项目依赖解析，正在后台处理...");
                put("language", languageLower);
                put("projectId", projectId);
                put("projectName", project.getName());
            }});

        } catch (Exception e) {
            System.err.println("手动重新解析项目失败: " + e.getMessage());
            e.printStackTrace();
            return RespBean.error(RespBeanEnum.ERROR, "重新解析失败: " + e.getMessage());
        }
    }

    /**
     * 批量解析多个语言（用于混合语言项目）
     *
     * @param projectId 项目ID
     * @param languages 语言列表，逗号分隔（如: "java,python,go"）
     */
    @PostMapping("/reparse/multiple")
    public RespBean reparseMultipleLanguages(
            @RequestParam("projectId") int projectId,
            @RequestParam("languages") String languages) {
        try {
            String[] languageArray = languages.split(",");
            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMessages = new StringBuilder();

            for (String language : languageArray) {
                try {
                    RespBean result = reparseProject(projectId, language.trim());
                    if (result.getCode() == 200) {
                        successCount++;
                    } else {
                        failCount++;
                        errorMessages.append(language).append(": ").append(result.getMessage()).append("; ");
                    }
                } catch (Exception e) {
                    failCount++;
                    errorMessages.append(language).append(": ").append(e.getMessage()).append("; ");
                }
            }

            // 使用final变量以便在匿名内部类中使用
            final int finalSuccessCount = successCount;
            final int finalFailCount = failCount;
            final String finalErrorMessages = errorMessages.toString();

            if (failCount == 0) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("status", "success");
                resultData.put("message", "成功触发" + finalSuccessCount + "个语言的解析任务");
                resultData.put("successCount", finalSuccessCount);
                return RespBean.success(resultData);
            } else {
                return RespBean.error(RespBeanEnum.ERROR,
                    "部分解析任务失败: 成功" + finalSuccessCount + "个, 失败" + finalFailCount + "个\n" +
                    "错误详情: " + finalErrorMessages);
            }

        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, "批量解析失败: " + e.getMessage());
        }
    }

    /**
     * 自动触发依赖解析（上传项目后调用）
     *
     * @param language 检测到的项目语言
     * @param filePath 项目文件路径
     */
    private void triggerAutoDependencyParsing(String language, String filePath) {
        System.out.println("========================================");
        System.out.println("自动触发依赖解析");
        System.out.println("语言: " + language);
        System.out.println("路径: " + filePath);
        System.out.println("========================================");

        try {
            String languageLower = language.toLowerCase();

            switch (languageLower) {
                case "java":
                    System.out.println("→ 触发 Java 依赖解析");
                    projectService.asyncParseJavaProject(filePath);
                    break;
                case "python":
                    System.out.println("→ 触发 Python 依赖解析");
                    projectService.asyncParsePythonProject(filePath);
                    break;
                case "php":
                    System.out.println("→ 触发 PHP 依赖解析");
                    projectService.asyncParsePhpProject(filePath);
                    break;
                case "ruby":
                    System.out.println("→ 触发 Ruby 依赖解析");
                    projectService.asyncParseRubyProject(filePath);
                    break;
                case "go":
                case "golang":
                    System.out.println("→ 触发 Go 依赖解析");
                    projectService.asyncParseGoProject(filePath);
                    break;
                case "rust":
                    System.out.println("→ 触发 Rust 依赖解析");
                    projectService.asyncParseRustProject(filePath);
                    break;
                case "javascript":
                case "js":
                case "node":
                case "nodejs":
                    System.out.println("→ 触发 JavaScript 依赖解析");
                    projectService.asyncParseJavaScriptProject(filePath);
                    break;
                case "erlang":
                    System.out.println("→ 触发 Erlang 依赖解析");
                    projectService.asyncParseErlangProject(filePath);
                    break;
                case "c":
                case "cpp":
                case "c++":
                    System.out.println("→ 触发 C/C++ 依赖解析");
                    projectService.asyncParseCProject(filePath);
                    break;
                default:
                    System.out.println("⚠ 不支持的语言: " + language + "，跳过依赖解析");
            }

            System.out.println("✓ 依赖解析任务已提交到后台线程池");

        } catch (Exception e) {
            System.err.println("✗ 自动触发依赖解析失败: " + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，避免影响项目创建
        }
    }


}
