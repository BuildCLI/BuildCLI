package org.buildcli.commands.doctor;

import org.buildcli.actions.tools.DockerChecker;
import org.buildcli.actions.tools.ToolCheckers;
import org.buildcli.domain.BuildCLICommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;

@Command(
    name = "scan",
    description = "Performs a comprehensive scan of the environment to check for required tools, their installation status, versions, and readiness (e.g., running state for Docker). Provides detailed instructions for installing missing tools.",
    mixinStandardHelpOptions = true
)
public class ScanCommand implements BuildCLICommand {
  private final Logger logger = LoggerFactory.getLogger("DoctorScanCommand");
  @Override
  public void run() {
    ToolCheckers.all().forEach(toolChecker -> {
      logger.info("Running environment scan...");
      logger.info("Checking {}...", toolChecker.name());

      if (toolChecker.isInstalled()) {
        logger.info("  Installed, version: {}", toolChecker.version());
        if (toolChecker instanceof DockerChecker c && !c.isRunning()) {
          logger.warn("  Docker is installed but not running.");
        }
      } else {
        logger.info("  Not installed. {}", toolChecker.installInstructions());
      }
    });
  }
}
