package org.buildcli.commands.project;

import org.buildcli.actions.commandline.CommandLineProcess;
import org.buildcli.actions.commandline.GradleProcess;
import org.buildcli.actions.commandline.MavenProcess;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.utils.tools.ToolChecks;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.logging.Logger;

@Command(name = "build", aliases = {"b"}, description = "Builds the project, either compiling or packaging, and logs the result.", mixinStandardHelpOptions = true)
public class BuildCommand implements BuildCLICommand {
  private final Logger logger = Logger.getLogger(BuildCommand.class.getName());

  @Option(names = {"--compileOnly", "--compile", "-c"}, description = "", defaultValue = "false")
  private boolean compileOnly;

  private String projectBuild = ToolChecks.checkIsMavenOrGradle();

  @Override
  public void run() {

    if (projectBuild.equals("Neither")) {
      logger.severe("Neither Maven nor Gradle project detected. Please ensure one of these build files (pom.xml or build.gradle) exists.");
      return;
    }

    CommandLineProcess process;

    if (compileOnly) {
      process = projectBuild.equals("Maven") ? MavenProcess.createCompileProcessor() : GradleProcess.createCompileProcessor();
    } else {
      process = projectBuild.equals("Maven") ? MavenProcess.createPackageProcessor() : GradleProcess.createPackageProcessor();
    }

    int exitCode = process.run();

    if (exitCode == 0) {
      logger.info("Project built successfully.");
    } else {
      logger.severe("Failed to build project. Process exited with code: " + exitCode);
    }
  }
}