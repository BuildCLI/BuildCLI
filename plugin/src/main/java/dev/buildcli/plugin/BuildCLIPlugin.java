package dev.buildcli.plugin;

import org.pf4j.Plugin;

public abstract class BuildCLIPlugin extends Plugin {
  public BuildCLIPlugin() {
    super();
  }

  public abstract String name();

  public abstract String description();

  public abstract String version();
}
