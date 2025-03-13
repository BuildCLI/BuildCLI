package dev.buildcli.cli;

import dev.buildcli.plugin.CommandFactory;
import dev.buildcli.core.utils.BuildCLIService;
import dev.buildcli.core.utils.input.InteractiveInputUtils;
import dev.buildcli.plugin.BuildCLICommandPlugin;
import dev.buildcli.plugin.PluginManager;
import dev.buildcli.hooks.HookManager;
import picocli.CommandLine;
import dev.buildcli.core.domain.configs.BuildCLIConfig;

import java.util.List;

public class CommandLineRunner {

  public static void main(String[] args) {
    if (BuildCLIService.shouldShowAsciiArt(args)) {
      BuildCLIService.welcome();
    }

    var commandPlugins = PluginManager.getCommands();

    BuildCLIConfig.initialize();
    var commandLine = new CommandLine(new BuildCLI());

    register(commandLine, commandPlugins);

    HookManager hook = new HookManager(commandLine);

    hook.executeHook(args, commandLine);

    BuildCLIService.checkUpdatesBuildCLIAndUpdate();

    System.exit(0);
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
