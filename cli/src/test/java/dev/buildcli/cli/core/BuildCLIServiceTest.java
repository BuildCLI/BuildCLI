package dev.buildcli.cli.core;

import dev.buildcli.core.domain.git.GitCommandExecutor;
import dev.buildcli.core.utils.BuildCLIService;
import dev.buildcli.core.utils.tools.CLIInteractions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BuildCLIServiceTest {


  @Test
  void testWelcome() {
    var standardOut = System.out;
    try {
      var outputStream = new java.io.ByteArrayOutputStream();
      System.setOut(new java.io.PrintStream(outputStream));

      BuildCLIService.welcome();
      String content = ",-----.          ,--.,--.   ,--. ,-----.,--.   ,--.\n" +
          "|  |) /_ ,--.,--.`--'|  | ,-|  |'  .--./|  |   |  |\n" +
          "|  .-.  \\|  ||  |,--.|  |' .-. ||  |    |  |   |  |       Built by the community, for the community\n" +
          "|  '--' /'  ''  '|  ||  |\\ `-' |'  '--'\\|  '--.|  |\n" +
          "`------'  `----' `--'`--' `---'  `-----'`-----'`--'\n" +
          "" +
          "\n";

      String output = outputStream.toString();
      assertEquals(content, output);
    } finally {
      System.setOut(standardOut);
    }
  }

  @Test
  void testShouldShowAsciiArt() {

    assertFalse(BuildCLIService.shouldShowAsciiArt(new String[]{}));

    assertTrue(BuildCLIService.shouldShowAsciiArt(new String[]{"--help"}));

    assertTrue(BuildCLIService.shouldShowAsciiArt(new String[]{"p", "run"}));

    assertTrue(BuildCLIService.shouldShowAsciiArt(new String[]{"p", "i", "-n"}));

    assertTrue(BuildCLIService.shouldShowAsciiArt(new String[]{"about"}));

    assertTrue(BuildCLIService.shouldShowAsciiArt(new String[]{"help"}));

    assertFalse(BuildCLIService.shouldShowAsciiArt(new String[]{"invalid"}));
  }

  @Test
  void testAbout() {
    GitCommandExecutor gitExecMock = mock(GitCommandExecutor.class);
    BuildCLIService service = new BuildCLIService(gitExecMock, "path/to/git");
    doNothing().when(gitExecMock).showContributors(anyString(), eq("https://github.com/BuildCLI/BuildCLI.git"));
    var standardOut = System.out;
    try {
      var outputStream = new java.io.ByteArrayOutputStream();
      System.setOut(new java.io.PrintStream(outputStream));

      service.about();
      String content = "BuildCLI is a command-line interface (CLI) tool for managing and automating common tasks in Java project development.\n" +
          "It allows you to create, compile, manage dependencies, and run Java projects directly from the terminal, simplifying the development process.\n" +
          "\n" +
          "Visit the repository for more details: https://github.com/BuildCLI/BuildCLI";

      String output = outputStream.toString();
      assertEquals(content, output.trim());
    }
    finally {
      System.setOut(standardOut);
    }
  }


  @Test
  void testCheckUpdatesBuildCLIAndUpdateTrue() {
    GitCommandExecutor gitExecMock = mock(GitCommandExecutor.class);

    when(gitExecMock.findGitRepository(anyString())).thenReturn("https://github.com/BuildCLI/BuildCLI.git");
    when(gitExecMock.checkIfLocalRepositoryIsUpdated(anyString(), anyString())).thenReturn(false);

    var standardOut = System.out;
    BuildCLIService service = spy(new BuildCLIService(gitExecMock, "path/to/git"));

    try (MockedStatic<CLIInteractions> mockedCLIInteractions = mockStatic(CLIInteractions.class)) {
      mockedCLIInteractions.when(() -> CLIInteractions.getConfirmation("update BuildCLI")).thenReturn(true);
      var outputStream = new java.io.ByteArrayOutputStream();
      System.setOut(new java.io.PrintStream(outputStream));

      service.checkUpdatesBuildCLIAndUpdate();

      assertTrue( outputStream.toString().contains("updated successfully!"));
      assertTrue(outputStream.toString().contains("ATTENTION: Your BuildCLI is outdated!"));
      mockedCLIInteractions.verify(
          () -> CLIInteractions.getConfirmation("update BuildCLI"),
          times(1)
      );
    } finally {
      System.setOut(standardOut);
    }
  }


  @Test
  void testCheckUpdatesBuildCLIAndUpdateFalse() {
    GitCommandExecutor gitExecMock = mock(GitCommandExecutor.class);

    when(gitExecMock.checkIfLocalRepositoryIsUpdated(anyString(),anyString())).thenReturn(true);

    var standardOut = System.out;

    BuildCLIService service = new BuildCLIService(gitExecMock, "path/to/git");

    try{
      var outputStream = new java.io.ByteArrayOutputStream();
      System.setOut(new java.io.PrintStream(outputStream));
      service.checkUpdatesBuildCLIAndUpdate();

      assertEquals("", outputStream.toString().trim());

    } finally {
      System.setOut(standardOut);
    }
  }
}

