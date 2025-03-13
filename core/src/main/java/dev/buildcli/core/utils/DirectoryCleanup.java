package dev.buildcli.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DirectoryCleanup {
  private static final Logger logger = LoggerFactory.getLogger(DirectoryCleanup.class);

  public static void cleanup(String directory) {
    var targetPath = new File(directory).toPath();

    if (!Files.exists(targetPath)) {
      logger.info("The '{}' directory does not exist.",targetPath);
      return;
    }

    try {
      Files.walkFileTree(targetPath, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
      logger.info("The '{}' directory was successfully cleaned.", targetPath);
    } catch (IOException e) {
      logger.error("Error clearing '{}' directory: {}", directory, directory, e);
    }
  }
}
