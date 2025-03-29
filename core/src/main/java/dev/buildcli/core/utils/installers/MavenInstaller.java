package dev.buildcli.core.utils.installers;


import dev.buildcli.core.utils.DirectoryCleanup;
import dev.buildcli.core.utils.OS;
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

public abstract class MavenInstaller {

  private static final Logger logger = LoggerFactory.getLogger(MavenInstaller.class);

  private MavenInstaller() {
  }

  private static final String MAVEN_VERSION = "3.9.9";
  private static final String MAVEN_NAME = "apache-maven-%s".formatted(MAVEN_VERSION);
  private static final String MAVEN_DOWNLOAD_URL = "https://dlcdn.apache.org/maven/maven-3/%s/binaries/%s-bin.".formatted(MAVEN_VERSION, MAVEN_NAME);


  public static void installMaven() {
    logger.info("Installing Maven operation started...");

    try {
      logger.info("Downloading Maven operation started...");
      var file = downloadMaven();
      logger.info("Downloading Maven operation finished...");

      logger.info("Maven downloaded to {}", file.getAbsolutePath());
      var outputFile = installProgramFilesDirectory();
      logger.info("Maven install path set to {}", outputFile.getAbsolutePath());

      logger.info("Extracting Maven operation started...");
      extractMaven(file.getAbsolutePath(), outputFile.getAbsolutePath());
      logger.info("Extracting Maven operation finished...");

      logger.info("Configuring Maven path operation started...");
      configurePath(Paths.get(outputFile.getAbsolutePath(), MAVEN_NAME).toFile().getAbsolutePath());
      logger.info("Configuring Maven path operation finished...");

      if (file.exists()) {
        logger.info("Cleaning up maven download path...");
        DirectoryCleanup.cleanup(file.getAbsolutePath());
        logger.info("Cleaning up maven download path finished...");
      }
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
      return new File(programFiles, "Maven");
    } else {
      return new File("/usr/local/maven");
    }
  }

  public static File downloadMaven() throws IOException, InterruptedException {
    var isWindows = OS.isWindows();
    var url = MAVEN_DOWNLOAD_URL + (isWindows ? "zip" : "tar.gz");
    var request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

    logger.info("Downloading Maven artifact from: {}", url);

    var client = HttpClient.newHttpClient();

    logger.info("Connecting to {}", url);
    var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    logger.info("Connected to {}", url);

    if (response.statusCode() != 200) {
      throw new IOException("Failed to download maven artifact: " + response.statusCode());
    }

    long contentLength = response.headers().firstValue("Content-Length").map(MavenInstaller::parseContentLengthToLong).orElse(0L);

    if (contentLength == 0L) {
      throw new IOException("Failed to download maven artifact: " + response.statusCode());
    }

    var mavenInstallDir = new File(MAVEN_NAME + (isWindows ? ".zip" : ".tar.gz"));

    if (mavenInstallDir.exists()) {
      logger.info("Cleaning up maven install directory: {}", mavenInstallDir);
      DirectoryCleanup.cleanup(mavenInstallDir.getAbsolutePath());
    }

    try (var bodyStream = response.body()) {
      try (var fos = new FileOutputStream(mavenInstallDir)) {
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


    if (!mavenInstallDir.exists()) {
      throw new IOException("Failed to create maven download directory: " + mavenInstallDir);
    }

    return mavenInstallDir;
  }

  private static long parseContentLengthToLong(String s) {
    return s.isEmpty() ? 0 : Long.parseLong(s);
  }

  public static void extractMaven(String filePath, String extractTo) throws IOException, InterruptedException {
    FileExtractor.extractFile(filePath, extractTo);
  }

  public static void configurePath(String mavenBinPath) throws IOException {
    var isWindows = OS.isWindows();
    if (isWindows) {
      Runtime.getRuntime().exec(new String[] {"setx PATH \"%PATH%;" + mavenBinPath  + "\\\\bin\""});
    } else {
      File bashrc = new File(System.getProperty("user.home"), ".bashrc");
      try (FileWriter fw = new FileWriter(bashrc, true)) {
        fw.write("\nexport PATH=$PATH:" + mavenBinPath + "/bin\n");
      }
      logger.info("Please run: source ~/.bashrc");
      logger.info("Please run: sudo chmod +x {}/bin/mvn\n", mavenBinPath );
    }
  }
}
