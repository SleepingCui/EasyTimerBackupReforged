package com.easytimerbackup.reforged;

import com.diogonunes.jcolor.Attribute;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private static int processedFiles = 0; // 已处理文件数

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
                            processedFiles++; // Update processed files count but no progress shown
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

    // 使用 zip4j 压缩文件
    private static void zipDirectory(String sourceDir, String zipFilePath) {
        try {
            ZipFile zipFile = new ZipFile(zipFilePath);
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(CompressionLevel.NORMAL);

            File dir = new File(sourceDir);
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    LOGGER.debug(" Adding folder to zip: " + file.getAbsolutePath());
                    zipFile.addFolder(file, parameters);
                } else {
                    LOGGER.debug(" Adding file to zip: " + file.getAbsolutePath());
                    zipFile.addFile(file, parameters);
                }
                processedFiles++; // Update processed files count but no progress shown
            }
        } catch (ZipException e) {
            LOGGER.error("Error creating zip file", e);
            throw new RuntimeException(e);
        }
    }

    // 获取压缩文件大小
    private static double getFileSize(@NotNull File file) {
        long filesizeBytes = file.length(); // 获取文件大小，单位为字节
        double filesizeGB = (double) filesizeBytes / (1024 * 1024 * 1024); // 转换为GB
        return Math.round(filesizeGB * 100) / 100.0; // 保留两位小数
    }

    public static void Pack(String sourceDir, String tempDirectory, String zipDirectory) throws IOException {

        long startTime = System.nanoTime();

        // 转换输入路径为 File 对象
        File sourceDirectory = new File(sourceDir);
        File tempDir = new File(tempDirectory);
        File zipDir = new File(zipDirectory);

        LOGGER.info("SourceDirectory: " + sourceDirectory.getAbsolutePath());
        LOGGER.info("TempDirectory: " + tempDir.getAbsolutePath());
        LOGGER.info("ZipDirectory: " + zipDir.getAbsolutePath());

        LOGGER.debug("Backup Threads: " + bk_threads);
        try {
            // 复制源文件到临时目录
            LOGGER.info("Copying files to temporary directory...");
            CopyFiles(sourceDirectory, tempDir);

            // 获取文件总数
            totalFiles = 0;
            processedFiles = 0;
            countTotalFiles(tempDir);
            LOGGER.debug("Total files to process: " + totalFiles);

            // 获取当前时间戳用于命名 zip 文件
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
            String timestamp = LocalDateTime.now().format(formatter);
            String zipFilePath = zipDir + "\\backup-" + timestamp + ".zip";

            LOGGER.info("Now Packing...");

            // 使用 zip4j 压缩临时目录内容到 zip 文件
            zipDirectory(tempDir.toString(), zipFilePath);

            LOGGER.info("Backup completed: " + zipFilePath);

            // 记录结束时间并计算耗时
            long endTime = System.nanoTime();
            double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
            LOGGER.info(colorize("Backup process completed in: " + durationInSeconds + " seconds", Attribute.GREEN_TEXT()));

            // 显示文件大小
            LOGGER.info("Backup file size: " + getFileSize(sourceDirectory) + " GB");



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
