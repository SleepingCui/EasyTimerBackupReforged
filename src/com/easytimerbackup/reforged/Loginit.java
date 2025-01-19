package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Loginit {
    private static final Logger LOGGER = LogManager.getLogger(Loginit.class);
    private static final String log4j_config_str = """
logFolder = logs

log4j.rootLogger = debug,stdout,file1

log4j.appender.stdout = org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target = System.out
log4j.appender.stdout.layout = org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern = %-d{yyyy-MM-dd HH:mm:ss} %-5p %m%n

log4j.appender.file1 = org.apache.log4j.DailyRollingFileAppender
log4j.appender.file1.File = ${logFolder}/log.log
log4j.appender.file1.Append = true
log4j.appender.file1.Threshold = INFO
log4j.appender.file1.layout = org.apache.log4j.PatternLayout
log4j.appender.file1.layout.ConversionPattern = %-d{yyyy-MM-dd HH:mm:ss} %p %m%n""";

    public static void initLog() {
        FileInputStream fileInputStream = null;
        try {
            File logFile = new File("log4j.properties");
            if (!logFile.exists()) {
                try (FileOutputStream fos = new FileOutputStream(logFile)) {
                    fos.write(log4j_config_str.getBytes());
                } catch (IOException e) {
                    System.out.println("Unable to write log4j properties.");
                    throw new RuntimeException(e);
                }
            }

            // 加载log4j配置文件
            fileInputStream = new FileInputStream(logFile);
            Properties properties = new Properties();
            properties.load(fileInputStream);
            PropertyConfigurator.configure(properties);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
}