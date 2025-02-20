package org.buildcli.commands;

import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import org.buildcli.utils.AutoCompleteManager;
import picocli.CommandLine.Command;

@Command(name = "autocomplete", description = "", mixinStandardHelpOptions = true)
public class AutocompleteCommand implements BuildCLICommand {
  @Override
  public void run() {
    try {
      new AutoCompleteManager().setupAutocomplete();
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
    }
  }
}
