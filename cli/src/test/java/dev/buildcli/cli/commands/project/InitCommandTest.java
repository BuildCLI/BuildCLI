package dev.buildcli.cli.commands.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.buildcli.core.utils.console.input.InteractiveInputUtils;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@DisplayName("InitCommand Tests")

@ExtendWith({MockitoExtension.class})
class InitCommandTest {

    @Test
    void executeCreatesCorrectly(@TempDir Path tempDir) throws IOException {
        
        // Create mocks
        MockedStatic<Paths> mockPaths = mockStatic(Paths.class);
        MockedStatic<InteractiveInputUtils> mockInputUtils = mockStatic(InteractiveInputUtils.class);
        mockPaths.when(() -> Paths.get("")).thenReturn(tempDir);
        mockInputUtils.when(() -> InteractiveInputUtils.question("Enter base-package")).thenReturn("TestBasePackage");
        

        // Setup
        File expectedDir = tempDir.resolve("TestRootDir").toFile();
        String[] expectedFiles = { "pom.xml", "README.md" };
        String[] expectedDirs = { "src/main/java/TestBasePackage", "src/main/resources", "src/test/java/TestBasePackage" };
       
        // Run init
        InitCommand initCommand = new InitCommand();
        new CommandLine(initCommand).execute("-n", "TestRootDir", "-j", "17");
        
        // Assertions
        assertTrue(expectedDir.exists() && expectedDir.isDirectory());
        for (String FileName : expectedFiles) {
            File file = new File(expectedDir, FileName);
            assertTrue(file.exists() && file.isFile(), "File " + FileName + " was not created.");
        }
        for (String DirName : expectedDirs) {
            File file = new File(DirName);
            assertTrue(file.exists() && file.isDirectory(), "Directory " + DirName + " was not created");
        }
    }
}
