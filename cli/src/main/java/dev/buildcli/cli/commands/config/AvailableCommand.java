package dev.buildcli.cli.commands.config;

import dev.buildcli.core.constants.ConfigDefaultConstants;
import dev.buildcli.core.domain.BuildCLICommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Command;

import java.io.PrintWriter;

@Command(name = "available", aliases = {"all", "a"}, description = "List all config keys and it's descriptions",
    mixinStandardHelpOptions = true)
public class AvailableCommand implements BuildCLICommand {

  @Spec
  private CommandSpec spec;

  @Override
  public void run() {
    PrintWriter out = spec.commandLine().getOut();
    ConfigDefaultConstants.listAll(out);
  }
}
