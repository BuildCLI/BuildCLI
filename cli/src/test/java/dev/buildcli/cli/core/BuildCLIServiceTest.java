package dev.buildcli.cli.core;

import dev.buildcli.core.domain.git.GitCommandExecutor;
import dev.buildcli.core.utils.BuildCLIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildCLIServiceTest {

  private static final String GIT_PATH = "path/to/git";
  private static final String REPO_URL = "https://github.com/BuildCLI/BuildCLI.git";

  @Mock
  private GitCommandExecutor gitExecMock;

  @InjectMocks
  private BuildCLIService service;

//TODO: fix this test when BuildCLIService is refactored
  void testWelcome() {
    var standardOut = System.out;
    try {
      var outputStream = new java.io.ByteArrayOutputStream();
      System.setOut(new java.io.PrintStream(outputStream));

      BuildCLIService.welcome();
      String content = """
                ,-----.          ,--.,--.   ,--. ,-----.,--.   ,--.
                |  |) /_ ,--.,--.`--'|  | ,-|  |'  .--./|  |   |  |
                |  .-.  \\|  ||  |,--.|  |' .-. ||  |    |  |   |  |       [3m[34mBuilt by the community, for the community[0m
                |  '--' /'  ''  '|  ||  |\\ `-' |'  '--'\\|  '--.|  |
                `------'  `----' `--'`--' `---'  `-----'`-----'`--'

                """;

      assertEquals(content, outputStream.toString());
    } finally {
      System.setOut(standardOut);
    }
  }

  // TODO BuildCLIService need a refactor to improve testing capacity
  void checkUpdates_shouldShowOutdatedMessage_whenUpdateAvailable() {
    when(gitExecMock.checkIfLocalRepositoryIsUpdated(any(), eq(REPO_URL))).thenReturn(false);

  }

  @Test
  void checkUpdates_shouldDoNothing_whenAlreadyUpToDate() {
    when(gitExecMock.checkIfLocalRepositoryIsUpdated(any(), eq(REPO_URL))).thenReturn(true);

    var standardOut = System.out;
    var outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));

    try {
      service.checkUpdatesBuildCLIAndUpdate();

      assertEquals("", outputStream.toString().trim());
      verify(gitExecMock).checkIfLocalRepositoryIsUpdated(any(), eq(REPO_URL));
      verifyNoMoreInteractions(gitExecMock);
    } finally {
      System.setOut(standardOut);
    }
  }
}