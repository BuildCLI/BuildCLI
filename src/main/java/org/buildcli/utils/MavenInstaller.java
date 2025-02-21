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

public abstract class MavenInstaller {
  private MavenInstaller() {
  }

  private static final String MAVEN_VERSION = "3.9.9";
  private static final String MAVEN_NAME = "apache-maven-%s".formatted(MAVEN_VERSION);
  private static final String MAVEN_DOWNLOAD_URL = "https://dlcdn.apache.org/maven/maven-3/%s/binaries/%s-bin.".formatted(MAVEN_VERSION, MAVEN_NAME);


  public static void installMaven() {
    SystemOutLogger.log("Installing Maven operation started...");
    try {
      SystemOutLogger.log("Downloading Maven operation started...");
      var file = downloadMaven();
      SystemOutLogger.log("Downloading Maven operation finished...");

      SystemOutLogger.log("Maven downloaded to " + file.getAbsolutePath());
      var outputFile = installProgramFilesDirectory();
      SystemOutLogger.log("Maven install path set to " + outputFile.getAbsolutePath());

      SystemOutLogger.log("Extracting Maven operation started...");
      extractMaven(file.getAbsolutePath(), outputFile.getAbsolutePath());
      SystemOutLogger.log("Extracting Maven operation finished...");

      SystemOutLogger.log("Configuring Maven path operation started...");
      configurePath(Paths.get(outputFile.getAbsolutePath(), MAVEN_NAME).toFile().getAbsolutePath());
      SystemOutLogger.log("Configuring Maven path operation finished...");

      if (file.exists()) {
        SystemOutLogger.log("Cleaning up maven download path...");
        DirectoryCleanup.cleanup(file.getAbsolutePath());
        SystemOutLogger.log("Cleaning up maven download path finished...");
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

    SystemOutLogger.log("Downloading Maven artifact from: " + url);

    var client = HttpClient.newHttpClient();

    SystemOutLogger.log("Connecting to " + url);
    var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    SystemOutLogger.log("Connected to " + url);

    if (response.statusCode() != 200) {
      throw new IOException("Failed to download maven artifact: " + response.statusCode());
    }

    long contentLength = response.headers().firstValue("Content-Length").map(MavenInstaller::parseContentLengthToLong).orElse(0L);

    if (contentLength == 0L) {
      throw new IOException("Failed to download maven artifact: " + response.statusCode());
    }

    var mavenInstallDir = new File(MAVEN_NAME + (isWindows ? ".zip" : ".tar.gz"));

    if (mavenInstallDir.exists()) {
      SystemOutLogger.log("Cleaning up maven install directory: " + mavenInstallDir);
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
      SystemOutLogger.log("Please run: source ~/.bashrc");
      SystemOutLogger.log("Please run: sudo chmod +x " + mavenBinPath + "/bin/mvn\n");
    }
  }
}
