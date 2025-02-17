package org.buildcli.utils;

import org.buildcli.log.SystemOutLogger;
import org.buildcli.utils.compress.FileExtractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.Scanner;

public abstract class GradleInstaller {
  private GradleInstaller() {
  }

  private static final String GRADLE_VERSION = "8.12.1";
  private static final String GRADLE_NAME = "gradle-%s".formatted(GRADLE_VERSION);
  private static final String GRADLE_DOWNLOAD_URL = "https://services.gradle.org/distributions/%s-bin.zip".formatted(GRADLE_NAME);
  private static final String USER_HOME = System.getProperty("user.home");
  public static void installGradle() {
    SystemOutLogger.log("Installing Gradle operation started...");
    try {
      SystemOutLogger.log("Downloading Gradle operation started...");
      var file = downloadGradle();
      SystemOutLogger.log("Downloading Gradle operation finished...");

      SystemOutLogger.log("Gradle downloaded to " + file.getAbsolutePath());
      var outputDir = installProgramFilesDirectory();
      SystemOutLogger.log("Gradle install path set to " + outputDir.getAbsolutePath());

      SystemOutLogger.log("Extracting Gradle operation started...");
      extractGradle(file.getAbsolutePath(), outputDir.getAbsolutePath());
      SystemOutLogger.log("Extracting Gradle operation finished...");

      String gradleExtractedDir = Paths.get(outputDir.getAbsolutePath(), GRADLE_NAME).toFile().getAbsolutePath();
      SystemOutLogger.log("Configuring Gradle path operation started...");
      configurePath(gradleExtractedDir);
      SystemOutLogger.log("Configuring Gradle path operation finished...");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static File installProgramFilesDirectory() {
    if (OS.isWindows()) {
      String programFiles = System.getenv("ProgramFiles");
      if (programFiles == null) {
        programFiles = "C:\\Program Files";
      }
      return new File(programFiles, "Gradle");
    } else {
      return new File("/" + USER_HOME + "/gradle");
    }
  }

  public static File downloadGradle() throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder().uri(URI.create(GRADLE_DOWNLOAD_URL)).GET().build();
    var client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();

    SystemOutLogger.log("Downloading Gradle artifact from: " + GRADLE_DOWNLOAD_URL);

    var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

    if (response.statusCode() != 200) {
      throw new IOException("Failed to download Gradle artifact: " + response.statusCode());
    }

    var gradleZip = new File(GRADLE_NAME +  ".zip");

    if (gradleZip.exists()) {
      SystemOutLogger.log("Cleaning up previous Gradle zip: " + gradleZip);
      DirectoryCleanup.cleanup(gradleZip.getAbsolutePath());
    }

    try (var bodyStream = response.body(); var fos = new FileOutputStream(gradleZip)) {
      byte[] buffer = new byte[1024];
      int read;
      while ((read = bodyStream.read(buffer)) != -1) {
        fos.write(buffer, 0, read);
      }
    }

    if (!gradleZip.exists()) {
      throw new IOException("Failed to create Gradle zip file: " + gradleZip);
    }

    return gradleZip;
  }

  public static void extractGradle(String filePath, String extractTo) throws IOException, InterruptedException {
    FileExtractor.extractFile(filePath, extractTo);
  }

  public static void configurePath(String gradleExtractedDir) throws IOException {
    if (OS.isWindows()) {
      Runtime.getRuntime().exec("setx PATH \"%PATH%;" + gradleExtractedDir + "\\bin\"");
    } else {
      String shellConfigFile;
      String shell = System.getenv("SHELL");

      if (shell != null && shell.contains("zsh")) {
        shellConfigFile = ".zshrc";
      } else {
        shellConfigFile = ".bashrc";
      }

      File shellConfig = new File(USER_HOME, shellConfigFile);
      try (FileWriter fw = new FileWriter(shellConfig, true)) {
        fw.write("\nexport PATH=$PATH:" + gradleExtractedDir + "/bin\n");
      }

      Scanner scanner = new Scanner(System.in);
      SystemOutLogger.log("Please run: sudo chmod +x " + gradleExtractedDir + "/bin/gradle");
      SystemOutLogger.log("Please run: source ~/" + shellConfigFile);
      SystemOutLogger.log("Do you want to run these commands now? (y/n)");

      String response = scanner.nextLine().trim().toLowerCase();
      if (response.equals("y") || response.equals("yes")) {
        SystemOutLogger.log("Running the commands...");

        Runtime.getRuntime().exec("sudo chmod +x " + gradleExtractedDir + "/bin/gradle");

        String sourceCommand = "bash -c 'source ~/" + shellConfigFile + " && echo \"Source command executed\"'";
        Runtime.getRuntime().exec(sourceCommand);
      } else {
        SystemOutLogger.log("You can run the commands later.");
      }
    }
  }
}

