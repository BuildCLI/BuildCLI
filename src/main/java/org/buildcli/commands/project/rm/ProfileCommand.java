package org.buildcli.commands.project.rm;

import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

@Command(name = "profile", aliases = {"p"}, description = "Deletes a configuration profile from the project. "
        + "Alias: 'p'. Removes the corresponding application-{profile}.properties file.", mixinStandardHelpOptions = true)
public class ProfileCommand implements BuildCLICommand {
  private final Logger LOGGER = Logger.getLogger(ProfileCommand.class.getName());

  @Parameters(index = "0")
  private String profile;

  @Override
  public void run() {
    String fileName = "src/main/resources/application-" + profile + ".properties";
    var path = Paths.get(fileName);

    try {
      Files.deleteIfExists(path);
      LOGGER.info(() -> "Configuration profile deleted: " + fileName);
    } catch (IOException e) {
      GlobalExceptionHandler.handleException(e);
      LOGGER.log(Level.SEVERE, "Failed to delete configuration profile: " + fileName, e);
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
    }
  }
}
