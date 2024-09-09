package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Pack {
    private static final Logger LOGGER = LogManager.getLogger(Pack.class);
    private static void CopyFiles(File source, File target) {

        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdirs();
            }

            String[] files = source.list();
            for (String file : files) {
                File srcFile = new File(source, file);
                File destFile = new File(target, file);
                CopyFiles(srcFile, destFile);
            }
        } else {
            try {
                
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Copied: " + source.getAbsolutePath() + " to " + target.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("copying: " + source.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }

    private static void zipDirectory(String sourceDir, String baseDir, ZipOutputStream zos) throws IOException {
        File dir = new File(sourceDir);
        File[] files = dir.listFiles();
        if (files == null) return; // 确保文件列表不为空

        int totalFiles = countFiles(files); // 计算总文件数量
        int processedFiles = 0; // 已处理的文件数量

        for (String fileName : dir.list()) {
            File file = new File(sourceDir + File.separator + fileName);
            if (file.isDirectory()) {
                zipDirectory(file.getPath(), baseDir, zos);
            } else {
                ZipEntry zipEntry = new ZipEntry(file.getPath().substring(baseDir.length() + 1));
                zos.putNextEntry(zipEntry);

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }

                processedFiles++; // 更新已处理的文件数量
                int progressPercentage = (int) ((processedFiles * 100) / totalFiles);
                System.out.printf("\r%d%% %d / %d Files", progressPercentage, processedFiles, totalFiles);
            }
        }
        System.out.println(); // 换行以清晰显示最后的进度信息
    }

    // 计算文件数量的辅助方法
    private static int countFiles(File[] files) {
        int count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                count += countFiles(file.listFiles()); // 递归计算子目录中的文件
            } else {
                count++;
            }
        }
        return count;
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

    private static void delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();

            for (File f : files) {
                delete(f);
            }
        }

        file.delete();
    }

    private static String getFileSize(String filename) {
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

    private static void Pack(File SourceDirectory, File TempDirectory, File ZipDirectory) throws IOException {
        // Record the start time
        long startTime = System.nanoTime();

        CopyFiles(SourceDirectory, TempDirectory);
        String sourceDir = TempDirectory.toString();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        String zipFilePath = ZipDirectory.toString() + "\\backup-" + timestamp + ".zip";
        LOGGER.info("Now Packing...");

        try (FileOutputStream fos = new FileOutputStream(zipFilePath)) {
            try (ZipOutputStream zos = new ZipOutputStream(fos)) {
                zipDirectory(sourceDir, sourceDir, zos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        removeTemp(TempDirectory.toString());
        LOGGER.info("Backup completed: " + zipFilePath);

        // Record the end time and calculate the elapsed time
        long endTime = System.nanoTime();
        double durationInSeconds = (endTime - startTime) / 1_000_000_000.0; // Convert nanoseconds to seconds
        LOGGER.info("Backup process completed in: " + durationInSeconds + " seconds");
        LOGGER.info("size of backup file: " + getFileSize(zipFilePath));
        //Upload
        Upload.UploadBackup(new File(zipFilePath));
        LOGGER.info("====== ALL IS DONE ======\n");
    }

    public static void PackBackup(String sourceDir,String TempDirectory,String ZipDirectory) throws FileNotFoundException {
        try {
            Pack(new File(sourceDir), new File(TempDirectory), new File(ZipDirectory));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}