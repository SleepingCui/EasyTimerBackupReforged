package com.easytimerbackup.reforged;

import com.alibaba.fastjson.JSON;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class config_write {
    private static final Logger LOGGER = LogManager.getLogger(config_write.class);
    private static final String CONFIG_FILE = "config.json"; // Change to JSON file

    private static void writeConfig() {
        File configFile = new File(CONFIG_FILE);

        // JSON content as a Map
        Map<String, Object> config = new HashMap<>();


        // Setting up backup_time with hours, minutes, seconds
        Map<String, Integer> backupTime = new HashMap<>();
        backupTime.put("hours", 12);
        backupTime.put("minutes", 0);
        backupTime.put("seconds", 0);
        config.put("backup_time", backupTime);

        // Directory settings with empty paths
        Map<String, String> directorySettings = new HashMap<>();
        directorySettings.put("source_path", "");
        directorySettings.put("temp_path", "");
        directorySettings.put("backup_path", "");
        config.put("directory_settings", directorySettings);


        // Empty strings for fields that should not have a value
        config.put("enable_upload_function", false);
        config.put("enable_upload_server", false);
        config.put("receive_path", "");
        config.put("delete_backup_after_upload", false);
        config.put("enable_verify_md5", false);


        // Server configuration with port number and empty IP
        Map<String, Object> server = new HashMap<>();
        server.put("ip", "");
        server.put("port", 8000);
        config.put("server", server);

        // Backup Thread Config
        config.put("backup_threads",4);
        // Compression format Config
        config.put("compression_format","zip");

        // Convert the Map to JSON format and write to file
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            String jsonContent = JSON.toJSONString(config, true); // Pretty-print JSON
            writer.write(jsonContent);
            LOGGER.info("Configuration file created successfully.");
        } catch (IOException e) {
            LOGGER.error("Failed to write configuration file", e);
            throw new RuntimeException("Failed to write configuration file", e);
        }
    }

    public static void ensureConfigFileExists() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            LOGGER.info("Generating configuration file...");
            writeConfig();
            System.exit(0); // Exit after generating config to avoid further errors
        }
    }
}
