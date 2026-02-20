package dev.buildcli.cli.commands;

import dev.buildcli.cli.CommandLineRunner;
import dev.buildcli.core.domain.BuildCLICommand;
import picocli.CommandLine;

@CommandLine.Command(name = "version", aliases = {"v"}, description = "Displays the current version of the BuildCLI.", mixinStandardHelpOptions = true)
public class VersionCommand implements BuildCLICommand {

  @Override
  public void run() {
    new CommandLine(new CommandLineRunner()).execute("-V");
  }
}
