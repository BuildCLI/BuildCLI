package org.buildcli.actions.commandline;

import java.util.List;
import java.util.Objects;

public class DockerProcess extends AbstractCommandLineProcess{
  private DockerProcess(boolean printOutput) {
    super("docker", printOutput);
  }

  public static DockerProcess createBuildProcess(String tag) {
    return createBuildProcess(tag, null);
  }

  public static DockerProcess createBuildProcess(String tag, String fileName) {
    var process = new DockerProcess(true);

    process.commands.addAll(List.of("build", "-t", tag));

    if (Objects.isNull(fileName)) {
      process.commands.add(".");
    } else {
      process.commands.add("-f");
      process.commands.add(fileName);
    }

    return process;
  }

  public static DockerProcess createRunProcess(String tagName) {
    return createProcess("run", "-p", "8080:8080", tagName);
  }

  public static DockerProcess createProcess(String... args) {
    var process = new DockerProcess(true);

    process.commands.addAll(List.of(args));

    return process;
  }

  public static DockerProcess createGetVersionProcess() {
    var process = new DockerProcess(false);

    process.commands.add("-v");

    return process;
  }

  public static DockerProcess createInfoProcess() {
    var process = new DockerProcess(false);

    process.commands.add("info");

    return process;
  }
}
