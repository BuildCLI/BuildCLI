package dev.buildcli.core.actions.dependency;

import com.google.gson.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import static dev.buildcli.core.utils.input.InteractiveInputUtils.options;
import static dev.buildcli.core.constants.DependencyConstants.DEPENDENCIES;

public class DependencySearchService {

    private static final String API_MAVEN = "https://search.maven.org/solrsearch/select?q=";
    private static final String ROWS = "&rows=25";
    private static final String OUTPUT ="&wt=json";
    private static final String AND ="+AND+";

    public HttpRequest createSearchGetRequest(String[] dependency){
        return HttpRequest.newBuilder()
            .uri(URI.create(getUri(dependency)))
                .GET()
                .build();
    }

    private String getUri(String[] dependency) {
        StringBuilder uri = new StringBuilder(API_MAVEN);
        uri.append("g:").append(dependency[0]);
        uri.append(AND);
        uri.append("a:").append(dependency[1]);
        if (dependency.length == 3) {
            uri.append(AND);
            uri.append("v:").append(dependency[2]);
        }
        uri.append(ROWS).append(OUTPUT);
        System.out.println("URI: " + uri);
        return uri.toString();
    }

    private String getResponseBody(HttpRequest request){
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            client.close();
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> promptOptionsToAdd(List<String> dependencies){
        return List.of(options("Which dependency do you wanna add?",dependencies));
    }

    public boolean isAlreadyConfigured(String dependency){
        return DEPENDENCIES.containsKey(dependency);
    }

    public List<String> searchDependency(String dependencyName){
        if(isAlreadyConfigured(dependencyName)){
            return DEPENDENCIES.get(dependencyName);
        }
        return promptOptionsToAdd(sendSearchRequest(dependencyName));
    }

    public List<String> sendSearchRequest(String dependencyName) {
        String[] dependency = dependencyName.split(":");
        HttpRequest request = createSearchGetRequest(dependency);
        String response = getResponseBody(request);

       return getDependencyList(response);
    }

    private List<String> getDependencyList(String response){
        JsonArray jsonArray  = new Gson().fromJson(response, JsonObject.class)
                .getAsJsonObject("response")
                .getAsJsonArray("docs");

        return StreamSupport.stream(jsonArray.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(this::getDependencyFromJson)
                .toList();
    }

    private String getDependencyFromJson(JsonObject jsonObject){
        String version = null == jsonObject.get("latestVersion")
            ? jsonObject.get("v").getAsString()
            : jsonObject.get("latestVersion").getAsString();

        return String.join("",jsonObject.get("g").getAsString(),
            ":",jsonObject.get("a").getAsString(),
            ":",version);
    }
}