package dev.buildcli.plugin;

import dev.buildcli.plugin.enums.TemplateType;

public abstract class BuildCLITemplatePlugin extends BuildCLIPlugin {
  public abstract TemplateType type();
  public abstract void execute();
}
