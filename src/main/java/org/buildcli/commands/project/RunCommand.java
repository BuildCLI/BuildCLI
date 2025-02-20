package org.buildcli.commands.project;

import org.buildcli.actions.commandline.JavaProcess;
import org.buildcli.actions.commandline.MavenProcess;
import org.buildcli.commands.project.run.DockerfileCommand;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import org.buildcli.utils.ProfileManager;
import picocli.CommandLine.Command;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Command(name = "run", description = "Executes the project with the active properties file.", subcommands = {DockerfileCommand.class}, mixinStandardHelpOptions = true)
public class RunCommand implements BuildCLICommand {
  private final Logger logger = Logger.getLogger(RunCommand.class.getName());
  private final ProfileManager profileManager = new ProfileManager();

  @Override
  public void run() {
    try {
      // Carregar o perfil ativo
      String activeProfile = profileManager.getActiveProfile();
      if (activeProfile == null) {
        logger.warning("No active profile set. Using default profile.");
        activeProfile = "default";
      }

      // Carregar as propriedades do perfil ativo
      Properties properties = loadProfileProperties(activeProfile);
      String profileMessage = properties.getProperty("app.message", "Running with no specific profile");

      // Exibir a mensagem do perfil ativo no console
      System.out.println("Active Profile: " + activeProfile);
      System.out.println(profileMessage);

      MavenProcess.createPackageProcessor().run();
      runJar(); // Executa o JAR gerado
    } catch (IOException | InterruptedException e) {
      GlobalExceptionHandler.handleException(e);
      logger.log(Level.SEVERE, "Failed to run project", e);
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
    }
  }

  private Properties loadProfileProperties(String profile) {
    Properties properties = new Properties();
    String propertiesFile = "src/main/resources/application-" + profile + ".properties";
    try (InputStream input = Files.newInputStream(Paths.get(propertiesFile))) {
      properties.load(input);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to load profile properties file: " + propertiesFile, e);
    }
    return properties;
  }

  private void runJar() throws IOException, InterruptedException {
    File targetDir = new File("target");
    if (!targetDir.exists() || !targetDir.isDirectory()) {
      throw new IOException("Target directory does not exist or is not a directory.");
    }

    // Busca pelo arquivo JAR na pasta target
    File[] jarFiles = targetDir.listFiles((dir, name) -> name.endsWith(".jar"));
    if (jarFiles == null || jarFiles.length == 0) {
      throw new IOException("No JAR file found in target directory.");
    }

    // Assume que o primeiro arquivo JAR encontrado é o correto
    File jarFile = jarFiles[0];
    String jarPath = jarFile.getAbsolutePath();

    int exitCode = JavaProcess.createRunJarProcess(jarPath).run();
    if (exitCode != 0) {
      throw new IOException("Failed to run project JAR. Process exited with code " + exitCode);
    }
  }
}
