package dev.buildcli.core.domain.docker.compose;

import java.util.HashMap;
import java.util.Map;

public record Volume(
    String driver,
    Boolean external,
    Map<String, String> driver_opts,
    Map<String, String> labels
) {
  public Volume {
    driver_opts = driver_opts != null ? new HashMap<>(driver_opts) : new HashMap<>();
    labels = labels != null ? new HashMap<>(labels) : new HashMap<>();
  }
}
