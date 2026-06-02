package dev.buildcli.core.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class OSTest {

  /** Original operating system name. */
  private static final String OS_NAME = System.getProperty("os.name");
  private Path tempDir;
  private Path secondTempDir;
  private RuntimeCommandExecutor mockRuntimeCommandExecutor;

  @BeforeEach
  void setUp() throws IOException {
    mockRuntimeCommandExecutor = mock(RuntimeCommandExecutor.class);
    tempDir = Files.createTempDirectory("output_dir");
    secondTempDir = Files.createTempDirectory("output_dir2");
    Files.createDirectories(tempDir);
  }

  @AfterEach
  void cleanup() throws IOException {
    System.setProperty("os.name", OS_NAME);
    Files.walk(tempDir)
        .sorted(Comparator.reverseOrder())
        .forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (IOException e) {
            throw new RuntimeException("Erro ao excluir o arquivo: " + p, e);
          }
        });
    Files.deleteIfExists(tempDir);
    Files.deleteIfExists(secondTempDir);
  }

  @Test
  void shouldDetectKnownOperatingSystem_whenGetOSNameIsCalled() {
    String osName = OS.getOSName().toLowerCase(Locale.ROOT);
    List<String> expectedOS = List.of("linux", "nix", "nux", "aix", "win", "mac");
    boolean matchesOS = expectedOS.stream()
        .anyMatch(osName::contains);
    assertTrue(matchesOS);
  }

  @Test
  void shouldReturnKnownArchitecture_whenGetArchitectureIsCalled() {
    String osArchitecture = OS.getArchitecture().toLowerCase();
    List<String> expectedArchitectures = List.of("amd64", "x86", "x86_64", "aarch64", "arch64", "arm", "sd");
    boolean matchesKnownArch = expectedArchitectures.stream()
        .anyMatch(osArchitecture::contains);
    assertFalse(osArchitecture.isEmpty());
    assertTrue(matchesKnownArch);
  }

  @Test
  void shouldReturnTrue_whenOSIsWindows() {
    System.setProperty("os.name", "Windows 10");
    try {
      assertTrue(OS.isWindows());
    } finally {
      System.setProperty("os.name", OS_NAME);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Linux", "nux", "nix", "aix"})
  void shouldReturnTrue_whenOSIsLinux(String osName) {
    System.setProperty("os.name", osName);
    assertTrue(OS.isLinux());
  }

  @Test
  void shouldReturnTrue_whenOSIsMac() {
    System.setProperty("os.name", "mac");
    try {
      assertTrue(OS.isMac());
    } finally {
      System.setProperty("os.name", OS_NAME);
    }
  }

  @Test
  void shouldReturnHomeBinPath_whenGetHomeBinDirectoryIsCalled() {
    String result = OS.getHomeBinDirectory();
    String expect = OS.isWindows() ? System.getenv("HOMEPATH") + "//bin" : System.getenv("HOME") + "/bin";
    assertEquals(expect, result);
  }

  @Test
  void shouldNotExecuteChmod_whenOSIsWindows() throws Exception {
    File ex = Files.createTempFile("test", "sh").toFile();
    try {
      System.setProperty("os.name", "Windows 10");
      OS.chmodX(ex.toString());
      verify(mockRuntimeCommandExecutor, times(0)).execute(any());
    } finally {
      System.setProperty("os.name", OS_NAME);
    }
  }


}
