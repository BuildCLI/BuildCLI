package org.buildcli.actions.commandline;

import org.buildcli.constants.MavenConstants;
import org.buildcli.utils.MavenInstaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.buildcli.utils.tools.ToolChecks.checksMaven;

public class MavenProcess extends BuildTool implements CommandLineProcess {
  private final List<String> commands = new ArrayList<>();

  private MavenProcess() {
    if (!checksMaven()) {
      MavenInstaller.installMaven();
    }

    commands.add(MavenConstants.MAVEN_CMD);
  }

  public static MavenProcess createProcessor(String... goals) {
    var processor = new MavenProcess();
    processor.commands.addAll(Arrays.asList(goals));
    return processor;
  }

  public static MavenProcess createPackageProcessor() {
    return createProcessor("clean", "package");
  }

  public static MavenProcess createCompileProcessor() {
    return createProcessor("clean", "compile");
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
