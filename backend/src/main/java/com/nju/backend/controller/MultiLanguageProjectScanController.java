package com.nju.backend.controller;

import com.nju.backend.service.project.impl.MultiLanguageProjectScanService;
import com.nju.backend.repository.po.WhiteList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 多语言项目扫描 Controller
 *
 * API端点:
 * 1. POST /project/scan - 扫描任意语言的项目
 * 2. GET /project/whitelist/{projectId} - 获取项目white-list
 * 3. GET /project/whitelist/{projectId}/{language} - 获取特定语言的white-list
 */
@RestController
@RequestMapping("/project")
@CrossOrigin(origins = "*")
public class MultiLanguageProjectScanController {

    @Autowired
    private MultiLanguageProjectScanService multiLanguageProjectScanService;

    /**
     * 扫描多语言项目并保存依赖到white-list
     *
     * 请求体格式:
     * {
     *   "projectPath": "/path/to/project",
     *   "projectId": 1
     * }
     *
     * 返回格式:
     * {
     *   "code": 200,
     *   "message": "Successfully scanned and saved X dependencies to white-list",
     *   "success": true,
     *   "data": {
     *     "projectPath": "/path/to/project",
     *     "projectId": 1,
     *     "detectedLanguage": "python",
     *     "dependencyCount": 25,
     *     "savedCount": 25,
     *     "dependencies": [...]
     *   }
     * }
     */
    @PostMapping("/scan")
    public Map<String, Object> scanProject(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String projectPath = (String) request.get("projectPath");
            Long projectId = ((Number) request.get("projectId")).longValue();

            if (projectPath == null || projectPath.isEmpty()) {
                response.put("code", 400);
                response.put("message", "projectPath is required");
                response.put("success", false);
                return response;
            }

            // 执行扫描
            MultiLanguageProjectScanService.MultiLangScanResult scanResult =
                multiLanguageProjectScanService.scanProject(projectPath, projectId);

            if (scanResult.isSuccess()) {
                response.put("code", 200);
                response.put("message", scanResult.getMessage());
                response.put("success", true);
                response.put("data", scanResult);
            } else {
                response.put("code", 400);
                response.put("message", scanResult.getMessage());
                response.put("success", false);
                response.put("error", scanResult.getError());
            }

        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "Error during project scan");
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * 获取项目的White-list记录（所有语言）
     *
     * 路径参数: projectId - 项目ID
     *
     * 返回格式:
     * {
     *   "code": 200,
     *   "message": "Success",
     *   "data": [
     *     {
     *       "id": 1,
     *       "projectId": 1,
     *       "componentName": "requests",
     *       "componentVersion": "2.28.0",
     *       "language": "python",
     *       "packageManager": "pip",
     *       "status": "APPROVED",
     *       "createdTime": "2025-11-13T15:00:00"
     *     },
     *     ...
     *   ]
     * }
     */
    @GetMapping("/whitelist/{projectId}")
    public Map<String, Object> getProjectWhiteList(@PathVariable Long projectId) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<WhiteList> whiteList = multiLanguageProjectScanService.getProjectWhiteList(projectId);

            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", whiteList);

        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "Error retrieving white-list");
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * 获取特定语言的White-list记录
     *
     * 路径参数:
     * - projectId: 项目ID
     * - language: 编程语言 (python, php, javascript, rust等)
     *
     * 返回格式:
     * {
     *   "code": 200,
     *   "message": "Success",
     *   "data": [...]
     * }
     */
    @GetMapping("/whitelist/{projectId}/{language}")
    public Map<String, Object> getProjectWhiteListByLanguage(
            @PathVariable Long projectId,
            @PathVariable String language) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<WhiteList> whiteList = multiLanguageProjectScanService
                    .getProjectWhiteListByLanguage(projectId, language);

            response.put("code", 200);
            response.put("message", "Success");
            response.put("data", whiteList);

        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "Error retrieving white-list");
            response.put("error", e.getMessage());
        }

        return response;
    }
}
