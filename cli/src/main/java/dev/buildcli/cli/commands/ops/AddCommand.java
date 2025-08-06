package dev.buildcli.cli.commands.ops;

import dev.buildcli.cli.commands.ops.add.DockerCommand;
import dev.buildcli.cli.commands.ops.add.PipelineCommand;
import picocli.CommandLine.Command;

@Command(name = "add", aliases = {"a"}, description = "Add an ops support", mixinStandardHelpOptions = true,
    subcommands = {DockerCommand.class, PipelineCommand.class})
public class AddCommand {
}
