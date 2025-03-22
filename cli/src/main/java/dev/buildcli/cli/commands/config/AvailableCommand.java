package dev.buildcli.cli.commands.config;

import dev.buildcli.core.constants.ConfigDefaultConstants;
import dev.buildcli.core.domain.BuildCLICommand;
import picocli.CommandLine.Command;

@Command(name = "available", aliases = {"all", "a"}, description = "List all config keys and it's descriptions",
    mixinStandardHelpOptions = true)
public class AvailableCommand implements BuildCLICommand {
  @Override
  public void run() {
    ConfigDefaultConstants.listAll();
  }
}
