package dev.buildcli.core.actions.ai.params;

import dev.buildcli.core.actions.ai.AIServiceParams;

import java.util.Objects;
import java.util.Optional;

public class OllamaAIServiceParams implements AIServiceParams {
  private final String url;
  private final String modelName;

  public OllamaAIServiceParams(String url, String modelName) {
    this.url = Objects.requireNonNull(url);
    this.modelName = Objects.requireNonNull(modelName);
  }

  @Override
  public Optional<String> model() {
    return Optional.of(modelName);
  }

  @Override
  public String vendor() {
    return "ollama";
  }

  public String url() {
    return url;
  }

  @Override
  public String toString() {
    return "OllamaAIServiceParams{" +
        "url='" + url + '\'' +
        ", modelName='" + modelName + '\'' +
        '}';
  }
}
