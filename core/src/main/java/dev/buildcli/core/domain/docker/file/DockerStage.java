package dev.buildcli.core.domain.docker.file;

import java.util.ArrayList;
import java.util.List;

public record DockerStage(
    String name,
    From from,
    List<DockerfileResource> resources
) {
  public DockerStage {
    if (resources == null) {
      resources = new ArrayList<>();
    }
  }
}
