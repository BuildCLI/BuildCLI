package dev.buildcli.cli.commands.project.add;

import ch.qos.logback.classic.spi.ILoggingEvent;
import dev.buildcli.cli.utilsfortest.LogbackExtension;
import dev.buildcli.cli.utilsfortest.LogbackLogger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DockerComposeCommand Tests")
@LogbackLogger(DockerComposeCommand.class)
@ExtendWith({MockitoExtension.class, LogbackExtension.class})
class DockerComposeCommandTest {

    private static final String VALID_DOCKER_FILE = "./ValidDockerFile";
    private static final String INVALID_DOCKER_FILE = "./InvalidDockerFile";
    private static final String DOCKER_COMPOSE_FILE = "./docker-compose.yml";

    @InjectMocks
    private DockerComposeCommand command;

    @BeforeEach
    @DisplayName("Set up test environment")
    void setUp() throws IOException {
        Files.write(Paths.get(VALID_DOCKER_FILE), "FROM alpine".getBytes());
    }

    @AfterAll
    @DisplayName("Clean up Dockerfile and docker-compose.yml")
    static void afterAll() throws IOException {
        Files.deleteIfExists(Paths.get(VALID_DOCKER_FILE));
        Files.deleteIfExists(Paths.get(DOCKER_COMPOSE_FILE));
    }

    @Test
    @DisplayName("Test DockerComposeCommand success scenario")
    void testDockerComposeCommandSuccess(List<ILoggingEvent> logs) {
        CommandLine cmd = new CommandLine(command);
        String[] options = {
                "--dockerfile", VALID_DOCKER_FILE,
                "--cpu", "2",
                "--memory", "512m",
                "--port", "8080:8080",
                "--volume", "./data:/app/data"
        };
        int exitCode = cmd.execute(options);

        assertEquals(0, exitCode);
        assertTrue(logs.stream()
                .anyMatch(event -> event.getFormattedMessage()
                        .equals("docker-compose.yml created successfully!")));
    }

    @Test
    @DisplayName("Test DockerComposeCommand failure scenario (Dockerfile not found)")
    void testDockerComposeCommandFailureDockerfileNotFound() {
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));

        CommandLine cmd = new CommandLine(command);
        String[] options = {
                "--dockerfile", INVALID_DOCKER_FILE,
                "--cpu", "2",
                "--memory", "512m",
                "--port", "8080:8080",
                "--volume", "./data:/app/data"
        };
        int exitCode = cmd.execute(options);

        System.setErr(originalErr);

        assertEquals(1, exitCode);

        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Dockerfile not found: ./InvalidDockerFile"), "Expected Dockerfile not found detail not found in stderr.");
    }

    @Test
    @DisplayName("Test run() failure scenario - catch block throws ExecutionException")
    void testRunFailureException() {

        command.setDockerFilePath(INVALID_DOCKER_FILE);

        CommandLine.ExecutionException thrown = assertThrows(CommandLine.ExecutionException.class,
                command::run,
                "Expected run() to throw CommandLine.ExecutionException");

        assertTrue(thrown.getMessage().contains("Failed to setup docker-compose:"),
                "Exception message should contain 'Failed to setup docker-compose:'");
    }

}