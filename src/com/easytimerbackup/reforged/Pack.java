package com.easytimerbackup.reforged;

import com.diogonunes.jcolor.Attribute;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.diogonunes.jcolor.Ansi.colorize;


public class Pack {
    private static final Logger LOGGER = LogManager.getLogger(Pack.class);
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
        } else {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Copied: " + source.getAbsolutePath() + " to " + target.getAbsolutePath());
        }

        // 等待所有任务完成
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int totalFiles = 0;
    private static int totalFolders = 0;
    private static int processedFiles = 0;
    private static int processedFolders = 0;

    // 递归统计文件和文件夹数量
    private static void countFilesAndFolders(@NotNull File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                totalFolders++;
                countFilesAndFolders(file); // 递归统计子文件夹
            } else {
                totalFiles++;
            }
        }
    }

    // 压缩目录并显示进度
    private static void zipDirectory(String sourceDir, String baseDir, ZipOutputStream zos) throws IOException {
        File dir = new File(sourceDir);
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                processedFolders++; // 记录已处理的文件夹数量
                zipDirectory(file.getPath(), baseDir, zos); // 递归处理子文件夹
            } else {
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                    // 计算 zipEntry 的名称
                    String zipEntryName = file.getPath().substring(baseDir.length() + 1);
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zos.putNextEntry(zipEntry);

                    // 使用缓冲区写入文件
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = bis.read(buffer)) != -1) { // 修复读取时的判断
                        zos.write(buffer, 0, len);
                    }

                    zos.closeEntry();
                    processedFiles++; // 记录已处理的文件数量

                    // 计算综合进度
                    int totalProcessed = processedFiles + processedFolders;
                    int totalItems = totalFiles + totalFolders;
                    double overallProgress = (double) totalProcessed / totalItems * 100;

                    // 显示进度条
                    int barLength = 50; // 进度条的长度
                    int progressBars = (int) (overallProgress / 100 * barLength);
                    String progressBar = "[" + "=".repeat(progressBars) + " ".repeat(barLength - progressBars) + "]";

                    // 显示进度信息，并覆盖上次输出
                    System.out.printf("\r%s %.2f%% | Files: %d/%d | Folders: %d/%d",
                            progressBar, overallProgress, processedFiles, totalFiles, processedFolders, totalFolders);
                }
            }
        }
    }




    private static void removeTemp(String path) {
        File directory = new File(path);

        if (directory.exists()) {
            File[] files = directory.listFiles();

            for (File file : files) {
                delete(file);
            }

            directory.delete();

            LOGGER.info("All files and directories in " + path + " have been deleted.");
        } else {
            LOGGER.error("The directory does not exist.");
        }
    }

    private static void delete(@NotNull File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();

            for (File f : files) {
                delete(f);
            }
        }

        file.delete();
    }

    private static @NotNull String getFileSize(String filename) {
        File file = new File(filename);
        if (!file.exists() || !file.isFile()) {
            LOGGER.error("The file " + filename + " does not exist.");
            return "-1";
        }

        long sizeInBytes = file.length();
        if (sizeInBytes == 0) {
            return "0 Bytes";
        }

        DecimalFormat df = new DecimalFormat("#.00");
        String[] units = {"Bytes", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = sizeInBytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return df.format(size) + " " + units[unitIndex];
    }

    private static void Pack(File SourceDirectory, File TempDirectory, @NotNull File ZipDirectory) throws IOException {
        // 记录开始时间
        long startTime = System.nanoTime();

        // 复制文件到临时目录
        CopyFiles(SourceDirectory, TempDirectory);
        String sourceDir = TempDirectory.toString();

        // 获取当前时间戳用于命名 zip 文件
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        String zipFilePath = ZipDirectory.toString() + "\\backup-" + timestamp + ".zip";

        LOGGER.info("Now Packing...");

        // 统计源目录中的总文件和文件夹数量，用于显示进度
        totalFiles = 0;
        totalFolders = 0;
        processedFiles = 0;
        processedFolders = 0;
        countFilesAndFolders(TempDirectory);  // 统计文件和文件夹数量

        // 压缩临时目录内容到 zip 文件，并显示进度
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipDirectory(sourceDir, sourceDir, zos);  // 压缩目录，显示进度
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println();
        // 移除临时目录
        removeTemp(TempDirectory.toString());
        LOGGER.info("Backup completed: " + zipFilePath);

        // 记录结束时间并计算耗时
        long endTime = System.nanoTime();
        double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;  // 转换为秒
        LOGGER.info("Backup process completed in: " + colorize(Double.toString(durationInSeconds), Attribute.BRIGHT_YELLOW_TEXT()) + " seconds");

        // 显示压缩文件的大小
        LOGGER.info("Size of backup file: " + getFileSize(zipFilePath));

        // 上传备份文件
        Upload.UploadBackup(new File(zipFilePath));
        LOGGER.info(colorize("======== ALL IS DONE ========",Attribute.GREEN_TEXT()));
    }

    public static void PackBackup(String sourceDir,String TempDirectory,String ZipDirectory) throws FileNotFoundException {
        try {
            Pack(new File(sourceDir), new File(TempDirectory), new File(ZipDirectory));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}