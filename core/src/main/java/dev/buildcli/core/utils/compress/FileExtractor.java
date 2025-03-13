package dev.buildcli.core.utils.compress;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class FileExtractor {
  private static final Logger logger = LoggerFactory.getLogger(FileExtractor.class);

  private FileExtractor() {
  }

  public static void extractFile(String filePath, String extractTo) throws IOException {
    CompressedFileExtractor fileExtractor;

    logger.info("Validating extension file to: {}", filePath);
    if (filePath.endsWith(".zip")) {
      fileExtractor = new ZipFileExtractor();
    } else if (filePath.endsWith(".tar.gz")) {
      fileExtractor = new TarGzFileExtractor();
    } else {
      throw new IllegalArgumentException("Archive format unsupported. Only use .zip or .tar.gz.");
    }

    logger.info("Trying to extract {} to {}", filePath, extractTo);
    fileExtractor.extract(filePath, extractTo);
  }

  public interface CompressedFileExtractor {
    void extract(String filePath, String extractTo) throws IOException;
  }

  public static class ZipFileExtractor implements CompressedFileExtractor {
    @Override
    public void extract(String filePath, String extractTo) throws IOException {
      Path outputPath = Paths.get(extractTo);
      Files.createDirectories(outputPath);

      try (ZipInputStream zis = new ZipInputStream(new FileInputStream(filePath))) {
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {
          Path entryPath = outputPath.resolve(zipEntry.getName());
          if (zipEntry.isDirectory()) {
            Files.createDirectories(entryPath);
          } else {
            Files.createDirectories(entryPath.getParent());
            try (OutputStream os = Files.newOutputStream(entryPath)) {
              os.write(zis.readAllBytes());
            }
          }
        }
      }
      logger.info("Extracted: {} to {}", filePath, extractTo);
    }
  }

  public static class TarGzFileExtractor implements CompressedFileExtractor {
    @Override
    public void extract(String filePath, String extractTo) throws IOException {
      Path outputPath = Paths.get(extractTo);
      Files.createDirectories(outputPath);

      try (TarArchiveInputStream tais = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(filePath)))) {

        TarArchiveEntry entry;
        while ((entry = tais.getNextEntry()) != null) {
          Path entryPath = outputPath.resolve(entry.getName());
          if (entry.isDirectory()) {
            Files.createDirectories(entryPath);
          } else {
            Files.createDirectories(entryPath.getParent());
            try (OutputStream os = Files.newOutputStream(entryPath)) {
              os.write(tais.readAllBytes());
            }
          }
        }
      }
      logger.info("Extracted: {} to {}", filePath, extractTo);

    }
  }
}
