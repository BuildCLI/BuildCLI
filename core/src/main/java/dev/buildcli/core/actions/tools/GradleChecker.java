package dev.buildcli.core.actions.tools;

import dev.buildcli.core.actions.commandline.GradleProcess;
import dev.buildcli.core.utils.installers.GradleInstaller;

public class GradleChecker implements ToolChecker {
  @Override
  public String name() {
    return "Gradle";
  }

  @Override
  public boolean isInstalled() {
    return GradleProcess.createGetVersionProcess().run() == 0;
  }

  @Override
  public String version() {
    var process = GradleProcess.createGetVersionProcess();
    var result = process.run();
    var lines = process.output().stream().filter(line -> line.contains("Gradle")).toList();

    if (result == 0 && !lines.isEmpty()) {
      var versionLine = lines.getFirst();
      return versionLine.split(" ")[1].replace("!", "");
    }

    return "N/A";
  }

  @Override
  public String installInstructions() {
    return "Install Gradle: https://gradle.org/install/";
  }

  @Override
  public void fixIssue() {
    GradleInstaller.installGradle();
  }
}
