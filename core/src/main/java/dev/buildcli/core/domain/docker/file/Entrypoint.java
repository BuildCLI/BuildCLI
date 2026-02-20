package dev.buildcli.core.domain.docker.file;

public record Entrypoint(String command) implements DockerfileResource {
  @Override
  public String toString() {
    return "ENTRYPOINT " + command;
  }

  @Override
  public DockerfileResourceType getType() {
    return DockerfileResourceType.ENTRYPOINT;
  }
}
