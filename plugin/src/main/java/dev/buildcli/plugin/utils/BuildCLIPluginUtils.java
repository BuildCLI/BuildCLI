package dev.buildcli.plugin.utils;

import dev.buildcli.core.domain.jar.Jar;
import dev.buildcli.plugin.BuildCLICommandPlugin;
import dev.buildcli.plugin.BuildCLIPlugin;
import dev.buildcli.plugin.BuildCLITemplatePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Predicate;

public final class BuildCLIPluginUtils {
  private static final List<Class<? extends BuildCLIPlugin>> classes;
  private static final Logger logger = LoggerFactory.getLogger(BuildCLIPluginUtils.class);

  static {
    classes = new ArrayList<>();
    classes.add(BuildCLIPlugin.class);
    classes.add(BuildCLICommandPlugin.class);
    classes.add(BuildCLITemplatePlugin.class);
  }

  private BuildCLIPluginUtils() {
  }

  public static boolean isValid(Jar jar) {
    return classes.stream().anyMatch(createPluginValidator(jar));
  }

  private static Predicate<Class<? extends BuildCLIPlugin>> createPluginValidator(Jar jar) {
    return aClass -> !loadPlugins(aClass, jar).isEmpty();
  }

  private static List<BuildCLIPlugin> loadPlugins(Class<? extends BuildCLIPlugin> clazz, Jar jar) {
    var plugins = new ArrayList<BuildCLIPlugin>();
    try {
      var jarUrl = jar.getFile().toURI().toURL();
      var serviceLoader = ServiceLoader.load(clazz, new URLClassLoader(new URL[]{jarUrl}, BuildCLIPluginUtils.class.getClassLoader()));

      serviceLoader.forEach(plugins::add);

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return plugins;
  }

  public static Optional<String> getPluginVersion(Jar jar) {
    var plugins = new ArrayList<BuildCLIPlugin>();

    for (var clazz : classes) {
      plugins.addAll(loadPlugins(clazz, jar));
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
      plugins.addAll(loadPlugins(clazz, jar));
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
      plugins.addAll(loadPlugins(clazz, jar));
    }

    var plugin = plugins.getFirst();

    if (plugin == null) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(plugin.name());
    }
  }
}
