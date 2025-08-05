package dev.buildcli.core.utils.tools;

import dev.buildcli.core.actions.commandline.GradleProcess;
import dev.buildcli.core.actions.commandline.MavenProcess;
import java.io.File;

/**
 * Utility class for detecting and verifying Java build tools.
 */
public abstract class ToolChecks {

  private ToolChecks() {
  }

  /**
   * Verifies whether Maven is available and correctly configured by
   * executing a version check command.
   *
   * @return {@code true} if Maven is available and responds correctly;
   *         {@code false} if an error occurs or Maven is not found.
   */
  public static boolean checksMaven() {
    try {
      int exitCode = MavenProcess.createGetVersionProcessor().run();
      return exitCode == 0;
    } catch (RuntimeException e) {
      return false;
    }
  }

  /**
   * Verifies whether Gradle is available and correctly configured by
   * executing a version check command.
   *
   * @return {@code true} if Gradle is available and responds correctly;
   *         {@code false} if an error occurs or Gradle is not found.
   */
  public static boolean checksGradle() {
    try {
      int exitCode = GradleProcess.createGetVersionProcess().run();
      return exitCode == 0;
    } catch (RuntimeException e) {
      return false;
    }
  }

  /**
   * Determines whether the specified directory represents a Maven or
   * Gradle project, based on the presence of standard build files.
   *
   * @param directory the project directory to analyze
   * @return {@code "Maven"} if a {@code pom.xml} is found;
   *         {@code "Gradle"} if a {@code build.gradle} is found;
   *         {@code "Neither"} if none are found.
   */
  public static String checkIsMavenOrGradle(final File directory) {
    final boolean isMaven = new File(directory, "pom.xml").exists();
    final boolean isGradle = new File(directory, "build.gradle").exists();

    return isMaven ? "Maven" : isGradle ? "Gradle" : "Neither";
  }
}
