package dev.buildcli.core.utils;

public class RuntimeCommandExecutor implements CommandExecutor {
  @Override
  public void execute(String[] command) throws Exception {
      Runtime.getRuntime().exec(command);
  }
}
