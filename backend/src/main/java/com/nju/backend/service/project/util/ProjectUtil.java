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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipFile;

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
        System.out.println("DEBUG: 基础上传目录: " + baseUploadDir);

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
        System.out.println("DEBUG: 目标解压目录: " + destDir.getAbsolutePath());

        // 检查上传文件的基本信息
        System.out.println("DEBUG: 上传文件名: " + file.getOriginalFilename());
        System.out.println("DEBUG: 上传文件大小: " + file.getSize() + " bytes");
        System.out.println("DEBUG: 上传文件内容类型: " + file.getContentType());
        System.out.println("DEBUG: 上传文件是否为空: " + file.isEmpty());

        // 先将文件内容保存到字节数组（避免临时文件被删除的问题）
        byte[] fileBytes;
        try (java.io.InputStream inputStream = file.getInputStream()) {
            // Java 8兼容的读取方式
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[4096];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            fileBytes = buffer.toByteArray();
            System.out.println("DEBUG: 成功读取文件内容到内存，大小: " + fileBytes.length + " bytes");
        } catch (IOException e) {
            System.err.println("DEBUG: 读取上传文件内容失败: " + e.getMessage());
            throw new IOException("无法读取上传文件内容: " + e.getMessage());
        }

        // 将字节数组保存到临时ZIP文件
        File tempZipFile = new File(destDir.getParent(), uniqueDirName + ".zip");
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempZipFile)) {
            fos.write(fileBytes);
            fos.flush();
            System.out.println("DEBUG: 成功将文件内容写入临时ZIP文件");
        } catch (IOException e) {
            System.err.println("DEBUG: 写入临时ZIP文件失败: " + e.getMessage());
            throw new IOException("无法创建临时ZIP文件: " + e.getMessage());
        }

        System.out.println("DEBUG: 临时ZIP文件路径: " + tempZipFile.getAbsolutePath());
        System.out.println("DEBUG: 临时ZIP文件是否存在: " + tempZipFile.exists());
        System.out.println("DEBUG: 临时ZIP文件大小: " + tempZipFile.length() + " bytes");
        System.out.println("DEBUG: 临时ZIP文件可读: " + tempZipFile.canRead());

        // 检查文件头，确认文件格式
        String fileFormat = "unknown";
        if (tempZipFile.length() >= 4) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(tempZipFile)) {
                byte[] header = new byte[6];
                int bytesRead = fis.read(header);

                // ZIP文件的魔术数字是 0x50 0x4B (PK)
                if (bytesRead >= 2 && header[0] == (byte)0x50 && header[1] == (byte)0x4B) {
                    fileFormat = "zip";
                    System.out.println("DEBUG: 检测到ZIP格式文件");
                }
                // 7z文件的魔术数字是 0x37 0x7A 0xBC 0xAF 0x27 0x1C
                else if (bytesRead >= 6 && header[0] == (byte)0x37 && header[1] == (byte)0x7A &&
                        header[2] == (byte)0xBC && header[3] == (byte)0xAF) {
                    fileFormat = "7z";
                    System.out.println("DEBUG: 检测到7z格式文件");
                }
                // RAR文件头 0x52 0x61 0x72 0x21 (Rar!)
                else if (bytesRead >= 4 && header[0] == (byte)0x52 && header[1] == (byte)0x61 &&
                        header[2] == (byte)0x72 && header[3] == (byte)0x21) {
                    fileFormat = "rar";
                    System.out.println("DEBUG: 检测到RAR格式文件");
                }
                else {
                    System.out.println("DEBUG: 未知文件格式，文件头: " +
                        String.format("0x%02X 0x%02X 0x%02X 0x%02X 0x%02X 0x%02X",
                        header[0], header[1], header[2], header[3], header[4], header[5]));
                }
            } catch (Exception e) {
                System.out.println("DEBUG: 文件头检查失败: " + e.getMessage());
            }
        } else {
            System.out.println("DEBUG: 文件太小，可能不是有效的压缩文件");
        }

        // 如果不是ZIP格式，提供友好的错误信息
        if (!"zip".equals(fileFormat)) {
            String errorMsg = "";
            switch (fileFormat) {
                case "7z":
                    errorMsg = "检测到7z格式文件。目前系统仅支持ZIP格式，请将文件重新打包为ZIP格式后上传。";
                    break;
                case "rar":
                    errorMsg = "检测到RAR格式文件。目前系统仅支持ZIP格式，请将文件重新打包为ZIP格式后上传。";
                    break;
                default:
                    errorMsg = "未知的文件格式或文件损坏。请确保上传的是有效的ZIP格式文件。";
                    break;
            }
            System.err.println("DEBUG: " + errorMsg);
            throw new IOException(errorMsg);
        }

        int fileCount = 0;
        int dirCount = 0;

        // 使用ZipFile代替ZipInputStream，支持更好的编码处理
        try (ZipFile zipFile = new ZipFile(tempZipFile, java.nio.charset.Charset.forName("GBK"))) {
            System.out.println("DEBUG: ZIP文件条目数量: " + zipFile.size());

            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                System.out.println("DEBUG: 处理ZIP条目: " + entry.getName() +
                                 ", 是否目录: " + entry.isDirectory() +
                                 ", 大小: " + entry.getSize());

                String entryName = entry.getName();
                // 处理路径分隔符
                entryName = entryName.replace('/', File.separatorChar);

                File entryFile = new File(destDir, entryName);

                // 防御路径遍历攻击
                String canonicalDestPath = destDir.getCanonicalPath();
                String canonicalFilePath = entryFile.getCanonicalPath();

                boolean isValidPath = canonicalFilePath.equals(canonicalDestPath) ||
                                    canonicalFilePath.startsWith(canonicalDestPath + File.separator);

                if (!isValidPath) {
                    System.out.println("DEBUG: 跳过潜在危险路径: " + entry.getName());
                    continue;
                }

                if (entry.isDirectory()) {
                    boolean created = entryFile.mkdirs();
                    System.out.println("DEBUG: 创建目录 " + entryFile.getAbsolutePath() + ", 结果: " + created);
                    dirCount++;
                } else {
                    // 确保父目录存在
                    File parentDir = entryFile.getParentFile();
                    if (!parentDir.exists()) {
                        boolean created = parentDir.mkdirs();
                        System.out.println("DEBUG: 创建父目录 " + parentDir.getAbsolutePath() + ", 结果: " + created);
                    }

                    // 处理文件重名
                    if (entryFile.exists()) {
                        String fileName = getUniqueFileName(entryFile);
                        entryFile = new File(parentDir, fileName);
                        System.out.println("DEBUG: 文件重名，重命名为: " + fileName);
                    }

                    // 写入文件
                    try (java.io.InputStream inputStream = zipFile.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(entryFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                        byte[] buffer = new byte[4096];
                        int len;
                        long totalBytes = 0;

                        while ((len = inputStream.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                            totalBytes += len;
                        }

                        System.out.println("DEBUG: 写入文件 " + entryFile.getName() + ", 大小: " + totalBytes + " bytes");
                        fileCount++;

                    } catch (IOException e) {
                        System.err.println("DEBUG: 写入文件失败 " + entryFile.getAbsolutePath() + ", 错误: " + e.getMessage());
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("DEBUG: ZIP文件处理失败: " + e.getMessage());
            // 如果GBK编码失败，尝试UTF-8编码
            try (ZipFile zipFile = new ZipFile(tempZipFile, java.nio.charset.Charset.forName("UTF-8"))) {
                System.out.println("DEBUG: 使用UTF-8重新尝试解压...");
                return unzipWithZipFile(zipFile, destDir);
            } catch (IOException e2) {
                System.err.println("DEBUG: UTF-8编码也失败，使用默认编码尝试...");
                // 最后使用系统默认编码
                try (ZipFile zipFile = new ZipFile(tempZipFile)) {
                    return unzipWithZipFile(zipFile, destDir);
                } catch (IOException e3) {
                    System.err.println("DEBUG: 系统默认编码也失败，尝试使用ZipInputStream...");
                    // 最后的备用方案：使用ZipInputStream
                    return unzipWithInputStream(fileBytes, destDir);
                }
            }
        } finally {
            // 删除临时ZIP文件（延迟删除，确保文件未被占用）
            if (tempZipFile.exists()) {
                try {
                    // 等待一小段时间确保文件句柄释放
                    Thread.sleep(100);
                    boolean deleted = tempZipFile.delete();
                    System.out.println("DEBUG: 删除临时ZIP文件: " + deleted);
                    if (!deleted) {
                        // 如果删除失败，标记为在JVM退出时删除
                        tempZipFile.deleteOnExit();
                        System.out.println("DEBUG: 标记临时ZIP文件在JVM退出时删除");
                    }
                } catch (InterruptedException e) {
                    System.out.println("DEBUG: 删除临时ZIP文件时中断: " + e.getMessage());
                }
            }
        }

        System.out.println("DEBUG: 解压完成，总计目录: " + dirCount + ", 文件: " + fileCount);

        // 检查最终目录内容
        File[] files = destDir.listFiles();
        if (files != null) {
            System.out.println("DEBUG: 解压目录最终包含 " + files.length + " 个项目:");
            for (File f : files) {
                System.out.println("DEBUG: - " + f.getName() + (f.isDirectory() ? " (目录)" : " (文件, " + f.length() + " bytes)"));
            }
        } else {
            System.out.println("DEBUG: 警告 - 解压目录为空或无法读取");
        }

        return destDir.getAbsolutePath();
    }

    /**
     * 使用ZipInputStream解压的备用方法
     */
    private String unzipWithInputStream(byte[] fileBytes, File destDir) throws IOException {
        System.out.println("DEBUG: 使用ZipInputStream备用方案解压");
        int fileCount = 0;
        int dirCount = 0;

        try (ZipInputStream zipIn = new ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
            ZipEntry entry = zipIn.getNextEntry();
            byte[] buffer = new byte[4096];

            while (entry != null) {
                System.out.println("DEBUG: [ZipInputStream] 处理ZIP条目: " + entry.getName() +
                                 ", 是否目录: " + entry.isDirectory());

                String entryName = entry.getName().replace('/', File.separatorChar);
                File entryFile = new File(destDir, entryName);

                // 安全检查
                String canonicalDestPath = destDir.getCanonicalPath();
                String canonicalFilePath = entryFile.getCanonicalPath();
                boolean isValidPath = canonicalFilePath.equals(canonicalDestPath) ||
                                    canonicalFilePath.startsWith(canonicalDestPath + File.separator);

                if (!isValidPath) {
                    System.out.println("DEBUG: [ZipInputStream] 跳过危险路径: " + entry.getName());
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                    continue;
                }

                if (entry.isDirectory()) {
                    boolean created = entryFile.mkdirs();
                    System.out.println("DEBUG: [ZipInputStream] 创建目录: " + entryFile.getName() + ", 结果: " + created);
                    dirCount++;
                } else {
                    File parentDir = entryFile.getParentFile();
                    if (!parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    try (FileOutputStream fos = new FileOutputStream(entryFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        int len;
                        long totalBytes = 0;
                        while ((len = zipIn.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                            totalBytes += len;
                        }
                        System.out.println("DEBUG: [ZipInputStream] 写入文件: " + entryFile.getName() +
                                         ", 大小: " + totalBytes + " bytes");
                        fileCount++;
                    }
                }

                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }

        System.out.println("DEBUG: [ZipInputStream] 解压完成，总计目录: " + dirCount + ", 文件: " + fileCount);
        return destDir.getAbsolutePath();
    }

    /**
     * 使用ZipFile解压的辅助方法
     */
    private String unzipWithZipFile(ZipFile zipFile, File destDir) throws IOException {
        int fileCount = 0;
        int dirCount = 0;

        java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            String entryName = entry.getName().replace('/', File.separatorChar);
            File entryFile = new File(destDir, entryName);

            // 安全检查
            String canonicalDestPath = destDir.getCanonicalPath();
            String canonicalFilePath = entryFile.getCanonicalPath();
            boolean isValidPath = canonicalFilePath.equals(canonicalDestPath) ||
                                canonicalFilePath.startsWith(canonicalDestPath + File.separator);

            if (!isValidPath) {
                continue;
            }

            if (entry.isDirectory()) {
                entryFile.mkdirs();
                dirCount++;
            } else {
                File parentDir = entryFile.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }

                try (java.io.InputStream inputStream = zipFile.getInputStream(entry);
                     FileOutputStream fos = new FileOutputStream(entryFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = inputStream.read(buffer)) > 0) {
                        bos.write(buffer, 0, len);
                    }
                    fileCount++;
                }
            }
        }

        System.out.println("DEBUG: 辅助解压完成，总计目录: " + dirCount + ", 文件: " + fileCount);
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

    // 初始化最近七天的日期映射，键为星期几的前三个字母
    public Map<String, Integer> initRecentSevenDaysMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            String dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH));
            map.put(dayOfWeek, 0);
        }
        return map;
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

    public static Map<String, Double> calcLanguagePercentByFileSize(String projectPath) {
    // 常见语言扩展名映射
    Map<String, String> EXT_LANG_MAP = new HashMap<>();
    EXT_LANG_MAP.put("java", "Java");
    EXT_LANG_MAP.put("c", "C");
    EXT_LANG_MAP.put("cpp", "C++");
    EXT_LANG_MAP.put("h", "C/C++头文件");
    EXT_LANG_MAP.put("py", "Python");
    EXT_LANG_MAP.put("js", "JavaScript");
    EXT_LANG_MAP.put("ts", "TypeScript");

    String[] ignoreExts = {"class", "o", "exe"};

    Map<String, Long> langSize = new HashMap<>();
    long total = countFileSize(new File(projectPath), langSize, EXT_LANG_MAP, ignoreExts);

    Map<String, Double> percent = new HashMap<>();
    for (Map.Entry<String, Long> entry : langSize.entrySet()) {
        percent.put(entry.getKey(), entry.getValue() * 100.0 / total);
    }
    return percent;
    }

    private static long countFileSize(File dir, Map<String, Long> langSize, Map<String, String> extLangMap, String... ignoreExts) {
        long total = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File file : files) {
            if (file.isDirectory()) {
                total += countFileSize(file, langSize, extLangMap, ignoreExts);
            } else {
                String ext = getFileExt(file.getName());
                if (Arrays.asList(ignoreExts).contains(ext)) continue;
                String lang = extLangMap.getOrDefault(ext, "Other");
                long size = file.length();
                langSize.put(lang, langSize.getOrDefault(lang, 0L) + size);
                total += size;
            }
        }
        return total;
    }
    private static String getFileExt(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    public String detectProjectType(String projectPath) throws IOException {
        Path path = Paths.get(projectPath);
        System.out.println("DEBUG: 检测项目类型，路径: " + projectPath);

        if (!Files.isDirectory(path)) {
            System.out.println("DEBUG: 路径不是目录: " + projectPath);
            throw new IllegalArgumentException("Invalid project directory");
        }

        final boolean[] hasJava = {false};
        final boolean[] hasC = {false};
        final List<String> javaFiles = new ArrayList<>();
        final List<String> cFiles = new ArrayList<>();
        final List<String> allFiles = new ArrayList<>();

        // 限制递归深度为3层（根目录+2级子目录）
        try (Stream<Path> stream = Files.walk(path, 3)) {
            stream.forEach(file -> {
                String fileName = file.getFileName().toString();
                String fileNameLower = fileName.toLowerCase();
                allFiles.add(file.toString());

                // 检测Java特征
                if (fileNameLower.equals("pom.xml")
                        || fileNameLower.equals("build.gradle")
                        || fileNameLower.endsWith(".java")) {
                    hasJava[0] = true;
                    javaFiles.add(fileName);
                    System.out.println("DEBUG: 发现Java特征文件: " + fileName);
                }

                // 检测C特征
                if (fileNameLower.equals("makefile")
                        || fileNameLower.equals("cmakelists.txt")
                        || fileNameLower.endsWith(".c")
                        || fileNameLower.endsWith(".h")) {
                    hasC[0] = true;
                    cFiles.add(fileName);
                    System.out.println("DEBUG: 发现C/C++特征文件: " + fileName);
                }
            });
        }

        System.out.println("DEBUG: 项目目录包含总文件数: " + allFiles.size());
        System.out.println("DEBUG: Java特征文件数: " + javaFiles.size());
        System.out.println("DEBUG: C/C++特征文件数: " + cFiles.size());

        // 显示前10个文件用于调试
        System.out.println("DEBUG: 目录中的前10个文件:");
        allFiles.stream().limit(10).forEach(f -> System.out.println("DEBUG: - " + f));

        // 决策逻辑：Java特征优先
        String result;
        if (hasJava[0] && hasC[0]) {
            result = "java"; // 同时存在时优先返回Java
            System.out.println("DEBUG: 同时检测到Java和C特征，返回Java");
        } else if (hasJava[0]) {
            result = "java";
            System.out.println("DEBUG: 检测到Java特征，返回java");
        } else if (hasC[0]) {
            result = "c";
            System.out.println("DEBUG: 检测到C/C++特征，返回c");
        } else {
            result = "unknown";
            System.out.println("DEBUG: 未检测到任何已知项目类型特征，返回unknown");
        }

        return result;
    }
}
