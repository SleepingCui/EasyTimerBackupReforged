package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
public class Loginit {
    private static final Logger LOGGER = LogManager.getLogger(Loginit.class);
    public static void initLog() {
            FileInputStream fileInputStream = null;
            try {
                Properties properties = new Properties();
                fileInputStream = new FileInputStream("src/resources/log4j.properties");
                properties.load(fileInputStream);
                PropertyConfigurator.configure(properties);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(),e);
            } finally {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(),e);
                    }
                }
            }
        }
    }

