package dev.buildcli.core.project;

import java.util.List;
import java.util.Objects;

public class ProjectUpdater extends ProjectExecutor {

  private static final String MVN_CHECK_UPDATES = "versions:display-dependency-updates";
  private static final String MVN_LATEST_RELEASES = "versions:use-latest-releases";

  private boolean isUpdateNow;

  @Override
  protected void addMvnCommand() {
    this.command.add(this.isUpdateNow ? MVN_LATEST_RELEASES : MVN_CHECK_UPDATES);
  }

  @Override
  protected String getErrorMessage() {
    return "Failed to check/update dependencies";
  }

  public void setAdditionalParameters(List<String> additionalParameters) {
    if (Objects.nonNull(additionalParameters)) {
      this.command.addAll(additionalParameters);
    }
  }

  public ProjectUpdater updateNow(boolean update) {
    this.isUpdateNow = update;
    return this;
  }
}
