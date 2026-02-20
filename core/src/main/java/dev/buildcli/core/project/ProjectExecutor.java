package dev.buildcli.core.project;

import dev.buildcli.core.utils.SystemCommands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ProjectExecutor {

  private static final Logger logger = Logger.getLogger(ProjectExecutor.class.getName());

  protected final List<String> command;

  protected ProjectExecutor() {
    this.command = new ArrayList<>();
    this.command.add(SystemCommands.MVN.getCommand());
  }

  protected abstract void addMvnCommand();

  protected abstract String getErrorMessage();

  public void execute() {

    this.addMvnCommand();

    try {
      var builder = new ProcessBuilder(this.command);
      builder.inheritIO();
      var process = builder.start();
      process.waitFor();
    } catch (IOException | InterruptedException e) {
      logger.log(Level.SEVERE, this.getErrorMessage(), e);
      Thread.currentThread().interrupt();
    }
  }
}
