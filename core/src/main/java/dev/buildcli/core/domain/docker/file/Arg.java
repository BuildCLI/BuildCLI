package dev.buildcli.core.domain.docker.file;

public record Arg(String name, String defaultValue) implements DockerfileResource {
  @Override
  public String toString() {
    return "ARG " + name + (defaultValue != null ? "=" + defaultValue : "");
  }

  @Override
  public DockerfileResourceType getType() {
    return DockerfileResourceType.ARG;
  }
}
