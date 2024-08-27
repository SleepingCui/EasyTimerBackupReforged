package main;

import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class timer {
    static Logger logger = Logger.getLogger(timer.class.getName());

    public static void timer_backup() {
        String read_hour = config_read.get_config("time_hour");
        String read_minute = config_read.get_config("time_minute");
        String read_second = config_read.get_config("time_second");
        String read_source_path = config_read.get_config("source_dir");
        String read_temp_path = config_read.get_config("temp_dir");
        String read_backup_path = config_read.get_config("backup_dir");

        logger.info("Starting backup...");

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
        long period = 24 * 60 * 60; // 每24小时执行一次

        // 安排任务
        scheduler.scheduleAtFixedRate(() -> {
            logger.info("Backup Start!");
            try {
                pack.PackBackup(read_source_path, read_temp_path, read_backup_path);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }, initialDelay, period, TimeUnit.SECONDS);
    }
}