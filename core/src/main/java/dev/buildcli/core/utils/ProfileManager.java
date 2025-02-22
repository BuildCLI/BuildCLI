package dev.buildcli.core.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfileManager {
    private static final Logger logger = Logger.getLogger(ProfileManager.class.getName());
    private static final String CONFIG_FILE = "environment.config";

    public String getActiveProfile() {
        Properties properties = new Properties();
        try {
            properties.load(Files.newBufferedReader(Paths.get(CONFIG_FILE)));
            return properties.getProperty("active.profile");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load environment configuration", e);
            return "default"; // Retorno padrão se o perfil não estiver definido
        }
    }
}
