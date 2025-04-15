package dev.buildcli.plugin;

import dev.buildcli.core.domain.BuildCLICommand;

public abstract class BuildCLICommandPlugin extends BuildCLIPlugin implements BuildCLICommand {
  public String[] parents(){
    return null;
  }
}
