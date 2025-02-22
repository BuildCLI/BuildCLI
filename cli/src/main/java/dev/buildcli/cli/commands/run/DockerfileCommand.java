package dev.buildcli.cli.commands.run;

import dev.buildcli.core.actions.commandline.DockerProcess;
import dev.buildcli.core.domain.BuildCLICommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.logging.Logger;

@Command(name = "dockerfile", aliases = {"docker", "d"}, description = "Builds and runs a Docker image for the project."
        + " Alias: 'docker' and 'd'. Builds the Docker image and starts the container, exposing port 8080.")
public class DockerfileCommand implements BuildCLICommand {
  private final Logger logger = Logger.getLogger(DockerfileCommand.class.getName());

  @Option(names = {"--name", "-n"}, description = "", defaultValue = "buildcli-app")
  private String name;

  @Override
  public void run() {
    var buildExitCode = DockerProcess.createBuildProcess(name).run();

    if (buildExitCode != 0) {
      logger.severe("Failed to build Docker image. Exit code: " + buildExitCode);
      return;
    }
    System.out.println("Docker image built successfully.");

    // Executar o comando "docker run"
    int runExitCode = DockerProcess.createRunProcess(name).run();
    if (runExitCode != 0) {
      logger.severe("Failed to run Docker container. Exit code: " + runExitCode);
    } else {
      System.out.println("Docker container is running on port 8080.");
    }
  }
}
