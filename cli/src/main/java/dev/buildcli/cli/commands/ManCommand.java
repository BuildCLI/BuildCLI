package dev.buildcli.cli.commands;

import dev.buildcli.cli.utils.BuildCLICommandMan;
import dev.buildcli.cli.utils.CommandUtils;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.man.CommandMan;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;

import static dev.buildcli.core.utils.BeautifyShell.*;
import static dev.buildcli.core.utils.console.PrintConsole.println;
import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.confirm;
import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.options;
import static java.lang.String.join;

@Command(name = "man", description = "Manual for all commands", mixinStandardHelpOptions = true)
public class ManCommand implements BuildCLICommand {
  @Parameters(index = "0", arity = "0..1", description = "search term")
  private String command;

  @Override
  public void run() {
    var commands = BuildCLICommandMan.getManual();

    var foundCommand = new ArrayList<CommandMan>();

    for (var cmd : commands) {
      if (cmd.containsCommand(command) && !foundCommand.contains(cmd)) {
        foundCommand.add(cmd);
      }
    }

    if (foundCommand.isEmpty()) {
      println("No command found");
      return;
    }

    println("Found ", foundCommand.size(), " commands");

    var chosenCommand = options("Choose a command", foundCommand, CommandMan::getLongCommand);

    println("Commands:", content(join(", ", chosenCommand.getCommands())).blueFg().bold());
    println("Description:", content(chosenCommand.getDescription()).bold());

    if (confirm("Do you want to run this command?")) {
      CommandUtils.call(chosenCommand.getLongCommand());
    }
  }
}
