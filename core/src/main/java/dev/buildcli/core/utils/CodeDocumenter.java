package dev.buildcli.core.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for documenting Java code using Ollama AI.
 */
public class CodeDocumenter {

    private static final Logger logger = Logger.getLogger(CodeDocumenter.class.getName());
    private static final String OLLAMA_URL = "http://localhost:11434/v1/chat/completions";

    private CodeDocumenter() {
    }

    /**
     * Processes the documentation for a file or all Java files in a directory.
     *
     * @param path Path to the file or directory.
     */
    public static void getDocumentationFromOllama(String path) {
        Path inputPath = Path.of(path);
        if (Files.isDirectory(inputPath)) {
            documentDirectory(inputPath);
        } else if (Files.isRegularFile(inputPath) && path.endsWith(".java")) {
            try {
                documentFile(inputPath);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error documenting file: " + path, e);
            }
        } else {
            logger.warning("Invalid path provided: " + path);
        }
    }

    /**
     * Processes all Java files in the specified directory.
     *
     * @param directoryPath Path to the directory.
     */
    private static void documentDirectory(Path directoryPath) {
        try {
            Files.walk(directoryPath)
                    .filter(Files::isRegularFile) // Process only files
                    .filter(path -> path.toString().endsWith(".java")) // Only Java files
                    .forEach(file -> {
                        try {
                            documentFile(file);
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Error documenting file: " + file, e);
                        }
                    });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error traversing directory: " + directoryPath, e);
        }
    }

    /**
     * Sends a single Java file to Ollama for documentation.
     *
     * @param filePath Path to the Java file to be documented.
     * @throws IOException if an I/O error occurs.
     */
    private static void documentFile(Path filePath) throws IOException {
        if (!Files.isRegularFile(filePath)) {
            logger.warning("Skipping non-regular file: " + filePath);
            return;
        }

        try {
            logger.info("Processing file: " + filePath);
            String fileContent = Files.readString(filePath);

            // Configure JSON payload for the request
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty(JsonProperty.MODEL.val(), "llama3.2");
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty(JsonProperty.ROLE.val(), "system");
            systemMessage.addProperty(JsonProperty.CONTENT.val(),
                    """
                                You are a helpful assistant. Document the following Java code following these guidelines:
                                1. Add concise and meaningful Javadoc comments.
                                2. Avoid adding redundant explanations or suggestions within the code.
                                3. Focus on good practices for getters and setters, such as returning the attribute directly without additional logic or comments.
                                4. Do not include 'TODO' or hypothetical improvement suggestions.
                                5. Preserve the original structure of the code.
                          """);
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty(JsonProperty.ROLE.val(), "user");
            userMessage.addProperty(JsonProperty.CONTENT.val(), "Document this code:\n\n" + fileContent);
            requestBody.add(JsonProperty.MESSAGES.val(), JsonParser.parseString("[" + systemMessage + "," + userMessage + "]"));

            // Send HTTP request to Ollama
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            logger.info("Sending request to Ollama for file: " + filePath);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                logger.info("Received response from Ollama for file: " + filePath);

                JsonObject responseObject = JsonParser.parseString(response.body()).getAsJsonObject();
                String content = responseObject.getAsJsonArray(JsonProperty.CHOICES.val())
                        .get(0).getAsJsonObject()
                        .getAsJsonObject(JsonProperty.MESSAGE.val())
                        .get(JsonProperty.CONTENT.val()).getAsString();

                // Extract code content between ```java tags
                int codeStart = content.indexOf("```java");
                int codeEnd = content.lastIndexOf("```");
                if (codeStart != -1 && codeEnd != -1) {
                    content = content.substring(codeStart + 7, codeEnd).trim();
                }

                Files.writeString(filePath, content);
                logger.log(Level.INFO, "Documentation added to file: {0}", filePath);

            } else {
                logger.log(Level.WARNING, "Failed to retrieve documentation for file: {0}. Status code: {1}",
                        new Object[]{filePath, response.statusCode()});
                logger.log(Level.WARNING, "Response body: {0}", response.body());
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Error occurred while documenting file: " + filePath, e);
            Thread.currentThread().interrupt();
        }
    }
}
