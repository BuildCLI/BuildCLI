package dev.buildcli.cli.commands.project.update;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.project.ProjectUpdater;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "dependency", aliases = {"d"}, description = "", mixinStandardHelpOptions = true)
public class DependencyCommand implements BuildCLICommand {
  @Option(names = {"--checkOnly"}, description = "", defaultValue = "false")
  private boolean checkOnly;

  @Override
  public void run() {
    ProjectUpdater updater = new ProjectUpdater();
    updater.updateNow(!checkOnly).execute();
  }
}
