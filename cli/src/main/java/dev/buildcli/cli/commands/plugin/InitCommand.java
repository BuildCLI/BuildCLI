package dev.buildcli.cli.commands.plugin;

import dev.buildcli.cli.utils.CommandUtils;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.plugin.enums.PluginType;
import dev.buildcli.plugin.builders.PluginBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.Arrays;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;
import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.*;

@Command(
    name = "init",
    aliases = {"i"},
    description = "Initialize a new plugin project with the specified configuration",
    mixinStandardHelpOptions = true
)
public class InitCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger(InitCommand.class);

  @Option(
      names = {"--name", "-n"},
      description = "Name of the plugin to be created",
      arity = "0..1"
  )
  private String name;

  @Option(
      names = {"--type", "-t"},
      description = "Type of plugin to create (COMMAND, EXTENSION, LIBRARY)",
      converter = PluginType.Converter.class
  )
  private PluginType type;

  @Option(
      names = {"--out", "-o"},
      description = "Parent directory where the plugin project will be created"
  )
  private String outputDirectory;

  private final BuildCLIConfig globalConfig = ConfigContextLoader.getAllConfigs();

  @Override
  public void run() {
    String pluginName = getPluginName();
    PluginType pluginType = getPluginType();
    File pluginDirectory = getPluginDirectory(pluginName);

    createPlugin(pluginDirectory, pluginName, pluginType);
  }

  private String getPluginName() {
    return name == null ? question("Plugin name", true) : name;
  }

  private PluginType getPluginType() {
    return type == null ?
        options("Plugin type", Arrays.stream(PluginType.values()).toList()) :
        type;
  }

  private File getPluginDirectory(String pluginName) {
    File directory;

    do {
      String parentDir = outputDirectory == null ?
          question("Plugin directory", true) :
          outputDirectory;

      directory = new File(parentDir, pluginName);

      if (directory.isFile()) {
        logger.warn("Cannot create plugin in '{}' as it's an existing file",
            directory.getAbsolutePath());
        outputDirectory = null; // Reset to prompt again
      } else {
        break;
      }
    } while (true);

    return directory;
  }

  private void createPlugin(File directory, String pluginName, PluginType pluginType) {
    logger.info("Creating {} plugin '{}' in directory: {}",
        pluginType, pluginName, directory.getAbsolutePath());

    var builder = PluginBuilderFactory.create(pluginType);
    var pluginFile = builder.build(directory, pluginName);

    logger.info("Plugin created successfully at: {}", pluginFile.getAbsolutePath());

    offerToInstallPlugin(pluginName, pluginFile);
  }

  private void offerToInstallPlugin(String pluginName, File pluginFile) {
    String coloredName = blueFg(pluginName);
    if (confirm("Do you want to install plugin %s?".formatted(coloredName))) {
      logger.info("Installing plugin: {}", pluginName);
      int exitCode = CommandUtils.call("plugin", "add", "-f", pluginFile.getAbsolutePath());

      if (exitCode == 0) {
        logger.info("Plugin '{}' installed successfully", pluginName);
      } else {
        logger.warn("Plugin installation exited with code: {}", exitCode);
      }
    } else {
      logger.info("Plugin installation skipped");
    }
  }
}