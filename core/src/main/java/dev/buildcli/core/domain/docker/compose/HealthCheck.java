package dev.buildcli.core.domain.docker.compose;

import java.util.ArrayList;
import java.util.List;

public record HealthCheck(
    List<String> test,
    String interval,
    String timeout,
    int retries,
    String start_period
) {
  public HealthCheck {
    test = test != null ? new ArrayList<>(test) : new ArrayList<>();
  }
}
