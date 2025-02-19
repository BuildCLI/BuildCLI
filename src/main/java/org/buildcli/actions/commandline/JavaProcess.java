package org.buildcli.actions.commandline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class JavaProcess extends AbstractCommandLineProcess {

  private JavaProcess() {
    super("java");
  }


  public static JavaProcess createRunJarProcess(String jarName, String...args) {
    var arg = Arrays.stream(args).reduce((a, b) -> a + " " + b).orElse("");
    return createProcess("-jar", jarName, arg);
  }

  public static JavaProcess createGetVersionProcess() {
    return createProcess("--version");
  }

  public static JavaProcess createProcess(String... args) {
    var process = new JavaProcess();

    process.commands.addAll(List.of(args));
    return process;
  }

  public static CommandLineProcess createRunClassProcess(String absolutePath, String...args) {
    var arg = Arrays.stream(args).reduce((a, b) -> a + " " + b).orElse("");
    return createProcess(absolutePath, arg);
  }
}
