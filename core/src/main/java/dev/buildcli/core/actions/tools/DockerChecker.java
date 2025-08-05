package dev.buildcli.core.actions.tools;

import dev.buildcli.core.actions.commandline.DockerProcess;
import dev.buildcli.core.log.SystemOutLogger;

public class DockerChecker implements ToolChecker {
  @Override
  public String name() {
    return "Docker";
  }

  @Override
  public boolean isInstalled() {
    return DockerProcess.createGetVersionProcess().run() == 0;
  }

  @Override
  public boolean isRunning() {
    return DockerProcess.createInfoProcess().run() == 0;
  }

  @Override
  public String version() {
    var process = DockerProcess.createGetVersionProcess();
    var result = process.run();
    var lines = process.output();

    if (result == 0 && !lines.isEmpty()) {
      var versionLine = lines.getFirst();
      return versionLine.split(" ")[2].replace(",", "");
    }

    return "N/A";
  }

  @Override
  public String installInstructions() {
    return "Install Docker: https://docs.docker.com/get-docker/";
  }

  @Override
  public void fixIssue() {
    SystemOutLogger.warn("Fixing Docker issues is not automated. Please ensure Docker is installed and running.");
  }
}
