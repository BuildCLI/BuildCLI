package dev.buildcli.cli.commands.project.add;

import dev.buildcli.core.actions.dependency.DependencySearchService;
import dev.buildcli.core.constants.MavenConstants;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.PomUtils;
import dev.buildcli.core.utils.tools.maven.PomReader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


@Command(name = "dependency", aliases = {"d"}, description = "Adds a new dependency to the project. Alias: 'd'. "
        + "This command allows adding dependencies.", mixinStandardHelpOptions = true)
public class DependencyCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger(DependencyCommand.class);
  @Parameters
  private String dependency;
  @Option(names = {"--manual", "-m"}, description = "Defines if dependency will be added manually or not.")
  Boolean manually;

  @Override
  public void run() {
    try {
      String pomWithAddedDependencies = getPomWithAddedDependencies();

      try {
        Files.write(Paths.get(MavenConstants.FILE), pomWithAddedDependencies.getBytes());
          logger.info("Dependency added to pom.xml.");
      } catch (IOException e) {
        logger.error("Error adding dependency to pom.xml", e);
      }

    } catch (Exception e) {
      logger.error("Error adding dependency to pom.xml", e);
    }
  }

  @NotNull
  private String getPomWithAddedDependencies() {
    DependencySearchService service = new DependencySearchService();
    List<String> dependencies =  List.of(dependency);

    if(manually==null) {
     dependencies = service.searchDependecy(dependency);
    }
    List<String> pomWithAddedDependencies = new ArrayList<>();
    dependencies.forEach(dep -> {
      try {
        pomWithAddedDependencies.add(PomReader.addOrUpdateDependency(MavenConstants.FILE, PomUtils.convertToDependency(dep)));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    return pomWithAddedDependencies.getFirst();
  }
}
