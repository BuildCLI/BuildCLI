package dev.buildcli.core.constants;

import dev.buildcli.core.utils.OS;

public class GradleConstants {
  public static final String GRADLE_CMD_WINDOWS = "gradle.bat";
  public static final String GRADLE_CMD_UNIX = "gradle";
  public static final String GRADLE_CMD = OS.isWindows() ? GRADLE_CMD_WINDOWS : GRADLE_CMD_UNIX;
  public static final String FILE = "build.gradle";
  public static final String DEPENDENCIES_PATTERN = "##dependencies##";
  public static final String BUILD_DIR = "build";
}
