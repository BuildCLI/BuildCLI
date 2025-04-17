package dev.buildcli.core.utils;

public interface CommandExecutor {
  void execute(String[] command) throws  Exception;
}
