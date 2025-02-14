package com.nju.backend.service.project.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nju.backend.config.FileStorageConfig;
import com.nju.backend.repository.mapper.ProjectVulnerabilityMapper;
import com.nju.backend.repository.po.ProjectVulnerability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ProjectUtil {

    @Autowired
    private ProjectVulnerabilityMapper projectVulnerabilityMapper;

    private final FileStorageConfig fileStorageConfig;

    public ProjectUtil(FileStorageConfig fileStorageConfig) {
        this.fileStorageConfig = fileStorageConfig;
    }

    public String saveFile(MultipartFile file) {
        String uploadDir = fileStorageConfig.getUploadDir();

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            if(!directory.mkdirs()){
                throw new RuntimeException("文件上传失败: 创建文件夹失败");
            }
        }

        String originalFilename = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID() + "_" + originalFilename;
        String filePath = uploadDir + uniqueFileName;

        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }

        return filePath;
    }


    public String getRiskLevel(int projectId,int riskThreshold) {
        QueryWrapper<ProjectVulnerability> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .eq("isDelete", 0);
        long vulnerabilityCount = projectVulnerabilityMapper.selectCount(wrapper);
        if(vulnerabilityCount >= riskThreshold) {
            return "高风险";
        }
        else if(vulnerabilityCount > 0) {
            return "低风险";
        }
        return "暂无风险";
    }


    public long getVulnerabilityCount(int projectId) {
        QueryWrapper<ProjectVulnerability> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .eq("isDelete", 0);
        return projectVulnerabilityMapper.selectCount(wrapper);
    }

    public String timeToDayOfWeek(Date time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);
        LocalDateTime localDateTime = time.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return localDateTime.format(formatter).substring(0,3);
    }

}
