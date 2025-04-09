package dev.buildcli.core.utils.net;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDownloaderTest {

  private MockedStatic<HttpClient> httpClientStatic;
  private final String url = "http://localhost/teste.txt";
  private final String filename = "test.txt";
  private final byte[] fakeContent = "test file".getBytes();
  private final ByteArrayInputStream inputStream = new ByteArrayInputStream(fakeContent);

  @AfterEach
  void cleanup() throws IOException {
    if (httpClientStatic != null) {
      httpClientStatic.close();
    }
    inputStream.close();
    File file = new File(filename);
    if (file.exists()) {
      file.delete();
    }
  }

  private void setupMockHttpClient(HttpResponse<InputStream> response) throws IOException, InterruptedException {
    HttpClient mockClient = mock(HttpClient.class);
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    httpClientStatic = mockStatic(HttpClient.class);
    httpClientStatic.when(HttpClient::newHttpClient).thenReturn(mockClient);
  }

  private HttpResponse<InputStream> setupMockHttpResponse(String contentLength, String contentDisposition) {
    HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(inputStream);

    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.firstValue("Content-Length")).thenReturn(Optional.ofNullable(contentLength));
    when(headers.firstValue("Content-disposition")).thenReturn(Optional.ofNullable(contentDisposition));
    when(mockResponse.headers()).thenReturn(headers);

    return mockResponse;
  }

  @Test
  void shouldDownloadFileSuccessfully_whenHttpRequestIsValid() throws IOException, InterruptedException {
    HttpResponse<InputStream> mockResponse = setupMockHttpResponse(
        String.valueOf(fakeContent.length),
        "attachment; filename=\"" + filename + "\""
    );
    setupMockHttpClient(mockResponse);

    var standardOut = System.out;
    try {
      var outputStream = new java.io.ByteArrayOutputStream();
      System.setOut(new PrintStream(outputStream));
      File file = FileDownloader.download(url);

      String line1 = "dev.buildcli.core.utils.net.FileDownloader -- Connecting to http://localhost/teste.txt";
      String line2 = "dev.buildcli.core.utils.net.FileDownloader -- Connected to http://localhost/teste.txt";
      String line3 = "[==================================================] 100%";

      assertTrue(outputStream.toString().contains(line1));
      assertTrue(outputStream.toString().contains(line2));
      assertTrue(outputStream.toString().contains(line3));
      assertEquals(filename, file.getName());
      assertEquals(fakeContent.length, file.length());
    } finally {
      System.setOut(standardOut);
    }
  }

  @Test
  void shouldThrowException_whenStatusCodeIsNot200() throws IOException, InterruptedException {
    HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(404);
    setupMockHttpClient(mockResponse);

    Exception ex = assertThrows(RuntimeException.class, () -> FileDownloader.download(url));
    assertTrue(ex.getMessage().contains("Failed to download file: 404"));
  }

  @MockitoSettings(strictness = Strictness.LENIENT)
  @Test
  void shouldThrowException_whenContentLengthIsInvalid() throws IOException, InterruptedException {
    HttpResponse<InputStream> mockResponse = setupMockHttpResponse("0", "attachment; filename=\"" + filename + "\"");
    setupMockHttpClient(mockResponse);

    Exception ex = assertThrows(RuntimeException.class, () -> FileDownloader.download(url));
    assertTrue(ex.getMessage().contains("Failed to download maven artifact: 200"));
  }

  @MockitoSettings(strictness = Strictness.LENIENT)
  @Test
  void shouldThrowException_whenFilenameInContentDispositionIsEmpty() throws IOException, InterruptedException {
    HttpResponse<InputStream> mockResponse = setupMockHttpResponse(
        String.valueOf(fakeContent.length),
        "attachment; filename=\"\""
    );
    setupMockHttpClient(mockResponse);

    Exception ex = assertThrows(RuntimeException.class, () -> FileDownloader.download(url));
    assertTrue(ex.toString().contains("Failed to download file: 200"));
  }

  @MockitoSettings(strictness = Strictness.LENIENT)
  @Test
  void shouldThrowException_whenContentDispositionHeaderIsMissing() throws IOException, InterruptedException {
    HttpResponse<InputStream> mockResponse = setupMockHttpResponse(
        String.valueOf(fakeContent.length),
        null
    );
    setupMockHttpClient(mockResponse);

    Exception ex = assertThrows(RuntimeException.class, () -> FileDownloader.download(url));
    assertTrue(ex.toString().contains("Failed to download file: 200"));
  }

  @Test
  void shouldThrowException_whenHttpRequestTimesOut() throws IOException, InterruptedException {
    HttpClient mockClient = mock(HttpClient.class);
    when(mockClient.send(any(), any())).thenThrow(new InterruptedException("Timeout"));
    httpClientStatic = mockStatic(HttpClient.class);
    httpClientStatic.when(HttpClient::newHttpClient).thenReturn(mockClient);

    assertThrows(RuntimeException.class, () -> FileDownloader.download(url));
  }

}