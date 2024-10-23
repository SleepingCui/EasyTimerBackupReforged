package com.easytimerbackup.reforged;

import com.diogonunes.jcolor.Attribute;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
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

    // 使用 Apache Commons Compress 压缩文件
    private static void zipDirectory(String rootDir, String sourceDir, ZipArchiveOutputStream zos) throws IOException {
        File dir = new File(sourceDir);
        for (File file : dir.listFiles()) {
            String entryName = file.getAbsolutePath().substring(rootDir.length() + 1);
            if (file.isDirectory()) {
                zipDirectory(rootDir, file.getAbsolutePath(), zos);  // 递归处理子目录
            } else {
                // 确保这里使用的是具体的 ZipArchiveEntry
                ZipArchiveEntry entry = new ZipArchiveEntry(file, entryName);
                zos.putArchiveEntry(entry);

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);  // 写入压缩文件
                    }
                }

                zos.closeArchiveEntry();  // 关闭当前条目
                processedFiles++;  // 更新已处理的文件数
                showProgress();  // 更新进度条
            }
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

        // 压缩临时目录内容到 zip 文件
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipArchiveOutputStream zos = new ZipArchiveOutputStream(fos)) {
            zipDirectory(sourceDir, sourceDir, zos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(); // 压缩完成，换行
        LOGGER.info("Backup completed: " + zipFilePath);

        // 记录结束时间并计算耗时
        long endTime = System.nanoTime();
        double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        LOGGER.info(colorize("Backup process completed in: " + durationInSeconds + " seconds", Attribute.GREEN_TEXT()));

        //上传
        //LOGGER.debug(zipFilePath);
        Upload.UploadBackup(new File(zipFilePath));
    }

    public static void PackBackup(String sourceDir, String TempDirectory, String ZipDirectory) throws FileNotFoundException {
        try {
            Pack(new File(sourceDir), new File(TempDirectory), new File(ZipDirectory));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
