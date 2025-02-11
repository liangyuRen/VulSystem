package com.nju.backend.service.project.util;

import com.nju.backend.config.FileStorageConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class ProjectUtil {

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
}
