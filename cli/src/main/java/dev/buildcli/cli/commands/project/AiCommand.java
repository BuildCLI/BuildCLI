package dev.buildcli.cli.commands.project;

import dev.buildcli.cli.commands.code.CommentCommand;
import dev.buildcli.cli.commands.code.DocumentCommand;
import picocli.CommandLine.Command;

@Command(name = "ai", description = "Command to use ai features", mixinStandardHelpOptions = true,
    subcommands = {DocumentCommand.class, CommentCommand.class}
)
public class AiCommand {
}
