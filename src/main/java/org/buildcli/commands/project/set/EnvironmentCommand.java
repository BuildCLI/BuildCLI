package org.buildcli.commands.project.set;

import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

@Command(name = "environment", aliases = {"env", "e"}, description = "", mixinStandardHelpOptions = true)
public class EnvironmentCommand implements BuildCLICommand {
  private final Logger logger = Logger.getLogger(EnvironmentCommand.class.getName());
  private final Path configPath = Path.of("environment.config");;

  @Parameters(index = "0")
  private String environment;

  @Override
  public void run() {
    try {
      String content = "active.profile=" + environment; // Formata a string no formato chave-valor
      Files.writeString(configPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      logger.info("Environment set to: " + environment);
      System.out.println("Environment set to: " + environment);
    } catch (IOException e) {
      GlobalExceptionHandler.handleException(e);
      logger.severe("Failed to set environment: " + e.getMessage());
      System.err.println("Error: Could not set environment.");
    }catch (Exception e){
      GlobalExceptionHandler.handleException(e);
    }
  }
}
