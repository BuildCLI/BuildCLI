package dev.buildcli.cli.commands.config;

import dev.buildcli.cli.commands.ConfigCommand;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.log.SystemOutLogger;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import static dev.buildcli.core.utils.config.ConfigContextLoader.getGlobalConfig;
import static dev.buildcli.core.utils.config.ConfigContextLoader.getLocalConfig;

/**
 * Command to print the current configuration.
 * Supports aliases: "p", "show", "list".
 *
 * This command determines whether to print the local or global configuration
 * based on the flags provided by the parent command.
 *
 * Usage:
 *   buildcli config print   - Prints the local configuration by default.
 *   buildcli config print --global  - Prints the global configuration.
 */
@Command(name = "list", aliases = {"ls"}, description = "Prints the current configuration (local or global).")
public class ListCommand implements BuildCLICommand {

  /**
   * Parent command providing context for local or global configuration.
   */
  @ParentCommand
  private ConfigCommand configCommand;

  /**
   * Executes the command, determining whether to print the local or global configuration.
   *
   * If the `--global` flag is set in the parent command, it prints the global configuration.
   * Otherwise, it prints the local configuration.
   */
  @Override
  public void run() {
    try {
      boolean isLocal = configCommand.isLocal() || !configCommand.isGlobal();
      BuildCLIConfig buildCliConfig = isLocal ? getLocalConfig() : getGlobalConfig();

      SystemOutLogger.log("Current Configuration (" + (isLocal ? "local" : "global") + "):");
      SystemOutLogger.log(buildCliConfig.toString());

    } catch (Exception e) {
      System.err.println("Error loading configuration: " + e.getMessage());
    }

  }
}
