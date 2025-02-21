package org.buildcli.actions.tools;

public interface ToolChecker {
  String name();
  boolean isInstalled();
  default boolean isRunning() {
    return false;
  }
  String version();
  String installInstructions();
  void fixIssue();
}

