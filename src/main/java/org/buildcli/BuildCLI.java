package org.buildcli;

import org.buildcli.commands.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "BuildCLI", mixinStandardHelpOptions = true,
    version = "BuildCLI 0.0.14",
    description = "BuildCLI - A CLI for Java Project Management",
    subcommands = {
        AutocompleteCommand.class, ProjectCommand.class, VersionCommand.class,
        AboutCommand.class, CommandLine.HelpCommand.class, ConfigCommand.class,
        ChangelogCommand.class
    }
)
public class BuildCLI {

}
