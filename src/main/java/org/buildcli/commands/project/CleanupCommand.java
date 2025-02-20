package org.buildcli.commands.project;

import org.buildcli.constants.MavenConstants;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import org.buildcli.utils.DirectoryCleanup;
import picocli.CommandLine.Command;

@Command(name = "cleanup", aliases = {"clean"}, description = "Removes generated files by cleaning the project's target directory.", mixinStandardHelpOptions = true)
public class CleanupCommand implements BuildCLICommand {
  @Override
  public void run() {
    try {
      DirectoryCleanup.cleanup(MavenConstants.TARGET);
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
    }
  }
}
