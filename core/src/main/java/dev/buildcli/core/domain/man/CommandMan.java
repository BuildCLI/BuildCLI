package dev.buildcli.core.domain.man;

import java.util.*;

public class CommandMan {
  private final Set<String> commands;
  private String description;

  private CommandMan() {
    commands = new HashSet<>();
  }

  public static CommandMan create() {
    return new CommandMan();
  }

  public CommandMan addCommand(String command) {
    commands.add(command);
    return this;
  }

  public CommandMan addCommands(String...command) {
    if (command == null) {
      return this;
    }

    this.commands.addAll(Arrays.asList(command));
    return this;
  }

  public CommandMan addDescription(String description) {
    this.description = description;
    return this;
  }

  public boolean containsCommand(String command) {
    if (commands.isEmpty()) {
      return false;
    }

    if (command == null || command.isEmpty()) {
      return true;
    }

    var contains = false;

    for (var cs : commands) {
      if (cs.contains(command)) {
        contains = true;
        break;
      }
    }

    return contains;
  }

  public Set<String> getCommands() {
    return commands;
  }

  public String getLongCommand() {
    String longCommand = "";
    for (var cs : commands) {
      if (cs.length() > longCommand.length()) {
        longCommand = cs;
      }
    }

    return longCommand;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof CommandMan that)) return false;

    return commands.equals(that.commands) && description.equals(that.description);
  }

  @Override
  public int hashCode() {
    int result = commands.hashCode();
    result = 31 * result + description.hashCode();
    return result;
  }
}
