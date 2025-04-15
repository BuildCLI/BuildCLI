package dev.buildcli.core.utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public final class ProjectUtils {
  public static boolean isValid(File directory) {
    if (!directory.exists() || !directory.isDirectory()) {
      return false;
    }

    var files = directory.listFiles();

    if (files == null) {
      return false;
    }

    return Arrays.stream(files)
        .filter(File::isFile)
        .anyMatch(ProjectUtils::containsProjectFiles);
  }

  private static boolean containsProjectFiles(File file) {
    return List.of("pom.xml", "build.gradle", "build.gradle.kts").contains(file.getName());
  }
}
