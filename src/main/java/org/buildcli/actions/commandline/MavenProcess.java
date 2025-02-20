package org.buildcli.actions.commandline;

import org.buildcli.constants.MavenConstants;

import java.util.Arrays;

public class MavenProcess extends AbstractCommandLineProcess {
  private MavenProcess(boolean printOutput) {
    super(MavenConstants.MAVEN_CMD, printOutput);
  }

  public static MavenProcess createProcessor(String... goals) {
    var processor = new MavenProcess(true);
    processor.commands.addAll(Arrays.asList(goals));
    return processor;
  }

  public static MavenProcess createPackageProcessor() {
    return createProcessor("clean", "package");
  }

  public static MavenProcess createCompileProcessor() {
    return createProcessor("clean", "compile");
  }

  public static MavenProcess createGetVersionProcessor() {
    var processor = new MavenProcess(false);

    processor.commands.add("-v");
    return processor;
  }

}
