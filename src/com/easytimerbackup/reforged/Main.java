package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Main {
    public static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static void main(String[] args) {
        Loginit.initLog();

        LOGGER.info("====== Loading EasyTimerBackupReforged ... ======");

        //检查配置文件
        config_write.ensureConfigFileExists();

        String isUpServerEnabled = config_read.get_config("server");
        if (isUpServerEnabled.equals("y")) {
            Server.UploadServer(); // Server
        }
        else {
            Timer.timer_backup();
        }
    }
}
