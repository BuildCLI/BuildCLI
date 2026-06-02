package dev.buildcli.core.utils;

import java.util.Locale;
import java.util.logging.Logger;

/** Utility methods for operating system checks and commands. */
public final class OS {
  /** Logger for operating system utility operations. */
  private static final Logger LOGGER = Logger.getLogger(OS.class.getName());

  private OS() {
  }

  /**
   * Checks if the current operating system is Windows.
   *
   * @return true when the current operating system is Windows
   */
  public static boolean isWindows() {
    return normalizedOSName().contains("win");
  }

  /**
   * Checks if the current operating system is macOS.
   *
   * @return true when the current operating system is macOS
   */
  public static boolean isMac() {
    return normalizedOSName().contains("mac");
  }

  /**
   * Checks if the current operating system is Linux or Unix-like.
   *
   * @return true when the current operating system is Linux or Unix-like
   */
  public static boolean isLinux() {
    String os = normalizedOSName();
    return os.contains("linux")
        || os.contains("nix")
        || os.contains("nux")
        || os.contains("aix");
  }

  /**
   * Gets the current operating system name.
   *
   * @return the operating system name
   */
  public static String getOSName() {
    return System.getProperty("os.name");
  }

  private static String normalizedOSName() {
    return System.getProperty("os.name").toLowerCase(Locale.ROOT);
  }

  /**
   * Gets the current operating system architecture.
   *
   * @return the operating system architecture
   */
  public static String getArchitecture() {
    return System.getProperty("os.arch");
  }

  /**
   * Runs a shell command to change directory.
   *
   * @param path the directory path
   */
  public static void cdDirectory(final String path) {
    try {
      String[] command;
      if (isWindows()) {
        command = new String[]{"cmd", "/c", "cd", path};
      } else {
        command = new String[]{"sh", "-c", "cd", path};
      }
      Runtime.getRuntime().exec(command);
    } catch (Exception e) {
      LOGGER.severe("Error changing directory: " + e.getMessage());
    }
  }

  /**
   * Runs a shell command to copy a directory or file.
   *
   * @param source the source path
   * @param destination the destination path
   */
  public static void cpDirectoryOrFile(
      final String source,
      final String destination
  ) {
    try {
      String[] command;
      if (isWindows()) {
        command = new String[]{"cmd", "/c", "copy", source, destination};
      } else {
        command = new String[]{"sh", "-c", "cp", source, destination};
      }
      Runtime.getRuntime().exec(command);
    } catch (Exception e) {
      LOGGER.severe("Error copying directory: " + e.getMessage());
    }
  }

  /**
   * Gets the user's home bin directory.
   *
   * @return the home bin directory
   */
  public static String getHomeBinDirectory() {
    String homeBin;
    if (isWindows()) {
      homeBin = System.getenv("HOMEPATH") + "//bin";
    } else {
      homeBin = System.getenv("HOME") + "/bin";
    }
    return homeBin;
  }

  /**
   * Runs chmod +x on non-Windows systems.
   *
   * @param path the path to make executable
   */
  public static void chmodX(final String path) {
    if (!isWindows()) {
      try {
        String chmodCommand = "chmod +x " + path;
        String[] command = new String[]{"sh", "-c", chmodCommand};
        Runtime.getRuntime().exec(command);
      } catch (Exception e) {
        LOGGER.severe("Error changing directory: " + e.getMessage());
      }
    }

  }
}
