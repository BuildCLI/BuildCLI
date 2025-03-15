package dev.buildcli.plugin.utils;

import dev.buildcli.core.domain.jar.Jar;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.core.utils.filesystem.FindFilesUtils;
import dev.buildcli.plugin.BuildCLICommandPlugin;
import dev.buildcli.plugin.BuildCLIPlugin;
import dev.buildcli.plugin.factories.CommandFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

import static dev.buildcli.core.constants.ConfigDefaultConstants.PLUGIN_PATHS;
import static dev.buildcli.core.utils.BeautifyShell.blueFg;
import static dev.buildcli.core.utils.input.InteractiveInputUtils.confirm;

public final class PluginManager {
  private static final Map<Class<? extends BuildCLIPlugin>, List<? extends BuildCLIPlugin>> PLUGINS = new HashMap<>();

  public static List<BuildCLICommandPlugin> getCommands() {
    if (PLUGINS.containsKey(BuildCLICommandPlugin.class)) {
      var commands = PLUGINS.get(BuildCLICommandPlugin.class);

      if (commands == null || commands.isEmpty()) {
        return new ArrayList<>();
      }

      return commands
          .stream()
          .map(command -> (BuildCLICommandPlugin) command)
          .toList();
    }

    var commands = loadJars()
        .stream()
        .map(PluginManager::loadCommandPluginFromJar)
        .flatMap(List::stream)
        .filter(plugin -> plugin.getClass().isAnnotationPresent(Command.class))
        .toList();

    PLUGINS.put(BuildCLICommandPlugin.class, commands);

    return commands;
  }

  private static String[] pluginPaths() {
    var defaultPath = System.getProperty("user.home") + "/.buildcli/plugins";
    return ConfigContextLoader.getAllConfigs().getProperty(PLUGIN_PATHS).orElse(defaultPath).split(";");
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

  public static void registerPlugins(CommandLine commandLine) {
    var plugins = getCommands();
    var subcommands = commandLine.getSubcommands();
    for (BuildCLICommandPlugin commandPlugin : plugins) {
      var command = CommandFactory.createCommandLine(commandPlugin);

      var commandName = command.getCommandName();
      if (subcommands.containsKey(commandName)) {
        var confirm = confirm("Do you want override the subcommand \"%s\"".formatted(blueFg(commandName)));
        if (confirm) {
          commandLine.getCommandSpec().removeSubcommand(commandName);
          commandLine.addSubcommand(command);
        }
      } else {
        commandLine.addSubcommand(command);
      }
    }
  }
}
