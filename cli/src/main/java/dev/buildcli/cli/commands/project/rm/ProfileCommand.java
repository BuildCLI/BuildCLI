package dev.buildcli.cli.commands.project.rm;

import dev.buildcli.core.domain.BuildCLICommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Command(name = "profile", aliases = {"p"}, description = "Deletes a configuration profile from the project. "
        + "Alias: 'p'. Removes the corresponding application-{profile}.properties file.", mixinStandardHelpOptions = true)
public class ProfileCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger(ProfileCommand.class);

  @Parameters(index = "0")
  private String profile;

  @Override
  public void run() {
    String fileName = "src/main/resources/application-" + profile + ".properties";
    var path = Paths.get(fileName);

    try {
      Files.deleteIfExists(path);
      logger.info( "Configuration profile deleted: {}", fileName);
    } catch (IOException e) {
      logger.error("Failed to delete configuration profile: {}", fileName, e);
    }
  }
}
