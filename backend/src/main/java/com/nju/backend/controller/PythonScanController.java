package com.nju.backend.controller;

import com.nju.backend.service.project.impl.PythonProjectScanService;
import com.nju.backend.repository.po.WhiteList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Python项目扫描和White-list管理 Controller
 *
 * API端点:
 * 1. POST /python/scan - 扫描Python项目
 * 2. GET /python/whitelist/{projectId} - 获取项目white-list
 */
@RestController
@RequestMapping("/python")
@CrossOrigin(origins = "*")
public class PythonScanController {

    @Autowired
    private PythonProjectScanService pythonProjectScanService;

    /**
     * 扫描Python项目并保存依赖到white-list
     *
     * 请求体格式:
     * {
     *   "projectPath": "/path/to/python/project",
     *   "projectId": 1
     * }
     *
     * 流程:
     * 1. 检测项目语言是否为Python
     * 2. 调用Flask Python解析器获取依赖列表
     * 3. 将依赖保存到white-list表
     *
     * 返回格式:
     * {
     *   "code": 200,
     *   "message": "Successfully scanned and saved X dependencies to white-list",
     *   "success": true,
     *   "data": {
     *     "projectPath": "/path/to/python/project",
     *     "projectId": 1,
     *     "detectedLanguage": "python",
     *     "dependencyCount": 25,
     *     "savedCount": 25,
     *     "dependencies": [
     *       {
     *         "name": "requests",
     *         "version": "2.28.0",
     *         "packageManager": "pip",
     *         "language": "python"
     *       },
     *       ...
     *     ]
     *   }
     * }
     */
    @PostMapping("/scan")
    public Map<String, Object> scanPythonProject(@RequestBody Map<String, Object> request) {
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
            PythonProjectScanService.PythonScanResult scanResult =
                pythonProjectScanService.scanPythonProject(projectPath, projectId);

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
            response.put("message", "Error during Python project scan");
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * 获取项目的White-list记录
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
     *       "createdTime": "2025-11-13T15:00:00",
     *       "remark": "Auto-detected from Python project scan"
     *     },
     *     ...
     *   ]
     * }
     */
    @GetMapping("/whitelist/{projectId}")
    public Map<String, Object> getProjectWhiteList(@PathVariable Long projectId) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<WhiteList> whiteList = pythonProjectScanService.getProjectWhiteList(projectId);

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
