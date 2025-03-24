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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

  private static final GitCommandExecutor gitExec = new GitCommandExecutor();

  private static final String buildCLIDirectory = getBuildCLIBuildDirectory();
  private static final String localRepository = gitExec.findGitRepository(buildCLIDirectory);

  public BuildCLIService() {
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

  public static boolean shouldShowAsciiArt(String[] args) {
    if (args.length == 0) {
      return false;
    }

    if (Arrays.asList(args).contains("--help")) {
      return true;
    }

    Map<String, List<String>> commandAliases = Map.of(
        "p", List.of("p", "project"),
        "about", List.of("a", "about"),
        "help", List.of("help", "h")
    );

    String mainCommand = args[0];
    if (matchesCommand(mainCommand, commandAliases.get("p"))) {
      return args.length > 1 && (args[1].equals("run") || (args.length > 2 && args[1].equals("i") && args[2].equals("-n")));
    }

    if (matchesCommand(mainCommand, commandAliases.get("help"))) {
      return true;
    }

    return matchesCommand(mainCommand, commandAliases.get("about"));
  }

  private static boolean matchesCommand(String input, List<String> validCommands) {
    return validCommands != null && validCommands.contains(input);
  }

  public static void about() {
    SystemOutLogger.log("BuildCLI is a command-line interface (CLI) tool for managing and automating common tasks in Java project development.\n" +
        "It allows you to create, compile, manage dependencies, and run Java projects directly from the terminal, simplifying the development process.\n");
    SystemOutLogger.log("Visit the repository for more details: https://github.com/BuildCLI/BuildCLI\n");

    SystemOutLogger.log(gitExec.showContributors());
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
      if (inputStream == null) {
        throw new IllegalStateException("Manifest not found.");
      }
      Manifest manifest = new Manifest(inputStream);
      Attributes attributes = manifest.getMainAttributes();
      String buildDirectory = attributes.getValue("Build-Directory");

      if (buildDirectory == null) {
        throw new IllegalStateException("Build-Directory not found in the Manifest.");
      }

      return buildDirectory;
    } catch (Exception e) {
      throw new RuntimeException("Error reading the Manifest.", e);
    }
  }

}
