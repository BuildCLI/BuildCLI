package dev.buildcli.cli.commands.config;

import dev.buildcli.cli.commands.ConfigCommand;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.log.SystemOutLogger;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

/**
 * Command to initialize a new configuration setup for BuildCLI.
 *
 * This command creates an empty configuration, setting it as local or global based on user preferences.
 *
 * Usage Examples:
 *   - Initialize a local configuration:
 *       {@code buildcli config init}
 *   - Initialize a global configuration:
 *       {@code buildcli config -g init}
 */
@Command(name = "init", aliases = {"i", "create"}, description = "Initializes a new configuration context for BuildCLI.", mixinStandardHelpOptions = true)
public class InitCommand implements BuildCLICommand {

  /**
   * Reference to the parent command to determine scope (local or global).
   */
  @ParentCommand
  private ConfigCommand configCommand;

  /**
   * Executes the "init" command, creating a new configuration file.
   */
  @Override
  public void run() {
    boolean isLocal = configCommand.isLocal() || !configCommand.isGlobal();
    BuildCLIConfig buildConfig = BuildCLIConfig.empty();
    buildConfig.setLocal(configCommand.isLocal());

    SaveConfig saveConfigFunction = isLocal ? ConfigContextLoader::saveLocalConfig : ConfigContextLoader::saveGlobalConfig;

    try {
      saveConfigFunction.save(buildConfig);
      String scope = isLocal ? "local" : "global";
      SystemOutLogger.success("Configuration successfully initialized in " + scope + " scope.");
    } catch (Exception e) {
      SystemOutLogger.error("Failed to initialize configuration: " + e.getMessage(), e);
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
