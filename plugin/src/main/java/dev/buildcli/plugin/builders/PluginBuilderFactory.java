package dev.buildcli.plugin.builders;

import dev.buildcli.plugin.PluginType;

public final class PluginBuilderFactory {
  public static PluginBuilder create(PluginType type) {
    return switch (type) {
      case COMMAND -> new CommandPluginBuilder();
      case TEMPLATE -> {
        throw new IllegalStateException("Template plugin builder is not supported yet.");
      }
    };
  }
}
