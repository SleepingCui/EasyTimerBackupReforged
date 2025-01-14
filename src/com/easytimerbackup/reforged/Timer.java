package com.easytimerbackup.reforged;


import com.diogonunes.jcolor.Attribute;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.diogonunes.jcolor.Ansi.colorize;
import static java.lang.System.exit;


public class Timer {
    private static final Logger LOGGER = LogManager.getLogger(Timer.class);

    private static boolean isValidPath(String path) {   // 辅助方法：检查路径是否有效
        return path != null && !path.trim().isEmpty();
    }

    public static void timer_backup() {
        // 从配置中读取时间
        String read_hour = config_read.get_config("backup_time.hours");
        String read_minute = config_read.get_config("backup_time.minutes");
        String read_second = config_read.get_config("backup_time.seconds");

        // 验证时间是否合法
        int hour=0, minute=0, second=0;
        try {
            hour = Integer.parseInt(read_hour);
            minute = Integer.parseInt(read_minute);
            second = Integer.parseInt(read_second);

            if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
                throw new IllegalArgumentException("Invalid time provided!");
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(" Invalid time configuration: " + e.getMessage());
            exit(1);
            //throw new RuntimeException("Invalid time configuration: " + e.getMessage());
        }

        String read_source_path = config_read.get_config("directory_settings.source_path");
        String read_temp_path = config_read.get_config("directory_settings.temp_path");
        String read_backup_path = config_read.get_config("directory_settings.backup_path");
        String read_compression_format = config_read.get_config("compression_format");
        String colored_time = colorize(read_hour + ":" + read_minute + ":" + read_second, Attribute.CYAN_TEXT());



        // 检查路径合法性
        if (!isValidPath(read_backup_path)) {
            LOGGER.error("Invalid backup path provided: " + read_backup_path + " Please provide a valid backup path.");
            exit(1);
        } else if (!isValidPath(read_source_path)) {
            LOGGER.error("Invalid source path provided: " + read_source_path + " Please provide a valid source path.");
            exit(1);
        } else if (read_temp_path == null) {
            LOGGER.error("Invalid temporary path provided: " + read_temp_path + " Please provide a valid temporary path.");
            exit(1);
        }
        //检查文件格式合法性
        else if (!Objects.equals(read_compression_format, "zip") && !Objects.equals(read_compression_format, "targz")) {
            LOGGER.error("Invalid compression format provided: " + read_compression_format +". Please provide 'zip' or 'targz'");
            exit(1);
        }


        LOGGER.info("Backup Time: " + colored_time);
        LOGGER.info("SourceDirectory: " + read_source_path);
        LOGGER.info("TempDirectory: " + read_temp_path);
        LOGGER.info("OutputDirectory: " + read_backup_path);
        LOGGER.info("CompressionFormat: " + read_compression_format);

        // 检查上传是否启用
        String upload_enabled = config_read.get_config("upload_function");
        if (upload_enabled.equals("y")) {
            int port = Integer.parseInt(config_read.get_config("server.port"));
            String ip = config_read.get_config("server.ip");
            LOGGER.info("Upload mode was enabled!");
            LOGGER.info("Server address: " + ip + ":" + port);
        } else {
            LOGGER.info("Upload mode was disabled!");
        }

        // 创建一个调度执行器
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // 获取当前时间
        Calendar now = Calendar.getInstance();
        Calendar nextBackup = Calendar.getInstance();
        nextBackup.set(Calendar.HOUR_OF_DAY, hour);
        nextBackup.set(Calendar.MINUTE, minute);
        nextBackup.set(Calendar.SECOND, second);

        // 如果设定的时间已经过去，则设置为明天的同一时间
        if (nextBackup.getTimeInMillis() < now.getTimeInMillis()) {
            nextBackup.add(Calendar.DAY_OF_MONTH, 1);
        }

        long initialDelay = nextBackup.getTimeInMillis() - now.getTimeInMillis();
        long period = 24 * 60 * 60 * 1000; // 每24小时执行一次，单位为毫秒

        // 安排任务
        scheduler.scheduleAtFixedRate(() -> {
            LOGGER.info("====== Backup Start! ======");
            try {
                Pack.Pack(read_source_path, read_temp_path, read_backup_path);
            } catch (IOException e) {
                LOGGER.error("Backup failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, initialDelay, period, TimeUnit.MILLISECONDS); // 确保使用毫秒作为单位
    }
}
