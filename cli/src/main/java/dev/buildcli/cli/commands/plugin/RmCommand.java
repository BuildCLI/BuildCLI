package dev.buildcli.cli.commands.plugin;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static dev.buildcli.core.constants.ConfigDefaultConstants.PLUGIN_PATHS;
import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.confirm;
import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.question;

@Command(
    name = "remove",
    aliases = {"rm"},
    description = "Remove one or more plugins from the BuildCLI installation",
    mixinStandardHelpOptions = true
)
public class RmCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger("RmPluginCommand");
  private static final String DEFAULT_PLUGINS_DIR = System.getProperty("user.home") + "/.buildcli/plugins";

  @Parameters(description = "Names of the plugins to remove (without .jar extension)")
  private List<String> names;

  private final BuildCLIConfig globalConfig = ConfigContextLoader.getAllConfigs();

  @Override
  public void run() {
    List<String> pluginsToRemove = getPluginsToRemove();
    String[] pluginPaths = getPluginPaths();

    removePlugins(pluginsToRemove, pluginPaths);
  }

  private List<String> getPluginsToRemove() {
    if (names != null && !names.isEmpty()) {
      return names;
    }

    List<String> pluginsToRemove = new ArrayList<>();

    while (true) {
      String pluginName = question("Please enter the name of the plugin to remove:");
      pluginsToRemove.add(pluginName);

      if (!confirm("Do you want to remove more plugins?")) {
        break;
      }
    }

    return pluginsToRemove;
  }

  private String[] getPluginPaths() {
    return globalConfig.getProperty(PLUGIN_PATHS)
        .orElse(DEFAULT_PLUGINS_DIR)
        .split(";");
  }

  private void removePlugins(List<String> pluginsToRemove, String[] pluginPaths) {
    boolean anyRemoved = false;

    for (String path : pluginPaths) {
      for (String pluginName : pluginsToRemove) {
        Path jarPath = Paths.get(path, pluginName + ".jar");
        anyRemoved |= removePlugin(jarPath);
      }
    }

    if (!anyRemoved) {
      logger.warn("No plugins were found to remove");
    }
  }

  private boolean removePlugin(Path jarPath) {
    logger.info("Checking for plugin at {}", jarPath);

    if (!Files.isRegularFile(jarPath)) {
      logger.info("Plugin {} does not exist", jarPath.getFileName());
      return false;
    }

    try {
      logger.info("Removing plugin {}", jarPath);
      Files.delete(jarPath);
      logger.info("Successfully removed plugin {}", jarPath.getFileName());
      return true;
    } catch (IOException e) {
      logger.error("Failed to remove plugin {}: {}", jarPath, e.getMessage());
      throw new RuntimeException("Failed to remove plugin: " + jarPath, e);
    }
  }
}