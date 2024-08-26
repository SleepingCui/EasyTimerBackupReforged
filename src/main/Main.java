package main;
public class Main {
    private static void logo() {
        System.out.println("""
                  _____          _   _____ _                    \s
                 | ____|__ _ ___(_) |_   _(_)_ __ ___   ___ _ __\s
                 |  _| / _` / __| |   | | | | '_ ` _ \\ / _ \\ '__|
                 | |__| (_| \\__ \\ |   | | | | | | | | |  __/ |  \s
                 |_____\\__,_|___/_|   |_| |_|_| |_| |_|\\___|_|  \s
                    ____             _                          \s
                   | __ )  __ _  ___| | ___   _ _ __            \s
                   |  _ \\ / _` |/ __| |/ / | | | '_ \\           \s
                   | |_) | (_| | (__|   <| |_| | |_) |          \s
                   |____/ \\__,_|\\___|_|\\_\\\\__,_| .__/           \s
                                               |_|              \s
                """);
        System.out.println("\n============= v0.0.1 Build by SleepingCui ============\n   github.com/SleepingCui/EasiTimerBackupReforged\n");
    }

    public static void main(String[] args) {
        logo();
        timer.timer_backup();
    }
}
