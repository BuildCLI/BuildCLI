package dev.buildcli.core.domain.maven;

import io.github.dumijdev.dpxml.stereotype.Pojolizable;
import io.github.dumijdev.dpxml.stereotype.RootElement;
import io.github.dumijdev.dpxml.stereotype.Xmlizable;

@Xmlizable
@Pojolizable
@RootElement(name = "project")
public class Project {
  private String modelVersion;
  private String groupId;
  private String artifactId;
  private String version;
  private String packaging;
  private String name;
  private String description;
  private Dependencies dependencies;
  private Repositories repositories;
  private Build build;

  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getPackaging() {
    return packaging;
  }

  public void setPackaging(String packaging) {
    this.packaging = packaging;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Dependencies getDependencies() {
    return dependencies;
  }

  public void setDependencies(Dependencies dependencies) {
    this.dependencies = dependencies;
  }

  public Repositories getRepositories() {
    return repositories;
  }

  public void setRepositories(Repositories repositories) {
    this.repositories = repositories;
  }

  public Build getBuild() {
    return build;
  }

  public void setBuild(Build build) {
    this.build = build;
  }
}