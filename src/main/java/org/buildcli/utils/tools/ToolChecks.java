package org.buildcli.utils.tools;

import java.io.File;
import java.io.IOException;

import static org.buildcli.utils.SystemCommands.GRADLE;
import static org.buildcli.utils.SystemCommands.MVN;

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

  public static String checkIsMavenOrGradle() {
     final boolean isMaven = new File("pom.xml").exists();
     final boolean isGradle = new File("build.gradle").exists();

     return isMaven ? "Maven" : isGradle ? "Gradle": "Neither" ;
  }
}
