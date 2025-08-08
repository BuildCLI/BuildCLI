package dev.buildcli.core.utils;

import dev.buildcli.core.log.SystemOutLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DirectoryCleanup {

  public static void cleanup(String directory) {
    var targetPath = new File(directory).toPath();

    if (!Files.exists(targetPath)) {
      SystemOutLogger.log("The '%s' directory does not exist.".formatted(targetPath.toString()));
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
      SystemOutLogger.log("The '%s' directory was successfully cleaned.".formatted(targetPath.toString()));
    } catch (IOException e) {
      SystemOutLogger.log("Error clearing '%s' directory: %s".formatted(directory, e.getMessage()));
    }
  }
}
