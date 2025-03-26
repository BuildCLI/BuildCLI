package dev.buildcli.plugin.enums;

import picocli.CommandLine.ITypeConverter;

import java.util.Arrays;

public enum PluginType {
  COMMAND, TEMPLATE;

  public static class Converter implements ITypeConverter<PluginType> {
    @Override
    public PluginType convert(String s) throws Exception {
      return Arrays.stream(PluginType.values())
          .filter(pluginType -> pluginType.name().equalsIgnoreCase(s))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(s + " is not a valid plugin type"));
    }
  }
}
