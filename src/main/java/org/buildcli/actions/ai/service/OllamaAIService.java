package org.buildcli.actions.ai.service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.buildcli.actions.ai.AIChat;
import org.buildcli.actions.ai.AIService;

import static org.buildcli.utils.ia.CodeUtils.endCode;
import static org.buildcli.utils.ia.CodeUtils.startCode;

public class OllamaAIService extends AbstractLangchain4jAIService {
  protected OllamaAIService(ChatLanguageModel model) {
    super(model);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final OllamaChatModel.OllamaChatModelBuilder builder;

    private Builder() {
      builder = new OllamaChatModel.OllamaChatModelBuilder();

      builder.temperature(.7).maxRetries(3);
    }

    public Builder modelName(String modelName) {
      builder.modelName(modelName);
      return this;
    }

    public Builder url(String url) {
      builder.baseUrl(url);
      return this;
    }

    public OllamaAIService build() {
      return new OllamaAIService(builder.build());
    }

  }
}
