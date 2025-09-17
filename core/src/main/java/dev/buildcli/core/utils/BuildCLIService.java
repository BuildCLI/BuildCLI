package dev.buildcli.core.utils;

import dev.buildcli.core.actions.commandline.CommandLineProcess;
import dev.buildcli.core.actions.commandline.MavenProcess;
import dev.buildcli.core.constants.ConfigDefaultConstants;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.domain.git.GitCommandExecutor;
import dev.buildcli.core.log.SystemOutLogger;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.core.utils.console.input.InteractiveInputUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static dev.buildcli.core.utils.BeautifyShell.content;

public class BuildCLIService {

  private final GitCommandExecutor gitExec;
  private final PrintStream out;

  /**
   * Default constructor for production use.
   * Instantiates its own dependencies.
   */
  public BuildCLIService() {
    this.gitExec = new GitCommandExecutor();
    this.out = System.out;
  }

  /**
   * Constructor for testing, allowing dependency injection of mocks.
   * @param gitExec The Git command executor.
   * @param out The output stream to print to.
   */
  public BuildCLIService(GitCommandExecutor gitExec, PrintStream out) {
    this.gitExec = gitExec;
    this.out = out;
  }

  public void welcome() {
    BuildCLIConfig configs = ConfigContextLoader.getAllConfigs();
    if (configs.getPropertyAsBoolean(ConfigDefaultConstants.BANNER_ENABLED).orElse(true)) {
      if (configs.getProperty(ConfigDefaultConstants.BANNER_PATH).isEmpty()) {
        printOfficialBanner();
      } else {
        Path path = Path.of(configs.getProperty(ConfigDefaultConstants.BANNER_PATH).get());
        if (Files.exists(path) && Files.isRegularFile(path)) {
          try {
            out.println(Files.readString(path));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        } else {
          printOfficialBanner();
        }
      }
    }
  }

  private void printOfficialBanner() {
    out.println(",-----.          ,--.,--.   ,--. ,-----.,--.   ,--.");
    out.println("|  |) /_ ,--.,--.`--'|  | ,-|  |'  .--./|  |   |  |");
    out.printf("|  .-.  \\|  ||  |,--.|  |' .-. ||  |    |  |   |  |       %s%n", content("Built by the community, for the community").blueFg().italic());
    out.println("|  '--' /'  ''  '|  ||  |\\ `-' |'  '--'\\|  '--.|  |");
    out.println("`------'  `----' `--'`--' `---'  `-----'`-----'`--'");
    out.println();
  }

  public void checkUpdatesBuildCLIAndUpdate() {
    String buildCLIDirectory = getBuildCLIBuildDirectory();
    if (buildCLIDirectory == null) {
      // Cannot determine directory, so we cannot check for updates.
      return;
    }
    String localRepository = gitExec.findGitRepository(buildCLIDirectory);
    if (localRepository == null) {
      // Not a git repository, so we cannot check for updates.
      return;
    }
    boolean isUpdated = gitExec.checkIfLocalRepositoryIsUpdated(localRepository, "https://github.com/BuildCLI/BuildCLI.git");
    if (!isUpdated) {
      SystemOutLogger.log("""
              \u001B[33m
              ATTENTION: Your BuildCLI is outdated!
              \u001B[0m""");
      updateBuildCLI(buildCLIDirectory, localRepository);
    }
  }

  private void updateBuildCLI(String buildCLIDirectory, String localRepository) {
    if (InteractiveInputUtils.confirm("Do you want to update BuildCLI?")) {
      gitExec.updateLocalRepositoryFromUpstream(localRepository, "https://github.com/BuildCLI/BuildCLI.git");
      generateBuildCLIJar(buildCLIDirectory);
      String homeBuildCLI = OS.getHomeBinDirectory();
      OS.cpDirectoryOrFile(buildCLIDirectory + "/cli/target/buildcli.jar", homeBuildCLI);
      OS.chmodX(homeBuildCLI + "/buildcli.jar");
      SystemOutLogger.log("\u001B[32mBuildCLI updated successfully!\u001B[0m");
    } else {
      SystemOutLogger.log("\u001B[33mBuildCLI update canceled!\u001B[0m");
    }
  }


  private void generateBuildCLIJar(String buildCLIDirectory) {
    CommandLineProcess process = MavenProcess.createPackageProcessor(new File(buildCLIDirectory));
    int exitCode = process.run();

    if (exitCode == 0) {
      SystemOutLogger.log("Success...");
    } else {
      SystemOutLogger.log("Failure...");
    }
  }

  private String getBuildCLIBuildDirectory() {
    try (InputStream inputStream = BuildCLIService.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
      if (inputStream == null) {
        return getFallbackDirectory();
      }
      return readManifest(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Error while trying to read the META-INF/MANIFEST.MF", e);
    }
  }

  private String getFallbackDirectory() {
    try {
      String classLocation = BuildCLIService.class
              .getProtectionDomain()
              .getCodeSource()
              .getLocation()
              .toURI().getPath();
      File location = new File(classLocation);
      // This logic helps find the project root when running from an IDE
      if (location.getAbsolutePath().contains("target" + File.separator + "classes")) {
        return location.getAbsolutePath().split("target")[0];
      }
      // If running from a JAR, it should point to the directory containing the JAR
      return location.getParentFile().getAbsolutePath();
    } catch(Exception e) {
      System.err.println("Could not determine build directory: " + e.getMessage());
      return null;
    }
  }

  private String readManifest(InputStream inputStream) {
    try {
      Manifest manifest = new Manifest(inputStream);
      Attributes attributes = manifest.getMainAttributes();
      String buildDirectory = attributes.getValue("Build-Directory");

      if (buildDirectory == null) {
        return getFallbackDirectory();
      }

      return buildDirectory;
    } catch (IOException e) {
      throw new RuntimeException("Error while trying to read the content of the MANIFEST.MF file", e);
    }
  }
}