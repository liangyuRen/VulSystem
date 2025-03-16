package com.nju.backend.controller;

import com.nju.backend.config.RespBean;
import com.nju.backend.config.RespBeanEnum;
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

@RestController
@RequestMapping("/project")
public class ProjectController {
    @Autowired
    private ProjectService projectService;

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


}
