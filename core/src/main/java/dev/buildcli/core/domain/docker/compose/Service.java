package dev.buildcli.core.domain.docker.compose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Service(
    String image,
    String build,
    List<String> command,
    List<String> depends_on,
    Map<String, String> environment,
    List<String> ports,
    List<String> volumes,
    List<String> networks,
    RestartPolicy restart,
    Map<String, String> labels,
    HealthCheck healthcheck
) {
  public Service {
    command = command != null ? new ArrayList<>(command) : new ArrayList<>();
    depends_on = depends_on != null ? new ArrayList<>(depends_on) : new ArrayList<>();
    environment = environment != null ? new HashMap<>(environment) : new HashMap<>();
    ports = ports != null ? new ArrayList<>(ports) : new ArrayList<>();
    volumes = volumes != null ? new ArrayList<>(volumes) : new ArrayList<>();
    networks = networks != null ? new ArrayList<>(networks) : new ArrayList<>();
    labels = labels != null ? new HashMap<>(labels) : new HashMap<>();
  }
}
