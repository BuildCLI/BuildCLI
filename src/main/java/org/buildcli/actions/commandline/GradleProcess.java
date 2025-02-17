package org.buildcli.actions.commandline;

import org.buildcli.constants.GradleConstants;
import org.buildcli.utils.GradleInstaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.buildcli.utils.tools.ToolChecks.checksGradle;

public class GradleProcess extends BuildTool implements CommandLineProcess {
  private final List<String> commands = new ArrayList<>();

  private GradleProcess() {
    if (!checksGradle()) {
      GradleInstaller.installGradle();
    }

    commands.add(GradleConstants.GRADLE_CMD);
  }

  public static GradleProcess createProcessor(String... tasks) {
    var processor = new GradleProcess();
    processor.commands.addAll(Arrays.asList(tasks));
    return processor;
  }

  public static GradleProcess createPackageProcessor() {
    return createProcessor("clean", "build");
  }

  public static GradleProcess createCompileProcessor() {
    return createProcessor("clean", "classes");
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
