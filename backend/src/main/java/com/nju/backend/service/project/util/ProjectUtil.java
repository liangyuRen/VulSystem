package com.nju.backend.service.project.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nju.backend.config.FileStorageConfig;
import com.nju.backend.repository.mapper.ProjectMapper;
import com.nju.backend.repository.mapper.ProjectVulnerabilityMapper;
import com.nju.backend.repository.mapper.VulnerabilityMapper;
import com.nju.backend.repository.po.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ProjectUtil {

    @Autowired
    private ProjectVulnerabilityMapper projectVulnerabilityMapper;

    @Autowired
    private VulnerabilityMapper vulnerabilityMapper;

    private final FileStorageConfig fileStorageConfig;
    @Autowired
    private ProjectMapper projectMapper;

    public ProjectUtil(FileStorageConfig fileStorageConfig) {
        this.fileStorageConfig = fileStorageConfig;
    }

    public String unzipAndSaveFile(MultipartFile file) throws IOException {
        String baseUploadDir = fileStorageConfig.getUploadDir();

        // 创建基础上传目录（如果不存在）
        File baseDir = new File(baseUploadDir);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new RuntimeException("文件上传失败: 创建基础文件夹失败");
        }

        // 生成唯一子目录（防止重名）
        String uniqueDirName = UUID.randomUUID().toString();
        File destDir = new File(baseDir, uniqueDirName);
        if (!destDir.mkdirs()) {
            throw new RuntimeException("文件上传失败: 创建解压文件夹失败");
        }

        try (ZipInputStream zipIn = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry = zipIn.getNextEntry();
            byte[] buffer = new byte[4096];

            while (entry != null) {
                String entryPath = destDir.getAbsolutePath() + File.separator + entry.getName();
                File entryFile = new File(entryPath);

                // 防御路径遍历攻击
                if (!entryFile.getCanonicalPath().startsWith(destDir.getCanonicalPath() + File.separator)) {
                    throw new IOException("非法解压路径：" + entry.getName());
                }

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    // 确保父目录存在
                    File parentDir = entryFile.getParentFile();
                    if (!parentDir.exists() && !parentDir.mkdirs()) {
                        throw new IOException("无法创建父目录: " + parentDir.getAbsolutePath());
                    }

                    // 处理文件重名（同一ZIP内部）
                    if (entryFile.exists()) {
                        String fileName = getUniqueFileName(entryFile);
                        entryPath = entryFile.getParent() + File.separator + fileName;
                        entryFile = new File(entryPath);
                    }

                    // 写入文件
                    try (FileOutputStream fos = new FileOutputStream(entryFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        int len;
                        while ((len = zipIn.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }

        // 返回唯一解压目录的绝对路径
        return destDir.getAbsolutePath();
    }

    /**
     * 生成唯一文件名（解决同一ZIP内部文件重名问题）
     */
    private String getUniqueFileName(File file) {
        String baseName = file.getName();
        String parentDir = file.getParent();
        String nameWithoutExt = baseName.replaceFirst("[.][^.]+$", "");
        String extension = baseName.substring(nameWithoutExt.length());

        int counter = 1;
        while (file.exists()) {
            String newName = nameWithoutExt + "_" + counter + extension;
            file = new File(parentDir, newName);
            counter++;
        }
        return file.getName();
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

    public int getRiskNum(int projectId, String riskLevel) {
        AtomicInteger riskNum = new AtomicInteger();
        Project project =projectMapper.selectById(projectId);
        projectVulnerabilityMapper.selectList(new QueryWrapper<ProjectVulnerability>().eq("project_id", project.getId()))
                .forEach(pv -> {
                    Vulnerability vulnerability = vulnerabilityMapper.selectById(pv.getVulnerabilityId());
                    if (vulnerability == null) {
                        return;
                    }
                    if (vulnerability.getRiskLevel().equals(riskLevel)) {
                        riskNum.getAndIncrement();
                    }
                });
        return riskNum.get();
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

    public  List<WhiteList> parseJsonData(String jsonData) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonData, new TypeReference<List<WhiteList>>() {
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}
