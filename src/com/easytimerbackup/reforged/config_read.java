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
            LOGGER.error(e.getMessage(),e);
        }
        return path;
    }

    public static String get_config(String target) {
        config_write.isFileExists();
        String config_file_path = "config.cfg";

        Map<String, Integer> targetMap = new HashMap<>();
        targetMap.put("time_hour", 17);
        targetMap.put("time_minute", 18);
        targetMap.put("time_second", 19);
        targetMap.put("source_dir", 32);
        targetMap.put("temp_dir", 33);
        targetMap.put("backup_dir", 34);
        targetMap.put("uploadenabled", 40);
        targetMap.put("server_ip", 45);
        targetMap.put("server_port", 48);
        targetMap.put("delbackup", 53);
        targetMap.put("server",59);
        targetMap.put("target_server_port",64);
        targetMap.put("rev_path",69);

        Integer lineNumber = targetMap.get(target);

        if (lineNumber == null) {
            return null;
        }

        // Check Upload,Server mode was enabled
        if (target.equals("uploadenabled") || target.equals("server")) {
            return f_read_config(lineNumber, config_file_path);
        } else if ( target.equals("target_server_port") || target.equals("delbackup")  || target.equals("server_port") || target.equals("rev_path")){
            // Check
            String uploadEnabledValue = f_read_config(targetMap.get("uploadenabled"), config_file_path);
            String UploadServerValue = f_read_config(targetMap.get("server"), config_file_path);
            if ("n".equals(uploadEnabledValue) || "n".equals(UploadServerValue)) {
                return "0";
            }

            else if ("y".equals(uploadEnabledValue)) {
                if (f_read_config(lineNumber, config_file_path) == null || f_read_config(lineNumber, config_file_path).isEmpty()) {
                    LOGGER.error(" The read data is empty!");
                    System.exit(1); // Terminate the program
                }

            }
        }

        return f_read_config(lineNumber, config_file_path);
    }



}
