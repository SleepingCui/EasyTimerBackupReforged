package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class config_read {
    private static final Logger LOGGER = LogManager.getLogger(config_read.class);
    @SuppressWarnings("CallToPrintStackTrace")
    private static String f_read_config(int lineToRead, String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                if (lineNumber == lineToRead) {
                    return line;
                }
                lineNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    public static String get_config(String target) {
        config_write.isFileExists();
        String config_file_path = "config.cfg";

        // Mapping of targets to configuration line numbers
        Map<String, Integer> targetMap = new HashMap<>();
        targetMap.put("time_hour", 17);
        targetMap.put("time_minute", 18);
        targetMap.put("time_second", 19);
        targetMap.put("source_dir", 32);
        targetMap.put("temp_dir", 33);
        targetMap.put("backup_dir", 34);
        targetMap.put("uploadenabled",40);
        targetMap.put("server_ip",45);
        targetMap.put("server_port",48);
        targetMap.put("delbackup",53);

        Integer lineNumber = targetMap.get(target);

        // If the target is not recognized, return null
        if (lineNumber == null) {
            return null;
        }

        String readed_data = f_read_config(lineNumber, config_file_path);

        // Check if readed_data is empty
        if (readed_data == null || readed_data.isEmpty()) {
            LOGGER.error("The read data is empty!");
            System.exit(1); // Terminate the program
        }

        return readed_data;
    }


}
