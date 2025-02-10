package com.nju.backend.controller;

import com.nju.backend.config.RespBean;
import com.nju.backend.config.RespBeanEnum;
import com.nju.backend.repository.po.Project;
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
    public RespBean createProject(@RequestBody Project project, @RequestBody(required = false) MultipartFile file, @RequestBody String companyName) {
        try {
            projectService.createProject(project,file,companyName);
            return RespBean.success();
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    //删除项目
    @PostMapping("/delete")
    public RespBean deleteProject(@RequestBody String projectName) {
        try {
            projectService.deleteProject(projectName);
            return RespBean.success();
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    //更新项目
    @PostMapping("/update")
    public RespBean updateProject(@RequestBody Project project) {
        try {
            projectService.updateProject(project);
            return RespBean.success();
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    //按页获取项目列表
    @GetMapping("/list")
    public RespBean getProjectList(@RequestParam("page") Integer page, @RequestParam("size") Integer size) {
        try {
            return RespBean.success(projectService.getProjectList(page, size));
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

}
