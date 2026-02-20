package dev.buildcli.core.domain.docker.file;

public record Expose(int port) implements DockerfileResource {
  @Override
  public String toString() {
    return "EXPOSE " + port;
  }

  @Override
  public DockerfileResourceType getType() {
    return DockerfileResourceType.EXPOSE;
  }
}
