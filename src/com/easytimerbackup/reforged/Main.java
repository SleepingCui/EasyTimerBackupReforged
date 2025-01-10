/*
EasyTimerBackupReforged
By SleepingCui https://github.com/SleepingCui/EasyTimerBackupReforged
*/


//   ***DEV VER***
package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import static java.lang.System.exit;

public class Main {
    public static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static void main(String[] args) {
        Loginit.initLog(); // 加载log4j配置文件

        LOGGER.info("====== Loading EasyTimerBackupReforged ... ======");
        LOGGER.info("You are using DEV version! \nVarious bugs can arise. To use the stable release, go to the GitHub page to download the stable release.");

        //检查配置文件
        config_write.ensureConfigFileExists();
        String isUpServerEnabled = config_read.get_config("upload_server_enabled");

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
