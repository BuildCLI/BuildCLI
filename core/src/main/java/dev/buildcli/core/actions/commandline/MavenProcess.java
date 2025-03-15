package dev.buildcli.core.actions.commandline;

import dev.buildcli.core.constants.MavenConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

public class MavenProcess extends AbstractCommandLineProcess {
  private static final Logger logger = LoggerFactory.getLogger(MavenProcess.class);
  private MavenProcess(boolean printOutput) {
    super(MavenConstants.MAVEN_CMD, printOutput);
  }

  public static MavenProcess createProcessor(String... goals) {
    var processor = new MavenProcess(true);
    processor.commands.addAll(Arrays.asList(goals));
    return processor;
  }

  public static MavenProcess createPackageProcessor(File directory) {
    logger.info("Running maven package command: {}", String.join(" ", "mvn", "clean", "package", "-f", directory.getAbsolutePath()));
    return createProcessor("clean", "package", "-f", directory.getAbsolutePath());
  }

  public static MavenProcess createCompileProcessor(File directory) {
    logger.info("Running maven compile command: {}", String.join(" ", "mvn", "compile", "-f", directory.getAbsolutePath()));
    return createProcessor("clean", "compile", "-f", directory.getAbsolutePath());
  }

  public static MavenProcess createGetVersionProcessor() {
    var processor = new MavenProcess(false);

    processor.commands.add("-v");
    return processor;
  }

}
