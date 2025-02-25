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

  public Dependency() {
    this(null, null);
  }

  public Dependency(String groupId, String artifactId) {
    this(groupId, artifactId, null);
  }

  public Dependency(String groupId, String artifactId, String version) {
    this(groupId, artifactId, version, null);
  }

  public Dependency(String groupId, String artifactId, String version, String scope) {
    if (groupId != null && groupId.isEmpty()) {
      throw new IllegalArgumentException("groupId cannot be empty");
    }

    if (artifactId != null && artifactId.isEmpty()) {
      throw new IllegalArgumentException("artifactId cannot be empty");
    }

    if (version != null && version.isEmpty()) {
      throw new IllegalArgumentException("version cannot be empty");
    }

    if (scope != null && scope.isEmpty()) {
      throw new IllegalArgumentException("scope cannot be empty");
    }

    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.scope = scope;
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

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof Dependency that)) return false;

    return groupId.equals(that.groupId) && artifactId.equals(that.artifactId);
  }

  @Override
  public int hashCode() {
    int result = groupId.hashCode();
    result = 31 * result + artifactId.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Dependency{" +
        "groupId='" + groupId + '\'' +
        ", artifactId='" + artifactId + '\'' +
        ", version='" + version + '\'' +
        ", scope='" + scope + '\'' +
        '}';
  }
}
