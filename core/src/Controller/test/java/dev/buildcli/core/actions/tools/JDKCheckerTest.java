package dev.buildcli.core.actions.tools;

import dev.buildcli.core.actions.commandline.JavaProcess;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JDKCheckerTest {
  private JDKChecker jdkChecker = new JDKChecker();

  @Test
  void testName() {
    String result = jdkChecker.name();
    String expected = "JDK";
    assertEquals(expected, result);
  }

  @Test
  void testIsInstalled_WhenJDKIsInstalled_ShouldReturnTrue() {
    JavaProcess mockProcess = mock(JavaProcess.class);
    when(mockProcess.run()).thenReturn(0);

    try (var mockedStatic = mockStatic(JavaProcess.class)) {
      mockedStatic.when(JavaProcess::createGetVersionProcess).thenReturn(mockProcess);
      assertTrue(jdkChecker.isInstalled());
    }
  }

  @Test
  void testIsInstalled_WhenJDKIsNotInstalled_ShouldReturnFalse() {
    JavaProcess mockProcess = mock(JavaProcess.class);
    when(mockProcess.run()).thenReturn(1);

    try (var mockedStatic = mockStatic(JavaProcess.class)) {
      mockedStatic.when(JavaProcess::createGetVersionProcess).thenReturn(mockProcess);
      assertFalse(jdkChecker.isInstalled());
    }
  }

  @Test
  void testVersion_WhenJDKIsInstalled_ShouldReturnVersion() {
    JavaProcess mockProcess = mock(JavaProcess.class);
    when(mockProcess.run()).thenReturn(0);
    when(mockProcess.output()).thenReturn(Collections.singletonList("openjdk 21.0.6 2025-01-21"));

    try (var mockedStatic = mockStatic(JavaProcess.class)) {
      mockedStatic.when(JavaProcess::createGetVersionProcess).thenReturn(mockProcess);
      String result = jdkChecker.version();
      String expected = "21.0.6";
      assertEquals(expected, result);
    }
  }

  @Test
  void testVersion_WhenJDKIsInstalled_ButOutputIsMalformed_ShouldReturnNA() {
    JavaProcess mockProcess = mock(JavaProcess.class);
    when(mockProcess.run()).thenReturn(1);
    when(mockProcess.output()).thenReturn(Collections.singletonList("some random text"));

    try (var mockedStatic = mockStatic(JavaProcess.class)) {
      mockedStatic.when(JavaProcess::createGetVersionProcess).thenReturn(mockProcess);
      String result = jdkChecker.version();
      String expected = "N/A";
      assertEquals(expected, result);
    }
  }

  @Test
  void testVersion_WhenJDKIsNotInstalled_ShouldReturnNA() {
    JavaProcess mockProcess = mock(JavaProcess.class);
    when(mockProcess.run()).thenReturn(1);
    when(mockProcess.output()).thenReturn(Collections.emptyList());

    try (var mockedStatic = mockStatic(JavaProcess.class)) {
      mockedStatic.when(JavaProcess::createGetVersionProcess).thenReturn(mockProcess);
      String result = jdkChecker.version();
      String expected = "N/A";
      assertEquals(expected, result);
    }
  }

  @Test
  void testInstall_Instructions() {
    String result = jdkChecker.installInstructions();
    String expected = "Install JDK: https://www.oracle.com/java/technologies/javase-downloads.html";;
    assertEquals(expected, result);
  }

  @Test
  void testFixIssue() {
    var standardOut = System.out;
    try {
      var outputStream = new java.io.ByteArrayOutputStream();
      System.setOut(new java.io.PrintStream(outputStream));

      jdkChecker.fixIssue();
      String content = "Fixing JDK issues is not automated. Follow the installation instructions.";

      String output = outputStream.toString().trim();
      assertEquals(content, output);
    }finally {
      System.setOut(standardOut);
    }
  }
}
