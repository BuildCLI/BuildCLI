package dev.buildcli.cli.commands;

import dev.buildcli.cli.commands.config.*;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main configuration command for BuildCLI.
 * <p>
 * This command provides access to configuration-related subcommands,
 * allowing users to initialize, set, remove, and print configurations.
 * <p>
 * Supports both local and global configuration scopes.
 * <p>
 * Usage:
 * buildcli config set <key> <value>   - Set a configuration key.
 * buildcli config print               - Print local configuration.
 * buildcli config print --global      - Print global configuration.
 * buildcli config rm <key>            - Remove a configuration key.
 */
@Command(name = "config", aliases = {"c"}, description = "Manage configuration settings.", mixinStandardHelpOptions = true,
    subcommands = {
        AvailableCommand.class, SetCommand.class, ClearCommand.class,
        InitCommand.class, ListCommand.class, RmCommand.class
})
public class ConfigCommand {

  /**
   * Defines whether the command applies to local or global configuration.
   */
  @ArgGroup
  private Scope scope;

  /**
   * Determines if the command should operate on the local configuration.
   * Defaults to local if no scope is explicitly defined.
   *
   * @return true if local configuration should be used, false otherwise.
   */
  public boolean isLocal() {
    return scope == null || scope.local;
  }

  /**
   * Determines if the command should operate on the global configuration.
   *
   * @return true if global configuration should be used, false otherwise.
   */
  public boolean isGlobal() {
    return scope != null && scope.global;
  }

  /**
   * Represents the configuration scope (local or global).
   */
  public static class Scope {
    /**
     * If set, applies the command to the global configuration.
     */
    @Option(names = {"--global", "-g"}, description = "Apply changes to the global configuration.", defaultValue = "false")
    private boolean global;

    /**
     * If set, applies the command to the local configuration.
     */
    @Option(names = {"--local", "-l"}, description = "Apply changes to the local configuration.", defaultValue = "false")
    private boolean local;
  }
}
