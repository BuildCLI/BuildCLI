package dev.buildcli.core.domain.maven;

import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.Pojolizable;

import java.util.List;

@Pojolizable
public class Plugins {
  @Element(name = "plugin")
  private List<Plugin> plugins;

  public List<Plugin> getPlugins() {
    return plugins;
  }

  public void setPlugins(List<Plugin> plugins) {
    this.plugins = plugins;
  }
}
