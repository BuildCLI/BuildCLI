package dev.buildcli.core.utils.filesystem;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FileSearchTest {
  private static Path tempDir;

  @BeforeAll
  static void setUp() throws IOException {
    tempDir = Files.createTempDirectory("fileSearchTest");
    // Diverse files
    createFile(tempDir.resolve("file1.txt"));
    createFile(tempDir.resolve("file2.csv"));
    createDirectories(tempDir.resolve("subdir"));
    createFile(tempDir.resolve("subdir/file3.txt"));
    createDirectories(tempDir.resolve("emptydir"));

    // Code
    createFile(tempDir.resolve("file4.java"));
    createFile(tempDir.resolve("file5.kt"));
    createFile(tempDir.resolve("file6.scala"));
    createFile(tempDir.resolve("file7.go"));
    createFile(tempDir.resolve("file8.tsx"));
    createFile(tempDir.resolve("file9.js"));
    createFile(tempDir.resolve("file_groovy.groovy"));
    createFile(tempDir.resolve("first_jar.jar"));
    createFile(tempDir.resolve("second_jar.jar"));
  }

  @AfterAll
  static void tearDown() throws IOException {
    try (var files = Files.walk(tempDir)) {
      files.sorted(Comparator.reverseOrder())
        .forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (IOException e) {
            throw new RuntimeException("Error cleaning temp file: " + p, e);
          }
        });
    }
  }

  @CsvSource({
    // searchParam, expectedSize, expectedFiles
    "'.txt', 2, 'file1.txt,file3.txt'",
    "'.csv', 1, 'file2.csv'",
    "'.notfound', 0, ''",
    "'empty_dir', 0, ''",
    "'inexistent', 0, ''"
  })
  @ParameterizedTest(name = "Should find {1} files when searching {0}")
  @DisplayName("Should find files when searching")
  void testSearch(String searchParam, int expectedSize, String expectedFiles) {
    File directory = tempDir.resolve(searchParam).toFile();
    String extension = searchParam.startsWith(".") ? searchParam : ".txt";
    File searchDir = searchParam.startsWith(".") ? tempDir.toFile() : directory;

    var files = FindFilesUtils.search(searchDir, extension);

    assertEquals(expectedSize, files.size());
    if (!expectedFiles.isEmpty()) {
      assertEquals(expectedFiles, files.stream().map(File::getName).collect(Collectors.joining(",")));
    }
  }

  @Test
  @DisplayName("Should return empty list when searching in a restricted directory")
  void testSearch_ListFilesReturnsNull() throws IOException {
    Path restrictedDir = Files.createTempDirectory("restrictedDir");
    Files.setPosixFilePermissions(restrictedDir, PosixFilePermissions.fromString("---------"));
    var file = restrictedDir.toFile();
    var files = FindFilesUtils.search(file, ".txt");

    assertEquals(0, files.size());
    Files.delete(restrictedDir);
  }

  @Test
  @DisplayName("Should find diverse code files when searching")
  void testSearchCodeFiles() {
    var files = FindFilesUtils.searchCodeFiles(tempDir.toFile());
    var fileNames = files.stream().map(File::getName).sorted().toList();
    assertEquals(5, files.size());

    assertEquals("file4.java", fileNames.get(0));
    assertEquals("file5.kt", fileNames.get(1));
    assertEquals("file6.scala", fileNames.get(2));
    assertEquals("file8.tsx", fileNames.get(3));
    assertEquals("file9.js", fileNames.get(4));

  }

  @Test
  @DisplayName("Should find diverse java files when searching")
  void testSearchJavaFiles() {
    var files = FindFilesUtils.searchJavaFiles(tempDir.toFile());
    var fileNames = files.stream().map(File::getName).sorted().toList();
    assertEquals(4, files.size());

    assertEquals("file4.java", fileNames.get(0));
    assertEquals("file5.kt", fileNames.get(1));
    assertEquals("file6.scala", fileNames.get(2));
    assertEquals("file_groovy.groovy", fileNames.get(3));
  }

  @Test
  @DisplayName("Should find jar files when searching")
  void testSearchJarFiles() {
    var files = FindFilesUtils.searchJarFiles(tempDir.toFile());
    var fileNames = files.stream().map(File::getName).sorted().toList();
    assertEquals(2, files.size());
    assertEquals("first_jar.jar", fileNames.get(0));
    assertEquals("second_jar.jar", fileNames.get(1));
  }

  @Test
  @DisplayName("Should return single file when searching a specific file")
  void testSearchSingleFile() throws IOException {
    var file = Files.createTempFile("by_file", ".txt").toFile();
    var files = FindFilesUtils.search(file, ".txt");

    assertEquals(1, files.size());
    assertEquals(file.getName(), files.getFirst().getName());
    Files.deleteIfExists(file.toPath());
  }

  @Test
  @DisplayName("Should find multiple files when searching multiple extensions")
  void testSearchMultipleExtensions() {
    var files = FindFilesUtils.search(tempDir.toFile(), ".txt", ".csv");
    var fileNames = files.stream().map(File::getName).sorted().toList();
    assertEquals(3, files.size());
    assertEquals("file1.txt", fileNames.get(0));
    assertEquals("file2.csv", fileNames.get(1));
    assertEquals("file3.txt", fileNames.get(2));
  }
}
