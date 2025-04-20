package dev.buildcli.core.actions.tools;

import dev.buildcli.core.actions.commandline.MavenProcess;
import dev.buildcli.core.utils.installers.MavenInstaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MavenCheckerTest {

  private MavenChecker mavenChecker;

  @BeforeEach
  public void setUp() {
    mavenChecker = new MavenChecker();
  }

  @Test
  void testName() {
    String result = mavenChecker.name();
    String expected = "Maven";
    assertEquals(expected, result);
  }

  @Test
  void testIsInstalled_WhenMavenIsInstalled_ShouldReturnTrue() {

    MavenProcess mockProcess = mock(MavenProcess.class);

    when(mockProcess.run()).thenReturn(0);

    try (var mockedStatic = mockStatic(MavenProcess.class)) {
      mockedStatic.when(MavenProcess::createGetVersionProcessor).thenReturn(mockProcess);

      assertTrue(mavenChecker.isInstalled());
    }
  }

  @Test
  void testIsInstalled_WhenMavenIsNotInstalled_ShouldReturnFalse() {

    MavenProcess mockProcess = mock(MavenProcess.class);

    when(mockProcess.run()).thenReturn(1);

    try (var mockedStatic = mockStatic(MavenProcess.class)) {
      mockedStatic.when(MavenProcess::createGetVersionProcessor).thenReturn(mockProcess);

      assertFalse(mavenChecker.isInstalled());
    }
  }

  @Test
  void testVersion_WhenMavenIsInstalled_ShouldReturnVersion() {

    MavenProcess mockProcess = mock(MavenProcess.class);

    when(mockProcess.output()).thenReturn(Collections.singletonList("Apache Maven 3.8.5"));

    try (var mockedStatic = mockStatic(MavenProcess.class)) {
      mockedStatic.when(MavenProcess::createGetVersionProcessor).thenReturn(mockProcess);

      String result = mavenChecker.version();
      String expected = "3.8.5";
      assertEquals(expected, result);
    }
  }

  @Test
  void testVersion_WhenMavenIsNotInstalled_ShouldReturnNA() {

    MavenProcess mockProcess = mock(MavenProcess.class);

    when(mockProcess.output()).thenReturn(Collections.emptyList());
    when(mockProcess.run()).thenReturn(1);

    try (var mockedStatic = mockStatic(MavenProcess.class)) {
      mockedStatic.when(MavenProcess::createGetVersionProcessor).thenReturn(mockProcess);

      String result = mavenChecker.version();
      String expected = "N/A";
      assertEquals(expected, result);
    }
  }

  @Test
  void testInstallInstructions() {
    String result = mavenChecker.installInstructions();
    String expected = "Install Maven: https://maven.apache.org/install.html";
    assertEquals(expected, result);
  }

  @Test
  void testFixIssue_ShouldCallInstallMaven() {
    try (var mockedStatic = mockStatic(MavenInstaller.class)) {
      mavenChecker.fixIssue();
      mockedStatic.verify(() -> MavenInstaller.installMaven(), times(1));
    }
  }
}
