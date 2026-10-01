package dev.buildcli.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which models a provider offers, for the model picker. OpenAI-compatible providers answer {@code GET /models}
 * (OpenRouter adds whether a model is free, takes tools and how long its context is); Ollama answers {@code /api/tags}.
 * Results are cached for the session; failures give an empty list and a reason, never an exception.
 */
public final class ModelCatalog {

    /** A model the picker can offer: {@code ref} is "provider:model". */
    public record Model(String ref, String note, boolean tools, boolean free) {}

    public record Result(List<Model> models, String problem) {}

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private final Map<String, CompletableFuture<Result>> cache = new ConcurrentHashMap<>();
    private final ProviderSettings settings;

    public ModelCatalog(ProviderSettings settings) {
        this.settings = settings;
    }

    public CompletableFuture<Result> models(String provider) {
        return cache.computeIfAbsent(provider, p -> CompletableFuture.supplyAsync(() -> fetch(p)));
    }

    private Result fetch(String provider) {
        ProviderSpec spec = settings.registry().find(provider).orElse(null);
        if (spec == null) {
            return new Result(List.of(), "unknown provider");
        }
        try {
            if (spec.kind() == ProviderSpec.Kind.OLLAMA) {
                var names = OllamaProbe.listModels(spec.baseUrl(), Duration.ofSeconds(3));
                if (names.isEmpty()) {
                    return new Result(List.of(), "Ollama is not reachable at " + spec.baseUrl());
                }
                return new Result(names.get().stream().map(n -> new Model(provider + ":" + n, "local", true, true)).toList(), null);
            }
            String key = spec.needsKey() ? settings.env().get(spec.apiKeyEnv()) : null;
            if (spec.needsKey() && (key == null || key.isBlank())) {
                return new Result(List.of(), spec.apiKeyEnv() + " is not set");
            }
            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(spec.baseUrl().replaceAll("/+$", "") + "/models")).timeout(TIMEOUT).GET();
            if (key != null) {
                req.header("Authorization", "Bearer " + key);
            }
            HttpResponse<String> res = HttpClient.newBuilder().connectTimeout(TIMEOUT).build().send(req.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                return new Result(List.of(), "HTTP " + res.statusCode() + ": " + ProviderErrors.message(new RuntimeException(res.body())));
            }
            List<Model> out = new ArrayList<>();
            for (JsonNode m : JSON.readTree(res.body()).path("data")) {
                String id = m.path("id").asText();
                if (id.isBlank()) {
                    continue;
                }
                boolean tools = true;
                StringBuilder note = new StringBuilder();
                if (m.has("supported_parameters")) {
                    tools = false;
                    for (JsonNode p : m.get("supported_parameters")) {
                        tools |= p.asText().equals("tools");
                    }
                }
                boolean free = id.endsWith(":free") || id.equals("openrouter/free")
                        || "0".equals(m.path("pricing").path("prompt").asText()) && "0".equals(m.path("pricing").path("completion").asText());
                if (free) {
                    note.append("free");
                }
                if (m.has("context_length")) {
                    note.append(note.isEmpty() ? "" : " · ").append(m.get("context_length").asLong() / 1000).append("k context");
                }
                if (!tools) {
                    note.append(note.isEmpty() ? "" : " · ").append("no tools");
                }
                out.add(new Model(provider + ":" + id, note.toString(), tools, free));
            }
            // models that can use tools first (agents need them), free ones first among those, then by name
            out.sort(Comparator.comparing((Model x) -> !x.tools()).thenComparing(x -> !x.free()).thenComparing(x -> x.ref().toLowerCase(Locale.ROOT)));
            return new Result(out, null);
        } catch (Exception e) {
            return new Result(List.of(), ProviderErrors.message(e));
        }
    }
}
