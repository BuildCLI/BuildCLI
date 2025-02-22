package dev.buildcli.cli.commands.project;

import dev.buildcli.cli.commands.code.CommentCommand;
import dev.buildcli.cli.commands.code.DocumentCommand;
import picocli.CommandLine.Command;

@Command(name = "code", aliases = {"source"}, description = "Command parent to interact with source code into a project", mixinStandardHelpOptions = true,
    subcommands = {DocumentCommand.class, CommentCommand.class}
)
public class CodeCommand {
}
