package dev.buildcli.cli;

import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.log.config.LoggingConfig;
import dev.buildcli.core.utils.BuildCLIService;
import dev.buildcli.core.utils.input.InteractiveInputUtils;
import dev.buildcli.plugin.BuildCLICommandPlugin;
import dev.buildcli.plugin.CommandFactory;
import dev.buildcli.plugin.PluginManager;
import picocli.CommandLine;

import java.util.List;

public class CommandLineRunner {

  public static void main(String[] args) {
    LoggingConfig.configure();

    if (BuildCLIService.shouldShowAsciiArt(args)) {
      BuildCLIService.welcome();
    }

    var commandPlugins = PluginManager.getCommands();

    BuildCLIConfig.initialize();
    var commandLine = new CommandLine(new BuildCLI());

    commandLine.addSubcommand(CommandLine.Model.CommandSpec.create().name("hello"));

    register(commandLine, commandPlugins);

    int exitCode = commandLine.execute(args);
    BuildCLIService.checkUpdatesBuildCLIAndUpdate();

    System.exit(exitCode);
  }

  private static void register(CommandLine commandLine, List<BuildCLICommandPlugin> plugins) {
    var subcommands = commandLine.getSubcommands();
    for (BuildCLICommandPlugin commandPlugin : plugins) {
      var command = CommandFactory.createCommandLine(commandPlugin);

      if (subcommands.containsKey(command.getCommandName())) {
        var confirm = InteractiveInputUtils.confirm("Do you want override the subcommand \"" + command.getCommandName() + "\"");
        if (confirm) {
          commandLine.getCommandSpec().removeSubcommand(command.getCommandName());
          commandLine.addSubcommand(command);
        }
      }
    }
  }
}
