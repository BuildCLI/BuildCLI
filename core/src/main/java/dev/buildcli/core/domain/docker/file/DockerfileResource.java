package dev.buildcli.core.domain.docker.file;

public interface DockerfileResource {
  String toString();

  DockerfileResourceType getType();
}
