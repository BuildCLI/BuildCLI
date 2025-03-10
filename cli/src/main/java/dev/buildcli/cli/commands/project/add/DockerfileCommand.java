package dev.buildcli.cli.commands.project.add;

import dev.buildcli.core.domain.BuildCLICommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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
  @Option(names = {"--env", "-e"}, description = "", defaultValue = "")
  private String envVariable;
  @Option(names = {"--force"}, description = "", defaultValue = "false")
  private Boolean force;

  @Override
  public void run() {
    try {
      File dockerfile = new File(name);
      if (dockerfile.createNewFile() || force) {
        try (FileWriter writer = new FileWriter(dockerfile, false)) {

          String[] envVars = process_env_variables(envVariable);

          var builder = new StringBuilder("FROM ").append(fromImage).append("\n");
          builder.append("WORKDIR ").append("/app").append("\n");
          builder.append("COPY ").append("target/*.jar app.jar").append("\n");
          ports.forEach(port -> {
            builder.append("EXPOSE ").append(port).append("\n");
          });
          if (envVars != null) {
            for (String s: envVars) {
              if (s != null) builder.append("ENV ").append(s).append("\n");
            }
          }
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
      logger.log(Level.SEVERE, "Failed to setup Docker", e);
      System.err.println("Error: Could not setup Docker environment.");
    }
  }

  private String[] process_env_variables(String envVariable) {
    String[] envVars = null;
    if (!envVariable.equals("")) {
      envVars = envVariable.split(";");
      for (int i = 0; i < envVars.length; i++) {
        if (envVars[i].contains("JAVA_TOOL_OPTIONS")) {
          String java_tool_options = "";
          java_tool_options = envVars[i].split("=")[1];
          java_tool_options = validate_jvm_options(java_tool_options);
          if (java_tool_options == null) {
            envVars[i] = null;
            continue;
          }
          envVars[i] = new StringBuffer().append("JAVA_TOOL_OPTIONS=\"").append(java_tool_options).append("\"").toString();
          continue;
        }
        envVars[i] = new StringBuffer(envVars[i].split("=")[0]).append("=\"").append(envVars[i].split("=")[1]).append("\"").toString();
      }
    }

    return envVars;
  }

  private String validate_jvm_options(String options) {
    if (!options.equals("")) {
      try {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("--dry-run");
        command.addAll(Arrays.asList(options.split(" ")));
        command.add("-version");
        var processBuilder = new ProcessBuilder(command);
        var process = processBuilder.start();
        var code = process.waitFor();
        if (code != 0) {
          BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
          System.err.println(br.readLine());
          return null;
        }
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }

    return options;
  }
}
