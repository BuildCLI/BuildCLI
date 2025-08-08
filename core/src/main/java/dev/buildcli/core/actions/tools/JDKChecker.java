package dev.buildcli.core.actions.tools;

import dev.buildcli.core.actions.commandline.JavaProcess;

public class JDKChecker implements ToolChecker {
  @Override
  public String name() {
    return "JDK";
  }

  @Override
  public boolean isInstalled() {
    return JavaProcess.createGetVersionProcess().run() == 0;
  }

  @Override
  public String version() {
    var process = JavaProcess.createGetVersionProcess();
    var result = process.run();

    var lines = process.output();

    if (result == 0 && !lines.isEmpty()) {
      var versionLine = lines.getFirst();
      return versionLine.split(" ")[1].replace("\"", "");
    }

    return "N/A";
  }

  @Override
  public String installInstructions() {
    return "Install JDK: https://www.oracle.com/java/technologies/javase-downloads.html";
  }

  @Override
  public void fixIssue() {
    System.out.println("Fixing JDK issues is not automated. Follow the installation instructions.");
  }
}

