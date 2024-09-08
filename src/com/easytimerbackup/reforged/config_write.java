package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;

public class config_write {
    private static final Logger LOGGER = LogManager.getLogger(config_write.class);

    private static void writeCfg() {
        File configFile = new File("config.cfg");
        try {
            // 创建新文件
            configFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 配置文件内容
        String cfg = """  
                EasyTimerBackup Config  
                v0.0.1  
                =================================================================  
                Do not change the number of lines in the configuration file!  
                Otherwise the read will fail!  
                =================================================================  
                   
                                
                
                Backup time (hours/minutes/seconds) a value line.(17,18,19)  
                  
                Example:  
                11      Hours  
                45      Minutes  
                14      Seconds  
                -------Config-------  
                
                
                
                --------------------  
                                
                
                Directory settings (source path/temp path/backup path) one path and one line.(32,33,34)  
                 
                 
                Example:  
                C:\\SourceFolder\\  (/home/SourceFolder/)  
                C:\\BackupTemp\\    (/home/BackupTemp/)  
                C:\\BackupFolder\\  (/home/BackupFolder/)  
                
                -------Config-------  
                       
                       
                                
                --------------------  
                 
                         
                Enable/disable file upload function(y/n) (40)  
                -------Config-------  
                                           
                --------------------  
                
                Server IP Port (45,48)
                -------IP-------  
                                           
                ----------------  
                ------Port------  
                                           
                ----------------  
                
                Enable/disable Delete Backup File after uploading(y/n) (53)
                -------Config-------  
                                           
                -------------------- 
                
                """;

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(configFile), "utf-8"))) {
            writer.println(cfg);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void isFileExists() {
        File configFile = new File("config.cfg");
        if (!configFile.exists()) {
                LOGGER.info("Generating configuration file...");
            writeCfg();
            System.exit(0);
        }
    }
}