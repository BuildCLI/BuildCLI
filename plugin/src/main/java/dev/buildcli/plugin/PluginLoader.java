package dev.buildcli.plugin;

import dev.buildcli.core.domain.jar.Jar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

abstract class PluginLoader {
  private static final Logger logger = LoggerFactory.getLogger(PluginLoader.class);

  public static <T extends BuildCLIPlugin> List<T> load(Class<T> tClass, Jar jar) {
    var plugins = new LinkedList<T>();
    try {
      var url = jar.getFile().toURI().toURL();
      try (var loader = new URLClassLoader(new URL[]{url}, PluginLoader.class.getClassLoader())) {
        var serviceLoader = ServiceLoader.load(tClass, loader);

        serviceLoader.forEach(plugins::add);
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }

    return plugins;
  }
}
