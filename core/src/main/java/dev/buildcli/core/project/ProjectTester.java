package dev.buildcli.core.project;

public class ProjectTester extends ProjectExecutor {

  @Override
  protected void addMvnCommand() {
    this.command.add("test");
  }

  @Override
  protected String getErrorMessage() {
    return "Failed to run tests";
  }

}
