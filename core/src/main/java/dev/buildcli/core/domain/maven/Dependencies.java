package dev.buildcli.core.domain.maven;

import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.Pojolizable;
import io.github.dumijdev.dpxml.stereotype.Xmlizable;

import java.util.Set;

@Xmlizable
@Pojolizable
public class Dependencies {
  @Element(name = "dependency")
  private Set<Dependency> dependencies;

  public Set<Dependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(Set<Dependency> dependencies) {
    this.dependencies = dependencies;
  }
}
