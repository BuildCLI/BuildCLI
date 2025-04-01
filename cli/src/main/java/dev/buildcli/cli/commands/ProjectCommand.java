package dev.buildcli.cli.commands;

import dev.buildcli.cli.commands.project.*;
import picocli.CommandLine.Command;

@Command(name = "project", aliases = {"p"}, description = "Manage and create Java projects.",
    subcommands = {
        AddCommand.class, RmCommand.class, BuildCommand.class, SetCommand.class,
        TestCommand.class, InitCommand.class, CleanupCommand.class, UpdateCommand.class
    },
    mixinStandardHelpOptions = true
)
public class ProjectCommand {

}
