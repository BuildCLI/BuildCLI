package dev.buildcli.core.utils.net;

import dev.buildcli.core.exceptions.DownloadFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class FileDownloader {
  private static final Logger log = LoggerFactory.getLogger(FileDownloader.class);

  private FileDownloader(){ }

  public static File download(String url) throws DownloadFailedException {
    try (var client = HttpClient.newHttpClient()) {
      var request = HttpRequest.newBuilder().GET().uri(URI.create(url)).timeout(Duration.ofMinutes(30)).build();

      log.info("Connecting to {}", url);
      var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      log.info("Connected to {}", url);

      if (response.statusCode() != 200) {
        throw new IOException("Failed to download file: " + response.statusCode());
      }

      long contentLength = response.headers().firstValue("Content-Length").map(s -> s.isEmpty() ? 0 : Long.parseLong(s)).orElse(0L);

      if (contentLength == 0L) {
        throw new IOException("Failed to download maven artifact: " + response.statusCode());
      }

      var contentDisposition = response.headers().firstValue("Content-disposition");

      if (contentDisposition.isEmpty()) {
        throw new IOException("Failed to download file: " + response.statusCode());
      }

      var filename = contentDisposition.map(s -> s.split("=")[1].replace("\"", "")).orElse("");

      if (filename.isEmpty()) {
        throw new IOException("Failed to download file: " + response.statusCode());
      }

      var file = new File(filename);

      try (var bodyStream = response.body()) {
        try (var fos = new FileOutputStream(file)) {
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
            log.info("Download progress: [{}] {}%", progressBar, progress);
          }
        }
      }
      log.info("Download completed successfully. File saved as: {}", filename);
      return file;
    } catch (IOException e) {
      throw new DownloadFailedException(e.getMessage());
    } catch (InterruptedException e) {
      log.error("Thread was interrupted. Cleanup performed. {}", e.getMessage());
      Thread.currentThread().interrupt();
    }
    return null;
  }
}
