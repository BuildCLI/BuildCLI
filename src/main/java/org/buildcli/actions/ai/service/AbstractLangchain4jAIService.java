package org.buildcli.actions.ai.service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.buildcli.actions.ai.AIChat;
import org.buildcli.actions.ai.AIService;

public abstract class AbstractLangchain4jAIService implements AIService {
  private final ChatLanguageModel model;

  protected AbstractLangchain4jAIService() {
    this(null);
  }

  protected AbstractLangchain4jAIService(ChatLanguageModel model) {
    this.model = model;
  }

  @Override
  public String generate(AIChat chat) {
    var aiMessageResponse = model.generate(
        new SystemMessage(chat.getSystemMessage()),
        new UserMessage(chat.getUserMessage())
    );

    return aiMessageResponse.content().text();
  }
}
