package org.buildcli.commands.project.add;

import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Command(name = "profile", aliases = {"p"}, description = "Adds a new dependency to the project. Alias: 'p'. "
        + "This command allows adding profile file (e.g., test, dev, production) with extension .properties.",
        mixinStandardHelpOptions = true)
public class ProfileCommand implements BuildCLICommand {
  private final Logger LOGGER = Logger.getLogger(ProfileCommand.class.getName());

  @Parameters(index = "0")
  private String profile;

  @Override
  public void run() {
    String fileName = "src/main/resources/application-" + profile + ".properties";
    File profileFile = new File(fileName);

    String content = """
        # Configurações do perfil de %s
        app.name=BuildCLI %s Environment
        app.message=Running in %s Mode
        logging.level=DEBUG
        """.formatted(profile, profile, profile);

    try {
      if (profileFile.createNewFile()) {
        try (FileWriter writer = new FileWriter(profileFile)) {
          writer.write(content);
        }
        LOGGER.info(() -> "Configuration profile created: " + fileName);
      } else {
        LOGGER.warning(() -> "Configuration profile already exists: " + fileName);
      }
    } catch (IOException e) {
      GlobalExceptionHandler.handleException(e);
      LOGGER.log(Level.SEVERE, "Failed to create or write configuration profile: " + fileName, e);
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
    }
  }
}
