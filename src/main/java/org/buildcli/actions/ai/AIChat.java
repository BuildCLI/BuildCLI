package org.buildcli.actions.ai;

import org.buildcli.constants.AIConstants;

import java.util.UUID;

public class AIChat {
  private final UUID chatId = UUID.randomUUID();
  private final String systemMessage;
  private final String userMessage;

  public AIChat(String systemMessage, String userMessage) {
    this.systemMessage = systemMessage;
    this.userMessage = userMessage;
  }

  public UUID getChatId() {
    return chatId;
  }

  public String getUserMessage() {
    return userMessage;
  }

  public String getSystemMessage() {
    return systemMessage;
  }
}
