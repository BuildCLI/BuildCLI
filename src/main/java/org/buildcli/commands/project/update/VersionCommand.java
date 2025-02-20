package org.buildcli.commands.project.update;

import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import org.buildcli.utils.SemVerManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "version", aliases = {"ver", "v"}, description = "", mixinStandardHelpOptions = true)
public class VersionCommand implements BuildCLICommand {
  @CommandLine.Parameters(index = "0")
  private String upgradeVersionType;

  @Override
  public void run() {
    try {
      new SemVerManager().manageVersion(upgradeVersionType);
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
    }
  }
}
