package org.buildcli.commands.project.add;

import org.buildcli.constants.MavenConstants;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import org.buildcli.log.SystemOutLogger;
import org.buildcli.utils.tools.maven.PomReader;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Command(name = "dependency", aliases = {"d"}, description = "Adds a new dependency to the project. Alias: 'd'. "
        + "This command allows adding dependencies.", mixinStandardHelpOptions = true)
public class DependencyCommand implements BuildCLICommand {
  private static final Logger logger = Logger.getLogger(DependencyCommand.class.getName());
  @Parameters
  private String[] dependencies;

  @Override
  public void run() {
    try {
      var pom = PomReader.read(MavenConstants.FILE);
      var pomData = PomReader.readAsString(MavenConstants.FILE);
      Stream.of(dependencies).forEach(pom::addDependency);

      try {
        String pomContent = pomData.replace(MavenConstants.DEPENDENCIES_PATTERN, pom.getDependencyFormatted());
        Files.write(Paths.get(MavenConstants.FILE), pomContent.getBytes());
        SystemOutLogger.log("Dependency added to pom.xml.");
      } catch (IOException e) {
        GlobalExceptionHandler.handleException(e);
        logger.log(Level.SEVERE, "Error adding dependency to pom.xml", e);
      }

    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
      logger.log(Level.SEVERE, "Error adding dependency to pom.xml", e);
    }
  }
}
