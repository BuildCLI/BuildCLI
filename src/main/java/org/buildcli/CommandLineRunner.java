package org.buildcli;

import org.buildcli.log.config.LoggingConfig;
import org.buildcli.utils.BuildCLIService;
import picocli.CommandLine;

public class CommandLineRunner {
  public static void main(String[] args) {
    LoggingConfig.configure();
    BuildCLIService.welcome();

    var commandLine = new CommandLine(new BuildCLI());

    int exitCode = commandLine.execute(args);
    BuildCLIService.checkUpdatesBuildCLIAndUpdate();

    System.exit(exitCode);
  }
}
