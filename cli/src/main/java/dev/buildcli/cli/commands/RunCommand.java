package dev.buildcli.cli.commands;

import dev.buildcli.core.actions.commandline.CommandLineProcess;
import dev.buildcli.core.actions.commandline.JavaProcess;
import dev.buildcli.core.actions.commandline.MavenProcess;
import dev.buildcli.cli.commands.run.DockerfileCommand;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.ProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Command(name = "run", description = "Executes the project with the active properties file.", subcommands = {DockerfileCommand.class}, mixinStandardHelpOptions = true)
public class RunCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger(RunCommand.class);
  private final ProfileManager profileManager = new ProfileManager();

  @Parameters(index = "0", description = "The file or directory to run. If a directory, it will package and run the project.", arity = "0..1", paramLabel = "<file-or-dir>", defaultValue = ".")
  private File file;

  @Parameters(index = "1..*", arity = "0..*", paramLabel = "<args>", description = "Arguments to pass to the program.")
  private String[] args;

  @Override
  public void run() {
    if (file == null || (!file.exists() && file.isDirectory())) {
      throw new IllegalArgumentException("The specified path does not exist or is not a directory.");
    }

    try {
      CommandLineProcess process;
      if (file.isDirectory()) {
        process = createRunProjectProcess();
      } else if (file.isFile()) {
        if (file.getName().endsWith(".jar")) {
          process = JavaProcess.createRunJarProcess(file.getAbsolutePath(), args);
        } else if (file.getName().endsWith(".java")) {
          process = JavaProcess.createRunClassProcess(file.getAbsolutePath(), args);
        } else {
          throw new IllegalArgumentException("File must be a .jar or .java file.");
        }
      } else {
        throw new IllegalArgumentException("Parameter must be a file or a directory.");
      }

      var exitedCode = process.run();
      if (exitedCode != 0) {
        throw new IOException("Process exited with code " + exitedCode);
      }
    } catch (IOException | InterruptedException e) {
      logger.error("Failed to run project", e);
      Thread.currentThread().interrupt();
    }
  }

  private CommandLineProcess createRunProjectProcess() throws IOException, InterruptedException {
    // Carregar o perfil ativo
    String activeProfile = profileManager.getActiveProfile();
    if (activeProfile == null) {
      logger.warn("No active profile set. Using default profile.");
      activeProfile = "default";
    }

    // Carregar as propriedades do perfil ativo
    Properties properties = loadProfileProperties(activeProfile);
    String profileMessage = properties.getProperty("app.message", "Running with no specific profile");

    // Exibir a mensagem do perfil ativo no console
    logger.info("Active Profile: {}", activeProfile);
    logger.info(profileMessage);

    MavenProcess.createPackageProcessor().run();
    var jarPath = findJar();

    return JavaProcess.createRunJarProcess(jarPath);
  }

  private Properties loadProfileProperties(String profile) {
    Properties properties = new Properties();
    String propertiesFile = "src/main/resources/application-" + profile + ".properties";
    try (InputStream input = Files.newInputStream(Paths.get(propertiesFile))) {
      properties.load(input);
    } catch (IOException e) {
      logger.info("Failed to load profile properties file: {}", propertiesFile, e);
    }
    return properties;
  }

  private String findJar() throws IOException, InterruptedException {
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
    return jarFile.getAbsolutePath();
  }
}
