package dev.buildcli.core.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

import dev.buildcli.core.log.SystemOutLogger;

public class EnvironmentConfigManager {

    private static final Logger logger = Logger.getLogger(EnvironmentConfigManager.class.getName());
    private static final Path configPath = Path.of("environment.config");

    /**
     * Sets the environment configuration.
     *
     * @param environment the environment to set (e.g., dev, test, prod)
     */
    public static void setEnvironment(String environment) {
        try {
            String content = "active.profile=" + environment; // Formata a string no formato chave-valor
            Files.writeString(configPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("Environment set to: " + environment);
            SystemOutLogger.success("Environment set to: " + environment);
        } catch (IOException e) {
            logger.severe("Failed to set environment: " + e.getMessage());
            SystemOutLogger.error("Could not set environment.");
        }
    }

    /**
     * Gets the current environment configuration.
     *
     * @return the current environment as a string, or null if not set
     */
    public static String getEnvironment() {
        try {
            String content = Files.readString(configPath).trim();
            if (content.startsWith("active.profile=")) {
                return content.split("=")[1]; // Extrai o valor do perfil ativo
            } else {
                logger.warning("Environment configuration is in an unexpected format.");
                return null;
            }
        } catch (IOException e) {
            logger.warning("No environment configuration found.");
            return null;
        }
    }
}
