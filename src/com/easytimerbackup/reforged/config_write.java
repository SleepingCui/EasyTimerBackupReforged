package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class config_write {
    private static final Logger LOGGER = LogManager.getLogger(config_write.class);
    private static final String CONFIG_FILE = "config.yml";

    private static void writeConfig() {
        File configFile = new File(CONFIG_FILE);

        // YAML content as a Map
        Map<String, Object> config = new HashMap<>();

        config.put("version", "v0.0.1");

        config.put("backup_time", Map.of(
                "hours", 11,
                "minutes", 45,
                "seconds", 14
        ));

        config.put("directory_settings", Map.of(
                "source_path", "C:\\SourceFolder\\",
                "temp_path", "C:\\BackupTemp\\",
                "backup_path", "C:\\BackupFolder\\"
        ));

        config.put("upload_function", "y");
        config.put("server", Map.of(
                "ip", "127.0.0.1",
                "port", 8080
        ));

        config.put("delete_backup_after_upload", "y");
        config.put("verify_md5", "y");
        config.put("upload_server_enabled", "y");
        config.put("receive_path", "C:\\ReceiveFolder\\");

        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(options);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), "utf-8")) {
            yaml.dump(config, writer);
            LOGGER.info("Configuration file created successfully.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to write configuration file", e);
        }
    }

    public static void ensureConfigFileExists() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            LOGGER.info("Generating configuration file...");
            writeConfig();
            System.exit(0);
        }
    }


}
