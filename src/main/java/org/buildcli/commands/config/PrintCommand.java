package org.buildcli.commands.config;

import org.buildcli.commands.ConfigCommand;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.domain.configs.BuildCLIConfig;
import org.buildcli.handler.GlobalExceptionHandler;
import org.buildcli.log.SystemOutLogger;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import static org.buildcli.utils.config.ConfigContextLoader.getGlobalConfig;
import static org.buildcli.utils.config.ConfigContextLoader.getLocalConfig;

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
@Command(name = "print", aliases = {"p", "show", "list"}, description = "Prints the current configuration.")
public class PrintCommand implements BuildCLICommand {

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
      GlobalExceptionHandler.handleException(e);
    }

  }
}
