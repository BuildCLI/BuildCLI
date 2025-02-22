package dev.buildcli.core.domain.maven;

import io.github.dumijdev.dpxml.stereotype.Pojolizable;
import io.github.dumijdev.dpxml.stereotype.Xmlizable;

import java.util.List;

@Xmlizable
@Pojolizable
public class Build {
  private List<Plugin> plugins;

  public List<Plugin> getPlugins() {
    return plugins;
  }

  public void setPlugins(List<Plugin> plugins) {
    this.plugins = plugins;
  }
}
