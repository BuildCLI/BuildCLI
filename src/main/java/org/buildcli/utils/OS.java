package org.buildcli.utils;

import java.util.logging.Logger;

public abstract class OS {
  private static final Logger logger = Logger.getLogger(OS.class.getName());
  private OS() {}

  private static final String OS = System.getProperty("os.name").toLowerCase();

  public static boolean isWindows() {
    return OS.contains("win");
  }

  public static boolean isMac() {
    return OS.contains("mac");
  }

  public static boolean isLinux() {
    return OS.contains("linux") || OS.contains("nix") || OS.contains("nux") || OS.contains("aix");
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
