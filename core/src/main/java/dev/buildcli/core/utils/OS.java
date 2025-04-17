package dev.buildcli.core.utils;

import dev.buildcli.core.domain.man.CommandMan;
import dev.buildcli.core.exceptions.CommandExecutorRuntimeException;

import java.util.logging.Logger;

public abstract class OS {
  private static final Logger logger = Logger.getLogger(OS.class.getName());
  private static RuntimeCommandExecutor runtimeCommandExecutor = new RuntimeCommandExecutor();
  private OS() {}

  public static void setCommandExecutor(RuntimeCommandExecutor executor) {
    runtimeCommandExecutor = executor;
  }

  public static boolean isWindows() {
    return getOSName().contains("win");
  }

  public static boolean isMac() {
    return getOSName().contains("mac");
  }

  public static boolean isLinux() {
    return getOSName().contains("linux") || getOSName().contains("nix") || getOSName().contains("nux") || getOSName().contains("aix");
  }

    public static String getOSName() {
        return System.getProperty("os.name").toLowerCase();
    }

    public static String getArchitecture() {
        return System.getProperty("os.arch");
    }

  public static void cdDirectory(String path){
    try {
      CommandMan command = CommandMan.create()
                .addCommand("cd " + path);
        executeCommand(command);
    } catch (Exception e) {
      logger.severe("Error changing directory: " + e.getMessage());
    }
  }

  public static void cpDirectoryOrFile(String source, String destination){
    try {
      CommandMan command;
      if (isWindows()) {
        command = CommandMan.create()
            .addCommand("copy" + " " + source + " " + destination);
      } else {
        command = CommandMan.create()
            .addCommand("cp" + " " + source + " " + destination);
      }
      executeCommand(command);
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

  public static void chmodX(String path) {
    if (!isWindows()) {
      try {
        CommandMan commandMan = CommandMan.create()
            .addCommand("chmod +x " + path);
        executeCommand(commandMan);
      } catch (Exception e) {
        logger.severe("Error changing permissions: " + e.getMessage());
      }
    }
  }

  private static void executeCommand(CommandMan commandMan) throws CommandExecutorRuntimeException {
    try {
      for (String cmd : commandMan.getCommands()) {
        String[] command = isLinux() ? new String[]{"sh", "-c", cmd}
                                     : new String[]{"cmd", "/c", cmd};
        runtimeCommandExecutor.execute(command);
      }
    } catch (Exception e) {
      String errorMessage = "Error executing command: " + e.getMessage();
      logger.severe(errorMessage);
      throw new CommandExecutorRuntimeException(e.getMessage());
    }
  }
}
