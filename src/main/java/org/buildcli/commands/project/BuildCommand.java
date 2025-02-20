package org.buildcli.commands.project;

import org.buildcli.actions.commandline.MavenProcess;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.logging.Logger;

@Command(name = "build", aliases = {"b"}, description = "Builds the project, either compiling or packaging, and logs the result.", mixinStandardHelpOptions = true)
public class BuildCommand implements BuildCLICommand {
  private final Logger logger = Logger.getLogger(BuildCommand.class.getName());

  @Option(names = {"--compileOnly", "--compile", "-c"}, description = "", defaultValue = "false")
  private boolean compileOnly;


  @Override
  public void run() {
    try{
    MavenProcess process;
    if (compileOnly) {
      process = MavenProcess.createCompileProcessor();
    } else {
      process = MavenProcess.createPackageProcessor();
    }

    int exitCode = process.run();

    if (exitCode == 0) {
      logger.info("Project compiled successfully. JAR file generated in target directory.");
    } else {
      GlobalExceptionHandler.handleException(new Exception("Failed to compile project. Maven exited with code: " + exitCode));
    }
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
    }
  }
}