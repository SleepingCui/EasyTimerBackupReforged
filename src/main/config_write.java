package main;

import java.io.*;
import java.util.logging.Logger;

public class config_write {
    static Logger logger = Logger.getLogger(config_write.class.getName());
    private static void WritCfg(){
        File config_file = new File("config.cfg");
        try {
            config_file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String cfg = """
                EasyTimerBackup Config
                v0.0.1
                =================================================================
                Do not change the number of lines in the configuration file!
                Otherwise the read will fail!
                不要更改配置文件的行数！否则会导致读取失败！
                =================================================================
                                
                备份时间(时/分/秒)一个数值一行(17,18,19)
                Backup time (hours/minutes/seconds) a value line.(17,18,19)
                例如：
                Example:
                11     --时 Hours
                45     --分 Minutes
                14     --秒 Seconds
                -------Config-------
                
                
                
                --------------------
                                
                目录设置(源目录/临时目录/备份目录)一个路径一行(32,33,34)
                Directory settings (source path/temp path/backup path) one path and one line.(32,33,34)
                ***注意：Windows系统要添加两个反斜杠(\\\\)Unix添加两个正斜杠(//)!
                ***Note: Windows add two backslashes (\\\\) Unix add two forward slashes (//)!
                例如:
                Example:
                C:\\\\SourceFolder\\\\  (//home//SourceFolder//)
                C:\\\\BackupTemp\\\\    (//home//BackupTemp//)
                C:\\\\BackupFolder\\\\  (//home//BackupFolder//)
                -------Config-------
                                
                                
                                
                --------------------
                                
                """;
        try {
            PrintStream ps = new PrintStream(new FileOutputStream(config_file));
            ps.println(cfg);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static void IsFileEsixt(){
        File config_file = new File("config.cfg");if (!config_file.exists()){

            logger.info("Generating configuration file...");
            WritCfg();
            System.exit(0);
        }


    }

}
