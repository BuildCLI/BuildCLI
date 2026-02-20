package dev.buildcli.core.domain.docker.file;

import java.util.ArrayList;
import java.util.List;

public record Dockerfile(String name, List<DockerStage> stages) {
  public Dockerfile {
    name = name == null ? "Dockerfile" : name;

    if (stages == null) {
      throw new IllegalArgumentException("Stages cannot be null");
    }

    stages = new ArrayList<>(stages);
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();

    for (DockerStage stage : stages) {
      builder.append(stage.from().toString()).append("\n");

      for (DockerfileResource resource : stage.resources()) {
        builder.append(resource.toString()).append("\n");
      }

      builder.append("\n");
    }

    return builder.toString();
  }
}
