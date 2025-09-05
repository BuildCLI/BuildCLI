package dev.buildcli.cli.commands.project;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.buildcli.cli.utilsfortest.LogbackExtension;
import dev.buildcli.cli.utilsfortest.LogbackLogger;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InitCommand Tests")
@LogbackLogger(InitCommand.class)


@ExtendWith({MockitoExtension.class, LogbackExtension.class})
class InitCommandTest {

    @TempDir
    private File tempDir;
    private InitCommand initCommand;

    @Test
    void executeCreatesCorrectly(@TempDir File tempDirFile) {
        
        tempDir = new File(tempDir, "StartDir");
        initCommand = new InitCommand();
        new CommandLine(initCommand).execute("-n", "TestRootDir", "-j", "17", "-t", "false");

        
        

    }

}
