package dev.buildcli.domain;

import java.util.Map;

/** Which model serves which agent: a default plus per-agent overrides. Either may be absent. */
public record ModelRouting(ModelRef defaultModel, Map<String, ModelRef> overrides) {

    public static ModelRouting unspecified() {
        return new ModelRouting(null, Map.of());
    }

    /** The model for an agent, or null if neither an override nor a default is configured. */
    public ModelRef forAgent(String agentName) {
        ModelRef override = overrides.get(agentName);
        return override != null ? override : defaultModel;
    }
}
