package dev.buildcli.core.actions.commandline;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaProcessTest {
  private Path tempDir;
  private JavaProcess javaProcess;

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
    javaProcess = JavaProcess.createProcess();
    assertEquals(JavaProcess.class, javaProcess.getClass());
    assertFalse(javaProcess.commands.isEmpty());
    assertEquals("java", javaProcess.commands.get(0));
    assertEquals(1, javaProcess.commands.size());
  }

  @Test
  void testCreateProcessor_withGivenParams() {
    javaProcess = JavaProcess.createProcess("-jar", tempDir.toString());

    assertFalse(javaProcess.commands.isEmpty());
    assertEquals("java", javaProcess.commands.get(0));
    assertEquals("-jar", javaProcess.commands.get(1));
    assertEquals(tempDir.toString(), javaProcess.commands.get(2));
  }

  @Test
  void testCreateRunJarProcess_withValidParameters() {
    javaProcess = JavaProcess.createRunJarProcess("test.jar", "-Xmx1024m", "-Xms512m");

    assertFalse(javaProcess.commands.isEmpty());
    assertEquals("java", javaProcess.commands.get(0));
    assertEquals("-jar", javaProcess.commands.get(1));
    assertEquals("test.jar", javaProcess.commands.get(2));
    assertEquals("-Xmx1024m -Xms512m", javaProcess.commands.get(3));
  }

  @Test
  void testCreateRunClassProcess() {
    javaProcess = JavaProcess.createRunClassProcess(tempDir.toAbsolutePath().toString(), "-Xmx1024m","-Xms512m");

    assertFalse(javaProcess.commands.isEmpty());
    assertEquals("java", javaProcess.commands.get(0));
    assertEquals(tempDir.toAbsolutePath().toString(), javaProcess.commands.get(1));
    assertEquals("-Xmx1024m -Xms512m", javaProcess.commands.get(2));
  }

  @Test
  void testCreateGetVersionProcess() {
    javaProcess = JavaProcess.createGetVersionProcess();
    assertFalse(javaProcess.commands.isEmpty());
    assertEquals("java", javaProcess.commands.get(0));
    assertEquals("--version", javaProcess.commands.get(1));
  }

  @Test
  void testRunCommands() {
    javaProcess = JavaProcess.createGetVersionProcess();
    int exitCode = javaProcess.run();
    List<String> output = javaProcess.output();

    assertEquals(0, exitCode);
    assertFalse(output.isEmpty());
    assertTrue(output.get(0).contains("openjdk"));
    assertTrue(output.get(1).contains("OpenJDK Runtime Environment"));
    assertTrue(output.get(2).contains("OpenJDK 64-Bit Server VM"));
  }

  @Test
  void testInvalidCommandFailsGracefully() {
    JavaProcess process = JavaProcess.createProcess("-invalidFlag");
    int exitCode = process.run();
    assertNotEquals(0, exitCode);
  }
}
