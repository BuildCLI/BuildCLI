package dev.buildcli.core.domain.maven;

import io.github.dumijdev.dpxml.stereotype.Pojolizable;
import io.github.dumijdev.dpxml.stereotype.Xmlizable;

@Xmlizable
@Pojolizable
public class Dependency {
  private String groupId;
  private String artifactId;
  private String version;
  private String scope;

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

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }
}
