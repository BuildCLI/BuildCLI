package dev.buildcli.cli.commands.ai;

import dev.buildcli.cli.commands.AiCommand;
import dev.buildcli.cli.commands.ai.code.CommentCommand;
import dev.buildcli.cli.commands.ai.code.DocumentCommand;
import dev.buildcli.cli.commands.ai.code.TestCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "code", description = "AI Code features", mixinStandardHelpOptions = true,
    subcommands = {CommentCommand.class, DocumentCommand.class, TestCommand.class}
)
public class CodeCommand {
  @ParentCommand
  private AiCommand parent;

  public String getModel() {
    return parent.getModel();
  }

  public String getVendor() {
    return parent.getVendor();
  }

  public String getHost() {
    return parent.getHost();
  }

  public String getUser() {
    return parent.getUser();
  }

  public String getPassword() {
    return parent.getPassword();
  }

  public String getToken() {
    return parent.getToken();
  }

}
