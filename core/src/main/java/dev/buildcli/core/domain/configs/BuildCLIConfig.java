package dev.buildcli.core.domain.configs;
import dev.buildcli.core.exceptions.ConfigException;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

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
          System.out.println(key + " = " + value);
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
      return Optional.of(Double.parseDouble(properties.getProperty(property)));
    } catch (NumberFormatException e) {
      throw new ConfigException("Invalid double value for property: " + property, e);
    }
  }

  public Optional<Boolean> getPropertyAsBoolean(String property) {
    return Optional.of(Boolean.parseBoolean(properties.getProperty(property)));
  }

  public Optional<String> getProperty(String property) {
    return Optional.ofNullable(properties.getProperty(property));
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
}
