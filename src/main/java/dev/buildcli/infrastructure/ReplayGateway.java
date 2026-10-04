package dev.buildcli.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.buildcli.domain.Agent;
import dev.buildcli.ports.LlmGateway;
import dev.buildcli.ports.LlmMessage;
import dev.buildcli.ports.LlmReply;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replays a session recorded by {@link RecordingGateway}: each agent gets its own replies back in order, with no
 * model involved. Makes real-model behaviour usable as a deterministic regression test.
 */
public final class ReplayGateway implements LlmGateway {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Map<String, Deque<JsonNode>> byAgent = new HashMap<>();

    public ReplayGateway(Path file) {
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    JsonNode n = JSON.readTree(line);
                    byAgent.computeIfAbsent(n.path("agent").asText(), k -> new ArrayDeque<>()).add(n);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read the recording " + file + ": " + e.getMessage(), e);
        }
    }

    @Override
    public LlmReply chat(Agent agent, List<LlmMessage> messages, List<ToolSpec> tools) {
        Deque<JsonNode> q = byAgent.get(agent.name());
        JsonNode n = q == null ? null : q.poll();
        if (n == null) {
            throw new IllegalStateException("the recording has no more replies for agent " + agent.name());
        }
        if (n.has("error")) {
            throw new IllegalStateException(n.get("error").asText());
        }
        List<ToolCall> calls = new ArrayList<>();
        for (JsonNode c : n.path("calls")) {
            Map<String, Object> args = JSON.convertValue(c.path("args"), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            calls.add(new ToolCall(c.path("id").asText(), c.path("name").asText(), args));
        }
        return new LlmReply(n.path("text").isNull() ? null : n.path("text").asText(), calls, n.path("in").asInt(), n.path("out").asInt());
    }
}
