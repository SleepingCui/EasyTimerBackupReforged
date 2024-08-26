package main;

import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class timer {
    public static void timer_backup() {
        String read_hour = config_read.get_config("time_hour");
        String read_minute = config_read.get_config("time_minute");
        String read_second = config_read.get_config("time_second");
        String read_source_path = config_read.get_config("source_dir");
        String read_temp_path = config_read.get_config("temp_dir");
        String read_backup_path = config_read.get_config("backup_dir");
        System.out.println("Starting backup...");
        Timer timer = new Timer();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, Integer.parseInt(read_hour));
        today.set(Calendar.MINUTE, Integer.parseInt(read_minute));
        today.set(Calendar.SECOND, Integer.parseInt(read_second));
        System.out.println("Backup time: " + read_hour + ":" + read_minute + ":" + read_second);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Backup Start");
                try{pack.PackBackup(read_source_path,read_temp_path,read_backup_path);}
                catch (FileNotFoundException e) {throw new RuntimeException(e);}


            }
        }, today.getTime(), 24 * 60 * 60 * 1000); // Repeat every day

    }
    
}

