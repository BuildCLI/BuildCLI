package dev.buildcli.infrastructure;

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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps a real gateway and appends every reply to a JSON Lines file, so a real model session can later be replayed
 * deterministically with {@link ReplayGateway}. Errors from the delegate are recorded too.
 */
public final class RecordingGateway implements LlmGateway {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final LlmGateway delegate;
    private final Path file;

    public RecordingGateway(LlmGateway delegate, Path file) {
        this.delegate = delegate;
        this.file = file;
    }

    @Override
    public LlmReply chat(Agent agent, List<LlmMessage> messages, List<ToolSpec> tools) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("agent", agent.name());
        try {
            LlmReply reply = delegate.chat(agent, messages, tools);
            line.put("text", reply.text());
            List<Map<String, Object>> calls = new ArrayList<>();
            for (ToolCall c : reply.toolCalls()) {
                calls.add(Map.of("id", c.id(), "name", c.name(), "args", c.args()));
            }
            line.put("calls", calls);
            line.put("in", reply.inputTokens());
            line.put("out", reply.outputTokens());
            append(line);
            return reply;
        } catch (RuntimeException e) {
            line.put("error", String.valueOf(e.getMessage()));
            append(line);
            throw e;
        }
    }

    private void append(Map<String, Object> line) {
        try {
            Files.writeString(file, JSON.writeValueAsString(line) + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("cannot write the recording " + file + ": " + e.getMessage(), e);
        }
    }
}
