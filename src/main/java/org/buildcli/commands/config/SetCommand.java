package org.buildcli.commands.config;

import org.buildcli.commands.ConfigCommand;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.domain.configs.BuildCLIConfig;
import org.buildcli.exceptions.ConfigException;
import org.buildcli.handler.GlobalExceptionHandler;
import org.buildcli.utils.config.ConfigContextLoader;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.List;
import java.util.regex.Pattern;

import static org.buildcli.utils.config.ConfigContextLoader.*;

/**
 * Command to configure BuildCLI by setting key-value properties.
 *
 * This command allows users to define configuration settings that
 * can be applied locally or globally within the CLI.
 *
 * Usage Examples:
 *   - Set a local configuration property:
 *       {@code buildcli config set key=value}
 *   - Set a global configuration property:
 *       {@code buildcli config set key=value -g}
 *
 * Key names should follow a hierarchical structure using dots (e.g., "core.timeout").
 */
@Command(name = "set", aliases = {"s"}, description = "Set configuration properties for BuildCLI.", mixinStandardHelpOptions = true)
public class SetCommand implements BuildCLICommand {

  /**
   * Reference to the parent command to determine if the configuration is local or global.
   */
  @ParentCommand
  private ConfigCommand configCommand;

  /**
   * List of key-value configuration pairs to be set.
   * Each entry must follow the format "key=value".
   */
  @Parameters(description = "Configuration properties in the format <key>=<value>.")
  private List<String> configs;

  /**
   * Regex pattern to validate configuration format (key=value).
   * - Keys: Alphanumeric with optional dashes and dots for hierarchy.
   * - Values: Alphanumeric, dashes, dots, and numeric values allowed.
   */
  private static final Pattern CONFIG_PATTERN = Pattern.compile("^[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*=.*$");

  /**
   * Executes the "set" command, updating configuration settings accordingly.
   *
   * @throws ConfigException If no configurations are provided or an invalid format is detected.
   */
  @Override
  public void run() {
    if (configs == null || configs.isEmpty()) {
      throw new ConfigException("No configuration properties provided. Please specify at least one key=value pair.");
    }

    boolean isLocalScope = configCommand.isLocal() || !configCommand.isGlobal();
    BuildCLIConfig buildCliConfig = isLocalScope ? getLocalConfig() : getGlobalConfig();

    for (String config : configs) {
      if (!isValidConfigFormat(config)) {
        throw new ConfigException("Invalid configuration format: '" + config + "'. Expected format: <key>=<value>.");
      }
      int separatorIndex = config.indexOf('=');
      String key = config.substring(0, separatorIndex).trim();
      String value = config.substring(separatorIndex + 1).trim();

      buildCliConfig.addOrSetProperty(key.trim(), value.trim());
    }

    SaveConfig saveConfigFunction = isLocalScope ? ConfigContextLoader::saveLocalConfig : ConfigContextLoader::saveGlobalConfig;

    try {
      saveConfigFunction.save(buildCliConfig);
      System.out.printf("Configuration successfully updated in %s scope.%n", isLocalScope ? "local" : "global");
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
    }
}

  /**
   * Validates if the given configuration string follows the expected "key=value" format.
   *
   * @param config The configuration string to validate.
   * @return true if the format is valid, false otherwise.
   */
  private boolean isValidConfigFormat(String config) {
    return CONFIG_PATTERN.matcher(config).matches();
  }

  /**
   * Functional interface for saving configuration based on scope (local or global).
   */
  @FunctionalInterface
  private interface SaveConfig {
    void save(BuildCLIConfig buildConfig) throws Exception;
  }
}
