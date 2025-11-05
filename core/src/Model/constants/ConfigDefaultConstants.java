package dev.buildcli.core.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Map;

import static dev.buildcli.core.utils.BeautifyShell.*;

public abstract class ConfigDefaultConstants {

  private static final Logger log = LoggerFactory.getLogger(ConfigDefaultConstants.class);

  public static final String BUILD_CLI_CONFIG_FILE_NAME = "buildcli.properties";
  public static final Path BUILD_CLI_CONFIG_GLOBAL_FILE = Path.of(System.getProperty("user.home"), ".buildcli", BUILD_CLI_CONFIG_FILE_NAME);

  //Logs
  public static final String LOGGING_PARENT = "logging";
  public static final String BANNER_ENABLED = composePropertyName(LOGGING_PARENT, "banner", "enabled");
  public static final String BANNER_PATH = composePropertyName(LOGGING_PARENT, "banner", "path");

  //Common Keys
  public static final String FILE_PATH = composePropertyName(LOGGING_PARENT, "file", "path");
  public static final String FILE_ENABLED = composePropertyName(LOGGING_PARENT, "file", "enabled");

  //Project
  public static final String PROJECT_PARENT = "project";
  public static final String PROJECT_NAME = composePropertyName(PROJECT_PARENT, "name");
  public static final String PROJECT_TYPE = composePropertyName(PROJECT_PARENT, "type");

  //AI
  public static final String AI_PARENT = "ai";
  public static final String AI_VENDOR = composePropertyName(AI_PARENT, "vendor");
  public static final String AI_MODEL = composePropertyName(AI_PARENT, "model");
  public static final String AI_URL = composePropertyName(AI_PARENT, "url");
  public static final String AI_TOKEN = composePropertyName(AI_PARENT, "token");

  //Plugins
  public static final String PLUGIN_PARENT = "plugin";
  public static final String PLUGIN_PATHS = composePropertyName(PLUGIN_PARENT, "paths");

  private static final Map<String, String> configs;

  static {
    configs = Map.of(
        BANNER_ENABLED, "Show or hidden banner. It's %s by default".formatted(greenFg(true)),
        BANNER_PATH, "Custom banner path",
        PROJECT_NAME, "Project name",
        PROJECT_TYPE, "Project type, e.g., %s, %s".formatted(greenFg("spring-boot"), greenFg("quarkus")),
        AI_VENDOR, "LLM vendor, e.g, %s, %s".formatted(greenFg("jlama"), greenFg("ollama")),
        AI_TOKEN, "LLM token",
        AI_URL, "LLM url, e.g, %s".formatted(content("http://localhost:11434").italic().greenFg()),
        AI_MODEL, "LLM model",
        PLUGIN_PATHS, "Path to yours plugins, separated by %s".formatted(greenFg(";"))
    );
  }

  private ConfigDefaultConstants() {

  }

  public static String composePropertyName(String... names) {
    var builder = new StringBuilder("buildcli");

    for (String name : names) {
      builder.append('.').append(name);
    }

    return builder.toString();
  }

  public static void listAll(PrintWriter out) {
    log.info("List of all configs:");
    for (var entry : configs.entrySet()) {
      var line = content(entry.getKey()).blueFg().bold() + " - " + italic(entry.getValue());
      out.println("  " + line);
      log.info("  {}", line);
    }
  }
}
