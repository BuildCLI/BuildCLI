package dev.buildcli.cli.commands.run;

import dev.buildcli.core.actions.commandline.DockerProcess;
import dev.buildcli.core.domain.BuildCLICommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "dockerfile", aliases = {"docker", "d"}, description = "Builds and runs a Docker image for the project."
        + " Alias: 'docker' and 'd'. Builds the Docker image and starts the container, exposing port 8080.")
public class DockerfileCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger(DockerfileCommand.class);

  @Option(names = {"--name", "-n"}, description = "", defaultValue = "buildcli-app")
  private String name;

  @Override
  public void run() {
    var buildExitCode = DockerProcess.createBuildProcess(name).run();

    if (buildExitCode != 0) {
      logger.error("Failed to build Docker image. Exit code: {}", buildExitCode);
      return;
    }
    logger.info("Docker image built successfully.");

    // Executar o comando "docker run"
    int runExitCode = DockerProcess.createRunProcess(name).run();
    if (runExitCode != 0) {
      logger.error("Failed to run Docker container. Exit code: {}", runExitCode);
    } else {
      logger.info("Docker container is running on port 8080.");
    }
  }
}
