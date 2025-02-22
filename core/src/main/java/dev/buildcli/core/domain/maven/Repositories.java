package dev.buildcli.core.domain.maven;

import io.github.dumijdev.dpxml.stereotype.Element;
import io.github.dumijdev.dpxml.stereotype.Pojolizable;
import io.github.dumijdev.dpxml.stereotype.Xmlizable;

import java.util.List;

@Xmlizable
@Pojolizable
public class Repositories {
  @Element(name = "repository")
  private List<Repository> repositories;

  public List<Repository> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<Repository> repository) {
    this.repositories = repository;
  }
}
