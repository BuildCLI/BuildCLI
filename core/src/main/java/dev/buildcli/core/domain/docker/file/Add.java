package dev.buildcli.core.domain.docker.file;

public record Add(String source, String destination) implements DockerfileResource {
  @Override
  public String toString() {
    return "ADD " + source + " " + destination;
  }

  @Override
  public DockerfileResourceType getType() {
    return DockerfileResourceType.ADD;
  }
}
