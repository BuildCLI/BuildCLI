package dev.buildcli.core.utils;

import java.util.Locale;
import java.util.logging.Logger;

public abstract class OS {
  private static final Logger logger = Logger.getLogger(OS.class.getName());
  private OS() {}

  public static boolean isWindows() {
    return normalizedOSName().contains("win");
  }

  public static boolean isMac() {
    return normalizedOSName().contains("mac");
  }

  public static boolean isLinux() {
    String os = normalizedOSName();
    return os.contains("linux") || os.contains("nix") || os.contains("nux") || os.contains("aix");
  }

  public static String getOSName() {
    return System.getProperty("os.name");
  }

  private static String normalizedOSName() {
    return System.getProperty("os.name").toLowerCase(Locale.ROOT);
  }

  public static String getArchitecture() {
    return System.getProperty("os.arch");
  }

  public static void cdDirectory(String path){
    try {
      String[] command;
      if (isWindows()) {
        command = new String[]{"cmd", "/c", "cd", path};
      } else {
        command = new String[]{"sh", "-c", "cd", path};
      }
      Runtime.getRuntime().exec(command);
    } catch (Exception e) {
      logger.severe("Error changing directory: " + e.getMessage());
    }
  }

  public static void cpDirectoryOrFile(String source, String destination){
    try {
      String[] command;
      if (isWindows()) {
        command = new String[]{"cmd", "/c", "copy", source, destination};
      } else {
        command = new String[]{"sh", "-c", "cp",  source, destination};
      }
      Runtime.getRuntime().exec(command);
    } catch (Exception e) {
      logger.severe("Error copying directory: " + e.getMessage());
    }
  }

  public static String getHomeBinDirectory(){
    String homeBin="";
    if(isWindows()){
      homeBin= System.getenv("HOMEPATH")+"//bin";
    }else {
      homeBin= System.getenv("HOME")+"/bin";
    }
    return homeBin;
  }

  public static void chmodX(String path){
    if(!isWindows()){
      try {
        String chmodCommand = "chmod +x " + path;
        String[] command = new String[]{"sh", "-c", chmodCommand};
        Runtime.getRuntime().exec(command);
      } catch (Exception e) {
        logger.severe("Error changing directory: " + e.getMessage());
      }
    }

  }
}
