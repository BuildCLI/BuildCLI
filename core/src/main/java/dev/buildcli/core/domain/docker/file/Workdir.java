package dev.buildcli.core.domain.docker.file;

public record Workdir(String path) implements DockerfileResource {
  @Override
  public String toString() {
    return "WORKDIR " + path;
  }

  @Override
  public DockerfileResourceType getType() {
    return DockerfileResourceType.WORKDIR;
  }
}
