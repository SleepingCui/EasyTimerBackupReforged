package com.easytimerbackup.reforged;

import com.diogonunes.jcolor.Attribute;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.diogonunes.jcolor.Ansi.colorize;

public class Pack {
    private static final Logger LOGGER = LogManager.getLogger(Pack.class);
    private static final String bk_threads = config_read.get_config("backup_threads"); // 线程数
    private static final int threadCount;
    private static final int DefaultBackupThreads = 4;   // 默认线程数


    static {
        int tempThreadCount;
        try {
            tempThreadCount = Integer.parseInt(Objects.requireNonNull(bk_threads));
            if (tempThreadCount <= 0) { // 检查解析后的值是否为0或负数
                tempThreadCount = DefaultBackupThreads;
            }
        } catch (NumberFormatException e) {
            tempThreadCount = DefaultBackupThreads; // 如果解析失败保持默认值
        }

        threadCount = tempThreadCount;
    }

    private static final ExecutorService executor = Executors.newFixedThreadPool(threadCount); // 线程池

    private static int totalFiles = 0; // 文件总数
    private static final AtomicInteger processedFiles = new AtomicInteger(); // 已处理文件数

    // 递归统计文件总数
    private static void countTotalFiles(File dir) {
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                countTotalFiles(file);
            } else {
                totalFiles++;
            }
        }
    }
    // 复制文件和文件夹
    private static void CopyFiles(@NotNull File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdirs();
                LOGGER.debug("  Created directory: " + target.getAbsolutePath());
            }

            File[] files = source.listFiles();
            if (files == null) {
                LOGGER.error("Failed to list files in: " + source.getAbsolutePath());
                return;
            }

            for (File file : files) {
                File destFile = new File(target, file.getName());
                executor.submit(() -> {
                    try {
                        if (file.isDirectory()) {
                            LOGGER.debug("  Recursively copying directory: " + file.getAbsolutePath());
                            CopyFiles(file, destFile);
                        } else {
                            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            LOGGER.info("Copied file: " + file.getAbsolutePath() + " to " + destFile.getAbsolutePath());
                        }
                        synchronized (Pack.class) {
                            processedFiles.getAndIncrement(); // Update processed files count but no progress shown
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error copying: " + file.getAbsolutePath(), e);
                    }
                });
            }
        } else {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Copied file: " + source.getAbsolutePath() + " to " + target.getAbsolutePath());
        }
    }

    // 通用压缩方法，支持 ZIP 或 TAR.GZ
    public static void Compress(String sourceDir, String outPath, String kind) {
        File dir = new File(sourceDir);
        try (OutputStream fileOut = new FileOutputStream(outPath)) {
            if ("zip".equalsIgnoreCase(kind)) {
                // Create ZIP file
                try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(fileOut)) {
                    zipOut.setMethod(ZipArchiveOutputStream.DEFLATED); // Use DEFLATE compression for ZIP
                    zipOut.setLevel(6); // Compression level (6 is normal)
                    addFilesToArchive(dir, dir.getAbsolutePath(), zipOut, kind);
                }
            } else if ("targz".equalsIgnoreCase(kind)) {
                // Create TAR.GZ file
                try (GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(fileOut);
                     TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzOut)) {
                    tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU); // Handle long file names
                    addFilesToArchive(dir, dir.getAbsolutePath(), tarOut, kind);
                }
            } else {
                throw new IllegalArgumentException("Unsupported kind: " + kind);
            }
        } catch (IOException e) {
            LOGGER.error("Error creating " + kind + " file", e);
            throw new RuntimeException(e);
        }
    }
    //通用复制方法

    // 将文件或文件夹添加到压缩包中
    private static void addFilesToArchive(File fileToAdd, String baseDirPath, OutputStream outStream, String kind) throws IOException {
        if ("zip".equalsIgnoreCase(kind)) {
            ZipArchiveOutputStream zipOut = (ZipArchiveOutputStream) outStream;
            if (fileToAdd.isDirectory()) {
                for (File file : Objects.requireNonNull(fileToAdd.listFiles())) {
                    addFilesToArchive(file, baseDirPath, zipOut, kind);
                }
            } else {
                String relativePath = fileToAdd.getAbsolutePath().substring(baseDirPath.length() + 1);
                ZipArchiveEntry entry = new ZipArchiveEntry(fileToAdd, relativePath);
                zipOut.putArchiveEntry(entry);
                try (FileInputStream fileInputStream = new FileInputStream(fileToAdd);
                     BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, bytesRead);
                    }
                }
                zipOut.closeArchiveEntry();
                processedFiles.getAndIncrement();
                LOGGER.debug("Adding file to zip: " + fileToAdd.getAbsolutePath());
            }
        } else if ("targz".equalsIgnoreCase(kind)) {
            TarArchiveOutputStream tarOut = (TarArchiveOutputStream) outStream;
            if (fileToAdd.isDirectory()) {
                for (File file : Objects.requireNonNull(fileToAdd.listFiles())) {
                    addFilesToArchive(file, baseDirPath, tarOut, kind);
                }
            } else {
                String relativePath = fileToAdd.getAbsolutePath().substring(baseDirPath.length() + 1);
                TarArchiveEntry entry = new TarArchiveEntry(fileToAdd, relativePath);
                tarOut.putArchiveEntry(entry);
                try (FileInputStream fileInputStream = new FileInputStream(fileToAdd);
                     BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                        tarOut.write(buffer, 0, bytesRead);
                    }
                }
                tarOut.closeArchiveEntry();
                processedFiles.getAndIncrement();
                LOGGER.debug("Adding file to tar.gz: " + fileToAdd.getAbsolutePath());
            }
        }
    }




    // 获取压缩文件大小
    private static double getFileSize(@NotNull File file) {
        long filesizeBytes = file.length(); // 获取文件大小，单位为字节
        double filesizeGB = (double) filesizeBytes / (1024 * 1024 * 1024); // 转换为GB
        return Math.round(filesizeGB * 100) / 100.0; // 保留两位小数
    }

    // 获取当前时间戳用于命名压缩文件
    private static String NameOutFile(File OutDir) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        if ((Objects.equals(config_read.get_config("compression_format"), "zip"))) {
            return OutDir + "\\backup-" + timestamp + ".zip";
        } else {
            return OutDir + "\\backup-" + timestamp + ".tar.gz";
        }
    }

    public static void Pack(String sourceDir, String tempDirectory, String OutDirectory) throws IOException {

        long startTime = System.nanoTime();

        // 转换输入路径为 File 对象
        File sourceDirectory = new File(sourceDir);
        File tempDir = new File(tempDirectory);
        File OutDir = new File(OutDirectory);

        LOGGER.info("SourceDirectory: " + sourceDirectory.getAbsolutePath());
        LOGGER.info("TempDirectory: " + tempDir.getAbsolutePath());
        LOGGER.info("OutputDirectory: " + OutDir.getAbsolutePath());

        LOGGER.debug("Backup Threads: " + bk_threads);
        try {
            // 复制源文件到临时目录
            LOGGER.info("Copying files to temporary directory...");
            CopyFiles(sourceDirectory, tempDir);

            // 获取文件总数
            totalFiles = 0;
            processedFiles.set(0);
            countTotalFiles(tempDir);
            LOGGER.debug("Total files to process: " + totalFiles);

            LOGGER.info("Now Packing...");
            // 压缩临时目录内容到压缩文件
            String OutputFileDir = NameOutFile(OutDir);
            if (Objects.equals(config_read.get_config("compression_format"), "zip")) {
                Compress(tempDir.toString(), OutputFileDir, "zip");
            }
            else {
                Compress(sourceDirectory.toString(), OutputFileDir, "tar.gz");
            }

            LOGGER.info("Backup completed: " +  OutputFileDir);

            // 记录结束时间并计算耗时
            long endTime = System.nanoTime();
            double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
            LOGGER.info(colorize("Backup process completed in: " + durationInSeconds + " seconds", Attribute.GREEN_TEXT()));

            // 显示文件大小
            LOGGER.info("Backup file size: " + getFileSize(sourceDirectory) + " GB");

            // 上传
            LOGGER.info("Uploading backup...");
            Upload.UploadBackup(new File(OutputFileDir));


        } catch (IOException e) {
            LOGGER.error("Backup process failed", e);
            throw new RuntimeException(e);
        }
    }


    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }));
    }


}