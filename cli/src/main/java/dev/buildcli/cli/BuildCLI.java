package dev.buildcli.cli;

import dev.buildcli.cli.commands.AboutCommand;
import dev.buildcli.cli.commands.AutocompleteCommand;
import dev.buildcli.cli.commands.ConfigCommand;
import dev.buildcli.cli.commands.ProjectCommand;
import dev.buildcli.cli.commands.VersionCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "buildcli", mixinStandardHelpOptions = true,
    version = "BuildCLI 0.0.14",
    description = "BuildCLI - A CLI for Java Project Management",
    subcommands = {
        AutocompleteCommand.class, ProjectCommand.class, VersionCommand.class,
        AboutCommand.class, CommandLine.HelpCommand.class, ConfigCommand.class,
    }
)
public class BuildCLI {

}
