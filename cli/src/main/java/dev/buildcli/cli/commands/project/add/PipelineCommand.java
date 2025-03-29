package dev.buildcli.cli.commands.project.add;

import dev.buildcli.core.actions.pipeline.PipelineFileGenerator;
import dev.buildcli.core.domain.BuildCLICommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Parameters;

@Command(name = "pipeline", aliases = {"pl"}, description = "Configure CI/CD for the specified tool (e.g., github, gitlab, jenkins)", mixinStandardHelpOptions = true)
public class PipelineCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger(PipelineCommand.class);

  @Parameters
  private String[] toolNames;

  @Override
  public void run() {
    for (String toolName : toolNames) {
      try {
        var generator = PipelineFileGenerator.PipelineFileGeneratorFactory.factory(toolName.toLowerCase());

        generator.generate();

        logger.info("CI/CD configuration created successfully for {}" , toolName);
      } catch (IOException | IllegalStateException e) {
        logger.error("Failed to configure CI/CD for {}", toolName, e);
      }
    }
  }
}
