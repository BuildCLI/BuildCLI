package dev.buildcli.cli.commands.project.add;

import dev.buildcli.core.actions.dependency.DependencySearchService;
import dev.buildcli.core.constants.MavenConstants;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.log.SystemOutLogger;
import dev.buildcli.core.utils.tools.maven.PomReader;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Command(name = "dependency", aliases = {"d"}, description = "Adds a new dependency to the project. Alias: 'd'. "
        + "This command allows adding dependencies.", mixinStandardHelpOptions = true)
public class DependencyCommand implements BuildCLICommand {
  private static final Logger logger = Logger.getLogger(DependencyCommand.class.getName());
  @Parameters
  private String dependency;
  @Option(names = {"--manual", "-m"}, description = "Defines if dependency will be added manually or not.")
  Boolean manually;

  @Override
  public void run() {
    try {
      validateDependencyFormat(dependency);
      DependencySearchService service = new DependencySearchService();
      var pom = PomReader.read(MavenConstants.FILE);
      var pomData = PomReader.readAsString(MavenConstants.FILE);
      List<String> dependencies =  List.of(dependency);

      if(manually==null) {
       dependencies = service.searchDependency(dependency);
      }
      dependencies.forEach(pom::addDependency);

      try {
        String pomContent = pomData.replace(MavenConstants.DEPENDENCIES_PATTERN, pom.getDependencyFormatted());
        Files.write(Paths.get(MavenConstants.FILE), pomContent.getBytes());
        SystemOutLogger.log("Dependency added to pom.xml.");
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Error adding dependency to pom.xml", e);
      }

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error adding dependency to pom.xml", e);
    }
  }

  private void validateDependencyFormat(String dependency) {
    String[] tokens = dependency.split(":");
    if ((tokens.length != 2) && (tokens.length != 3)) {
      logger.log(Level.WARNING, "Invalid dependency format. Use 'groupId:artifactId'"
                  + "or 'groupId:artifactId:version'.");
      throw new RuntimeException();
    }
  }
}
