
package dev.buildcli.core.utils.tools;

import dev.buildcli.core.actions.commandline.GradleProcess;
import dev.buildcli.core.actions.commandline.MavenProcess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolChecksTest {

  @TempDir
  private Path tempDir;

  @Test
  void mavenCheckTrue() {
    try (
        MockedStatic<MavenProcess> mockedStatic = Mockito.mockStatic(MavenProcess.class)
    ){
      MavenProcess mockProcess = mock(MavenProcess.class);
      when(mockProcess.run()).thenReturn(0);

      mockedStatic.when(MavenProcess::createGetVersionProcessor).thenReturn(mockProcess);

      var result = ToolChecks.checksMaven();
      assertTrue(result);
    }
  }

  @Test
  void mavenCheckFalse() {
    try (
        MockedStatic<MavenProcess> mockedStatic = Mockito.mockStatic(MavenProcess.class)
    ){
      MavenProcess mockProcess = mock(MavenProcess.class);
      when(mockProcess.run()).thenReturn(1);

      mockedStatic.when(MavenProcess::createGetVersionProcessor).thenReturn(mockProcess);

      var result = ToolChecks.checksMaven();
      assertFalse(result);
    }
  }

  @Test
  void gradleCheck() {
    try (
        MockedStatic<GradleProcess> mockedStatic = Mockito.mockStatic(GradleProcess.class)
    ){
      GradleProcess mockProcess = mock(GradleProcess.class);
      when(mockProcess.run()).thenReturn(0);

      mockedStatic.when(GradleProcess::createGetVersionProcess).thenReturn(mockProcess);

      var result = ToolChecks.checksGradle();
      assertTrue(result);
    }
  }

  @Test
  void gradleCheckFalse() {
    try (
        MockedStatic<GradleProcess> mockedStatic = Mockito.mockStatic(GradleProcess.class)
    ){
      GradleProcess mockProcess = mock(GradleProcess.class);
      when(mockProcess.run()).thenReturn(1);

      mockedStatic.when(GradleProcess::createGetVersionProcess).thenReturn(mockProcess);

      var result = ToolChecks.checksGradle();
      assertFalse(result);
    }
  }

  @Test
  void gradleCheckRuntimeError() {
    try (
        MockedStatic<GradleProcess> mockedStatic = Mockito.mockStatic(GradleProcess.class)
    ){
      mockedStatic.when(GradleProcess::createGetVersionProcess)
          .thenThrow(new RuntimeException("Error running Gradle process"));

      boolean result = ToolChecks.checksGradle();
      assertFalse(result);
    }
  }

  @Test
  void mavenCheckRuntimeError() {
    try (
        MockedStatic<MavenProcess> mockedStatic = Mockito.mockStatic(MavenProcess.class)
    ){
      mockedStatic.when(MavenProcess::createGetVersionProcessor)
          .thenThrow(new RuntimeException("Error running Gradle process"));

      boolean result = ToolChecks.checksMaven();
      assertFalse(result);
    }
  }

  @Test
  void mavenIsMavenOrGradle_whenIsMaven() throws IOException {
    String expected = "Maven";
    createMockApplication(tempDir, "pom.xml");

    String result = ToolChecks.checkIsMavenOrGradle(tempDir.toFile());
    assertEquals(expected, result);
  }

  @Test
  void mavenIsMavenOrGradle_whenIsGradle() throws IOException {
    String expected = "Gradle";
    createMockApplication(tempDir, "build.gradle");

    String result = ToolChecks.checkIsMavenOrGradle(tempDir.toFile());
    assertEquals(expected, result);
  }

  @Test
  void mavenIsMavenOrGradle_whenIsNeither() throws IOException {
    String expected = "Neither";
    createMockApplication(tempDir, "someFile.txt");

    String result = ToolChecks.checkIsMavenOrGradle(tempDir.toFile());
    assertEquals(expected, result);
  }

  private void createMockApplication(Path rootDir, String nameFile) throws IOException {
    Files.createFile(rootDir.resolve(nameFile));
  }
}
