package dev.buildcli.plugin;

import dev.buildcli.core.domain.BuildCLICommand;

public interface BuildCLICommandPlugin extends BuildCLICommand, BuildCLIPlugin {
  default String[] parents(){
    return null;
  }
}
