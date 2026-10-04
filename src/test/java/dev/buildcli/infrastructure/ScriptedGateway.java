package dev.buildcli.infrastructure;

import dev.buildcli.domain.Agent;
import dev.buildcli.ports.LlmGateway;
import dev.buildcli.ports.LlmMessage;
import dev.buildcli.ports.LlmReply;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Deterministic fake LLM: each agent replays its own queue of replies. Used by tests and the TUI demo. */
public final class ScriptedGateway implements LlmGateway {
    private final Map<String, Deque<Object>> script = new HashMap<>();

    /** Queue a reply for an agent: an LlmReply, or a RuntimeException to throw. */
    public ScriptedGateway then(String agent, Object replyOrError) {
        script.computeIfAbsent(agent, k -> new ArrayDeque<>()).add(replyOrError);
        return this;
    }

    public ScriptedGateway call(String agent, String tool, Map<String, Object> args) {
        return then(agent, new LlmReply("", List.of(new ToolCall("c" + System.nanoTime(), tool, args)), 100, 20));
    }

    public ScriptedGateway say(String agent, String text) {
        return then(agent, new LlmReply(text, List.of(), 100, 20));
    }

    @Override
    public LlmReply chat(Agent agent, List<LlmMessage> messages, List<ToolSpec> tools) {
        Deque<Object> q = script.get(agent.name());
        Object next = q == null ? null : q.poll();
        if (next == null) {
            throw new IllegalStateException("script exhausted for agent " + agent.name());
        }
        if (next instanceof RuntimeException e) {
            throw e;
        }
        return (LlmReply) next;
    }
}
