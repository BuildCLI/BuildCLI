package dev.buildcli.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.Files.newBufferedReader;

public class ProfileManager {
    private static final Logger logger = Logger.getLogger(ProfileManager.class.getName());
    private static final String CONFIG_FILE = "environment.config";

    public String getActiveProfile() {
        Properties properties = new Properties();
        try (BufferedReader reader = newBufferedReader(Paths.get(CONFIG_FILE))) {
            properties.load(reader);
            return properties.getProperty("active.profile");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Environment configuration file not found. Using default profile.");
            return "default"; // Retorno padrão se o perfil não estiver definido
        }
    }
}
