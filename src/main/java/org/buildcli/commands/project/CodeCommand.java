package org.buildcli.commands.project;

import org.buildcli.commands.code.CommentCommand;
import org.buildcli.commands.code.DocumentCommand;
import picocli.CommandLine.Command;

@Command(name = "code", aliases = {"source"}, description = "Command parent to interact with source code into a project", mixinStandardHelpOptions = true,
    subcommands = {DocumentCommand.class, CommentCommand.class}
)
public class CodeCommand {
}
