package dev.buildcli.domain;

/** Tokens consumed by one agent in one run, derived from the event log. */
public record AgentUsage(String agent, int calls, long inputTokens, long outputTokens) {}
