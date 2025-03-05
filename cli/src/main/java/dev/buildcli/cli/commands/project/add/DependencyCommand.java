package dev.buildcli.cli.commands.project.add;

import dev.buildcli.core.constants.MavenConstants;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.PomUtils;
import dev.buildcli.core.utils.tools.maven.PomWriter;
import dev.buildcli.core.utils.tools.maven.PomReader;
import io.github.dumijdev.dpxml.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.function.Predicate;


@Command(name = "dependency", aliases = {"d", "dep"}, description = "Adds a new dependency to the project. Alias: 'd'. "
    + "This command allows adding dependencies.", mixinStandardHelpOptions = true)
public class DependencyCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger(DependencyCommand.class);
  @Parameters
  private String[] dependencies;

  private static Predicate<? super Node> compareDependency(Node node) {
    var groupIdNode =  node.child("groupId").content();
    var artifactIdNode =  node.child("artifactId").content();

    return dep -> {
      var groupIdDep = dep.child("groupId").content();
      var artifactIdDep = dep.child("artifactId").content();

      return groupIdDep.equals(groupIdNode) && artifactIdDep.equals(artifactIdNode);
    };
  }

  @Override
  public void run() {
    try {
      var pomProject = PomReader.read(MavenConstants.FILE);

      var dependencies = pomProject.child("dependencies").children("dependency");

      for (var dependency : this.dependencies) {
        var dep = PomUtils.parseDependency(dependency);

        if (dependencies.stream().noneMatch(compareDependency(dep))) {
          dependencies.add(dep);
        }
      }

      PomWriter.write(pomProject);

    } catch (Exception e) {
      logger.error("Error adding dependency to pom.xml", e);
    }
  }
}
