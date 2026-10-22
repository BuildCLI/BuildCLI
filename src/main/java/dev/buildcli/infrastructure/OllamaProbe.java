package dev.buildcli.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Asks a local Ollama which models it has, for {@code buildcli doctor}. */
public final class OllamaProbe {
    private static final ObjectMapper JSON = new ObjectMapper();

    private OllamaProbe() {}

    /** @return the model names, or empty if Ollama could not be reached or answered something unexpected */
    public static Optional<List<String>> listModels(String baseUrl, Duration timeout) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$", "") + "/api/tags")).timeout(timeout).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonNode models = JSON.readTree(response.body()).path("models");
            List<String> names = new ArrayList<>();
            models.forEach(m -> names.add(m.path("name").asText()));
            return Optional.of(names);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
