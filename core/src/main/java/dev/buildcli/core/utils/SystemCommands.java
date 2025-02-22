package dev.buildcli.core.utils;

public enum SystemCommands {
  MVN {
    public String getCommand() {
      boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
      String command = "mvn";
      if (isWindows)
        command += ".cmd";
      return command;
    }
  },
  GRADLE {
    @Override
    public String getCommand() {
      var builder = new StringBuilder("gradle");
      if (OS.isWindows()) {
        builder.append(".bat");
      }

      return builder.toString();
    }
  };

  public abstract String getCommand();
}
