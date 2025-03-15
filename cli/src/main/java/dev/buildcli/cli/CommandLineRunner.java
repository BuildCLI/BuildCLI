package dev.buildcli.cli;

import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.log.config.LoggingConfig;
import dev.buildcli.core.utils.BuildCLIService;
import dev.buildcli.hooks.HookManager;
import dev.buildcli.plugin.utils.PluginManager;
import picocli.CommandLine;

public class CommandLineRunner {

  public static void main(String[] args) {
    LoggingConfig.configure();

    if (BuildCLIService.shouldShowAsciiArt(args)) {
      BuildCLIService.welcome();
    }

    BuildCLIConfig.initialize();
    var commandLine = new CommandLine(new BuildCLI());

    PluginManager.registerPlugins(commandLine);

    HookManager hook = new HookManager(commandLine);

    hook.executeHook(args, commandLine);

    BuildCLIService.checkUpdatesBuildCLIAndUpdate();

    System.exit(0);
  }
}
