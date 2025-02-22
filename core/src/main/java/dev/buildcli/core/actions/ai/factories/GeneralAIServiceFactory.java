package dev.buildcli.core.actions.ai.factories;

import dev.buildcli.core.actions.ai.AIService;
import dev.buildcli.core.actions.ai.AIServiceParams;

@SuppressWarnings("rawtypes")
public class GeneralAIServiceFactory implements AIServiceFactory {

  @SuppressWarnings({"unchecked"})
  @Override
  public AIService create(AIServiceParams params) {

    AIServiceFactory factory = switch (params.vendor().toLowerCase()) {
      case "ollama" -> new OllamaAIServiceFactory();
      default -> new JLamaAIServiceFactory();
    };

    return factory.create(params);
  }
}
