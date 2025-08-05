package dev.buildcli.cli.commands;

import dev.buildcli.cli.commands.ops.AddCommand;
import picocli.CommandLine.Command;

@Command(name = "ops", description = "manage ops support like docker, pipelines, kubernetes, etc...",
    mixinStandardHelpOptions = true, subcommands = {AddCommand.class}
)
public class OpsCommand {
}
