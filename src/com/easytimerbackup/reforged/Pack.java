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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.diogonunes.jcolor.Ansi.colorize;

public class Pack {
    private static final Logger LOGGER = LogManager.getLogger(Pack.class);
    private static int totalFiles = 0; // 文件总数
    private static int processedFiles = 0; // 已处理文件数

    // 递归统计文件总数
    private static void countTotalFiles(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                countTotalFiles(file);
            } else {
                totalFiles++;
            }
        }
    }

    // 显示进度条
    private static void showProgress() {
        int progressPercentage = (int) ((double) processedFiles / totalFiles * 100);
        int progressBars = (progressPercentage / 5); // 每5%一个进度条
        StringBuilder progressBar = new StringBuilder("[");
        for (int i = 0; i < 20; i++) {
            if (i < progressBars) {
                progressBar.append("=");
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("]");
        System.out.print(String.format("\r%d%% %d/%d Files %s", progressPercentage, processedFiles, totalFiles, progressBar));
    }

    // 复制文件和文件夹
    private static void CopyFiles(@NotNull File source, File target) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(4); // 创建 4 个线程池

        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdirs();
            }

            String[] files = source.list();
            for (String file : files) {
                File srcFile = new File(source, file);
                File destFile = new File(target, file);

                executor.submit(() -> {
                    try {
                        if (srcFile.isDirectory()) {
                            CopyFiles(srcFile, destFile); // 递归复制文件夹
                        } else {
                            Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            LOGGER.info("Copied: " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error copying: " + srcFile.getAbsolutePath(), e);
                    }
                });
            }

            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Copied: " + source.getAbsolutePath() + " to " + target.getAbsolutePath());
        }
    }

    // 使用 zip4j 压缩文件
    private static void zipDirectoryWithZip4j(String sourceDir, String zipFilePath) {
        try {
            ZipFile zipFile = new ZipFile(zipFilePath);
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(CompressionLevel.NORMAL);

            File dir = new File(sourceDir);
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    zipFile.addFolder(file, parameters);
                } else {
                    zipFile.addFile(file, parameters);
                }
                processedFiles++;
                showProgress();
            }
        } catch (ZipException e) {
            LOGGER.error("Error creating zip file", e);
            throw new RuntimeException(e);
        }
    }

    public static void Pack(File SourceDirectory, File TempDirectory, File ZipDirectory) throws IOException {
        long startTime = System.nanoTime();

        // 复制源文件到临时目录
        CopyFiles(SourceDirectory, TempDirectory);
        String sourceDir = TempDirectory.toString();

        // 获取文件总数
        totalFiles = 0;
        processedFiles = 0;
        countTotalFiles(TempDirectory);

        // 获取当前时间戳用于命名 zip 文件
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        String zipFilePath = ZipDirectory.toString() + "\\backup-" + timestamp + ".zip";

        LOGGER.info("Now Packing...");

        // 使用 zip4j 压缩临时目录内容到 zip 文件
        zipDirectoryWithZip4j(sourceDir, zipFilePath);

        System.out.println(); // 压缩完成，换行
        LOGGER.info("Backup completed: " + zipFilePath);

        // 记录结束时间并计算耗时
        long endTime = System.nanoTime();
        double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        LOGGER.info(colorize("Backup process completed in: " + durationInSeconds + " seconds", Attribute.GREEN_TEXT()));

        // 上传
        Upload.UploadBackup(new File(zipFilePath));
    }

    public static void PackBackup(String sourceDir, String TempDirectory, String ZipDirectory) throws IOException {
        try {
            Pack(new File(sourceDir), new File(TempDirectory), new File(ZipDirectory));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
