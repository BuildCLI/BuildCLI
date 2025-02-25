package dev.buildcli.core.domain.maven;

import io.github.dumijdev.dpxml.stereotype.Pojolizable;
import io.github.dumijdev.dpxml.stereotype.Xmlizable;

@Xmlizable
@Pojolizable
public class Build {
  private Plugins plugins;

  public Plugins getPlugins() {
    return plugins;
  }

  public void setPlugins(Plugins plugins) {
    this.plugins = plugins;
  }
}
