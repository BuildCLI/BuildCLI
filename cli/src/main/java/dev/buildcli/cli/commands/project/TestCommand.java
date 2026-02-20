package dev.buildcli.cli.commands.project;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.project.ProjectTester;
import picocli.CommandLine.Command;

@Command(name = "test", aliases = {"t"}, description = "Executes the project tests.", mixinStandardHelpOptions = true)
public class TestCommand implements BuildCLICommand {
  @Override
  public void run() {
    new ProjectTester().execute();
  }
}
