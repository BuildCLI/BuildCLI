package dev.buildcli.cli.commands.ai;

import dev.buildcli.cli.commands.ai.code.CommentCommand;
import dev.buildcli.cli.commands.ai.code.DocumentCommand;
import dev.buildcli.cli.commands.ai.code.TestCommand;
import picocli.CommandLine.Command;

@Command(name = "code", description = "AI Code features", mixinStandardHelpOptions = true,
    subcommands = {CommentCommand.class, DocumentCommand.class, TestCommand.class}
)
public class CodeCommand {

}
