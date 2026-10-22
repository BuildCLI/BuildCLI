package dev.buildcli.ports;

import dev.buildcli.domain.Agent;
import java.util.List;
import java.util.function.Consumer;

public interface LlmGateway {
    /** Implementations throw a RuntimeException on provider failure. */
    LlmReply chat(Agent agent, List<LlmMessage> messages, List<ToolSpec> tools);

    /**
     * Like {@link #chat} but reports the text as it is generated. The default has no streaming: it reports the whole
     * text at the end, so every gateway works in a UI that shows live output.
     */
    default LlmReply chatStreaming(Agent agent, List<LlmMessage> messages, List<ToolSpec> tools, Consumer<String> onText) {
        LlmReply reply = chat(agent, messages, tools);
        if (reply.text() != null && !reply.text().isEmpty()) {
            onText.accept(reply.text());
        }
        return reply;
    }
}
