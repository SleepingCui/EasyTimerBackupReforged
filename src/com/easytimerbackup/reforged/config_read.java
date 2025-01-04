package com.easytimerbackup.reforged;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class config_read {
    private static final Logger LOGGER = LogManager.getLogger(config_read.class);
    private static final String CONFIG_FILE = "config.json";  // Target configuration file
    private static JSONObject config;

    static {
        try {
            // Get the current working directory
            String userDir = System.getProperty("user.dir");
            LOGGER.info("Current working directory: " + userDir);

            // Configuration file path
            File configFile = new File(userDir, CONFIG_FILE);
            if (!configFile.exists()) {
                LOGGER.error("Configuration file not found: " + configFile.getAbsolutePath());
                throw new IllegalStateException("Configuration file not found: " + CONFIG_FILE);
            }

            // Read the file
            try (FileInputStream inputStream = new FileInputStream(configFile)) {
                config = JSON.parseObject(inputStream, JSONObject.class);
                LOGGER.info("Configuration file loaded successfully: " + configFile.getAbsolutePath());
            }

        } catch (IOException e) {
            LOGGER.error("Failed to load configuration file", e);
            throw new RuntimeException("Failed to load configuration file", e);
        }
    }

    public static String get_config(String key) {
        if (config == null) {
            LOGGER.warn("Configuration not loaded");
            return null;
        }

        // Handle nested fields for backup_time
        if (key.equals("backup_time.hours") || key.equals("backup_time.minutes") || key.equals("backup_time.seconds")) {
            JSONObject backupTime = config.getJSONObject("backup_time");
            if (backupTime != null) {
                if (key.equals("backup_time.hours")) {
                    return backupTime.getString("hours");
                } else if (key.equals("backup_time.minutes")) {
                    return backupTime.getString("minutes");
                } else if (key.equals("backup_time.seconds")) {
                    return backupTime.getString("seconds");
                }
            }
        }

        // Handle nested fields for directory_settings
        if (key.equals("directory_settings.source_path") ||
                key.equals("directory_settings.temp_path") ||
                key.equals("directory_settings.backup_path")) {

            JSONObject directorySettings = config.getJSONObject("directory_settings");
            if (directorySettings != null) {
                if (key.equals("directory_settings.source_path")) {
                    return directorySettings.getString("source_path");
                } else if (key.equals("directory_settings.temp_path")) {
                    return directorySettings.getString("temp_path");
                } else if (key.equals("directory_settings.backup_path")) {
                    return directorySettings.getString("backup_path");
                }
            }
        }

        // Handle nested fields for server

        if (key.equals("server.port") || key.equals("server.ip")) {
            JSONObject serverSettings = config.getJSONObject("server");
            if (serverSettings != null) {
                if (key.equals("server.port")) {
                    return serverSettings.getString("port");
                } else if (key.equals("server.ip")) {
                    return serverSettings.getString("ip");
                }
            }
        }



        // Handle other simple key-value pairs
        if (config.containsKey(key)) {
            return config.getString(key);
        }

        LOGGER.warn("Configuration item not found: " + key);
        return null;
    }
}
