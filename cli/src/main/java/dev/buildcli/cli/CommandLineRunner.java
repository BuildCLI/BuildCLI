package dev.buildcli.cli;

import dev.buildcli.cli.commands.*;
import dev.buildcli.cli.utils.BuildCLICommandMan;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.log.config.LoggingConfig;
import dev.buildcli.core.utils.BuildCLIService;
import dev.buildcli.hooks.HookManager;
import dev.buildcli.plugin.utils.BuildCLIPluginManager;
import picocli.CommandLine;

@CommandLine.Command(name = "buildcli", mixinStandardHelpOptions = true,
    version = "BuildCLI 0.0.14",
    description = "BuildCLI - A CLI for Java Project Management",
    subcommands = {
        AboutCommand.class, AiCommand.class, AutocompleteCommand.class, ChangelogCommand.class, ConfigCommand.class,
        DoctorCommand.class, HookCommand.class, ProjectCommand.class, PluginCommand.class, RunCommand.class,
        VersionCommand.class, CommandLine.HelpCommand.class, BugCommand.class, ManCommand.class, OpsCommand.class
    }
)
public class CommandLineRunner {

  public static void main(String[] args) {
    LoggingConfig.configure();

    BuildCLIService.welcome();

    BuildCLIConfig.initialize();
    var commandLine = new CommandLine(new CommandLineRunner());
    BuildCLICommandMan.setCmd(commandLine);

    BuildCLIPluginManager.registerPlugins(commandLine);

    HookManager hook = new HookManager(commandLine);
    hook.executeHook(args, commandLine);

    BuildCLIService.checkUpdatesBuildCLIAndUpdate();

    System.exit(0);
  }
}