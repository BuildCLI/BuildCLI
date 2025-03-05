package dev.buildcli.core.constants;

import java.nio.file.Path;

public abstract class ConfigDefaultConstants {

  private ConfigDefaultConstants() {
  }

  public static final String BUILD_CLI_CONFIG_FILE_NAME = "buildcli.properties";
  public static final Path BUILD_CLI_CONFIG_GLOBAL_FILE = Path.of(System.getProperty("user.home"), ".buildcli", BUILD_CLI_CONFIG_FILE_NAME);


  //Common Keys

  //Logs
  public static final String LOGGING_PARENT = "logging";
  public static final String BANNER_ENABLED = composePropertyName(LOGGING_PARENT, "banner", "enabled");
  public static final String BANNER_PATH = composePropertyName(LOGGING_PARENT, "banner", "path");
  public static final String FILE_PATH = composePropertyName(LOGGING_PARENT, "file", "path");
  public static final String FILE_ENABLED = composePropertyName(LOGGING_PARENT, "file", "enabled");

  //Project
  public static final String PROJECT_NAME = composePropertyName(LOGGING_PARENT, "project", "name");
  public static final String PROJECT_TYPE = composePropertyName(LOGGING_PARENT, "project", "type");

  //AI
  public static final String AI_PARENT = "ai";
  public static final String AI_VENDOR = composePropertyName(AI_PARENT, "vendor");
  public static final String AI_MODEL = composePropertyName(AI_PARENT, "model");
  public static final String AI_URL = composePropertyName(AI_PARENT, "url");
  public static final String AI_TOKEN = composePropertyName(AI_PARENT, "token");

  //Plugins
  public static final String PLUGIN_PARENT = "plugin";
  public static final String PLUGIN_PATHS = composePropertyName(PLUGIN_PARENT, "paths");


  public static String composePropertyName(String... names) {
    var builder = new StringBuilder("buildcli");

    for (String name : names) {
      builder.append('.').append(name);
    }

    return builder.toString();
  }
}
