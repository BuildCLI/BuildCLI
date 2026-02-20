package dev.buildcli.core.actions.ai;

import java.util.UUID;

public class AIChat {
  private final UUID chatId = UUID.randomUUID();
  private final String systemMessage;
  private final String userMessage;
  private final boolean persistent;

  public AIChat(String systemMessage, String userMessage) {
    this(systemMessage, userMessage, false);
  }

  public AIChat(String systemMessage, String userMessage, boolean persistent) {
    this.systemMessage = systemMessage;
    this.userMessage = userMessage;
    this.persistent = persistent;
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

  public boolean isPersistent() {
    return persistent;
  }
}
