package dev.buildcli.core.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import dev.buildcli.core.actions.commandline.DockerProcess;
import dev.buildcli.core.exceptions.DockerException;
import dev.buildcli.core.model.DockerComposeConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import utilsfortest.LogbackExtension;
import utilsfortest.LogbackLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static dev.buildcli.core.exceptions.DockerException.DockerfileNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@LogbackLogger(DockerManager.class)
@DisplayName("DockerManager Test")
@ExtendWith({MockitoExtension.class, LogbackExtension.class})
class DockerManagerTest {

    private static final String DOCKERFILE_PATH = "./Dockerfile";
    private static final String DOCKER_COMPOSE_FILE_PATH = "./docker-compose.yml";

    @Mock
    private DockerProcess dockerProcessMock;

    @InjectMocks
    private DockerManager dockerManager;

    @AfterAll
    @DisplayName("Clean up Dockerfile and docker-compose.yml")
    static void afterAll() throws IOException {
        Files.deleteIfExists(Paths.get(DOCKERFILE_PATH));
        Files.deleteIfExists(Paths.get(DOCKER_COMPOSE_FILE_PATH));
    }

    @Test
    @DisplayName("Test setupDocker success")
    void testSetupDockerSuccess(List<ILoggingEvent> logs) throws IOException {
       DockerManager dockerManagerSpy = spy(new DockerManager());
       doNothing().when(dockerManagerSpy).createDockerfile();

       dockerManagerSpy.setupDocker();

       assertTrue(logs
               .stream()
               .anyMatch(
                       event -> event.getFormattedMessage().contains("Dockerfile created successfully.")),
               "Expected success log not found");

       assertTrue(logs.stream().anyMatch(event -> event.getFormattedMessage().contains("Use 'buildcli --docker-build")),
               "Expected build instruction log not found");
    }

    @Test
    @DisplayName("Test setupDocker failure")
    void testSetupDockerFailure(List<ILoggingEvent> logs) throws IOException {
       DockerManager dockerManagerSpy = spy(new DockerManager());
       doThrow(new IOException()).when(dockerManagerSpy).createDockerfile();

       dockerManagerSpy.setupDocker();

       assertTrue(logs
               .stream()
               .anyMatch(
                       event -> event.getFormattedMessage().contains("Error: Could not setup Docker environment.")),
               "Expected error log not found");
    }

    @Test
    @DisplayName("Test createDockerfile success")
    void testCreateDockerfileSuccess(List<ILoggingEvent> logs) throws IOException {
        DockerManager dockerManagerSpy = spy(new DockerManager());

        Files.deleteIfExists(Paths.get(DOCKERFILE_PATH));

        dockerManagerSpy.createDockerfile();

        assertTrue(Files.exists(Paths.get(DOCKERFILE_PATH)), "Dockerfile should exist");
        assertTrue(logs.stream().anyMatch(event -> event.getFormattedMessage().equals("Dockerfile generated.")), "Expected exists log not found");
    }

    @Test
    @DisplayName("Test createDockerfile failure (Dockerfile already exists)")
    void testCreateDockerfileFailure(List<ILoggingEvent> logs) throws IOException {
        DockerManager dockerManagerSpy = spy(new DockerManager());

        if(!Files.exists(Paths.get(DOCKERFILE_PATH))){
            Files.createFile(Paths.get(DOCKERFILE_PATH));
        }
        dockerManagerSpy.createDockerfile();
        assertTrue(logs
                .stream()
                .anyMatch(
                        event -> event.getFormattedMessage().contains("Dockerfile already exists.")),
                "Expected error log not found");
    }

    @Test
    @DisplayName("Should throw DockerfileNotFoundException when Dockerfile does not exist")
    void shouldThrowExceptionWhenDockerfileDoesNotExist() {

        DockerComposeConfig configMock = mock(DockerComposeConfig.class);
        when(configMock.dockerFilePath()).thenReturn("non_existent_Dockerfile");

        DockerException exception = assertThrows(DockerfileNotFoundException.class, () -> DockerManager.setupDockerCompose(configMock));

        assertTrue(exception.getMessage().contains("Dockerfile not found:"), "Expected exception message not found");
    }

    @Test
    @DisplayName("Test buildContent success")
    void testBuildContentUsesDefaultValues() {
        DockerComposeConfig config = new DockerComposeConfig(
                List.of(), // Ports
                List.of(), // Volumes
                "",        // CPU
                "",        // Memory
                ""         // Dockerfile Path
        );

        String content = DockerManager.buildContent(config);

        assertTrue(content.contains("dockerfile: ./Dockerfile"));
        assertTrue(content.contains("8080:8080"), "Default port should be 8080:8080");
        assertTrue(content.contains("app_data_volume:/app/data"), "Should create default volume mapping");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3000:3000", "5000:5000"})
    @DisplayName("Should correctly format ports section when valid ports are provided")
    void testAppendPortsWithValidPorts(String port) {
        StringBuilder content = new StringBuilder();
        DockerManager.appendPorts(content, List.of(port));

        assertTrue(content.toString().contains(port));
    }

    @Test
    @DisplayName("Should correctly format volumes section when valid volumes are provided")
    void testAppendVolumesWithValidVolumes() {
        StringBuilder content = new StringBuilder();
        DockerManager.appendVolumes(content, List.of("/data:/app/data", "/logs:/app/logs"));

        StringBuilder expected = new StringBuilder();
        expected.append("    volumes:\n");
        expected.append("      - /data:/app/data\n");
        expected.append("      - /logs:/app/logs\n");

        assertTrue(content.toString().contains(expected));
    }

    @Test
    @DisplayName("Should add default volume when no volumes are provided")
    void testAppendVolumesWithEmptyList() {
        StringBuilder content = new StringBuilder();
        DockerManager.appendVolumes(content, List.of());

        assertTrue(content.toString().contains("app_data_volume:/app/data"));
    }

    @Test
    @DisplayName("Should correctly format CPU and memory limits when values are provided")
    void testAppendResourceLimitsWithValidValues() {
        StringBuilder content = new StringBuilder();
        DockerManager.appendResourceLimits(content, "2.0", "1024m");

        assertTrue(content.toString().contains("cpu_shares: 2048"));
        assertTrue(content.toString().contains("mem_limit: 1024m"));
    }

    @Test
    @DisplayName("Should not append CPU and memory limits when values are null")
    void testAppendResourceLimitsWithNullValues() {
        StringBuilder content = new StringBuilder();
        DockerManager.appendResourceLimits(content, null, null);

        assertFalse(content.toString().contains("cpu_shares"));
        assertFalse(content.toString().contains("mem_limit"));
    }

    @Test
    @DisplayName("Test upContainer success - Start containers")
    void testUpContainerSuccess(List<ILoggingEvent> logs) throws DockerException, IOException {

        try (MockedStatic<DockerProcess> dockerProcessMockedStatic = Mockito.mockStatic(DockerProcess.class)) {

            dockerProcessMockedStatic.when(DockerProcess::createInfoProcess)
                    .thenReturn(dockerProcessMock);

            dockerProcessMockedStatic.when(() -> DockerProcess.createProcess(Mockito.any(String[].class)))
                    .thenReturn(dockerProcessMock);

            when(dockerProcessMock.run()).thenReturn(0); // Exit code 0 = Docker Engine is running

            if(!Files.exists(Paths.get("docker-compose.yml"))){
                Files.createFile(Paths.get("docker-compose.yml"));
            }

            dockerManager.upContainer(true);

            // The run() is called twice: one in isDockerEngineNotRunning() and another time in the upContainer()
            verify(dockerProcessMock, times(2)).run();
            assertTrue(logs.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("Successfully started container(s).")));
        }
    }

    @Test
    @DisplayName("Test downContainer failure - Docker Engine not running")
    void testDownContainerFailureEngineNotRunning() {
        dockerManager = spy(dockerManager);
        when(dockerManager.isDockerEngineNotRunning()).thenReturn(false);
        when(dockerManager.getActiveContainers()).thenReturn(List.of(new String[0]));

        DockerException exception = assertThrows(DockerException.NoRunningContainersException.class, () -> dockerManager.downContainer("container1"));

        assertTrue(exception.getMessage().contains("No containers are currently running."), "Expected exception message to contain 'No containers are currently running.'");
    }

    @Test
    @DisplayName("Test downContainer failure - No running containers")
    void testDownContainerFailureNoContainers() {
        dockerManager = spy(dockerManager);
        when(dockerManager.isDockerEngineNotRunning()).thenReturn(false);

        DockerException exception = assertThrows(DockerException.NoRunningContainersException.class, () -> dockerManager.downContainer("container1"));

        assertTrue(exception.getMessage().contains("No containers are currently running."), "Expected exception message to contain 'No containers are currently running.'");
    }

    @Test
    @DisplayName("Test downContainer failure - Container not running")
    void testDownContainerFailureContainerNotRunning(){

        dockerManager = spy(dockerManager);
        when(dockerManager.isDockerEngineNotRunning()).thenReturn(false);
        when(dockerManager.getActiveContainers()).thenReturn(List.of("container1"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dockerManager.downContainer("container2"));

        assertTrue(exception.getMessage().contains("The specified container '" + "container2" + "' is not running." ));
    }

}