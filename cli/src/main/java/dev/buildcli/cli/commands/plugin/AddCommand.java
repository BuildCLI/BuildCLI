package dev.buildcli.cli.commands.plugin;

import dev.buildcli.cli.utils.CommandUtils;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.domain.jar.Jar;
import dev.buildcli.core.utils.ProjectUtils;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.core.utils.filesystem.FindFilesUtils;
import dev.buildcli.core.utils.net.FileDownloader;
import dev.buildcli.plugin.utils.BuildCLIPluginUtils;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.confirm;
import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.question;

@Command(
    name = "add",
    aliases = {"a"},
    description = "Add plugin from git repository, jar (web or local) or a valid Java project",
    mixinStandardHelpOptions = true
)
public class AddCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger("AddPluginCommand");
  private static final String PLUGINS_DIR = Path.of(System.getProperty("user.home"), ".buildcli", "plugins").toString();
  private final BuildCLIConfig globalConfig = ConfigContextLoader.getAllConfigs();
  @Option(names = {"--file", "-f"}, description = "File can be a project or jar locally or remote")
  private String file;

  @Override
  public void run() {
    try {
      String pathOrUrl = file == null ? question("Enter Plugin path or URL", true) : file;
      processPluginSource(pathOrUrl);
    } catch (Exception e) {
      logger.error("Failed to add plugin", e);
      throw new RuntimeException("Failed to add plugin", e);
    }
  }

  private void processPluginSource(String pathOrUrl) throws IOException {
    if (pathOrUrl.startsWith("http")) {
      processRemoteSource(pathOrUrl);
    } else {
      processLocalSource(pathOrUrl);
    }
  }

  private void processRemoteSource(String url) throws IOException {
    if (url.endsWith(".git")) {
      processGitRepository(url);
    } else {
      processRemoteJar(url);
    }
  }

  private void processGitRepository(String gitUrl) throws IOException {
    File tempDir = Files.createTempDirectory(UUID.randomUUID().toString()).toFile();

    try (Git git = Git.cloneRepository().setURI(gitUrl).setDirectory(tempDir).call()) {
      buildAndCopyPlugins(tempDir);
    } catch (Exception e) {
      logger.error("Failed to clone git repository: {}", gitUrl, e);
      throw new IOException("Failed to clone git repository: " + gitUrl, e);
    }
  }

  private void processRemoteJar(String jarUrl) throws IOException {
    File downloadedFile = FileDownloader.download(jarUrl);

    if (isValidJarFile(downloadedFile)) {
      Jar jar = new Jar(downloadedFile);
      if (BuildCLIPluginUtils.isValid(jar)) {
        copyJarPlugin(jar);
      } else {
        logger.warn("Downloaded JAR is not a valid plugin: {}", downloadedFile);
      }
    } else {
      logger.warn("Downloaded file is not a valid JAR: {}", downloadedFile);
    }
  }

  private void processLocalSource(String path) throws IOException {
    Path filePath = Path.of(path);

    if (!Files.exists(filePath)) {
      throw new IllegalArgumentException("Plugin path does not exist: " + path);
    }

    if (Files.isDirectory(filePath)) {
      buildAndCopyPlugins(filePath.toFile());
    } else if (isValidJarFile(filePath.toFile())) {
      processLocalJar(filePath.toFile());
    } else {
      throw new IllegalArgumentException("Plugin path is not a valid JAR file or directory: " + path);
    }
  }

  private void processLocalJar(File jarFile) throws IOException {
    Jar jar = new Jar(jarFile);
    logger.info("Validating jar: {}", jar.getFile());

    if (BuildCLIPluginUtils.isValid(jar)) {
      logger.info("Jar is a valid plugin");
      copyJarPlugin(jar);
    } else {
      logger.info("Jar is not a valid plugin");
    }
  }

  private boolean isValidJarFile(File file) {
    return file.isFile() && file.getName().endsWith(".jar");
  }

  private void buildAndCopyPlugins(File directory) throws IOException {
    List<Jar> jars = loadPluginFromDirectory(directory);

    for (Jar jar : jars) {
      copyJarPlugin(jar);
    }
  }

  private void copyJarPlugin(Jar jar) throws IOException {
    final int maxRetries = 5;
    int retries = maxRetries;
    boolean copied = false;

    logger.info("Copying jar {}...", jar.getFile());
    Path destPath = Path.of(PLUGINS_DIR, jar.getFile().getName());
    Files.createDirectories(destPath.getParent());

    // Usar um nome de arquivo temporário para evitar conflitos
    Path tempDestPath = Path.of(PLUGINS_DIR, jar.getFile().getName() + ".tmp");

    if (Files.exists(destPath)) {
      if (!confirm("Do you want to overwrite existing plugin file?")) {
        logger.info("Plugin installation cancelled by user");
        return;
      }
    }

    while (retries > 0 && !copied) {
      try {
        // Primeiro copiar para o arquivo temporário
        Files.copy(jar.getFile().toPath(), tempDestPath, StandardCopyOption.REPLACE_EXISTING);

        // Tentar diferentes abordagens para substituir o arquivo original
        if (Files.exists(destPath)) {
          try {
            // Tentar renomear o arquivo temporário para o destino final
            Files.move(tempDestPath, destPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            copied = true;
          } catch (IOException e) {
            // Se falhar com ATOMIC_MOVE, tentar sem essa opção
            try {
              Files.move(tempDestPath, destPath, StandardCopyOption.REPLACE_EXISTING);
              copied = true;
            } catch (IOException e2) {
              // Se ainda falhar, tentar primeiro excluir e depois copiar
              tryDeleteWithRetries(destPath, 3);
              Files.move(tempDestPath, destPath);
              copied = true;
            }
          }
        } else {
          // Se o arquivo de destino não existir, basta renomear o temporário
          Files.move(tempDestPath, destPath);
          copied = true;
        }
      } catch (Exception e) {
        logger.error("Failed to copy plugin file: {}", jar.getFile(), e);
        logger.info("Retrying ({} attempts left)", retries - 1);
        retries--;

        // Aumentar o tempo de espera progressivamente entre as tentativas
        try {
          TimeUnit.MILLISECONDS.sleep(2000L + (maxRetries - retries) * 1000L);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new IOException("Operation interrupted", ex);
        }
      }
    }

    // Limpar arquivo temporário se ainda existir
    if (Files.exists(tempDestPath)) {
      try {
        Files.delete(tempDestPath);
      } catch (IOException e) {
        logger.warn("Could not delete temporary file: {}", tempDestPath, e);
      }
    }

    if (copied) {
      logger.info("Jar copied to {}...", destPath);
    } else {
      throw new IOException("Failed to copy plugin after " + maxRetries + " attempts. " +
          "Please ensure the plugin is not in use and try again later.");
    }
  }

  private void tryDeleteWithRetries(Path path, int retries) throws IOException {
    IOException lastException = null;

    for (int i = 0; i < retries; i++) {
      try {
        Files.delete(path);
        return; // Sucesso, saindo da função
      } catch (IOException e) {
        lastException = e;
        logger.warn("Failed to delete file (attempt {}): {}", i + 1, path);

        try {
          // Aguardar antes de tentar novamente (tempo crescente)
          TimeUnit.MILLISECONDS.sleep(1000L * (i + 1));
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new IOException("Operation interrupted", ie);
        }

        // Sugerir ao GC que execute para liberar recursos
        System.gc();
      }
    }

    // Se chegou aqui, todas as tentativas falharam
    if (lastException != null) {
      throw lastException;
    }
  }

  private List<Jar> loadPluginFromDirectory(File directory) {
    validateDirectory(directory);

    List<Jar> validPlugins = new ArrayList<>();

    logger.info("Building project...");
    int exitCode = CommandUtils.call("project", "build", "-p", directory.getAbsolutePath());

    if (exitCode != 0) {
      logger.warn("Build failed with exit code {}", exitCode);
      throw new RuntimeException("Build failed with exit code " + exitCode);
    }

    logger.info("Build complete. Finding jars...");
    List<File> jars = FindFilesUtils.searchJarFiles(directory);

    for (File jarFile : jars) {
      Jar jar = new Jar(jarFile);
      logger.info("Validating jar {}", jarFile);

      if (BuildCLIPluginUtils.isValid(jar)) {
        logger.info("Validated jar, is a valid jar plugin");
        validPlugins.add(jar);
      } else {
        logger.info("Jar is not a valid plugin");
      }
    }

    return validPlugins;
  }

  private void validateDirectory(File directory) {
    if (!directory.isDirectory()) {
      throw new IllegalArgumentException("Plugin path must be a directory");
    }

    if (!ProjectUtils.isValid(directory)) {
      throw new IllegalArgumentException("Plugin path must be a valid Java project directory (Maven or Gradle)");
    }
  }
}