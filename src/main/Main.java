package main;
import java.util.logging.Logger;
public class Main {

    public static void main(String[] args) {
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.info("EasyTimerBackupReforged v0.0.1");
        timer.timer_backup();
    }
}
