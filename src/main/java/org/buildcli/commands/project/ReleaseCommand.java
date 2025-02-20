package org.buildcli.commands.project;

import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import org.buildcli.utils.ReleaseManager;
import picocli.CommandLine.Command;

@Command(name = "release", description = "", mixinStandardHelpOptions = true)
public class ReleaseCommand implements BuildCLICommand {
  @Override
  public void run() {
    try{
    new ReleaseManager().automateRelease();
  }catch(Exception e){
      GlobalExceptionHandler.handleException(e);
    }
  }
}
