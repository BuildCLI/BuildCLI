package dev.buildcli.cli.commands.project.rm;

import dev.buildcli.core.constants.MavenConstants;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.PomUtils;
import dev.buildcli.core.utils.tools.maven.PomReader;
import dev.buildcli.core.utils.tools.maven.PomWriter;
import io.github.dumijdev.dpxml.model.Node;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Command(name = "dependency", aliases = {"d", "dep"}, description = "Removes one or more dependencies from the project's "
    + "pom.xml file.", mixinStandardHelpOptions = true)
public class DependencyCommand implements BuildCLICommand {
  private final Logger logger = Logger.getLogger(DependencyCommand.class.getName());
  @Parameters
  private String[] dependencies;

  @Override
  public void run() {
    try {
      var pom = PomReader.read(MavenConstants.FILE);
      var dependenciesNodes = pom.child("dependencies").children("dependency");

      for (String s : dependencies) {
        Node node = PomUtils.parseDependency(s);
        dependenciesNodes.removeIf(dependency -> compareDependency(node).test(dependency));
      }

      PomWriter.write(pom);
      logger.info("Dependency removed from pom.xml.");

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error removing dependency from pom.xml", e);
    }
  }

  private static Predicate<? super Node> compareDependency(Node node) {
    var groupIdNode = node.child("groupId").content();
    var artifactIdNode = node.child("artifactId").content();

    return dep -> {
      var groupIdDep = dep.child("groupId").content();
      var artifactIdDep = dep.child("artifactId").content();

      return groupIdDep.equals(groupIdNode) && artifactIdDep.equals(artifactIdNode);
    };
  }
}
