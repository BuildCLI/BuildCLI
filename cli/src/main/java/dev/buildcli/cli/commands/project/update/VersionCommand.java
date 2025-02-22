package dev.buildcli.cli.commands.project.update;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.SemVerManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "version", aliases = {"ver", "v"}, description = "", mixinStandardHelpOptions = true)
public class VersionCommand implements BuildCLICommand {
  @CommandLine.Parameters(index = "0")
  private String upgradeVersionType;

  @Override
  public void run() {
    new SemVerManager().manageVersion(upgradeVersionType);
  }
}
