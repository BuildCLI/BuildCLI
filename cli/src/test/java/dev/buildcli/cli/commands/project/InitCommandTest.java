package dev.buildcli.cli.commands.project;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.buildcli.cli.utilsfortest.LogbackExtension;
import dev.buildcli.cli.utilsfortest.LogbackLogger;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InitCommand Tests")
@LogbackLogger(InitCommand.class)


@ExtendWith({MockitoExtension.class, LogbackExtension.class})
class InitCommandTest {

    private File tempDir;
    
    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("startDir").toFile();
    }

    @AfterEach
    void tearDown() {
        for (File file : tempDir.listFiles()) {
            file.delete();
        }
        tempDir.delete();
    }

    @Test
    void executeCreatesCorrectly() {
        String[] args = {"-n", "testDirName"};
        int exitCode = new CommandLine(new GenerateCommand()).execute(args);
    }

}
