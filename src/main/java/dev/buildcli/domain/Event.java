package dev.buildcli.domain;

import java.time.Instant;

/** An entry of the append-only event log. Token counts are set on the events that consume tokens, 0 otherwise. */
public record Event(Instant ts, String type, int taskId, String agent, String payload, int inputTokens, int outputTokens) {

    public Event(Instant ts, String type, int taskId, String agent, String payload) {
        this(ts, type, taskId, agent, payload, 0, 0);
    }
}
