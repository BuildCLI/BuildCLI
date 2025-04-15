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
import static org.mockito.Mockito.*;

@DisplayName("OrchestrationDownCommand Tests")
@LogbackLogger(OrchestrationDownCommand.class)
@ExtendWith({MockitoExtension.class, LogbackExtension.class})
class OrchestrationDownCommandTest {

    @Mock
    private DockerManager dockerManagerMock;

    @InjectMocks
    private OrchestrationDownCommand command;

    @Test
    @DisplayName("Test run() success - Stop all containers")
    void testRunSuccessStopAllContainers(List<ILoggingEvent> logs) throws DockerException {

        command.run();
        verify(dockerManagerMock).downContainer(null);
        assertTrue(logs
                .stream()
                .anyMatch(
                        event -> event.getFormattedMessage().equals("All running containers have been successfully stopped.")));
    }

    @Test
    @DisplayName("Test run() success - Stop a single container")
    void testRunSuccessStopSingleContainer(List<ILoggingEvent> logs) throws DockerException {

        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("-n", "container1");

        assertEquals(0, exitCode);
        verify(dockerManagerMock).downContainer("container1");
        assertTrue(logs
                .stream()
                .anyMatch(
                        event -> event.getFormattedMessage().equals("Container 'container1' has been successfully stopped.")));
    }

    @Test
    @DisplayName("Test run() failure - Exception thrown with 'Failed to stop containers: ' message")
    void testRunFailureExceptionMessage() throws Exception {

        command.setContainerName("container1");

        DockerException exception = new DockerException("Test exception");
        doThrow(exception).when(dockerManagerMock).downContainer(anyString());

        CommandLine.ExecutionException thrown = assertThrows(CommandLine.ExecutionException.class,
                command::run,
                "Expected run() to throw CommandLine.ExecutionException");

        assertTrue(thrown.getMessage().contains("Failed to stop containers: Test exception"),
                "Exception message should contain 'Failed to stop containers: Test exception'");

        assertEquals(exception, thrown.getCause());
    }
}