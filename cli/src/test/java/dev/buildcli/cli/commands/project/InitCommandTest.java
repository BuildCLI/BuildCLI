package dev.buildcli.cli.commands.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InitCommand Tests")


@ExtendWith({MockitoExtension.class})
class InitCommandTest {

    @Test
    void executeCreatesCorrectly(@TempDir Path tempDir) throws IOException {
        
        // Setup
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());
        File expectedDir = new File(tempDir.toFile(), "TestRootDir");
        String[] expectedFiles = { "pom.xml", "README.md", "Main.java" };
       
        // Run init
        InitCommand initCommand = new InitCommand();
        new CommandLine(initCommand).execute("-n", "TestRootDir", "-j", "17", "-t", "false");
        
        // Assertions
        assertTrue(expectedDir.exists() && expectedDir.isDirectory());
        for (String FileName : expectedFiles) {
            File file = new File(expectedDir, FileName);
            assertTrue(file.exists() && file.isFile(), "File " + FileName + " was not created.");
        }
    }
}
