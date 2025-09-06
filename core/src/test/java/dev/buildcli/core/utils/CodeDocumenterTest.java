package dev.buildcli.core.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.contains;


class CodeDocumenterTest {

    @TempDir
    Path tempDir;

    private Path javaFile;
    private Path nonJavaFile;

    @BeforeEach
    void setUp() throws IOException {
        javaFile = tempDir.resolve("TestClass.java");
        Files.writeString(javaFile, "public class TestClass {}");
        nonJavaFile = tempDir.resolve("README.md");
        Files.writeString(nonJavaFile, "# Not Java");
    }

    @Test
    void testGetDocumentationFromOllama_withDirectory_processesJavaFiles() throws Exception {
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class, CALLS_REAL_METHODS);
            MockedStatic<HttpClient> httpClientMock = mockHttpClientWithResponse("```java\n// documented code\n```")) {

            filesMock.when(() -> Files.isDirectory(any())).thenReturn(true);
            filesMock.when(() -> Files.walk(any())).thenReturn(List.of(javaFile, nonJavaFile).stream());
            filesMock.when(() -> Files.isRegularFile(any())).thenReturn(true);
            filesMock.when(() -> Files.readString(any())).thenReturn("public class TestClass {}");
            filesMock.when(() -> Files.writeString(any(), anyString())).thenReturn(javaFile);

            CodeDocumenter.getDocumentationFromOllama(tempDir.toString());

            filesMock.verify(() -> Files.writeString(eq(javaFile), contains("// documented code")), times(1));
        }
    }

    @Test
    void testGetDocumentationFromOllama_withFile_processesFile() throws Exception {
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class, CALLS_REAL_METHODS);
            MockedStatic<HttpClient> httpClientMock = mockHttpClientWithResponse("```java\n// documented code\n```")) {

            filesMock.when(() -> Files.isDirectory(any())).thenReturn(false);
            filesMock.when(() -> Files.isRegularFile(any())).thenReturn(true);
            filesMock.when(() -> Files.readString(any())).thenReturn("public class TestClass {}");
            filesMock.when(() -> Files.writeString(any(), anyString())).thenReturn(javaFile);

            CodeDocumenter.getDocumentationFromOllama(javaFile.toString());

            filesMock.verify(() -> Files.writeString(eq(javaFile), contains("// documented code")), times(1));
        }
    }

    @Test
    void testGetDocumentationFromOllama_withInvalidPath_logsWarning() {
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class, CALLS_REAL_METHODS)) {
            filesMock.when(() -> Files.isDirectory(any())).thenReturn(false);
            filesMock.when(() -> Files.isRegularFile(any())).thenReturn(false);

            // No exception should be thrown
            assertDoesNotThrow(() -> CodeDocumenter.getDocumentationFromOllama("invalid/path"));
        }
    }

    @Test
    void testDocumentFile_skipsNonRegularFile() throws Exception {
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class, CALLS_REAL_METHODS)) {
            filesMock.when(() -> Files.isRegularFile(any())).thenReturn(false);

            // Use reflection to call private method
            var method = CodeDocumenter.class.getDeclaredMethod("documentFile", Path.class);
            method.setAccessible(true);

            assertDoesNotThrow(() -> method.invoke(null, javaFile));
        }
    }

    @Test
    void testDocumentFile_httpError_logsWarning() throws Exception {
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class, CALLS_REAL_METHODS);
             MockedStatic<HttpClient> httpClientMock = Mockito.mockStatic(HttpClient.class)) {

            filesMock.when(() -> Files.isRegularFile(any())).thenReturn(true);
            filesMock.when(() -> Files.readString(any())).thenReturn("public class TestClass {}");

            HttpClient mockClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(500);
            when(mockResponse.body()).thenReturn("error");
            when(mockClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
            httpClientMock.when(HttpClient::newHttpClient).thenReturn(mockClient);

            var method = CodeDocumenter.class.getDeclaredMethod("documentFile", Path.class);
            method.setAccessible(true);

            assertDoesNotThrow(() -> method.invoke(null, javaFile));
        }
    }

    @Test
    void testDocumentFile_successfulResponse_writesExtractedCode() throws Exception {
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class, CALLS_REAL_METHODS);
             MockedStatic<HttpClient> httpClientMock = Mockito.mockStatic(HttpClient.class)) {

            filesMock.when(() -> Files.isRegularFile(any())).thenReturn(true);
            filesMock.when(() -> Files.readString(any())).thenReturn("public class TestClass {}");
            filesMock.when(() -> Files.writeString(any(), anyString())).thenReturn(javaFile);

            HttpClient mockClient = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);

            // Simulate Ollama response JSON
            String ollamaResponse = "{ \"choices\": [ { \"message\": { \"content\": \"```java\\n// documented code\\n```\" } } ] }";
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(ollamaResponse);
            when(mockClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
            httpClientMock.when(HttpClient::newHttpClient).thenReturn(mockClient);

            var method = CodeDocumenter.class.getDeclaredMethod("documentFile", Path.class);
            method.setAccessible(true);

            method.invoke(null, javaFile);

            filesMock.verify(() -> Files.writeString(eq(javaFile), eq("// documented code")), times(1));
        }
    }

    // Helper to mock HTTP client with a given code block in the response
    private MockedStatic<HttpClient> mockHttpClientWithResponse(String codeBlock) throws Exception {
        MockedStatic<HttpClient> httpClientMock = Mockito.mockStatic(HttpClient.class);
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        String ollamaResponse = "{ \"choices\": [ { \"message\": { \"content\": \"" +
                codeBlock.replace("\n", "\\n").replace("\"", "\\\"") + "\" } } ] }";
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(ollamaResponse);
        when(mockClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);
        httpClientMock.when(HttpClient::newHttpClient).thenReturn(mockClient);

        return httpClientMock;
    }
}