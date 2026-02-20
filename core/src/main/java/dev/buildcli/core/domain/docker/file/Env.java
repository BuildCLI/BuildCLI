package dev.buildcli.core.domain.docker.file;

public record Env(String variable) implements DockerfileResource {
  @Override
  public String toString() {
    return "ENV " + variable;
  }

  @Override
  public DockerfileResourceType getType() {
    return DockerfileResourceType.ENV;
  }
}
