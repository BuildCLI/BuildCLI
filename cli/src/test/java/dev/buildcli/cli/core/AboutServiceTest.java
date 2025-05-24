package dev.buildcli.cli.core;

import dev.buildcli.core.domain.git.GitCommandExecutor;
import dev.buildcli.core.utils.AboutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AboutServiceTest {

    @Mock
    private GitCommandExecutor gitExecMock;

    @InjectMocks
    private AboutService aboutService;

    @Test
    void testAbout() {
        when(gitExecMock.showContributors()).thenReturn("contributor1, contributor2");

        var standardOut = System.out;
        try {
            var outputStream = new java.io.ByteArrayOutputStream();
            System.setOut(new java.io.PrintStream(outputStream));

            AboutService.about();
            String expected = """
                BuildCLI is a command-line interface (CLI) tool for managing and automating common tasks in Java project development.
                It allows you to create, compile, manage dependencies, and run Java projects directly from the terminal, simplifying the development process.

                Visit the repository for more details: https://github.com/BuildCLI/BuildCLI

                contributor1, contributor2""";

            List<String> expectedLines = expected.lines().toList();
            List<String> actualLines = outputStream.toString().lines().toList();

            assertLinesMatch(expectedLines, actualLines);

        } finally {
            System.setOut(standardOut);
        }
    }

}
