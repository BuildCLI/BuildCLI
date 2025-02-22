package dev.buildcli.cli.commands.project;

import dev.buildcli.core.constants.MavenConstants;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.DirectoryCleanup;
import picocli.CommandLine.Command;

@Command(name = "cleanup", aliases = {"clean"}, description = "Removes generated files by cleaning the project's target directory.", mixinStandardHelpOptions = true)
public class CleanupCommand implements BuildCLICommand {
  @Override
  public void run() {
    DirectoryCleanup.cleanup(MavenConstants.TARGET);
  }
}
