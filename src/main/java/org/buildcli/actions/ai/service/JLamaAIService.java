package org.buildcli.actions.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.jlama.JlamaChatModel;
import dev.langchain4j.model.jlama.JlamaChatModel.JlamaChatModelBuilder;

import java.nio.file.Path;

public class JLamaAIService extends AbstractLangchain4jAIService {

  protected JLamaAIService(ChatLanguageModel model) {
    super(model);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final JlamaChatModelBuilder builder;

    private Builder() {
      builder = new JlamaChatModelBuilder();
      var path = Path.of(System.getProperty("user.home"), ".buildcli", "ai", "jlama");

      builder.workingDirectory(path).modelCachePath(path).temperature(.7f);
    }

    public Builder modelName(String modelName) {
      builder.modelName(modelName);
      return this;
    }

    public JLamaAIService build() {
      return new JLamaAIService(builder.build());
    }
  }
}
