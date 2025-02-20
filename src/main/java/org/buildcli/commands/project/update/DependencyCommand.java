package org.buildcli.commands.project.update;

import org.buildcli.core.ProjectUpdater;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "dependency", aliases = {"d"}, description = "", mixinStandardHelpOptions = true)
public class DependencyCommand implements BuildCLICommand {
  @Option(names = {"--checkOnly"}, description = "", defaultValue = "false")
  private boolean checkOnly;

  @Override
  public void run() {
    try{
    ProjectUpdater updater = new ProjectUpdater();
    updater.updateNow(!checkOnly).execute();
      System.out.println(checkOnly ? "Checked for updates." : "Dependencies updated successfully.");
  }catch (Exception e){
      GlobalExceptionHandler.handleException(e);
    }
  }
}
