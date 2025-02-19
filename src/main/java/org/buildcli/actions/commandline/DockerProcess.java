package org.buildcli.actions.commandline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DockerProcess implements CommandLineProcess{
  private final List<String> commands = new ArrayList<>();
  private DockerProcess() {
    commands.add("docker");
  }

  public static DockerProcess createBuildProcess(String tag) {
    return createBuildProcess(tag, null);
  }

  public static DockerProcess createBuildProcess(String tag, String fileName) {
    var process = new DockerProcess();

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
    var process = new DockerProcess();
    process.commands.addAll(List.of("run", "-p", "8080:8080", tagName));
    return process;
  }

  @Override
  public int run() {
    try {
      return new ProcessBuilder().command(commands).inheritIO().start().waitFor();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
