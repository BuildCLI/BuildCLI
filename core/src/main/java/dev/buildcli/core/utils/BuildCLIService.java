package dev.buildcli.core.utils;

import dev.buildcli.core.actions.commandline.CommandLineProcess;
import dev.buildcli.core.actions.commandline.MavenProcess;
import dev.buildcli.core.constants.ConfigDefaultConstants;
import dev.buildcli.core.domain.git.GitCommandExecutor;
import dev.buildcli.core.log.SystemOutLogger;
import dev.buildcli.core.utils.config.ConfigContextLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static dev.buildcli.core.utils.BeautifyShell.content;

import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.confirm;

/*
*
*
,-----.          ,--.,--.   ,--. ,-----.,--.   ,--.
|  |) /_ ,--.,--.`--'|  | ,-|  |'  .--./|  |   |  |
|  .-.  \|  ||  |,--.|  |' .-. ||  |    |  |   |  |
|  '--' /'  ''  '|  ||  |\ `-' |'  '--'\|  '--.|  |
`------'  `----' `--'`--' `---'  `-----'`-----'`--'

*
* */

public class BuildCLIService {

  private static GitCommandExecutor gitExec = new GitCommandExecutor();

  private static final String buildCLIDirectory = getBuildCLIBuildDirectory();
  private static  String localRepository = gitExec.findGitRepository(buildCLIDirectory);

  public BuildCLIService() {
  }

  public BuildCLIService(GitCommandExecutor gitCommandExecutor, String localRepository) {
    gitExec = gitCommandExecutor;
    this.localRepository = localRepository;
  }

  public static void welcome() {
    var configs = ConfigContextLoader.getAllConfigs();
    if (configs.getPropertyAsBoolean(ConfigDefaultConstants.BANNER_ENABLED).orElse(true)) {
      if (configs.getProperty(ConfigDefaultConstants.BANNER_PATH).isEmpty()) {
        printOfficialBanner();
      } else {
        var path = Path.of(configs.getProperty(ConfigDefaultConstants.BANNER_PATH).get());
        if (Files.exists(path) && Files.isRegularFile(path)) {
          try {
            System.out.println(Files.readString(path));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        } else {
          printOfficialBanner();
        }
      }
    }
  }

  private static void printOfficialBanner() {
    System.out.println(",-----.          ,--.,--.   ,--. ,-----.,--.   ,--.");
    System.out.println("|  |) /_ ,--.,--.`--'|  | ,-|  |'  .--./|  |   |  |");
    System.out.printf("|  .-.  \\|  ||  |,--.|  |' .-. ||  |    |  |   |  |       %s%n", content("Built by the community, for the community").blueFg().italic());
    System.out.println("|  '--' /'  ''  '|  ||  |\\ `-' |'  '--'\\|  '--.|  |");
    System.out.println("`------'  `----' `--'`--' `---'  `-----'`-----'`--'");
    System.out.println();
  }

  private static void updateBuildCLI() {
    if (updateRepository()) {
      generateBuildCLIJar();
      String homeBuildCLI = OS.getHomeBinDirectory();
      OS.cpDirectoryOrFile(buildCLIDirectory + "/target/buildcli.jar", homeBuildCLI);
      OS.chmodX(homeBuildCLI + "/buildcli.jar");
      SystemOutLogger.log("\u001B[32mBuildCLI updated successfully!\u001B[0m");
    } else {
      SystemOutLogger.log("\u001B[33mBuildCLI update canceled!\u001B[0m");
    }
  }

  public static void checkUpdatesBuildCLIAndUpdate() {
    boolean updated = gitExec.checkIfLocalRepositoryIsUpdated(localRepository, "https://github.com/BuildCLI/BuildCLI.git");
    if (!updated) {
      SystemOutLogger.log("""
          \u001B[33m
          ATTENTION: Your BuildCLI is outdated!
          \u001B[0m""");
      updateBuildCLI();
    }
  }

  private static boolean updateRepository() {
    if (confirm("update BuildCLI?")) {
      gitExec.updateLocalRepositoryFromUpstream(localRepository, "https://github.com/BuildCLI/BuildCLI.git");
      return true;
    }
    return false;
  }

  private static void generateBuildCLIJar() {
    OS.cdDirectory("");
    OS.cdDirectory(buildCLIDirectory);

    CommandLineProcess process = MavenProcess.createPackageProcessor(new File("."));

    var exitedCode = process.run();

    if (exitedCode == 0) {
      System.out.println("Success...");
    } else {
      System.out.println("Failure...");
    }
  }

  private static String getBuildCLIBuildDirectory() {
    try (InputStream inputStream = BuildCLIService.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
      if (inputStream == null || !inputStream.toString().endsWith(".jar")) {
        return getFallbackDirectory();
      }
      return readManifest(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Error while trying to read the META-INF/MANIFEST.MF", e);
    }
  }

  private static String getFallbackDirectory() {
    String classLocation = BuildCLIService.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .toString();
    File location = new File(classLocation);
    return location.getAbsolutePath();
  }

  private static String readManifest(InputStream inputStream) {
    try {
      Manifest manifest = new Manifest(inputStream);
      Attributes attributes = manifest.getMainAttributes();
      String buildDirectory = attributes.getValue("Build-Directory");

      if (buildDirectory == null) {
        throw new IllegalStateException("'Build-Directory' attribute not found in the MANIFEST.MF file.");
      }

      return buildDirectory;
    } catch (IOException e) {
      throw new RuntimeException("Error while trying to read the content of the MANIFEST.MF file", e);
    }
  }
}