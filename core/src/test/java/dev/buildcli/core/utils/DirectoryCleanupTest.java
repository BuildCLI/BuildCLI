package dev.buildcli.core.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class DirectoryCleanupTest {

  @TempDir
  Path tempDir;

  @Test
  void cleanup_shouldDeleteAllFilesAndDirectories() throws IOException {
    createSampleDirectoryStructure(tempDir);

    assertTrue(Files.exists(tempDir.resolve("file1.txt")));
    assertTrue(Files.exists(tempDir.resolve("subDir/file2.txt")));
    assertTrue(Files.exists(tempDir.resolve("subDir")));

    DirectoryCleanup.cleanup(tempDir.toString());

    assertFalse(Files.exists(tempDir.resolve("file1.txt")));
    assertFalse(Files.exists(tempDir.resolve("subDir/file2.txt")));
    assertFalse(Files.exists(tempDir.resolve("subDir")));
    assertFalse(Files.exists(tempDir));
  }

  @Test
  void cleanup_shouldPrintErrorWhenDirectoryDoesNotExist() throws IOException {
    PrintStream originalOut = System.out;
    try (ByteArrayOutputStream outContent = new ByteArrayOutputStream()){
      System.setOut(new PrintStream(outContent));

      DirectoryCleanup.cleanup("non_existent_directory");

      String output = outContent.toString();

      assertTrue(output.contains("The 'non_existent_directory'" +
          " directory does not exist."));
    } finally {
      System.setOut(originalOut);
    }
  }

  @Test
  void cleanup_shouldPrintErrorWhenIOExceptionOccurs() throws IOException {
    PrintStream originalOut = System.out;
    Path mockPath = Path.of("some/path/");

    try (
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        MockedStatic<Files> mockedFiles = mockStatic(Files.class)
    ) {
      System.setOut(new PrintStream(outContent));

      mockedFiles.when(() -> Files.exists(eq(mockPath))).thenReturn(true);
      mockedFiles.when(() -> Files.walkFileTree(eq(mockPath), any())).thenThrow(new IOException("Simulated error"));

      DirectoryCleanup.cleanup(mockPath.toString());

      String output = outContent.toString();
      assertTrue(output.contains("Error clearing 'some/path' directory: Simulated error"));
    } finally {
      System.setOut(originalOut);
    }
  }

  private void createSampleDirectoryStructure(Path rootDir) throws IOException {
    Path subDir = Files.createDirectory(rootDir.resolve("subDir"));
    Files.createFile(rootDir.resolve("file1.txt"));
    Files.createFile(subDir.resolve("file2.txt"));
  }
}
