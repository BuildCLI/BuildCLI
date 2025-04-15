package dev.buildcli.cli.utils;

import dev.buildcli.core.domain.man.CommandMan;
import picocli.CommandLine;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.join;
import static java.util.Arrays.asList;

public final class BuildCLICommandMan {
  private static CommandLine cmd;

  public static void setCmd(CommandLine cmd) {
    BuildCLICommandMan.cmd = cmd;
  }

  public static Set<CommandMan> getManual() {
    return collectCommands(cmd, "").stream().sorted(Comparator.comparing(CommandMan::getLongCommand)).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static Set<CommandMan> collectCommands(CommandLine cmd, String prefix) {
    var commands = new HashSet<CommandMan>();
    var subCommands = cmd.getSubcommands().values();
    var commandPrefix = prefix + (prefix.isEmpty() ? "" : " ");

    for (var command : subCommands) {
      var names = new ArrayList<String>();

      names.add(commandPrefix + command.getCommandSpec().name());

      for (var alias : command.getCommandSpec().aliases()) {
        names.add(commandPrefix + alias);
      }

      var desc = join("\n", asList(command.getCommandSpec().usageMessage().description()));

      commands.add(CommandMan.create().addCommands(names.toArray(String[]::new)).addDescription(desc));

      var commandsMan = collectCommands(command, commandPrefix + command.getCommandName());

      if (!commandsMan.isEmpty()) {
        commands.addAll(commandsMan);
      }
    }

    return commands;
  }
}
