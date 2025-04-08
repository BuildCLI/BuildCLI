package dev.buildcli.core.actions.commandline;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class DockerProcessTest {

  private String tag = "dockerCompose";
  private String fileName = "dockerFile";

  @Test
  void testCreateBuildProcess_withGivenTagParams() {
    DockerProcess dockerProcess = DockerProcess.createBuildProcess(tag);

    assertFalse(dockerProcess.commands.isEmpty());
    assertEquals("docker", dockerProcess.commands.get(0));
    assertEquals("build", dockerProcess.commands.get(1));
    assertEquals("-t", dockerProcess.commands.get(2));
    assertEquals(tag, dockerProcess.commands.get(3));
    assertEquals(".", dockerProcess.commands.get(4));
    assertEquals(5, dockerProcess.commands.size());
  }

  @Test
  void testCreateBuildProcess_withTagAndFileName() {
    DockerProcess dockerProcess = DockerProcess.createBuildProcess(tag, fileName);

    assertFalse(dockerProcess.commands.isEmpty());
    assertEquals("docker", dockerProcess.commands.get(0));
    assertEquals("build", dockerProcess.commands.get(1));
    assertEquals("-t", dockerProcess.commands.get(2));
    assertEquals(tag, dockerProcess.commands.get(3));
    assertEquals("-f", dockerProcess.commands.get(4));
    assertEquals(fileName, dockerProcess.commands.get(5));
    assertEquals(6, dockerProcess.commands.size());
  }

  @Test
  void testCreateBuildProcessWithoutAdditionalParams() {
    DockerProcess dockerProcess = DockerProcess.createProcess();
    assertEquals(DockerProcess.class, dockerProcess.getClass());
    assertFalse(dockerProcess.commands.isEmpty());
    assertEquals("docker", dockerProcess.commands.get(0));
    assertFalse(dockerProcess.commands.contains("build"));
  }

  @Test
  void testCreateProcess_withSpecificParams() {
    DockerProcess dockerProcess = DockerProcess.createProcess("clean", "build", "-f");

    assertFalse(dockerProcess.commands.isEmpty());
    assertEquals("docker", dockerProcess.commands.get(0));
    assertEquals("clean", dockerProcess.commands.get(1));
    assertEquals("build", dockerProcess.commands.get(2));
    assertEquals("-f", dockerProcess.commands.get(3));
  }

  @Test
  void testCreateRunProcessor() {
    DockerProcess dockerProcess = DockerProcess.createRunProcess(tag);

    assertFalse(dockerProcess.commands.isEmpty());
    assertEquals("docker", dockerProcess.commands.get(0));
    assertEquals("run", dockerProcess.commands.get(1));
    assertEquals("-p", dockerProcess.commands.get(2));
    assertEquals("8080:8080", dockerProcess.commands.get(3));
    assertEquals(tag, dockerProcess.commands.get(4));
  }

  @Test
  void testCreateGetVersionProcess() {
    DockerProcess dockerProcess = DockerProcess.createGetVersionProcess();
    assertFalse(dockerProcess.commands.isEmpty());
    assertEquals("docker", dockerProcess.commands.get(0));
    assertEquals("-v", dockerProcess.commands.get(1));
  }

  @Test
  void testCreateInfoProcess() {
    DockerProcess dockerProcess = DockerProcess.createInfoProcess();
    assertFalse(dockerProcess.commands.isEmpty());
    assertEquals("docker", dockerProcess.commands.get(0));
    assertEquals("info", dockerProcess.commands.get(1));
  }
}
