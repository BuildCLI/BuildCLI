package dev.buildcli.core.actions.commandline;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static dev.buildcli.core.constants.MavenConstants.MAVEN_CMD;
import static org.junit.jupiter.api.Assertions.*;


public class MavenProcessTest {

  private Path tempDir;
  private MavenProcess mavenProcess;

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
    mavenProcess = MavenProcess.createProcessor();
    assertEquals(MavenProcess.class, mavenProcess.getClass());
    assertFalse(mavenProcess.commands.isEmpty());
    assertEquals(MAVEN_CMD, mavenProcess.commands.get(0));
    assertFalse(mavenProcess.commands.contains("build"));
  }

  @Test
  void testCreateProcessor_withGivenParams() {
    mavenProcess = MavenProcess.createProcessor("clean", "build", "-f");

    assertFalse(mavenProcess.commands.isEmpty());
    assertEquals(MAVEN_CMD, mavenProcess.commands.get(0));
    assertEquals("clean", mavenProcess.commands.get(1));
    assertEquals("build", mavenProcess.commands.get(2));
    assertEquals("-f", mavenProcess.commands.get(3));
  }

  @Test
  void testCreatePackageProcessor_withValidDirectory() {
    var standardOut = System.out;
    try {
      var outputStream = new java.io.ByteArrayOutputStream();
      System.setOut(new PrintStream(outputStream));
      mavenProcess = MavenProcess.createPackageProcessor(tempDir.toFile());

      String expectedLogMessage = "Running maven package command: mvn clean package -f " + tempDir.toString();
      assertTrue(outputStream.toString().contains(expectedLogMessage));
      assertFalse(mavenProcess.commands.isEmpty());
      assertEquals(MAVEN_CMD, mavenProcess.commands.get(0));
      assertEquals("clean", mavenProcess.commands.get(1));
      assertEquals("package", mavenProcess.commands.get(2));
      assertEquals("-f", mavenProcess.commands.get(3));
      assertEquals(tempDir.toString(), mavenProcess.commands.get(4));
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
      mavenProcess = MavenProcess.createCompileProcessor(tempDir.toFile());

      String expectedLogMessage = "Running maven compile command: mvn compile -f " + tempDir.toString();
      assertTrue(outputStream.toString().contains(expectedLogMessage));
      assertFalse(mavenProcess.commands.isEmpty());
      assertEquals(MAVEN_CMD, mavenProcess.commands.get(0));
      assertEquals("clean", mavenProcess.commands.get(1));
      assertEquals("compile", mavenProcess.commands.get(2));
      assertEquals("-f", mavenProcess.commands.get(3));
      assertEquals(tempDir.toString(), mavenProcess.commands.get(4));
    } finally {
      System.setOut(standardOut);
    }
  }

  @Test
  void testCreateGetVersionProcess() {
    mavenProcess = MavenProcess.createGetVersionProcessor();
    assertFalse(mavenProcess.commands.isEmpty());
    assertEquals(MAVEN_CMD, mavenProcess.commands.get(0));
    assertEquals("-v", mavenProcess.commands.get(1));
  }

  @Test
  void testRunCommands() {
    mavenProcess = MavenProcess.createGetVersionProcessor();
    int exitCode = mavenProcess.run();
    List<String> output = mavenProcess.output();

    assertEquals(0, exitCode);
    assertFalse(output.isEmpty());
    assertTrue(output.get(0).contains("Apache Maven"));
  }
}