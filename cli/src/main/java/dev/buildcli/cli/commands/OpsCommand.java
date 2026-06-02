package dev.buildcli.cli.commands;

import dev.buildcli.cli.commands.ops.add.DockerCommand;
import picocli.CommandLine.Command;

@Command(
    name = "ops",
    aliases = {"o"},
    description = "Manage operational resources.",
    subcommands = {DockerCommand.class},
    mixinStandardHelpOptions = true
)
public class OpsCommand {

}
