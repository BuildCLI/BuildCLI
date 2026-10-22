package dev.buildcli.infrastructure;

/**
 * One place models can come from. {@code kind} says which wire protocol it speaks; {@code apiKeyEnv} names the
 * environment variable holding its key (keys are never stored), or is null when the endpoint needs none.
 */
public record ProviderSpec(String name, Kind kind, String baseUrl, String apiKeyEnv, String description) {

    public enum Kind { OLLAMA, OPENAI_COMPATIBLE }

    public boolean needsKey() {
        return apiKeyEnv != null;
    }
}
