package dev.buildcli.plugin.utils;

import dev.buildcli.core.domain.jar.Jar;
import dev.buildcli.plugin.BuildCLICommandPlugin;
import dev.buildcli.plugin.BuildCLIPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class PluginUtils {
  private static final List<Class<? extends BuildCLIPlugin>> classes;

  static {
    classes = new ArrayList<>();
    classes.add(BuildCLIPlugin.class);
    classes.add(BuildCLICommandPlugin.class);
  }
  public static boolean isValid(Jar jar) {
    Predicate<Class<? extends BuildCLIPlugin>> validate =
        clazz -> !PluginLoader.load(clazz, jar).isEmpty();

    return classes.stream().anyMatch(validate);
  }

  public static Optional<String> getPluginVersion(Jar jar) {
    var plugins = new ArrayList<BuildCLIPlugin>();

    for (var clazz : classes) {
      plugins.addAll(PluginLoader.load(clazz, jar));
    }

    var plugin = plugins.getFirst();

    if (plugin == null) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(plugin.version());
    }
  }

  public static Optional<String> getPluginDescription(Jar jar) {
    var plugins = new ArrayList<BuildCLIPlugin>();

    for (var clazz : classes) {
      plugins.addAll(PluginLoader.load(clazz, jar));
    }

    var plugin = plugins.getFirst();

    if (plugin == null) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(plugin.description());
    }
  }

  public static Optional<String> getPluginName(Jar jar) {
    var plugins = new ArrayList<BuildCLIPlugin>();

    for (var clazz : classes) {
      plugins.addAll(PluginLoader.load(clazz, jar));
    }

     var plugin = plugins.getFirst();

     if (plugin == null) {
       return Optional.empty();
     } else {
       return Optional.ofNullable(plugin.name());
     }
  }
}
