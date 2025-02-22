package dev.buildcli.cli.commands.project;

import dev.buildcli.cli.commands.project.update.DependencyCommand;
import dev.buildcli.cli.commands.project.update.JDKCommand;
import dev.buildcli.cli.commands.project.update.VersionCommand;
import picocli.CommandLine.Command;

@Command(name = "update", aliases = {"up"}, description = "Updates project versions and dependencies.", mixinStandardHelpOptions = true,
    subcommands = {VersionCommand.class, DependencyCommand.class, JDKCommand.class}
)
public class UpdateCommand {
}
