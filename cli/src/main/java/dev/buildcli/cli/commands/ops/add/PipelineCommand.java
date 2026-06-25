package dev.buildcli.cli.commands.ops.add;

import dev.buildcli.core.actions.pipeline.PipelineFileGenerator;
import dev.buildcli.core.domain.BuildCLICommand;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Parameters;

@Command(name = "pipeline", aliases = {"pl"}, description = "Configure CI/CD for the specified tool (e.g., github, gitlab, jenkins)", mixinStandardHelpOptions = true)
public class PipelineCommand implements BuildCLICommand {
  private static final Logger logger = Logger.getLogger(PipelineCommand.class.getName());

  @Parameters
  private String[] toolNames;

  @Override
  public void run() {
    for (String toolName : toolNames) {
      try {
        var generator = PipelineFileGenerator.PipelineFileGeneratorFactory.factory(toolName.toLowerCase());

        generator.generate();

        logger.info("CI/CD configuration created successfully for " + toolName);
      } catch (IOException | IllegalStateException e) {
        logger.log(Level.SEVERE, "Failed to configure CI/CD for " + toolName, e);
      }
    }
  }
}
