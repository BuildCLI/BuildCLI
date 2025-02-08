package org.buildcli.domain.configs;

import org.buildcli.BuildCLI;
import org.buildcli.exceptions.ConfigException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class BuildCLIConfig {
  private final Properties properties = new Properties();
  private boolean local = true;

  public BuildCLIConfig() {

  }

  public static BuildCLIConfig from(File file) {
    return new BuildCLIConfig(file);
  }

  private BuildCLIConfig(File file) {
    try {
      this.properties.load(new FileInputStream(file));
    } catch (IOException e) {
      throw new ConfigException(e.getMessage());
    }
  }

  public static BuildCLIConfig empty() {
    return new BuildCLIConfig();
  }

  public Optional<Integer> getPropertyAsInt(String property) {
    var value = properties.getProperty(property);

    if (value != null) {
      return Optional.of(Integer.parseInt(value));
    }

    return Optional.empty();
  }

  public Optional<Double> getPropertyAsDouble(String property) {
    var value = properties.getProperty(property);

    if (value != null) {
      return Optional.of(Double.parseDouble(value));
    }

    return Optional.empty();
  }

  public Optional<Boolean> getPropertyAsBoolean(String property) {
    var value = properties.getProperty(property);

    if (value != null) {
      return Optional.of(Boolean.parseBoolean(value));
    }

    return Optional.empty();
  }

  public Optional<String> getProperty(String property) {
    var value = properties.getProperty(property);

    if (value != null) {
      return Optional.of(value);
    }

    return Optional.empty();
  }

  public String getProperty(String property, String defaultValue) {
    return properties.getProperty(property, defaultValue);
  }

  public void addOrSetProperty(String property, String value) {
    if (property != null && property.contains(" ")) {
      throw new ConfigException("property name contains whitespace");
    }

    properties.setProperty(property, value);
  }

  public boolean removeProperty(String property) {
    var removedProperty = properties.remove(property);

    return removedProperty != null;
  }

  public boolean isLocal() {
    return local;
  }

  public void setLocal(boolean local) {
    this.local = local;
  }

  public Properties getProperties() {
    return (Properties) Map.copyOf(properties);
  }

  @Override
  public String toString() {
    return "BuildCLIConfig{" +
        "properties=" + properties +
        '}';
  }

}
