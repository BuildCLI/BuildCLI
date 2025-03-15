package dev.buildcli.plugin.builders;

import java.io.File;

public interface PluginBuilder {
  File build(File directory, String name);
}
