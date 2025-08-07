package dev.buildcli.cli.commands.config;

import dev.buildcli.cli.commands.ConfigCommand;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.exceptions.ConfigException;
import dev.buildcli.core.log.SystemOutLogger;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.List;

import static dev.buildcli.core.utils.config.ConfigContextLoader.getGlobalConfig;
import static dev.buildcli.core.utils.config.ConfigContextLoader.getLocalConfig;

/**
 * Command to remove configuration properties from BuildCLI.
 *
 * Allows users to delete specific configuration keys from local or global configuration files.
 *
 * Usage Examples:
 *   - Remove a local configuration key:
 *       {@code buildcli config remove key}
 *   - Remove a global configuration key:
 *       {@code buildcli config remove -g key}
 */
@Command(name = "remove", aliases = {"rm", "r"}, description = "Removes specified configuration properties from BuildCLI.", mixinStandardHelpOptions = true)
public class RmCommand implements BuildCLICommand {

  /**
   * Reference to the parent command to determine scope (local or global).
   */
  @ParentCommand
  private ConfigCommand configCommand;

  /**
   * List of configuration keys to be removed.
   */
  @Parameters(arity = "1..*", description = "Configuration keys to remove.")
  private List<String> configs;

  /**
   * Executes the "remove" command, deleting specified configuration properties.
   *
   * @throws ConfigException if no configuration keys are provided.
   */
  @Override
  public void run() {
    if (configs == null || configs.isEmpty()) {
      throw new ConfigException("No configuration keys provided for removal.");
    }

    boolean isLocal = configCommand.isLocal() || !configCommand.isGlobal();
    BuildCLIConfig buildCliConfig = isLocal ? getLocalConfig() : getGlobalConfig();
    int removedCount = 0;

    for (String config : configs) {
      if (buildCliConfig.removeProperty(config)) {
        removedCount++;
      }
    }

    SaveConfig saveConfigFunction = isLocal ? ConfigContextLoader::saveLocalConfig : ConfigContextLoader::saveGlobalConfig;

    try {
      saveConfigFunction.save(buildCliConfig);
      SystemOutLogger.log("Successfully removed %d configuration properties from %s scope.".formatted(removedCount, isLocal ? "local" : "global"));
    } catch (Exception e) {
      System.err.println("Failed to remove configuration properties: " + e.getMessage());
    }
  }

  /**
   * Functional interface for saving configuration based on scope.
   */
  @FunctionalInterface
  private interface SaveConfig {
    void save(BuildCLIConfig buildConfig) throws Exception;
  }
}
