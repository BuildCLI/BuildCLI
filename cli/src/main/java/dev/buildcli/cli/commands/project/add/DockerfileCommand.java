package dev.buildcli.cli.commands.project.add;

import dev.buildcli.cli.commands.config.PrintCommand;
import dev.buildcli.core.domain.BuildCLICommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Command(name = "dockerfile", aliases = {"docker", "df"}, description = "Generates a Dockerfile for the project. "
        + "Alias: 'docker' and 'df'. Allows customizing the base image, exposed ports, and file name.",
        mixinStandardHelpOptions = true)
public class DockerfileCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger(PrintCommand.class);

  @Option(names = {"--name", "-n"}, description = "", defaultValue = "Dockerfile")
  private String name;
  @Option(names = {"--from", "-f"}, description = "", defaultValue = "openjdk:17-jdk-slim")
  private String fromImage;
  @Option(names = {"--port", "-p"}, description = "", defaultValue = "8080", split = ",")
  private List<Integer> ports;

  @Override
  public void run() {
    try {
      File dockerfile = new File(name);
      if (dockerfile.createNewFile()) {
        try (FileWriter writer = new FileWriter(dockerfile)) {

          var builder = new StringBuilder("FROM ").append(fromImage).append("\n");
          builder.append("WORKDIR ").append("/app").append("\n");
          builder.append("COPY ").append("target/*.jar app.jar").append("\n");
          ports.forEach(port -> {
            builder.append("EXPOSE ").append(port).append("\n");
          });
          builder.append("ENTRYPOINT ").append("[\"java\", \"-jar\", \"app.jar\"]").append("\n");

          writer.write(builder.toString());
          logger.info("Dockerfile generated.");
        }
      } else {
        logger.info("Dockerfile already exists.");
      }
      logger.info("Dockerfile created successfully.");
      logger.info("Use 'buildcli project run docker' to build and run the Docker container.");
    } catch (IOException e) {
      logger.error("Failed to setup Docker", e);
      logger.error("Error: Could not setup Docker environment.");
    }
  }
}
