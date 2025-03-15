package dev.buildcli.core.utils.tools;

import java.io.File;
import java.io.IOException;

import static dev.buildcli.core.utils.SystemCommands.GRADLE;
import static dev.buildcli.core.utils.SystemCommands.MVN;

public abstract class ToolChecks {
  private ToolChecks() {
  }

  public static boolean checksMaven() {
    try {
      var process = new ProcessBuilder().command(MVN.getCommand(), "-v").start();

      int exitCode = process.waitFor();

      return exitCode == 0;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  public static boolean checksGradle() {
    try {
      var process = new ProcessBuilder().command(GRADLE.getCommand(), "-v").start();

      int exitCode = process.waitFor();

      return exitCode == 0;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }

  public static String checkIsMavenOrGradle(File directory) {
     final boolean isMaven = new File(directory, "pom.xml").exists();
     final boolean isGradle = new File(directory, "build.gradle").exists();

     return isMaven ? "Maven" : isGradle ? "Gradle": "Neither" ;
  }
}
