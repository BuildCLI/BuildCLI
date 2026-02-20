package dev.buildcli.core.domain.docker.file;

public record From(String baseImage, String alias) implements DockerfileResource {
  @Override
  public String toString() {
    return "FROM " + baseImage + (alias != null && !alias.isEmpty() ? " AS " + alias : "");
  }

  @Override
  public DockerfileResourceType getType() {
    return DockerfileResourceType.FROM;
  }
}
