package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Main {
    public static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static void main(String[] args) {
        Loginit.initLog();
        LOGGER.info("====== Loading EasyTimerBackupReforged ... ======");
        Timer.timer_backup();
    }
}
