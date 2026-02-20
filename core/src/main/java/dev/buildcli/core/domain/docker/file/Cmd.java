package dev.buildcli.core.domain.docker.file;

public record Cmd(String command) implements DockerfileResource {
  @Override
  public DockerfileResourceType getType() {
    return DockerfileResourceType.CMD;
  }
}
