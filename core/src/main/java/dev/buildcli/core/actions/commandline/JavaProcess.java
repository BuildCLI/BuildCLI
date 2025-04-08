package dev.buildcli.core.actions.commandline;

import java.util.Arrays;
import java.util.List;


public class JavaProcess extends AbstractCommandLineProcess {

  private JavaProcess(boolean printOutput) {
    super("java", printOutput);
  }


  public static JavaProcess createRunJarProcess(String jarName, String...args) {
    return createProcess("-jar", jarName, mergeArgs(args));
  }

  public static JavaProcess createGetVersionProcess() {
    var process = new JavaProcess(false);

    process.commands.add("--version");
    return process;
  }

  public static JavaProcess createProcess(String... args) {
    var process = new JavaProcess(true);

    process.commands.addAll(List.of(args));
    return process;
  }

  public static JavaProcess createRunClassProcess(String absolutePath, String...args) {
    return createProcess(absolutePath, mergeArgs(args));
  }

  private static String mergeArgs(String...args) {
    return Arrays.stream(args).reduce((a, b) -> a + " " + b).orElse("");
  }
}
