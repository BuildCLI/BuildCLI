package dev.buildcli.core.actions.ai.params;

import dev.buildcli.core.actions.ai.AIServiceParams;

import java.util.Optional;

public class OllamaAIServiceParams implements AIServiceParams {
  private final String url;
  private final String modelName;

  public OllamaAIServiceParams(String url, String modelName) {
    this.url = url;
    this.modelName = modelName;
  }

  @Override
  public Optional<String> model() {
    return Optional.ofNullable(modelName);
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
