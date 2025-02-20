package org.buildcli.commands.project;

import org.buildcli.core.ProjectTester;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import picocli.CommandLine.Command;

@Command(name = "test", aliases = {"t"}, description = "Executes the project tests.", mixinStandardHelpOptions = true)
public class TestCommand implements BuildCLICommand {
  @Override
  public void run() {
    try{
    new ProjectTester().execute();
  }catch(Exception e){
      GlobalExceptionHandler.handleException(e);
    }
  }
}
