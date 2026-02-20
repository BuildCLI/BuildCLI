package dev.buildcli.core.actions.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder;

import java.time.Duration;

public class GeminiAIService extends AbstractLangchain4jAIService {
  protected GeminiAIService(ChatLanguageModel model) {
    super(model);
  }

  public static GeminiAIServiceBuilder builder() {
    return new GeminiAIServiceBuilder();
  }

  public static final class GeminiAIServiceBuilder {
    private final GoogleAiGeminiChatModelBuilder builder;

    private GeminiAIServiceBuilder() {
      builder = GoogleAiGeminiChatModel.builder()
          .maxRetries(5)
          .temperature(0.7)
          .maxOutputTokens(Integer.MAX_VALUE)
          .timeout(Duration.ofMinutes(5));
    }

    public GeminiAIServiceBuilder model(String model) {
      builder.modelName(model);
      return this;
    }

    public GeminiAIServiceBuilder token(String token) {
      builder.apiKey(token);
      return this;
    }

    public GeminiAIService build() {
      return new GeminiAIService(builder.build());
    }
  }
}
