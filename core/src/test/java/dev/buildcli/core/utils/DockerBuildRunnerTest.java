package dev.buildcli.core.utils;

import dev.buildcli.core.actions.commandline.DockerProcess;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DockerBuildRunnerTest {
  @Test
  void testBuildAndRunDocker_SuccessWithStaticMocks() {
    DockerProcess buildMock = mock(DockerProcess.class);
    DockerProcess runMock = mock(DockerProcess.class);

    when(buildMock.run()).thenReturn(0);
    when(runMock.run()).thenReturn(0);

    try (MockedStatic<DockerProcess> dockerMockStatic = mockStatic(DockerProcess.class)) {
      dockerMockStatic.when(() -> DockerProcess.createBuildProcess("buildcli-app"))
          .thenReturn(buildMock);

      dockerMockStatic.when(() -> DockerProcess.createRunProcess("buildcli-app"))
          .thenReturn(runMock);

      DockerBuildRunner runner = new DockerBuildRunner();
      runner.buildAndRunDocker();

      // Verificações
      verify(buildMock).run();
      verify(runMock).run();
    }
  }

  @Test
  void testBuildAndRunDocker_FailWithStaticMocks() {
    DockerProcess buildMock = mock(DockerProcess.class);
    DockerProcess runMock = mock(DockerProcess.class);

    when(buildMock.run()).thenReturn(1);
    when(runMock.run()).thenReturn(1);
    try (
        MockedStatic<DockerProcess> dockerMockStatic = mockStatic(DockerProcess.class)
        ) {
      dockerMockStatic.when(() -> DockerProcess.createBuildProcess("buildcli-app"))
          .thenReturn(buildMock);

      dockerMockStatic.when(() -> DockerProcess.createRunProcess("buildcli-app"))
          .thenReturn(runMock);

      DockerBuildRunner runner = new DockerBuildRunner();
      runner.buildAndRunDocker();
      verify(buildMock).run();
      verifyNoInteractions(runMock);
    }
  }

  @Test
  void testBuildAndRunDocker_FailAtRunStep() throws IOException {

    DockerProcess buildMock = mock(DockerProcess.class);
    DockerProcess runMock = mock(DockerProcess.class);

    when(buildMock.run()).thenReturn(0);
    when(runMock.run()).thenReturn(1);

    var outputStreamDefault = System.out;
    try (
        MockedStatic<DockerProcess> dockerMockStatic = mockStatic(DockerProcess.class);
        ByteArrayOutputStream outContent = new ByteArrayOutputStream()
    ) {
      System.setOut(new PrintStream(outContent));
      dockerMockStatic.when(() -> DockerProcess.createBuildProcess("buildcli-app"))
          .thenReturn(buildMock);

      dockerMockStatic.when(() -> DockerProcess.createRunProcess("buildcli-app"))
          .thenReturn(runMock);

      DockerBuildRunner runner = new DockerBuildRunner();
      runner.buildAndRunDocker();

      String output = outContent.toString();

      assertEquals("Docker image built successfully.", output.trim());
      verify(buildMock).run();
      verify(runMock).run();

    } finally {
      System.setOut(outputStreamDefault);
    }
  }
}

