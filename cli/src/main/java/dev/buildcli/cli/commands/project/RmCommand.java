package dev.buildcli.cli.commands.project;

import dev.buildcli.cli.commands.project.rm.DependencyCommand;
import dev.buildcli.cli.commands.project.rm.ProfileCommand;
import picocli.CommandLine.Command;

@Command(name = "remove", aliases = {"rm"}, description = "Removes dependencies and profiles from the project.",
    subcommands = {DependencyCommand.class, ProfileCommand.class},
    mixinStandardHelpOptions = true
)
public class RmCommand {

}
