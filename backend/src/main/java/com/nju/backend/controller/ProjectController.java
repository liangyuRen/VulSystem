package com.nju.backend.controller;

import com.nju.backend.config.RespBean;
import com.nju.backend.config.RespBeanEnum;
import com.nju.backend.service.project.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("companyName") String companyName) {
        try {
            // 调用服务层的方法创建项目
            projectService.createProject(name, description, language, risk_threshold, file, companyName);
            return RespBean.success();
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    @PostMapping("/uploadFile")
    public RespBean uploadFile(@RequestParam("id") int id,@RequestParam("file") MultipartFile file) {
        try {
            projectService.uploadFile(id,file);
            return RespBean.success();
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
    public RespBean updateProject(@RequestParam("id") int id,@RequestParam("name") String name,@RequestParam("description") String description,@RequestParam("risk_threshold") int risk_threshold) {
        try {
            projectService.updateProject(id,name,description,risk_threshold);
            return RespBean.success();
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

}
