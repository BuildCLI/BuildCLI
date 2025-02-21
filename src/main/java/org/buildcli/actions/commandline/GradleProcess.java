package org.buildcli.actions.commandline;

import org.buildcli.constants.GradleConstants;
import org.buildcli.utils.GradleInstaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.buildcli.utils.tools.ToolChecks.checksGradle;

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
