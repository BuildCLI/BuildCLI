package dev.buildcli.core.actions.ai.factories;

import dev.buildcli.core.actions.ai.params.GeminiAIServiceParams;
import dev.buildcli.core.actions.ai.service.GeminiAIService;

public class GeminiAIServiceFactory implements AIServiceFactory<GeminiAIService, GeminiAIServiceParams> {
  @Override
  public GeminiAIService create(GeminiAIServiceParams params) {
    return GeminiAIService.builder()
        .model(params.model().orElse("gemini-2.0-flash-lite"))
        .token(params.token().orElseThrow())
        .build();
  }
}
