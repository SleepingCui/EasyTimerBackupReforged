/*
EasyTimerBackupReforged
By SleepingCui https://github.com/SleepingCui/EasyTimerBackupReforged
*/
package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import static java.lang.System.exit;

public class Main {
    public static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static void main(String[] args) {
        Loginit.initLog(); // 加载log4j配置文件
        LOGGER.info("====== Loading EasyTimerBackupReforged ... ======");

        //检查配置文件
        config_write.ensureConfigFileExists();
        String isUpServerEnabled = config_read.get_config("server");

        if (isUpServerEnabled.equals("y")) {
            Server.UploadServer(); // Server
        }
        else if (isUpServerEnabled.equals("n") || isUpServerEnabled.equals("")) {
            Timer.timer_backup();
        }
        else {
            LOGGER.error("Invalid Launch Mode: " +isUpServerEnabled+" Please check your configuration file.");
            exit(1);
        }
    }
}
