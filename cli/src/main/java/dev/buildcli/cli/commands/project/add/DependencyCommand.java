package dev.buildcli.cli.commands.project.add;

import dev.buildcli.core.constants.MavenConstants;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.tools.maven.PomReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Command(name = "dependency", aliases = {"d"}, description = "Adds a new dependency to the project. Alias: 'd'. "
        + "This command allows adding dependencies.", mixinStandardHelpOptions = true)
public class DependencyCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger(DependencyCommand.class);
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
        logger.info("Dependency added to pom.xml.");
      } catch (IOException e) {
        logger.error("Error adding dependency to pom.xml", e);
      }

    } catch (Exception e) {
      logger.error("Error adding dependency to pom.xml", e);
    }
  }
}
