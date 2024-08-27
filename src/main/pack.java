package main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class pack {
    static Logger logger = Logger.getLogger(pack.class.getName());
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
                logger.info("INFO Copied: " + source.getAbsolutePath() + " to " + target.getAbsolutePath());
            } catch (IOException e) {
                logger.info("Error copying: " + source.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }

    private static void zipDirectory(String sourceDir, String baseDir, ZipOutputStream zos) throws IOException {
        File dir = new File(sourceDir);
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

            logger.info("INFO All files and directories in " + path + " have been deleted.");
        } else {
            logger.info("ERROR The directory does not exist.");
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
    private static void Pack(File SourceDirectory,File TempDirectory,File ZipDirectory) throws FileNotFoundException {

        CopyFiles(SourceDirectory, TempDirectory);
        String sourceDir = TempDirectory.toString();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        String zipFilePath = ZipDirectory.toString() + "backup-" + timestamp + ".zip";
        logger.info("INFO ZipDirectory: " + zipFilePath);
        logger.info("INFO SourceDirectory: " + sourceDir);
        logger.info("INFO TempDirectory: " + TempDirectory);

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
        logger.info("INFO Backup completed: " + zipFilePath);
    }

    public static void PackBackup(String sourceDir,String TempDirectory,String ZipDirectory) throws FileNotFoundException {
        try{
            Pack(new File(sourceDir), new File(TempDirectory), new File(ZipDirectory));
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }

}