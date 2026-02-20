package dev.buildcli.core.utils.installers;

import dev.buildcli.core.log.SystemOutLogger;
import dev.buildcli.core.utils.DirectoryCleanup;
import dev.buildcli.core.utils.OS;
import dev.buildcli.core.utils.compress.FileExtractor;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.net.http.HttpHeaders;
import java.nio.file.Files;
import java.util.Map;
import java.util.List; 

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;


public class GradleInstallerTest {
    @Test
    void testInstallProgramFilesDirectory_windows() {
        try (MockedStatic<OS> osMock = Mockito.mockStatic(OS.class)) {
            osMock.when(OS::isWindows).thenReturn(true);
            File dir = GradleInstaller.installProgramFilesDirectory();
            assertTrue(dir.getAbsolutePath().contains("Program Files"));
        }
    }

    @Test
    void testInstallProgramFilesDirectory_linuxOrMac() {
        try (MockedStatic<OS> osMock = Mockito.mockStatic(OS.class)) {
            osMock.when(OS::isWindows).thenReturn(false);
            File dir = GradleInstaller.installProgramFilesDirectory();
            assertTrue(dir.getAbsolutePath().contains(System.getProperty("user.home")));
        }
    }

    @Test
    void testDownloadGradle_successful() throws Exception {
        try (MockedStatic<SystemOutLogger> loggerMock = Mockito.mockStatic(SystemOutLogger.class);
            MockedStatic<DirectoryCleanup> cleanupMock = Mockito.mockStatic(DirectoryCleanup.class);
            MockedStatic<HttpClient> httpClientMock = Mockito.mockStatic(HttpClient.class)) {

            HttpClient.Builder builderMock = mock(HttpClient.Builder.class);
            HttpClient clientMock = mock(HttpClient.class);
            HttpResponse<java.io.InputStream> responseMock = mock(HttpResponse.class);

            when(builderMock.followRedirects(any())).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(clientMock);
            httpClientMock.when(HttpClient::newBuilder).thenReturn(builderMock);

            when(responseMock.statusCode()).thenReturn(200);
            when(responseMock.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Length", List.of("123")), (s1, s2) -> true));
            when(responseMock.body()).thenReturn(Files.newInputStream(Files.createTempFile("gradle", ".zip")));
            when(clientMock.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(responseMock);

            File gradleZip = new File("gradle-8.12.1.zip");
            gradleZip.createNewFile();

            File result = GradleInstaller.downloadGradle();
            assertNotNull(result);
            assertTrue(result.exists());

            gradleZip.delete();
        }
    }

    @Test
    void testExtractGradle_callsExtractor() throws Exception {
        try (MockedStatic<FileExtractor> extractorMock = Mockito.mockStatic(FileExtractor.class)) {
            GradleInstaller.extractGradle("file.zip", "outputDir");
            extractorMock.verify(() -> FileExtractor.extractFile("file.zip", "outputDir"), times(1));
        }
    }
}