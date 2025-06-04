package dev.buildcli.core.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class DirectorYCleanupTest {

  @TempDir
  Path tempDir;
  @Test
  void cleanup_shouldDeleteAllFilesAndDirectories()
      throws IOException {
    Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
    Path file1 = Files.createFile(tempDir.resolve("file1.txt"));
    Path file2 = Files.createFile(subDir.resolve("file2.txt"));

    assertTrue(Files.exists(file1));
    assertTrue(Files.exists(file2));
    assertTrue(Files.exists(subDir));

    DirectoryCleanup.cleanup(tempDir.toString());

    assertFalse(Files.exists(file1));
    assertFalse(Files.exists(file2));
    assertFalse(Files.exists(subDir));
    assertFalse(Files.exists(tempDir));
  }

  @Test
  void cleanup_shouldErroAllFilesAndDirectories() throws IOException {
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
  void cleanup_shouldErroAllFilesAndDirectoriesThrows() throws IOException {
    PrintStream originalOut = System.out;
    Path mockPath = Path.of("algum/caminho/qualquer");
    try (ByteArrayOutputStream outContent = new ByteArrayOutputStream();
         MockedStatic<Files> mockedFiles = mockStatic(Files.class);
         ) {
      System.setOut(new PrintStream(outContent));


      mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
      mockedFiles.when(() -> Files.walkFileTree(eq(mockPath), any())).thenCallRealMethod();

      mockedFiles.when(() -> Files.delete(Mockito.any(Path.class))).thenThrow(IOException.class);


      var ex = assertThrows(IOException.class,() -> DirectoryCleanup.cleanup(mockPath.toString()));
      String output = outContent.toString();

      assertTrue(output.contains("The 'non_existent_directory'" +
          " directory does not exist."));
    } finally {
      System.setOut(originalOut);
    }
  }
}
