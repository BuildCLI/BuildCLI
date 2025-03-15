package dev.buildcli.core.actions.commandline;

import dev.buildcli.core.constants.GradleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

public class GradleProcess extends AbstractCommandLineProcess {
  private static final Logger logger = LoggerFactory.getLogger(GradleProcess.class);
  private GradleProcess(boolean printOutput) {
    super(GradleConstants.GRADLE_CMD, printOutput);
  }

  public static GradleProcess createProcessor(String... tasks) {
    var processor = new GradleProcess(true);
    processor.commands.addAll(Arrays.asList(tasks));
    return processor;
  }

  public static GradleProcess createPackageProcessor(File directory) {
    logger.info("Running gradle package command: {}", String.join(" ", "gradle", "clean", "build", "-f", directory.getAbsolutePath()));
    return createProcessor("clean", "build", "-f", directory.getAbsolutePath());
  }

  public static GradleProcess createCompileProcessor(File directory) {
    logger.info("Running gradle compile command: {}", String.join(" ", "gradle", "clean", "classes", "-f", directory.getAbsolutePath()));
    return createProcessor("clean", "classes", "-f", directory.getAbsolutePath());
  }

  public static GradleProcess createGetVersionProcess() {
    var processor = new GradleProcess(false);
    processor.commands.add("--version");
    return processor;
  }
}
