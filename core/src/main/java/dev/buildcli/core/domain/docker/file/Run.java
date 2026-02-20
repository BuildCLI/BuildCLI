package dev.buildcli.core.domain.docker.file;

public record Run(String command) implements DockerfileResource {
  @Override
  public String toString() {
    return "RUN " + command;
  }

  @Override
  public DockerfileResourceType getType() {
    return DockerfileResourceType.RUN;
  }
}
