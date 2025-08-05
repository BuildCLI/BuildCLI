package dev.buildcli.core.domain.configs;
import dev.buildcli.core.exceptions.ConfigException;
import dev.buildcli.core.log.SystemOutLogger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static dev.buildcli.core.constants.ConfigDefaultConstants.BUILD_CLI_CONFIG_GLOBAL_FILE;

public class BuildCLIConfig {
  private final Properties properties = new Properties();
  private boolean local = true;

  private BuildCLIConfig() {
  }

  public static BuildCLIConfig from(File file) {
    if (!file.exists()) {
      return new BuildCLIConfig();
    }
    return new BuildCLIConfig(file);
  }

  private BuildCLIConfig(File file) {
    try (var inputStream = new FileInputStream(file)) {
      this.properties.load(inputStream);

      for (var key : properties.stringPropertyNames()) {
        var value = properties.getProperty(key);

        //Add support to env vars system
        if (value != null && value.matches("^\\$\\{[A-Z]+}$")) {
          value = value.substring(value.indexOf("${") + 2, value.indexOf("}"));
          SystemOutLogger.debug(key + " = " + value);
          value = System.getenv(value);
        }

        properties.setProperty(key, value);
      }
    } catch (IOException e) {
      throw new ConfigException("Error loading config from file: " + file.getAbsolutePath(), e);
    }
  }

  public static BuildCLIConfig empty() {
    return new BuildCLIConfig();
  }

  public Optional<Integer> getPropertyAsInt(String property) {
    try {
      return Optional.of(Integer.parseInt(properties.getProperty(property)));
    } catch (NumberFormatException e) {
      throw new ConfigException("Invalid integer value for property: " + property, e);
    }
  }

  public Optional<Double> getPropertyAsDouble(String property) {
    try {
      var value = properties.getProperty(property);

      if (value == null) {
        return Optional.empty();
      }

      return Optional.of(Double.parseDouble(value));
    } catch (NumberFormatException e) {
      throw new ConfigException("Invalid double value for property: " + property, e);
    }
  }

  public Optional<Boolean> getPropertyAsBoolean(String property) {
    var value = properties.getProperty(property);

    if (value == null) {
      return Optional.empty();
    }

    return Optional.of(Boolean.parseBoolean(value));
  }

  public Optional<String> getProperty(String property) {
    var value = properties.getProperty(property);

    if (value == null) {
      return Optional.empty();
    }

    return Optional.of(value);
  }

  public void addOrSetProperty(String property, String value) {
    if (property != null && property.contains(" ")) {
      throw new ConfigException("Property name contains whitespace");
    }
    properties.setProperty(property, value);
  }

  public boolean removeProperty(String property) {
    return properties.remove(property) != null;
  }

  public boolean isLocal() {
    return local;
  }

  public void setLocal(boolean local) {
    this.local = local;
  }

  public Set<ImmutableProperty> getProperties() {
    return properties.entrySet().stream()
        .map(ImmutableProperty::from)
        .collect(Collectors.toSet());
  }

  @Override
  public String toString() {
    return properties.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining("\n"));
  }

  public record ImmutableProperty(String name, String value) {
    public static ImmutableProperty from(Map.Entry<Object, Object> entry) {
      return new ImmutableProperty(entry.getKey().toString(), entry.getValue().toString());
    }
  }

  private static boolean isPresentFileProperties(){
    Path pathFile = BUILD_CLI_CONFIG_GLOBAL_FILE.toAbsolutePath();
    return Files.exists(pathFile)
        && Files.isRegularFile(pathFile)
        && pathFile.getFileName().toString().equals("buildcli.properties");
  }

  private void generatePropertiesFile() {
    File pathFile = BUILD_CLI_CONFIG_GLOBAL_FILE.toFile();


    File parentDir = pathFile.getParentFile();
    if (!parentDir.exists()) {
      if (!parentDir.mkdirs()) {
        throw new ConfigException("Could not create directory: " + parentDir.getAbsolutePath());
      }
    }

    String content = "buildcli.ai.vendor=jlama\n" +
        "\n" +
        "# If you want to use Ollama\n" +
        "#buildcli.ai.vendor=ollama\n" +
        "#buildcli.ai.model=llama3.2\n" +
        "#buildcli.ai.url=http://localhost:11434/\n" +
        "#buildcli.ai.token=#ai-token";

    try {
      if (pathFile.createNewFile()) {
        try (FileWriter writer = new FileWriter(pathFile)) {
          writer.write(content);
        }
        SystemOutLogger.success("Default configuration file created: " + BUILD_CLI_CONFIG_GLOBAL_FILE.toAbsolutePath());
      }
    } catch (IOException e) {
      throw new ConfigException("Error writing properties file to: " + pathFile.toString(), e);
    }
  }

  public static BuildCLIConfig initialize() {
    BuildCLIConfig config = new BuildCLIConfig();
    if (!isPresentFileProperties()){
      config.generatePropertiesFile();
      return config;
    }
   return config;
  }
}
