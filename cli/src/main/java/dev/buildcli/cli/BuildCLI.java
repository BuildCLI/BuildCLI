package dev.buildcli.cli;

import dev.buildcli.cli.commands.*;
import dev.buildcli.cli.commands.project.AiCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(name = "buildcli", mixinStandardHelpOptions = true,
    version = "BuildCLI 0.0.14",
    description = "BuildCLI - A CLI for Java Project Management",
    subcommands = {
        AboutCommand.class, AiCommand.class, AutocompleteCommand.class, ChangelogCommand.class, ConfigCommand.class,
        DoctorCommand.class, HookCommand.class, ProjectCommand.class, PluginCommand.class, RunCommand.class,
        VersionCommand.class, HelpCommand.class
    }
)
public class BuildCLI {

}
