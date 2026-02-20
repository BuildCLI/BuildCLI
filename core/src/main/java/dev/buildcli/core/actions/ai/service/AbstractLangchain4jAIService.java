package dev.buildcli.core.actions.ai.service;

import dev.buildcli.core.actions.ai.AIChat;
import dev.buildcli.core.actions.ai.AIService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;

public abstract class AbstractLangchain4jAIService implements AIService {
  private final ChatLanguageModel model;
  private final ChatMemory memory;

  protected AbstractLangchain4jAIService() {
    this(null);
  }

  protected AbstractLangchain4jAIService(ChatLanguageModel model) {
    this.model = model;
    this.memory = MessageWindowChatMemory.withMaxMessages(20);
  }

  @Override
  public String generate(AIChat chat) {
    var systemMessage = new SystemMessage(chat.getSystemMessage());
    var userMessage = new UserMessage(chat.getUserMessage());

    memory.add(systemMessage);
    memory.add(userMessage);

    var aiMessageResponse = model.chat(memory.messages());
    var response = aiMessageResponse.aiMessage();

    memory.add(response);

    if (!chat.isPersistent()) {
      memory.clear();
    }

    return response.text();
  }
}
