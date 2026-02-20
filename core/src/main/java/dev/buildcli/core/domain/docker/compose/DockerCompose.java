package dev.buildcli.core.domain.docker.compose;

import java.util.HashMap;
import java.util.Map;

public record DockerCompose(
    String name,
    String version,
    Map<String, Service> services,
    Map<String, Network> networks,
    Map<String, Volume> volumes
) {
  public DockerCompose {
    name = name == null ? "docker-compose" : name;
    services = services != null ? new HashMap<>(services) : new HashMap<>();
    networks = networks != null ? new HashMap<>(networks) : new HashMap<>();
    volumes = volumes != null ? new HashMap<>(volumes) : new HashMap<>();
  }

  @Override
  public String toString() {
    StringBuilder yaml = new StringBuilder();
    if (version != null) {
      yaml.append("version: \"").append(version).append("\"\n\n");
    }

    if (!services.isEmpty()) {
      yaml.append("services:\n");
      services.forEach((name, service) -> {
        yaml.append("  ").append(name).append(":\n");
        if (service.image() != null) yaml.append("    image: ").append(service.image()).append("\n");
        if (service.build() != null) yaml.append("    build: ").append(service.build()).append("\n");
        if (!service.command().isEmpty())
          yaml.append("    command: ").append(String.join(" ", service.command())).append("\n");
        if (!service.depends_on().isEmpty()) {
          yaml.append("    depends_on:\n");
          service.depends_on().forEach(dep -> yaml.append("      - ").append(dep).append("\n"));
        }
        if (!service.environment().isEmpty()) {
          yaml.append("    environment:\n");
          service.environment().forEach((k, v) -> yaml.append("      ").append(k).append(": ").append(v).append("\n"));
        }
        if (!service.ports().isEmpty()) {
          yaml.append("    ports:\n");
          service.ports().forEach(port -> yaml.append("      - \"").append(port).append("\"\n"));
        }
        if (!service.volumes().isEmpty()) {
          yaml.append("    volumes:\n");
          service.volumes().forEach(volume -> yaml.append("      - ").append(volume).append("\n"));
        }
        if (!service.networks().isEmpty()) {
          yaml.append("    networks:\n");
          service.networks().forEach(network -> yaml.append("      - ").append(network).append("\n"));
        }
        if (service.restart() != null)
          yaml.append("    restart: ").append(service.restart().toString().toLowerCase().replace("_", "-")).append("\n");
        if (!service.labels().isEmpty()) {
          yaml.append("    labels:\n");
          service.labels().forEach((k, v) -> yaml.append("      ").append(k).append(": ").append(v).append("\n"));
        }
        if (service.healthcheck() != null) {
          yaml.append("    healthcheck:\n");
          if (!service.healthcheck().test().isEmpty()) {
            yaml.append("      test:\n");
            service.healthcheck().test().forEach(test -> yaml.append("        - ").append(test).append("\n"));
          }
          if (service.healthcheck().interval() != null)
            yaml.append("      interval: ").append(service.healthcheck().interval()).append("\n");
          if (service.healthcheck().timeout() != null)
            yaml.append("      timeout: ").append(service.healthcheck().timeout()).append("\n");
          if (service.healthcheck().retries() != 0)
            yaml.append("      retries: ").append(service.healthcheck().retries()).append("\n");
          if (service.healthcheck().start_period() != null)
            yaml.append("      start_period: ").append(service.healthcheck().start_period()).append("\n");
        }
      });
    }

    if (!networks.isEmpty()) {
      yaml.append("\nnetworks:\n");
      networks.forEach((name, network) -> {
        yaml.append("  ").append(name).append(":\n");
        if (network.driver() != null) yaml.append("    driver: ").append(network.driver()).append("\n");
        if (network.external() != null) yaml.append("    external: ").append(network.external()).append("\n");
        if (!network.driver_opts().isEmpty()) {
          yaml.append("    driver_opts:\n");
          network.driver_opts().forEach((k, v) -> yaml.append("      ").append(k).append(": ").append(v).append("\n"));
        }
        if (!network.labels().isEmpty()) {
          yaml.append("    labels:\n");
          network.labels().forEach((k, v) -> yaml.append("      ").append(k).append(": ").append(v).append("\n"));
        }
      });
    }

    if (!volumes.isEmpty()) {
      yaml.append("\nvolumes:\n");
      volumes.forEach((name, volume) -> {
        yaml.append("  ").append(name).append(":\n");
        if (volume.driver() != null) yaml.append("    driver: ").append(volume.driver()).append("\n");
        if (volume.external() != null) yaml.append("    external: ").append(volume.external()).append("\n");
        if (!volume.driver_opts().isEmpty()) {
          yaml.append("    driver_opts:\n");
          volume.driver_opts().forEach((k, v) -> yaml.append("      ").append(k).append(": ").append(v).append("\n"));
        }
        if (!volume.labels().isEmpty()) {
          yaml.append("    labels:\n");
          volume.labels().forEach((k, v) -> yaml.append("      ").append(k).append(": ").append(v).append("\n"));
        }
      });
    }
    return yaml.toString();
  }

}