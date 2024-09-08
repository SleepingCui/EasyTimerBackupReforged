package com.easytimerbackup.reforged;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Timer {
    private static final Logger LOGGER = LogManager.getLogger(Timer.class);
    public static void timer_backup() {
        String read_hour = config_read.get_config("time_hour");
        String read_minute = config_read.get_config("time_minute");
        String read_second = config_read.get_config("time_second");
        String read_source_path = config_read.get_config("source_dir");
        String read_temp_path = config_read.get_config("temp_dir");
        String read_backup_path = config_read.get_config("backup_dir");

        LOGGER.info("Starting backup...");
        LOGGER.info("Backup Time: " + read_hour + ":" + read_minute + ":" + read_second);
        LOGGER.info("SourceDirectory: " + read_source_path);
        LOGGER.info("TempDirectory: " + read_temp_path);
        LOGGER.info("ZipDirectory: " + read_backup_path);



        //Checkout Upload
        String upload_enabled = config_read.get_config("uploadenabled");
        if (upload_enabled.equals("y")) {
            int port = Integer.parseInt(config_read.get_config("server_port"));
            String ip = config_read.get_config("server_ip");
            LOGGER.info("Upload mode was enabled!");
            LOGGER.info("Server address: " + ip + ":"+port);
        }
        else {
            LOGGER.info("Upload mode was disabled!");
        }

        // 创建一个调度执行器
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // 获取当前时间
        Calendar now = Calendar.getInstance();
        Calendar nextBackup = Calendar.getInstance();
        nextBackup.set(Calendar.HOUR_OF_DAY, Integer.parseInt(read_hour));
        nextBackup.set(Calendar.MINUTE, Integer.parseInt(read_minute));
        nextBackup.set(Calendar.SECOND, Integer.parseInt(read_second));

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
                Pack.PackBackup(read_source_path, read_temp_path, read_backup_path);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, initialDelay, period, TimeUnit.MILLISECONDS); // 确保使用毫秒作为单位



    }
}