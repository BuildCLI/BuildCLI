package dev.buildcli.plugin.builders;

import dev.buildcli.plugin.enums.PluginType;

public final class PluginBuilderFactory {
  public static PluginBuilder create(PluginType type) {
    return switch (type) {
      case COMMAND -> new CommandPluginBuilder();
      case TEMPLATE -> new TemplatePluginBuilder();
    };
  }
}
