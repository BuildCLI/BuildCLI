package dev.buildcli.core.domain.maven;

import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.Pojolizable;
import io.github.dumijdev.dpxml.stereotype.Xmlizable;

import java.util.List;

@Xmlizable
@Pojolizable
public class Dependencies {
  @Element(name = "dependency")
  private List<Dependency> dependencies;

  public List<Dependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies;
  }
}
