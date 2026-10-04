package dev.buildcli.infrastructure;

import dev.buildcli.domain.ModelRef;
import dev.buildcli.ports.LlmGateway;
import java.nio.file.Path;
import java.util.Map;

/**
 * Machine configuration for talking to models: which providers exist (built in, plus the user's {@code providers.yaml}),
 * the environment the API keys are read from (never stored), and generation settings. Model choice ("openrouter /
 * openrouter/free") is configuration of the agents.
 */
public record ProviderSettings(ProviderRegistry registry, Map<String, String> env, int ollamaThreads, double temperature,
                               boolean streaming) {

    public static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
    private static final int LOCAL_MAX_OUTPUT = 512;
    private static final int CLOUD_MAX_OUTPUT = 4096;

    /** Built-in providers only, with OLLAMA_HOST and OPENAI_BASE_URL applied. */
    public static ProviderSettings fromEnvironment(Map<String, String> env) {
        return of(ProviderRegistry.builtIn(), env);
    }

    /** Built-in providers plus {@code <globalDir>/providers.yaml}. */
    public static ProviderSettings fromEnvironment(Map<String, String> env, Path globalDir) {
        return of(ProviderRegistry.load(globalDir), env);
    }

    private static ProviderSettings of(ProviderRegistry registry, Map<String, String> env) {
        String ollama = env.get("OLLAMA_HOST");
        if (ollama != null && !ollama.isBlank()) {
            registry = registry.withBaseUrl("ollama", ollama.startsWith("http") ? ollama : "http://" + ollama);
        }
        String openai = env.get("OPENAI_BASE_URL");
        if (openai != null && !openai.isBlank()) {
            registry = registry.withBaseUrl("openai", openai);
        }
        return new ProviderSettings(registry, env, 4, 0.0, true);
    }

    public ProviderSettings with(Integer threads, Double temperature, boolean streaming) {
        return new ProviderSettings(registry, env, threads == null ? ollamaThreads : threads,
                temperature == null ? this.temperature : temperature, streaming);
    }

    public ProviderSettings withBaseUrl(String provider, String url) {
        return new ProviderSettings(registry.withBaseUrl(provider, url), env, ollamaThreads, temperature, streaming);
    }

    public String ollamaUrl() {
        return registry.find("ollama").map(ProviderSpec::baseUrl).orElse(DEFAULT_OLLAMA_URL);
    }

    /** The API key for a provider, or null when it needs none. Throws with a fix when it needs one and it is not set. */
    public String keyFor(ProviderSpec spec) {
        if (!spec.needsKey()) {
            return null;
        }
        String key = env.get(spec.apiKeyEnv());
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("no API key for '" + spec.name() + "': set " + spec.apiKeyEnv() + " in the environment");
        }
        return key;
    }

    /** Builds the gateway for one provider/model pair. */
    public LlmGateway gatewayFor(ModelRef ref) {
        ProviderSpec spec = registry.find(ref.provider()).orElseThrow(() -> new IllegalArgumentException(
                "unknown provider '" + ref.provider() + "'. Known: " + registry.all().stream().map(ProviderSpec::name).toList()
                        + ". Add your own with: buildcli provider add"));
        return switch (spec.kind()) {
            case OLLAMA -> LangChain4jGateway.ollama(spec.baseUrl(), ref.model(), ollamaThreads, temperature, streaming);
            case OPENAI_COMPATIBLE -> {
                boolean local = !spec.needsKey();
                yield LangChain4jGateway.openAiCompatible(spec.baseUrl(), local ? "not-needed" : keyFor(spec), ref.model(), temperature,
                        streaming, local ? LOCAL_MAX_OUTPUT : CLOUD_MAX_OUTPUT);
            }
        };
    }
}
