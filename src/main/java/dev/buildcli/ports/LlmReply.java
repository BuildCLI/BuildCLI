package dev.buildcli.ports;

import java.util.List;

public record LlmReply(String text, List<ToolCall> toolCalls, int inputTokens, int outputTokens) {}
