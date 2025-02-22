package dev.buildcli.cli.commands.project;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.ReleaseManager;
import picocli.CommandLine.Command;

@Command(name = "release", description = "", mixinStandardHelpOptions = true)
public class ReleaseCommand implements BuildCLICommand {
  @Override
  public void run() {
    new ReleaseManager().automateRelease();
  }
}
