package org.buildcli.actions.commandline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JavaProcess implements CommandLineProcess {
  private final List<String> commands = new ArrayList<>();

  private JavaProcess() {
    commands.add("java");
  }

  public static JavaProcess createRunJarProcess(String jarName, String... args) {
    var process = new JavaProcess();

    process.commands.addAll(List.of("-jar", jarName));
    if (args != null && args.length > 0) {
      process.commands.addAll(List.of(args));
    }

    return process;
  }

  public static JavaProcess createRunClassProcess(String className, String... args) {
    var process = new JavaProcess();

    process.commands.add(className);
    if (args != null && args.length > 0) {
      process.commands.addAll(List.of(args));
    }

    return process;
  }

  @Override
  public int run() {
    try {
      return new ProcessBuilder(commands).inheritIO().start().waitFor();
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
