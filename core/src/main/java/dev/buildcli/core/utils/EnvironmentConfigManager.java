package dev.buildcli.core.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EnvironmentConfigManager {

  /** Logger for environment configuration operations. */
  private static final Logger LOGGER =
      Logger.getLogger(EnvironmentConfigManager.class.getName());
  /** Default environment configuration file path. */
  private static final Path DEFAULT_CONFIG_PATH = Path.of("environment.config");
  /** Active environment configuration file path. */
  private static Path configPath = DEFAULT_CONFIG_PATH;

  private EnvironmentConfigManager() {
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
        LOGGER.warning("Environment configuration is in an unexpected format.");
        return null;
      }
    } catch (IOException e) {
      LOGGER.warning("No environment configuration found.");
      return null;
    }
  }

  /**
   * Sets the environment configuration.
   *
   * @param environment the environment to set (e.g., dev, test, prod)
   */
  public static void setEnvironment(final String environment) {
    try {
      String content = "active.profile=" + environment;
      Files.writeString(
          configPath,
          content,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING
      );
      LOGGER.log(Level.INFO, "Environment set to: {0}", environment);
      System.out.println("Environment set to: " + environment);
    } catch (IOException e) {
      LOGGER.log(
          Level.SEVERE,
          "Failed to set environment: {0}",
          e.getMessage());
      System.err.println("Error: Could not set environment.");
    }
  }

  static void setConfigPathForTest(final Path path) {
    configPath = path;
  }
}
