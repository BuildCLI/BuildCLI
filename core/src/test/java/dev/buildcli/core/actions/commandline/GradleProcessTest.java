package dev.buildcli.core.actions.commandline;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static dev.buildcli.core.constants.GradleConstants.GRADLE_CMD;
import static org.junit.jupiter.api.Assertions.*;

public class GradleProcessTest {

  private Path tempDir;

  @BeforeEach
  void beforeEach() throws IOException {
    tempDir = Files.createTempDirectory("output_dir");
    Files.createDirectories(tempDir);
  }

  @AfterEach
  void cleanup() throws IOException {
    Files.walk(tempDir)
        .sorted(Comparator.reverseOrder())
        .forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (IOException e) {
            throw new RuntimeException("Erro ao excluir o arquivo: " + p, e);
          }
        });
  }

  @Test
  void testCreateProcessor() {
    GradleProcess gradleProcess = GradleProcess.createProcessor();
    assertEquals(GradleProcess.class, gradleProcess.getClass());
    assertFalse(gradleProcess.commands.isEmpty());
    assertEquals(GRADLE_CMD, gradleProcess.commands.get(0));
    assertFalse(gradleProcess.commands.contains("build"));
  }

  @Test
  void testCreateProcessor_withGivenParams() {
    GradleProcess gradleProcess = GradleProcess.createProcessor("clean", "build", "-f");

    assertFalse(gradleProcess.commands.isEmpty());
    assertEquals(GRADLE_CMD, gradleProcess.commands.get(0));
    assertEquals("clean", gradleProcess.commands.get(1));
    assertEquals("build", gradleProcess.commands.get(2));
    assertEquals("-f", gradleProcess.commands.get(3));
  }

  @Test
  void testCreatePackageProcessor_withValidDirectory() {
    var standardOut = System.out;
    try {
      var outputStream = new java.io.ByteArrayOutputStream();
      System.setOut(new PrintStream(outputStream));
      GradleProcess gradleProcess = GradleProcess.createPackageProcessor(tempDir.toFile());

      String expectedLogMessage = "Running gradle package command: gradle clean build -f " + tempDir.toString();
      assertTrue(outputStream.toString().contains(expectedLogMessage));
      assertFalse(gradleProcess.commands.isEmpty());
      assertEquals(GRADLE_CMD, gradleProcess.commands.get(0));
      assertEquals("clean", gradleProcess.commands.get(1));
      assertEquals("build", gradleProcess.commands.get(2));
      assertEquals("-f", gradleProcess.commands.get(3));
    } finally {
      System.setOut(standardOut);
    }
  }

  @Test
  void testCreateCompileProcessor_withValidDirectory() {
    var standardOut = System.out;
    try {
      var outputStream = new java.io.ByteArrayOutputStream();
      System.setOut(new PrintStream(outputStream));
      GradleProcess gradleProcess = GradleProcess.createCompileProcessor(tempDir.toFile());

      String expectedLogMessage = "Running gradle compile command: gradle clean classes -f " + tempDir.toString();
      assertTrue(outputStream.toString().contains(expectedLogMessage));
      assertFalse(gradleProcess.commands.isEmpty());
      assertEquals(GRADLE_CMD, gradleProcess.commands.get(0));
      assertEquals("clean", gradleProcess.commands.get(1));
      assertEquals("classes", gradleProcess.commands.get(2));
      assertEquals("-f", gradleProcess.commands.get(3));
    } finally {
      System.setOut(standardOut);
    }
  }

  @Test
  void testCreateGetVersionProcess() {
    GradleProcess gradleProcess = GradleProcess.createGetVersionProcess();
    assertFalse(gradleProcess.commands.isEmpty());
    assertEquals(GRADLE_CMD, gradleProcess.commands.get(0));
    assertEquals("--version", gradleProcess.commands.get(1));
  }
}
