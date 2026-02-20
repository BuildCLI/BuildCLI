package dev.buildcli.core.actions.ai.params;

import dev.buildcli.core.actions.ai.AIServiceParams;

import java.util.Objects;
import java.util.Optional;

public class JlamaAIServiceParams implements AIServiceParams {
  private final String model;

  public JlamaAIServiceParams(String model) {
    this.model = Objects.requireNonNull(model);
  }

  @Override
  public Optional<String> model() {
    return Optional.of(model);
  }

  @Override
  public String vendor() {
    return "jlama";
  }

  @Override
  public String toString() {
    return "JlamaAIServiceParams{" +
        "model='" + model + '\'' +
        '}';
  }
}
