package dev.buildcli.cli.commands;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.AutoCompleteManager;
import picocli.CommandLine.Command;

@Command(name = "autocomplete",
        aliases = {"ac"},
        description = "Sets up autocomplete for BuildCLI commands to enhance user experience by suggesting commands as you type.",
        mixinStandardHelpOptions = true)
public class AutocompleteCommand implements BuildCLICommand {
  @Override
  public void run() {
    new AutoCompleteManager().setupAutocomplete();
  }
}
