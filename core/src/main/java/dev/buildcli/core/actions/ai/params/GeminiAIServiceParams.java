package dev.buildcli.core.actions.ai.params;

import dev.buildcli.core.actions.ai.AIServiceParams;

import java.util.Objects;
import java.util.Optional;

public class GeminiAIServiceParams implements AIServiceParams {
  private final String model;
  private final String token;

  public GeminiAIServiceParams(String model, String token) {
    this.model = Objects.requireNonNull(model);
    this.token = Objects.requireNonNull(token);
  }

  @Override
  public Optional<String> model() {
    return Optional.of(model);
  }

  @Override
  public String vendor() {
    return "gemini";
  }

  @Override
  public Optional<String> token() {
    return Optional.of(token);
  }
}
