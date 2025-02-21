package org.buildcli.actions.tools;

import org.buildcli.actions.commandline.MavenProcess;
import org.buildcli.utils.MavenInstaller;

public class MavenChecker implements ToolChecker {

  @Override
  public String name() {
    return "Maven";
  }

  @Override
  public boolean isInstalled() {
    return MavenProcess.createGetVersionProcessor().run() == 0;
  }

  @Override
  public String version() {

    var process = MavenProcess.createGetVersionProcessor();
    var result = process.run();
    var lines = process.output();

    if (result == 0 && !lines.isEmpty()) {
      var versionLine = lines.getFirst();
      return versionLine.split(" ")[2];
    }

    return "N/A";
  }

  @Override
  public String installInstructions() {
    return "Install Maven: https://maven.apache.org/install.html";
  }

  @Override
  public void fixIssue() {
    MavenInstaller.installMaven();
  }
}

