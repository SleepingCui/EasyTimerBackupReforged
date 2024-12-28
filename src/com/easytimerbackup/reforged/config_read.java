package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

public class config_read {
    private static final Logger LOGGER = LogManager.getLogger(config_read.class);
    private static final String CONFIG_FILE_PATH = "config.yaml";
    private static Map<String, Object> config;

    static {
        try (InputStream inputStream = config_read.class.getClassLoader().getResourceAsStream(CONFIG_FILE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Configuration file not found: " + CONFIG_FILE_PATH);
            }
            Yaml yaml = new Yaml();
            config = yaml.load(inputStream);
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration file.", e);
            throw new RuntimeException(e);
        }
    }

    public static String get_config(String key) {
        if (config == null || !config.containsKey(key)) {
            LOGGER.warn("Configuration key not found: " + key);
            return null;
        }

        // 检查上传或服务器模式是否启用
        Supplier<String> returnZeroIfDisabled = () -> "0";
        String uploadEnabledValue = (String) config.get("uploadenabled");
        String serverValue = (String) config.get("server");

        if ("uploadenabled".equals(key)) {
            return ("n".equals(uploadEnabledValue) || uploadEnabledValue == null)
                    ? returnZeroIfDisabled.get()
                    : (String) config.get(key);

        } else if ("server".equals(key)) {
            return ("n".equals(serverValue) || serverValue == null)
                    ? returnZeroIfDisabled.get()
                    : (String) config.get(key);

        } else if ("target_server_port".equals(key) || "delbackup".equals(key) || "server_port".equals(key) || "rev_path".equals(key)) {
            return ("n".equals(serverValue) || serverValue == null)
                    ? returnZeroIfDisabled.get()
                    : (String) config.get(key);
        }

        return (String) config.get(key);
    }
}
