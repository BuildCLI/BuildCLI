package org.buildcli.actions.ai.factories;

import org.buildcli.actions.ai.AIService;
import org.buildcli.actions.ai.AIServiceParams;

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
