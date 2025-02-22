package dev.buildcli.core.actions.commandline;

import dev.buildcli.core.constants.GradleConstants;

import java.util.Arrays;

public class GradleProcess extends AbstractCommandLineProcess {
  private GradleProcess(boolean printOutput) {
    super(GradleConstants.GRADLE_CMD, printOutput);
  }

  public static GradleProcess createProcessor(String... tasks) {
    var processor = new GradleProcess(true);
    processor.commands.addAll(Arrays.asList(tasks));
    return processor;
  }

  public static GradleProcess createPackageProcessor() {
    return createProcessor("clean", "build");
  }

  public static GradleProcess createCompileProcessor() {
    return createProcessor("clean", "classes");
  }

  public static GradleProcess createGetVersionProcess() {
    var processor = new GradleProcess(false);
    processor.commands.add("--version");
    return processor;
  }
}
