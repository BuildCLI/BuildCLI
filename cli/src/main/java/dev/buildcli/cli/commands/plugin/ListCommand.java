package dev.buildcli.cli.commands.plugin;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.domain.jar.Jar;
import dev.buildcli.core.log.SystemOutLogger;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.core.utils.filesystem.FindFilesUtils;
import dev.buildcli.plugin.utils.BuildCLIPluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static dev.buildcli.core.constants.ConfigDefaultConstants.PLUGIN_PATHS;
import static dev.buildcli.core.utils.BeautifyShell.*;

@Command(
    name = "list",
    aliases = {"ls"},
    description = "List all installed plugins with details",
    mixinStandardHelpOptions = true
)
public class ListCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger("ListPluginCommand");
  private static final String DEFAULT_PLUGINS_DIR = System.getProperty("user.home") + "/.buildcli/plugins";

  @Option(
      names = {"--verbose", "-v"},
      description = "Show detailed information about each plugin"
  )
  private boolean verbose = false;

  @Option(
      names = {"--name-only", "-n"},
      description = "Show only plugin names"
  )
  private boolean nameOnly = false;

  private final BuildCLIConfig globalConfig = ConfigContextLoader.getAllConfigs();

  @Override
  public void run() {
    List<PluginInfo> plugins = findInstalledPlugins();

    if (plugins.isEmpty()) {
      SystemOutLogger.warn("No plugins installed.");
      return;
    }

    displayPlugins(plugins);
  }

  private List<PluginInfo> findInstalledPlugins() {
    String[] pluginPaths = globalConfig.getProperty(PLUGIN_PATHS)
        .orElse(DEFAULT_PLUGINS_DIR)
        .split(";");

    List<PluginInfo> plugins = new ArrayList<>();

    for (String pathStr : pluginPaths) {
      Path path = Path.of(pathStr);

      if (!Files.exists(path)) {
        continue;
      }

      List<File> jarFiles = FindFilesUtils.searchJarFiles(path.toFile());

      for (File jarFile : jarFiles) {
        try {
          Jar jar = new Jar(jarFile);
          if (BuildCLIPluginUtils.isValid(jar)) {
            plugins.add(extractPluginInfo(jar));
          }
        } catch (Exception e) {
          logger.warn("Failed to process plugin jar {}: {}", jarFile, e.getMessage());
        }
      }
    }

    return plugins.stream()
        .sorted(Comparator.comparing(PluginInfo::name))
        .collect(Collectors.toList());
  }

  private PluginInfo extractPluginInfo(Jar jar) {
    String name = jar.getFile().getName().replace(".jar", "");
    String version = "Unknown";
    String description = "No description available";

    try {
      // These methods would need to be implemented in PluginUtils
      // to extract metadata from the JAR manifest or plugin class
      name = BuildCLIPluginUtils.getPluginName(jar).orElse(name);
      version = BuildCLIPluginUtils.getPluginVersion(jar).orElse("Unknown");
      description = BuildCLIPluginUtils.getPluginDescription(jar).orElse("No description available");
    } catch (Exception e) {
      logger.debug("Failed to extract detailed info from plugin {}: {}", name, e.getMessage());
    }

    return new PluginInfo(name, version, description, jar.getFile().getAbsolutePath());
  }

  private void displayPlugins(List<PluginInfo> plugins) {
    if (nameOnly) {
      plugins.forEach(plugin -> SystemOutLogger.println(plugin.name()));
      return;
    }

    SystemOutLogger.println(yellowFg("Found " + plugins.size() + " plugin(s):\n"));

    for (PluginInfo plugin : plugins) {
      SystemOutLogger.println(blueFg("Plugin: ") + bold(plugin.name()));
      SystemOutLogger.println(blueFg("Version: ") + plugin.version());

      if (verbose) {
        SystemOutLogger.println(blueFg("Description: ") + plugin.description());
        SystemOutLogger.println(blueFg("Path: ") + plugin.path());
      }

      SystemOutLogger.println(""); // Empty line between plugins
    }
  }

  // Record to store plugin information
  private record PluginInfo(String name, String version, String description, String path) {
  }
}
