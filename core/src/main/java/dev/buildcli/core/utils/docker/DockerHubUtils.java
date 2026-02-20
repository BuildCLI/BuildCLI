package dev.buildcli.core.utils.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.buildcli.core.exceptions.DockerHubException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

public abstract class DockerHubUtils {
  private static final String API_URL = "https://hub.docker.com/v2";
  private static final String DEFAULT_TAG = "latest";
  private static final String TAG_SEPARATOR = ":";
  private static final String PATH_SEPARATOR = "/";

  private static final HttpClient httpClient = HttpClient.newHttpClient();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private DockerHubUtils() {
  }

  public static List<String> getImageTags(String namespace, String repository) {
    validateParameters(namespace, repository);

    try {
      String path = String.format("/repositories/%s/%s/tags", namespace, repository);
      URI uri = buildUri(path, null);

      HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();

      var response = httpClient.send(request, ofString());
      JsonNode root = objectMapper.readTree(response.body());

      List<String> tags = new ArrayList<>();
      for (JsonNode result : root.get("results")) {
        tags.add(result.get("name").asText());
      }

      return Collections.unmodifiableList(tags);
    } catch (IOException | InterruptedException e) {
      throw new DockerHubException("Failed to fetch image tags", e);
    }
  }

  public static List<DockerImage> searchImagesWithTags(String query) {
    if (query == null || query.isBlank()) {
      throw new IllegalArgumentException("Search query cannot be null or empty");
    }

    try {
      URI uri = buildUri("/search/repositories", query);

      HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();

      HttpResponse<String> response = httpClient.send(request, ofString());
      JsonNode root = objectMapper.readTree(response.body());

      List<DockerImage> images = new ArrayList<>();
      for (JsonNode result : root.get("results")) {
        String name = result.get("repo_name").asText();
        try {
          String[] parts = name.split(PATH_SEPARATOR);
          getImageTags(parts[0], parts[1])
              .stream()
              .map(tag -> new DockerImage(name, tag))
              .forEach(images::add);
        } catch (Exception e) {
          System.out.println(e.getMessage());
          images.add(new DockerImage(name, DEFAULT_TAG));
        }
      }

      return Collections.unmodifiableList(images);
    } catch (Exception e) {
      throw new DockerHubException("Failed to search images", e);
    }
  }

  private static void validateParameters(String namespace, String repository) {
    if (namespace == null || namespace.isBlank()) {
      throw new IllegalArgumentException("Namespace cannot be null or empty");
    }
    if (repository == null || repository.isBlank()) {
      throw new IllegalArgumentException("Repository cannot be null or empty");
    }
  }

  private static URI buildUri(String path, String query) {
    StringBuilder builder = new StringBuilder(API_URL + path);
    if (query != null) {
      builder.append("?query=").append(query);
    }
    return URI.create(builder.toString());
  }

  public record DockerImage(String name, String tag) {
    public DockerImage {
      Objects.requireNonNull(name, "Name cannot be null");
      Objects.requireNonNull(tag, "Tag cannot be null");
    }

    @Override
    public String toString() {
      return name + TAG_SEPARATOR + tag;
    }
  }
}