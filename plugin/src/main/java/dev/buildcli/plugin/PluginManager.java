package dev.buildcli.plugin;

import dev.buildcli.core.constants.ConfigDefaultConstants;
import dev.buildcli.core.domain.jar.Jar;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.core.utils.filesystem.FindFilesUtils;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public abstract class PluginManager {
  public static List<BuildCLICommandPlugin> getCommands() {
    return loadJars()
        .stream()
        .map(PluginManager::loadCommandPluginFromJar)
        .flatMap(List::stream)
        .filter(plugin -> plugin.getClass().isAnnotationPresent(Command.class))
        .toList();
  }


  private static String[] pluginPaths() {
    return ConfigContextLoader.getAllConfigs().getProperty(ConfigDefaultConstants.PLUGIN_PATHS).orElse(System.getProperty("user.home") + "/.buildcli/plugins").split(";");
  }

  private static List<Jar> loadJars() {
    return Arrays.stream(pluginPaths())
        .filter(Predicate.not(String::isBlank))
        .map(File::new)
        .map(FindFilesUtils::searchJarFiles)
        .flatMap(List::stream)
        .map(Jar::new)
        .toList();
  }

  private static List<BuildCLICommandPlugin> loadCommandPluginFromJar(Jar jar) {
    return PluginLoader.load(BuildCLICommandPlugin.class, jar);
  }
}
