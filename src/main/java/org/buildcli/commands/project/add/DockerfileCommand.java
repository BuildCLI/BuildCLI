package org.buildcli.commands.project.add;

import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Command(name = "dockerfile", aliases = {"docker", "df"}, description = "Generates a Dockerfile for the project. "
        + "Alias: 'docker' and 'df'. Allows customizing the base image, exposed ports, and file name.",
        mixinStandardHelpOptions = true)
public class DockerfileCommand implements BuildCLICommand {
  private Logger logger = Logger.getLogger(DockerfileCommand.class.getName());

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
          System.out.println("Dockerfile generated.");
        }
      } else {
        System.out.println("Dockerfile already exists.");
      }
      System.out.println("Dockerfile created successfully.");
      System.out.println("Use 'buildcli project run docker' to build and run the Docker container.");
    } catch (IOException e) {
      GlobalExceptionHandler.handleException(e);
      logger.log(Level.SEVERE, "Failed to setup Docker", e);
      System.err.println("Error: Could not setup Docker environment.");
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
    }
  }
}
