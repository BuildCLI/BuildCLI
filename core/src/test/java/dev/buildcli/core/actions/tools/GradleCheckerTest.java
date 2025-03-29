package dev.buildcli.core.actions.tools;

import dev.buildcli.core.actions.commandline.GradleProcess;
import dev.buildcli.core.utils.installers.GradleInstaller;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;


import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GradleCheckerTest {

    private GradleChecker gradleChecker = new GradleChecker();

    @Test
    void testName() {
      String result = gradleChecker.name();
      String expected = "Gradle";
      assertEquals(expected, result);
    }

    @Test
    void testIsInstalled_WhenGradleIsInstalled_ShouldReturnTrue() {
      GradleProcess mockProcess = mock(GradleProcess.class);
      when(mockProcess.run()).thenReturn(0);

      try (var mockedStatic = mockStatic(GradleProcess.class)) {
        mockedStatic.when(GradleProcess::createGetVersionProcess).thenReturn(mockProcess);
        assertTrue(gradleChecker.isInstalled());
      }
    }

    @Test
    void testIsInstalled_WhenGradleIsNotInstalled_ShouldReturnFalse() {
      GradleProcess mockProcess = mock(GradleProcess.class);
      when(mockProcess.run()).thenReturn(1);

      try (var mockedStatic = mockStatic(GradleProcess.class)) {
        mockedStatic.when(GradleProcess::createGetVersionProcess).thenReturn(mockProcess);
        assertFalse(gradleChecker.isInstalled());
      }
    }

    @Test
    void testVersion_WhenGradleIsInstalled_ShouldReturnVersion() {
      GradleProcess mockProcess = mock(GradleProcess.class);
      when(mockProcess.output()).thenReturn(Collections.singletonList("Gradle 3.8.5"));

      try (var mockedStatic = mockStatic(GradleProcess.class)) {
        mockedStatic.when(GradleProcess::createGetVersionProcess).thenReturn(mockProcess);
        String result = gradleChecker.version();
        String expected = "3.8.5";
        assertEquals(expected, result);
      }
    }

    @Test
    void testVersion_WhenGradleIsInstalled_ButOutputIsMalformed_ShouldReturnNA() {
      GradleProcess mockProcess = mock(GradleProcess.class);
      when(mockProcess.run()).thenReturn(0);
      when(mockProcess.output()).thenReturn(Collections.singletonList("Some random text"));

      try (MockedStatic<GradleProcess> mockedStatic = mockStatic(GradleProcess.class)) {
        mockedStatic.when(GradleProcess::createGetVersionProcess).thenReturn(mockProcess);
        assertEquals("N/A", gradleChecker.version());
      }
    }

    @Test
    void testVersion_WhenGradleIsNotInstalled_ShouldReturnNA() {
      GradleProcess mockProcess = mock(GradleProcess.class);
      when(mockProcess.output()).thenReturn(Collections.emptyList());
      when(mockProcess.run()).thenReturn(1);

      try (var mockedStatic = mockStatic(GradleProcess.class)) {
        mockedStatic.when(GradleProcess::createGetVersionProcess).thenReturn(mockProcess);
        String result = gradleChecker.version();
        String expected = "N/A";
        assertEquals(expected, result);
      }
    }

    @Test
    void testInstall_Instructions() {
      String result = gradleChecker.installInstructions();
      String expected = "Install Gradle: https://gradle.org/install/";
      assertEquals(expected, result);
    }

  @Test
  void testFixIssueCallsGradleInstaller() {
    try (MockedStatic<GradleInstaller> mockedStatic = Mockito.mockStatic(GradleInstaller.class)) {
      GradleChecker fixer = new GradleChecker();
      fixer.fixIssue();

      mockedStatic.verify(GradleInstaller::installGradle, times(1));
    }
  }
}
