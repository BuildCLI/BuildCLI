package dev.buildcli.plugin.utils;

import dev.buildcli.core.domain.jar.Jar;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.core.utils.filesystem.FindFilesUtils;
import dev.buildcli.plugin.BuildCLICommandPlugin;
import dev.buildcli.plugin.BuildCLIPlugin;
import dev.buildcli.plugin.BuildCLITemplatePlugin;
import dev.buildcli.plugin.enums.TemplateType;
import dev.buildcli.plugin.factories.CommandFactory;
import dev.buildcli.plugin.utils.pf4j.CustomDefaultPluginManager;
import org.pf4j.PluginWrapper;
import picocli.CommandLine;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static dev.buildcli.core.constants.ConfigDefaultConstants.PLUGIN_PATHS;
import static dev.buildcli.core.utils.BeautifyShell.blueFg;
import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.confirm;

public final class BuildCLIPluginManager {
  private static final org.pf4j.PluginManager pluginManager = new CustomDefaultPluginManager(loadJars());

  public static List<BuildCLICommandPlugin> getCommands() {
    return getPlugins(BuildCLICommandPlugin.class);
  }

  private static <T extends BuildCLIPlugin> List<T> getPlugins(Class<T> type) {
    return pluginManager.getPlugins().stream()
        .map(PluginWrapper::getPlugin)
        .filter(plugin -> type.isAssignableFrom(plugin.getClass()))
        .map(type::cast)
        .toList();
  }

  public static List<BuildCLITemplatePlugin> getTemplates() {
    return getPlugins(BuildCLITemplatePlugin.class);
  }

  public static List<BuildCLITemplatePlugin> getTemplatesByType(TemplateType type) {
    if (type == null) {
      return getTemplates();
    }

    return getTemplates().stream()
        .filter(buildCLITemplatePlugin -> buildCLITemplatePlugin.type().equals(type))
        .toList();
  }

  private static String[] pluginPaths() {
    var defaultPath = System.getProperty("user.home") + "/.buildcli/plugins";
    var property = ConfigContextLoader.getAllConfigs().getProperty(PLUGIN_PATHS);

    return property.orElse("").concat((property.isPresent() ? "" : ";") + defaultPath).split(";");
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
