package org.buildcli.commands;

import org.buildcli.BuildCLI;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import picocli.CommandLine;

@CommandLine.Command(name = "version", aliases = {"v"}, description = "Displays the current version of the BuildCLI.", mixinStandardHelpOptions = true)
public class VersionCommand implements BuildCLICommand {

  @Override
  public void run() {
    try{
    new CommandLine(new BuildCLI()).execute("-V");
  }catch(Exception e){
      GlobalExceptionHandler.handleException(e);
    }
  }
}
