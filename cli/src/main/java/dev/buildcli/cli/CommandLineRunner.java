package dev.buildcli.cli;

import dev.buildcli.plugin.CommandFactory;
import dev.buildcli.core.log.config.LoggingConfig;
import dev.buildcli.core.utils.BuildCLIService;
import dev.buildcli.plugin.BuildCLICommandPlugin;
import dev.buildcli.plugin.PluginManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

public class CommandLineRunner {
  public static void main(String[] args) {
    LoggingConfig.configure();

    if (BuildCLIService.shouldShowAsciiArt(args)) {
      BuildCLIService.welcome();
    }

    var commandPlugins = PluginManager.getCommands();

    var commandLine = new CommandLine(new BuildCLI());

    for (BuildCLICommandPlugin commandPlugin : commandPlugins) {
      commandLine.addSubcommand(CommandFactory.createCommandLine(commandPlugin));
    }

    int exitCode = commandLine.execute(args);
    BuildCLIService.checkUpdatesBuildCLIAndUpdate();

    System.exit(exitCode);
  }
}
