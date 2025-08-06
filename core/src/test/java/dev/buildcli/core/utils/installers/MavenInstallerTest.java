package dev.buildcli.core.utils.installers;

import dev.buildcli.core.log.SystemOutLogger;
import dev.buildcli.core.utils.DirectoryCleanup;
import dev.buildcli.core.utils.EnvUtils;
import dev.buildcli.core.utils.OS;
import dev.buildcli.core.utils.compress.FileExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MavenInstallerTest {

  @TempDir
  Path tempDir;
  Path tempZipFile;
  private MockedStatic<HttpClient> httpClientStatic;
  private HttpClient mockHttpClient;

  @BeforeEach
  void setUpHttpClientMock() throws Exception {
    mockHttpClient = mock(HttpClient.class);
    httpClientStatic = mockStatic(HttpClient.class);
    httpClientStatic.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

    tempZipFile = tempDir.resolve("maven-installer.zip");
    createEmptyZipFile(tempZipFile);
  }

  @AfterEach
  void tearDown() throws IOException {
    if (httpClientStatic != null) {
      httpClientStatic.close();
    }
    if (tempDir != null) {
      Files.walk(tempDir)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

  @Test
  void installMaven_shouldCompleteSuccessfully_whenAllStepsSucceed() {
    File mockDownloadedFile = mock(File.class);
    File mockInstallDir = mock(File.class);

    when(mockDownloadedFile.getAbsolutePath()).thenReturn("/mock/download/maven.tar.gz");
    when(mockDownloadedFile.exists()).thenReturn(true);

    when(mockInstallDir.getAbsolutePath()).thenReturn("/mock/install/maven");

    try (
        MockedStatic<MavenInstaller> mavenMock = mockStatic(MavenInstaller.class, CALLS_REAL_METHODS);
        MockedStatic<DirectoryCleanup> cleanupMock = mockStatic(DirectoryCleanup.class);
        MockedStatic<FileExtractor> extractorMock = mockStatic(FileExtractor.class)
    ) {
      mavenMock.when(MavenInstaller::downloadMaven).thenReturn(mockDownloadedFile);
      mavenMock.when(MavenInstaller::installProgramFilesDirectory).thenReturn(mockInstallDir);
      mavenMock.when(() -> MavenInstaller.extractMaven(anyString(), anyString())).thenAnswer(inv -> null);
      mavenMock.when(() -> MavenInstaller.configurePath(anyString())).thenAnswer(inv -> null);

      cleanupMock.when(() -> DirectoryCleanup.cleanup(anyString())).thenAnswer(inv -> null);
      extractorMock.when(() -> FileExtractor.extractFile(anyString(), anyString())).thenAnswer(inv -> null);

      MavenInstaller.installMaven();

      mavenMock.verify(MavenInstaller::downloadMaven);
      mavenMock.verify(MavenInstaller::installProgramFilesDirectory);
      mavenMock.verify(() -> MavenInstaller.extractMaven("/mock/download/maven.tar.gz", "/mock/install/maven"));
      mavenMock.verify(() -> MavenInstaller.configurePath("/mock/install/maven/apache-maven-3.9.9"));

      cleanupMock.verify(() -> DirectoryCleanup.cleanup("/mock/download/maven.tar.gz"));
    }
  }

  @Test
  void installMaven_shouldThrowRuntimeException_whenDownloadFails() {
    try (MockedStatic<MavenInstaller> mockedInstaller = mockStatic(MavenInstaller.class)) {
      mockedInstaller.when(MavenInstaller::downloadMaven)
          .thenThrow(new IOException("Simulated download failure"));

      mockedInstaller.when(MavenInstaller::installMaven).thenCallRealMethod();

      RuntimeException ex = assertThrows(RuntimeException.class, MavenInstaller::installMaven);
      assertInstanceOf(IOException.class, ex.getCause());
      assertEquals("Simulated download failure", ex.getCause().getMessage());
    }
  }

  @Test
  void installMaven_shouldIOException_whenContentLengthIs0() throws Exception {
    try (MockedStatic<OS> osMock = mockStatic(OS.class)) {

      osMock.when(OS::isWindows).thenReturn(false);

      httpClientStatic.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

      InputStream inputStream = new ByteArrayInputStream("fake content".getBytes());

      HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
      when(mockResponse.statusCode()).thenReturn(200);
      when(mockResponse.body()).thenReturn(inputStream);
      when(mockResponse.body()).thenReturn(inputStream);
      when(mockResponse.headers()).thenReturn(HttpHeaders.of(
          Map.of("Content-Length", List.of("0")), (k, v) -> true
      ));

      when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
          .thenReturn(mockResponse);
      String expectedMessage = "Failed to download maven artifact: 200";
      IOException ex = assertThrows(IOException.class, MavenInstaller::downloadMaven);
      assertEquals(ex.getMessage(), expectedMessage);

    }
  }

  @Test
  void downloadMaven_shouldReturnFile_whenWindows() throws IOException, InterruptedException {
    try (MockedStatic<OS> osMock = mockStatic(OS.class)) {

      osMock.when(OS::isWindows).thenReturn(true);

      httpClientStatic.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

      InputStream inputStream = new ByteArrayInputStream("fake content".getBytes());

      HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
      when(mockResponse.statusCode()).thenReturn(200);
      when(mockResponse.body()).thenReturn(inputStream);
      when(mockResponse.headers()).thenReturn(HttpHeaders.of(
          Map.of("Content-Length", List.of("1234")), (k, v) -> true
      ));

      when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
          .thenReturn(mockResponse);

      File result = null;
      try {
        result = MavenInstaller.downloadMaven();

        assertNotNull(result);
        assertEquals("apache-maven-3.9.9.zip", result.getName());
      } finally {
        if (result != null && result.exists()) {
          result.delete();
        }
      }
    }
  }

  @Test
  void downloadMaven_shouldDownloadUsingHttpClient_whenHttpCallSucceeds() throws Exception {
    try (MockedStatic<OS> osMock = mockStatic(OS.class)) {
      osMock.when(OS::isWindows).thenReturn(false);

      httpClientStatic.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

      HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
      when(mockResponse.statusCode()).thenReturn(404);

      when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
          .thenReturn(mockResponse);

      String expectedMessage = "Failed to download maven artifact: 404";
      IOException ex = assertThrows(IOException.class, MavenInstaller::downloadMaven);
      assertEquals(ex.getMessage(), expectedMessage);
    }
  }

  @Test
  void downloadMaven_shouldDownloadSuccessfully_onLinux() throws Exception {
    try (MockedStatic<OS> osMock = mockStatic(OS.class)) {

      osMock.when(OS::isWindows).thenReturn(false);


      httpClientStatic.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

      InputStream inputStream = new ByteArrayInputStream("fake content".getBytes());

      HttpResponse<InputStream> mockResponse = mock(HttpResponse.class);
      when(mockResponse.statusCode()).thenReturn(200);
      when(mockResponse.body()).thenReturn(inputStream);
      when(mockResponse.headers()).thenReturn(HttpHeaders.of(
          Map.of("Content-Length", List.of("1234")), (k, v) -> true
      ));

      when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
          .thenReturn(mockResponse);

      File result = MavenInstaller.downloadMaven();

      assertNotNull(result);
      assertEquals("apache-maven-3.9.9.tar.gz", result.getName());
      if (result != null && result.exists()) {
        result.delete();
      }
    }
  }

  @Test
  void installProgramFilesDirectory_shouldReturnWindowsPath_whenEnvIsSet() {
    try (MockedStatic<OS> osMock = mockStatic(OS.class);
         MockedStatic<EnvUtils> envUtilsMock = mockStatic(EnvUtils.class);
         MockedStatic<MavenInstaller> mockedStatic = mockStatic(MavenInstaller.class, CALLS_REAL_METHODS)) {

      osMock.when(OS::isWindows).thenReturn(true);
      envUtilsMock.when(() -> EnvUtils.getEnv("ProgramFiles")).thenReturn("D:\\Program Files");
      File result = MavenInstaller.installProgramFilesDirectory();

      assertTrue(result.toString().contains("D:\\Program Files"));
    }
  }

  @Test
  void installProgramFilesDirectory_shouldFallbackToDefault_whenEnvIsNull() {
    try (MockedStatic<OS> osMock = mockStatic(OS.class);
         MockedStatic<EnvUtils> envUtilsMock = mockStatic(EnvUtils.class)) {

      osMock.when(OS::isWindows).thenReturn(true);
      envUtilsMock.when(() -> EnvUtils.getEnv("ProgramFiles")).thenReturn(null);

      File result = MavenInstaller.installProgramFilesDirectory();

      assertTrue(result.toPath().toString().contains("C:\\Program Files"));
    }
  }

  @Test
  void installProgramFilesDirectory_shouldReturnLinuxPath() {
    try (MockedStatic<OS> osMock = mockStatic(OS.class)) {
      osMock.when(OS::isWindows).thenReturn(false);

      File result = MavenInstaller.installProgramFilesDirectory();

      assertEquals("/usr/local/maven", result.toPath().toString());
    }
  }

  @Test
  void configurePath_shouldExecSet_onWindows() throws Exception {
    try (MockedStatic<OS> osMock = mockStatic(OS.class);
         MockedStatic<SystemOutLogger> loggerMock = mockStatic(SystemOutLogger.class);
         MockedStatic<Runtime> runtimeMocked = mockStatic(Runtime.class)) {

      osMock.when(OS::isWindows).thenReturn(true);

      Runtime mockRuntime = mock(Runtime.class);
      runtimeMocked.when(Runtime::getRuntime).thenReturn(mockRuntime);

      String fakePath = "C:\\fake\\maven";

      MavenInstaller.configurePath(fakePath);

      String expectedCommand = "setx PATH \"%PATH%;" + fakePath + "\\\\bin\"";

      verify(mockRuntime).exec(new String[]{expectedCommand});


      loggerMock.verifyNoInteractions();
    }
  }

  @Test
  void configurePath_shouldAppendToBashrc_andLog_onLinux() throws Exception {
    try (MockedStatic<OS> osMock = mockStatic(OS.class);
         MockedStatic<SystemOutLogger> loggerMock = mockStatic(SystemOutLogger.class)) {

      osMock.when(OS::isWindows).thenReturn(false);

      Path fakeHome = tempDir.resolve("fakeHome");
      Files.createDirectories(fakeHome);
      System.setProperty("user.home", fakeHome.toString());

      Path bashrc = fakeHome.resolve(".bashrc");
      Files.createFile(bashrc);

      String path = "/fake/maven/path";

      MavenInstaller.configurePath(path);

      String content = Files.readString(bashrc);
      assertTrue(content.contains("export PATH=$PATH:" + path + "/bin"));

      loggerMock.verify(() -> SystemOutLogger.log("Please run: source ~/.bashrc"));
      loggerMock.verify(() -> SystemOutLogger.log("Please run: sudo chmod +x " + path + "/bin/mvn\n"));
    }
  }

  @Test
  void extractMaven_shouldExtractDummyFile() throws IOException, InterruptedException {
    MavenInstaller.extractMaven(tempZipFile.toString(), tempDir.toString());

    Path extractedFile = tempDir.resolve("dummy.txt");

    assertTrue(Files.exists(extractedFile));

    String content = Files.readString(extractedFile);
    assertEquals("dummy", content);
  }

  private void createEmptyZipFile(Path zipFilePath) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
      ZipEntry entry = new ZipEntry("dummy.txt");
      zos.putNextEntry(entry);
      zos.write("dummy".getBytes());
      zos.closeEntry();
    }
  }
}