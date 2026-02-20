package dev.buildcli.core.domain.docker.file;

public record Copy(String source, String destination) implements DockerfileResource {
  @Override
  public String toString() {
    return "COPY " + source + " " + destination;
  }

  @Override
  public DockerfileResourceType getType() {
    return DockerfileResourceType.COPY;
  }
}
