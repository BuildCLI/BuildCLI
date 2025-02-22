package dev.buildcli.core.log.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

public class LoggingConfig {

    private LoggingConfig() {
    }

    public static void configure() {
        try (InputStream configFile = LoggingConfig.class.getResourceAsStream("/logging.properties")) {
            if (configFile != null) {
                LogManager.getLogManager().readConfiguration(configFile);
            } else {
                System.err.println("Could not find logging.properties file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
