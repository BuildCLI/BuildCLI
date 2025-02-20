package org.buildcli.commands.config;

import org.buildcli.commands.ConfigCommand;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.domain.configs.BuildCLIConfig;
import org.buildcli.handler.GlobalExceptionHandler;
import org.buildcli.utils.config.ConfigContextLoader;
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
      System.out.printf("Configuration successfully initialized in %s scope.%n", isLocal ? "local" : "global");
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
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
