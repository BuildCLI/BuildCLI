package dev.buildcli.infrastructure;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.ModelRef;
import dev.buildcli.domain.ModelRouting;
import dev.buildcli.ports.LlmGateway;
import dev.buildcli.ports.LlmMessage;
import dev.buildcli.ports.LlmReply;
import dev.buildcli.ports.ToolSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Sends each agent to the model chosen for it: a per-agent override, else the default model, else the fallback
 * (for example a model chosen on the command line). Gateways are created lazily and reused per provider/model pair.
 */
public final class RoutingGateway implements LlmGateway {
    private final ModelRouting routing;
    private final ModelRef fallback;
    private final Function<ModelRef, LlmGateway> factory;
    private final Map<ModelRef, LlmGateway> cache = new HashMap<>();

    public RoutingGateway(ModelRouting routing, ModelRef fallback, Function<ModelRef, LlmGateway> factory) {
        this.routing = routing;
        this.fallback = fallback;
        this.factory = factory;
    }

    @Override
    public LlmReply chat(Agent agent, List<LlmMessage> messages, List<ToolSpec> tools) {
        return gatewayFor(agent).chat(agent, messages, tools);
    }

    @Override
    public LlmReply chatStreaming(Agent agent, List<LlmMessage> messages, List<ToolSpec> tools, Consumer<String> onText) {
        return gatewayFor(agent).chatStreaming(agent, messages, tools, onText);
    }

    /** The model that will serve an agent, for display ("ana runs on openai/gpt-x"). */
    public ModelRef modelFor(Agent agent) {
        ModelRef ref = routing.forAgent(agent.name());
        ref = ref != null ? ref : fallback;
        if (ref == null) {
            throw new IllegalStateException("no model configured for agent '" + agent.name()
                    + "': type /connect in the chat, set a model in Settings (F2), or pass --model provider:model");
        }
        return ref;
    }

    private synchronized LlmGateway gatewayFor(Agent agent) {
        return cache.computeIfAbsent(modelFor(agent), factory);
    }
}
