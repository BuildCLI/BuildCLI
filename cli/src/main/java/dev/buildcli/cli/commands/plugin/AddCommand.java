package dev.buildcli.cli.commands.plugin;

import dev.buildcli.cli.utils.CommandUtils;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.domain.jar.Jar;
import dev.buildcli.core.utils.ProjectUtils;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.core.utils.filesystem.FindFilesUtils;
import dev.buildcli.core.utils.net.FileDownloader;
import dev.buildcli.plugin.utils.PluginUtils;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.buildcli.core.utils.input.InteractiveInputUtils.question;

@Command(
    name = "add",
    aliases = {"a"},
    description = "Add plugin from git repository, jar (web or local) or a valid Java project",
    mixinStandardHelpOptions = true
)
public class AddCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger("AddPluginCommand");
  private static final String PLUGINS_DIR = Path.of(System.getProperty("user.home"), ".buildcli", "plugins").toString();

  @Option(names = {"--file", "-f"}, description = "File can b, a project or jar locally or remote")
  private String file;

  private final BuildCLIConfig globalConfig = ConfigContextLoader.getAllConfigs();

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
      if (PluginUtils.isValid(jar)) {
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

    if (PluginUtils.isValid(jar)) {
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
    logger.info("Copying jar {}...", jar.getFile());
    Path destPath = Path.of(PLUGINS_DIR, jar.getFile().getName());
    Files.createDirectories(destPath.getParent());
    Files.copy(jar.getFile().toPath(), destPath);
    logger.info("Jar copied to {}...", destPath);
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

      if (PluginUtils.isValid(jar)) {
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