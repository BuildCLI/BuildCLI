package dev.buildcli.core.actions.tools;

import dev.buildcli.core.actions.commandline.DockerProcess;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DockerCheckerTest {
  
  private DockerChecker dockerChecker= new DockerChecker();

  @Test
  void testName() {
    String result = dockerChecker.name();
    String expected = "Docker";
    assertEquals(expected, result);
  }

  @Test
  void testIsInstalled_WhenDockerIsInstalled_ShouldReturnTrue() {
    DockerProcess mockProcess = mock(DockerProcess.class);
    when(mockProcess.run()).thenReturn(0);

    try (var mockedStatic = mockStatic(DockerProcess.class)) {
      mockedStatic.when(DockerProcess::createGetVersionProcess).thenReturn(mockProcess);
      assertTrue(dockerChecker.isInstalled());
    }
  }

   @Test
  void testIsInstalled_WhenDockerIsNotInstalled_ShouldReturnFalse() {
    DockerProcess mockProcess = mock(DockerProcess.class);
    when(mockProcess.run()).thenReturn(1);

     try (var mockedStatic = mockStatic(DockerProcess.class)) {
       mockedStatic.when(DockerProcess::createGetVersionProcess).thenReturn(mockProcess);
      assertFalse(dockerChecker.isInstalled());
    }
  }

  @Test
  void testIsRunning_WhenDockerIsNotRunning_ShouldReturnFalse() {
    DockerProcess mockProcess = mock(DockerProcess.class);
    when(mockProcess.run()).thenReturn(1);

    try (var mockedStatic = mockStatic(DockerProcess.class)) {
      mockedStatic.when(DockerProcess::createInfoProcess).thenReturn(mockProcess);
      assertFalse(dockerChecker.isRunning());
    }
  }

  @Test
  void testIsRunning_WhenDockerIsRunning_ShouldReturnTrue() {
    DockerProcess mockProcess = mock(DockerProcess.class);
    when(mockProcess.run()).thenReturn(0);

    try (var mockedStatic = mockStatic(DockerProcess.class)) {
      mockedStatic.when(DockerProcess::createInfoProcess).thenReturn(mockProcess);
      assertTrue(dockerChecker.isRunning());
    }
  }

  @Test
  void testVersion_WhenDockerIsInstalled_ShouldReturnVersion() {
    DockerProcess mockProcess = mock(DockerProcess.class);
    when(mockProcess.run()).thenReturn(0);
    when(mockProcess.output()).thenReturn(Collections.singletonList("Docker version 20.10.6, build 370c289"));

    try (var mockedStatic = mockStatic(DockerProcess.class)) {
      mockedStatic.when(DockerProcess::createGetVersionProcess).thenReturn(mockProcess);
      String result = dockerChecker.version();
      String expected = "20.10.6";
      assertEquals(expected, result);
    }
  }

  @Test
  void testVersion_WhenDockerIsInstalled_ButOutputIsMalformed_ShouldReturnNA() {
    DockerProcess mockProcess = mock(DockerProcess.class);
    when(mockProcess.run()).thenReturn(1);
    when(mockProcess.output()).thenReturn(Collections.singletonList("some random text"));

    try (var mockedStatic = mockStatic(DockerProcess.class)) {
      mockedStatic.when(DockerProcess::createGetVersionProcess).thenReturn(mockProcess);
      String result = dockerChecker.version();
      String expected = "N/A";
      assertEquals(expected, result);
    }
  }

  @Test
  void testVersion_WhenDockerIsNotInstalled_ShouldReturnNA() {
    DockerProcess mockProcess = mock(DockerProcess.class);
    when(mockProcess.run()).thenReturn(1);
    when(mockProcess.output()).thenReturn(Collections.emptyList());

    try (var mockedStatic = mockStatic(DockerProcess.class)) {
      mockedStatic.when(DockerProcess::createGetVersionProcess).thenReturn(mockProcess);
      String result = dockerChecker.version();
      String expected = "N/A";
      assertEquals(expected, result);
    }
  }

  @Test
  void testInstall_Instructions() {
    String result = dockerChecker.installInstructions();
    String expected = "Install Docker: https://docs.docker.com/get-docker/";
    assertEquals(expected, result);
  }

  @Test
  void testFixIssue() {
    var standardOut = System.out;
    try {
      var outputStream = new java.io.ByteArrayOutputStream();
      System.setOut(new java.io.PrintStream(outputStream));

      dockerChecker.fixIssue();
      String content = "Fixing Docker issues is not automated. Please ensure Docker is installed and running.";

      String output = outputStream.toString().trim();
      assertEquals(content, output);
    }finally {
      System.setOut(standardOut);
    }
  }
}
