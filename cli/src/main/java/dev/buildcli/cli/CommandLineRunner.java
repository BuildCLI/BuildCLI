package dev.buildcli.cli;

import dev.buildcli.cli.utils.BuildCLICommandMan;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.log.config.LoggingConfig;
import dev.buildcli.core.utils.AsciiArtManager;
import dev.buildcli.core.utils.BuildCLIService;
import dev.buildcli.hooks.HookManager;
import dev.buildcli.plugin.utils.BuildCLIPluginManager;
import picocli.CommandLine;

public class CommandLineRunner {

  public static void main(String[] args) {
    LoggingConfig.configure();

    // Only display ASCII art welcome banner for specific commands
    if (AsciiArtManager.shouldShowAsciiArt(args)) {
      BuildCLIService.welcome();
    }

    BuildCLIConfig.initialize();
    var commandLine = new CommandLine(new BuildCLI());
    BuildCLICommandMan.setCmd(commandLine);

    BuildCLIPluginManager.registerPlugins(commandLine);

    HookManager hook = new HookManager(commandLine);
    hook.executeHook(args, commandLine);

    BuildCLIService.checkUpdatesBuildCLIAndUpdate();

    System.exit(0);
  }
}