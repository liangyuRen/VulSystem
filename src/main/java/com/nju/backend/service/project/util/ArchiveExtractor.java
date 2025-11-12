package com.nju.backend.service.project.util;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 统一的压缩文件解压工具类
 * 支持: ZIP, TAR, TAR.GZ, GZ
 */
public class ArchiveExtractor {

    /**
     * 检测文件格式
     */
    public static String detectArchiveFormat(File file) throws IOException {
        if (file.length() < 6) {
            return "unknown";
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[6];
            int bytesRead = fis.read(header);

            if (bytesRead < 2) {
                return "unknown";
            }

            // ZIP: 0x50 0x4B (PK)
            if (header[0] == (byte)0x50 && header[1] == (byte)0x4B) {
                return "zip";
            }

            // GZIP: 0x1F 0x8B
            if (header[0] == (byte)0x1F && header[1] == (byte)0x8B) {
                return "gzip";
            }

            // TAR: 检查ustar标识 (offset 257)
            if (file.length() >= 262) {
                fis.close();
                try (FileInputStream fis2 = new FileInputStream(file)) {
                    fis2.skip(257);
                    byte[] tarHeader = new byte[5];
                    if (fis2.read(tarHeader) == 5) {
                        String ustar = new String(tarHeader, "US-ASCII");
                        if (ustar.equals("ustar")) {
                            return "tar";
                        }
                    }
                }
            }

            return "unknown";
        }
    }

    /**
     * 统一解压接口
     */
    public static void extract(File archiveFile, File destDir) throws IOException {
        String format = detectArchiveFormat(archiveFile);
        System.out.println("检测到压缩格式: " + format);

        switch (format) {
            case "zip":
                extractZip(archiveFile, destDir);
                break;
            case "tar":
                extractTar(archiveFile, destDir);
                break;
            case "gzip":
                // GZIP可能是.tar.gz或单独的.gz文件
                extractGzip(archiveFile, destDir);
                break;
            default:
                throw new IOException("不支持的压缩格式: " + format + "。目前支持 ZIP、TAR、TAR.GZ 格式");
        }
    }

    /**
     * 解压ZIP文件
     */
    private static void extractZip(File zipFile, File destDir) throws IOException {
        System.out.println("使用ZIP解压器");

        try (ZipFile zf = new ZipFile(zipFile, java.nio.charset.Charset.forName("GBK"))) {
            java.util.Enumeration<? extends ZipEntry> entries = zf.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                extractEntry(zf.getInputStream(entry), entry.getName(), entry.isDirectory(), destDir);
            }
        } catch (IOException e) {
            // GBK失败，尝试UTF-8
            try (ZipFile zf = new ZipFile(zipFile, java.nio.charset.Charset.forName("UTF-8"))) {
                java.util.Enumeration<? extends ZipEntry> entries = zf.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    extractEntry(zf.getInputStream(entry), entry.getName(), entry.isDirectory(), destDir);
                }
            }
        }
    }

    /**
     * 解压TAR文件
     */
    private static void extractTar(File tarFile, File destDir) throws IOException {
        System.out.println("使用TAR解压器");

        try (FileInputStream fis = new FileInputStream(tarFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             TarArchiveInputStream tais = new TarArchiveInputStream(bis)) {

            ArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                extractEntry(tais, entry.getName(), entry.isDirectory(), destDir);
            }
        }
    }

    /**
     * 解压GZIP文件 (通常是.tar.gz)
     */
    private static void extractGzip(File gzFile, File destDir) throws IOException {
        System.out.println("使用GZIP解压器");

        try (FileInputStream fis = new FileInputStream(gzFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis)) {

            // 检查解压后是否是TAR文件
            byte[] header = new byte[262];
            gzis.mark(300);
            int bytesRead = gzis.read(header);
            gzis.reset();

            boolean isTar = false;
            if (bytesRead >= 262) {
                String ustar = new String(header, 257, 5, "US-ASCII");
                isTar = ustar.equals("ustar");
            }

            if (isTar) {
                System.out.println("检测到TAR.GZ格式");
                try (TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
                    ArchiveEntry entry;
                    while ((entry = tais.getNextEntry()) != null) {
                        extractEntry(tais, entry.getName(), entry.isDirectory(), destDir);
                    }
                }
            } else {
                // 单独的GZ文件
                String fileName = gzFile.getName();
                if (fileName.endsWith(".gz")) {
                    fileName = fileName.substring(0, fileName.length() - 3);
                }
                File outFile = new File(destDir, fileName);
                ensureParentDir(outFile);

                try (FileOutputStream fos = new FileOutputStream(outFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = gzis.read(buffer)) > 0) {
                        bos.write(buffer, 0, len);
                    }
                }
            }
        }
    }

    /**
     * 提取单个条目（文件或目录）
     */
    private static void extractEntry(InputStream inputStream, String entryName,
                                     boolean isDirectory, File destDir) throws IOException {
        // 路径安全检查
        entryName = entryName.replace('/', File.separatorChar);
        File targetFile = new File(destDir, entryName);

        String canonicalDestPath = destDir.getCanonicalPath();
        String canonicalFilePath = targetFile.getCanonicalPath();

        if (!canonicalFilePath.startsWith(canonicalDestPath + File.separator)
            && !canonicalFilePath.equals(canonicalDestPath)) {
            System.out.println("跳过危险路径: " + entryName);
            return;
        }

        if (isDirectory) {
            targetFile.mkdirs();
        } else {
            ensureParentDir(targetFile);

            try (FileOutputStream fos = new FileOutputStream(targetFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    bos.write(buffer, 0, len);
                }
            }
            System.out.println("解压文件: " + entryName);
        }
    }

    /**
     * 确保父目录存在
     */
    private static void ensureParentDir(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }
}
