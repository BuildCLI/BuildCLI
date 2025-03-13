package dev.buildcli.core.utils;

import dev.buildcli.core.utils.compress.FileExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger logger = LoggerFactory.getLogger(GradleInstaller.class);

  private GradleInstaller() {
  }

  private static final String GRADLE_VERSION = "8.12.1";
  private static final String GRADLE_NAME = "gradle-%s".formatted(GRADLE_VERSION);
  private static final String GRADLE_DOWNLOAD_URL = "https://services.gradle.org/distributions/%s-bin.zip".formatted(GRADLE_NAME);
  private static final String USER_HOME = System.getProperty("user.home");
  public static void installGradle() {
    logger.info("Installing Gradle operation started...");
    try {
      logger.info("Downloading Gradle operation started...");
      var file = downloadGradle();
      logger.info("Downloading Gradle operation finished...");

      logger.info("Gradle downloaded to {}", file.getAbsolutePath());
      var outputDir = installProgramFilesDirectory();
      logger.info("Gradle install path set to {}", outputDir.getAbsolutePath());

      logger.info("Extracting Gradle operation started...");
      extractGradle(file.getAbsolutePath(), outputDir.getAbsolutePath());
      logger.info("Extracting Gradle operation finished...");

      String gradleExtractedDir = Paths.get(outputDir.getAbsolutePath(), GRADLE_NAME).toFile().getAbsolutePath();
      logger.info("Configuring Gradle path operation started...");
      configurePath(gradleExtractedDir);
      logger.info("Configuring Gradle path operation finished...");
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

    logger.info("Downloading Gradle artifact from: {}", GRADLE_DOWNLOAD_URL);

    var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

    if (response.statusCode() != 200) {
      throw new IOException("Failed to download Gradle artifact: " + response.statusCode());
    }

    long contentLength = response.headers().firstValue("Content-Length").map(GradleInstaller::parseContentLengthToLong).orElse(0L);

    if (contentLength == 0L) {
      throw new IOException("Failed to download maven artifact: " + response.statusCode());
    }

    var gradleZip = new File(GRADLE_NAME +  ".zip");

    if (gradleZip.exists()) {
      logger.info("Cleaning up previous Gradle zip: {}", gradleZip);
      DirectoryCleanup.cleanup(gradleZip.getAbsolutePath());
    }

    try (var bodyStream = response.body()) {
      try (var fos = new FileOutputStream(gradleZip)) {
        byte[] buffer = new byte[1024];
        long totalRead = 0;
        int read;

        while ((read = bodyStream.read(buffer)) != -1) {
          fos.write(buffer, 0, read);
          totalRead += read;

          int progress = (int) ((totalRead * 100) / contentLength);
          int progressBarLength = 50;
          int filledLength = (int) ((progress / 100.0) * progressBarLength);

          String progressBar = "=".repeat(filledLength) + " ".repeat(progressBarLength - filledLength);

          System.out.printf("\r[%s] %d%%", progressBar, progress);
        }
        System.out.println();
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
      Runtime.getRuntime().exec(new String[] {"setx PATH \"%PATH%;" + gradleExtractedDir + "\\bin\""});
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
      logger.info("Please run: sudo chmod +x {}/bin/gradle", gradleExtractedDir);
      logger.info("Please run: source ~/{}", shellConfigFile);
      logger.info("Do you want to run these commands now? (y/n)");

      String response = scanner.nextLine().trim().toLowerCase();
      if (response.equals("y") || response.equals("yes")) {
        logger.info("Running the commands...");

        Runtime.getRuntime().exec(new String[] {"sudo chmod +x " + gradleExtractedDir + "/bin/gradle"});

        String sourceCommand = "bash -c 'source ~/" + shellConfigFile + " && echo \"Source command executed\"'";
        Runtime.getRuntime().exec(new String[] {sourceCommand});
      } else {
        logger.info("You can run the commands later.");
      }

      scanner.close();
    }
  }

  private static long parseContentLengthToLong(String s) {
    return s.isEmpty() ? 0 : Long.parseLong(s);
  }
}

