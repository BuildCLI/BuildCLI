package dev.buildcli.cli.commands.project;

import dev.buildcli.core.actions.commandline.CommandLineProcess;
import dev.buildcli.core.actions.commandline.GradleProcess;
import dev.buildcli.core.actions.commandline.MavenProcess;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.tools.ToolChecks;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.logging.Logger;

@Command(name = "build", aliases = {"b"}, description = "Builds the project, either compiling or packaging, and logs the result.", mixinStandardHelpOptions = true)
public class BuildCommand implements BuildCLICommand {
  private final Logger logger = Logger.getLogger(BuildCommand.class.getName());

  @Option(names = {"--compileOnly", "--compile", "-c"}, description = "Indicator to only compile project, e.g, mvn clean compile", defaultValue = "false")
  private boolean compileOnly;

  @Option(names = {"--path", "-p"}, description = "Path to project", defaultValue = ".")
  private File path;


  @Override
  public void run() {
    final var projectBuild = ToolChecks.checkIsMavenOrGradle(path);

    if (projectBuild.equals("Neither")) {
      logger.severe("Neither Maven nor Gradle project detected. Please ensure one of these build files (pom.xml or build.gradle) exists.");
      return;
    }

    CommandLineProcess process;

    if (compileOnly) {
      process = projectBuild.equals("Maven") ? MavenProcess.createCompileProcessor(path) : GradleProcess.createCompileProcessor(path);
    } else {
      process = projectBuild.equals("Maven") ? MavenProcess.createPackageProcessor(path) : GradleProcess.createPackageProcessor(path);
    }

    int exitCode = process.run();

    if (exitCode == 0) {
      logger.info("Project built successfully.");
    } else {
      logger.severe("Failed to build project. Process exited with code: " + exitCode);
    }
  }
}