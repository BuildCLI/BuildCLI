package dev.buildcli.cli.commands.run;

import ch.qos.logback.classic.spi.ILoggingEvent;
import dev.buildcli.cli.utilsfortest.LogbackExtension;
import dev.buildcli.cli.utilsfortest.LogbackLogger;
import dev.buildcli.core.exceptions.DockerException;
import dev.buildcli.core.utils.DockerManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@DisplayName("OrchestrationUpCommand Tests")
@LogbackLogger(OrchestrationUpCommand.class)
@ExtendWith({MockitoExtension.class, LogbackExtension.class})
class OrchestrationUpCommandTest {

    @Mock
    private DockerManager dockerManagerMock;

    @InjectMocks
    private OrchestrationUpCommand command;

    @Test
    @DisplayName("Test run() success - Start all containers")
    void testRunSuccessStartAllContainers(List<ILoggingEvent> logs) throws DockerException {

        command.run();
        verify(dockerManagerMock).upContainer(false);
        assertTrue(logs
                .stream()
                .anyMatch(event -> event.getFormattedMessage().equals("All containers have been successfully started.")));
    }

    @Test
    @DisplayName("Test run() success - Start all containers with rebuild")
    void testRunSuccessStartAllContainersWithRebuild(List<ILoggingEvent> logs) throws DockerException {

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("--build");

        assertEquals(0, exitCode);
        verify(dockerManagerMock).upContainer(true);
        assertTrue(logs
                .stream()
                .anyMatch(event -> event.getFormattedMessage().equals("All containers have been successfully started.")));
    }

    @Test
    @DisplayName("Test run() failure - Exception thrown with 'Failed to start containers: ' message")
    void testRunFailureExceptionMessage() throws Exception {

        DockerException exception = new DockerException("Test exception");
        doThrow(exception).when(dockerManagerMock).upContainer(anyBoolean());

        CommandLine.ExecutionException thrown = assertThrows(CommandLine.ExecutionException.class,
                command::run,
                "Expected run() to throw CommandLine.ExecutionException");

        assertTrue(thrown.getMessage().contains("Failed to start containers: Test exception"),
                "Exception message should contain 'Failed to start containers: Test exception'");

        assertEquals(exception, thrown.getCause());
    }
}