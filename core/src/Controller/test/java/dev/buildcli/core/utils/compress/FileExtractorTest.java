package dev.buildcli.core.utils.compress;


import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.Set;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FileExtractorTest {

  private static Path tempDir;
  private static Path tempFileZip;
  private static Path tempFileTarGz;
  private static Path forbiddenZip;

  @BeforeEach
  void setup() throws IOException {
    tempDir = Files.createTempDirectory("output_dir");
    Files.createDirectories(tempDir);

    tempFileZip = Files.createTempFile("test-", ".zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempFileZip))) {
      ZipEntry entry = new ZipEntry("parentDir/subDir/test.txt");
      zos.putNextEntry(entry);
      zos.write("Content test".getBytes());
      zos.closeEntry();
    }

    tempFileTarGz = Files.createTempFile("test-", ".tar.gz");
    try (OutputStream fos = Files.newOutputStream(tempFileTarGz);
         BufferedOutputStream buffered = new BufferedOutputStream(fos);
         GzipCompressorOutputStream compressor = new GzipCompressorOutputStream(buffered);
         TarArchiveOutputStream tarOut = new TarArchiveOutputStream(compressor)) {

      byte[] content = "Content test".getBytes();
      TarArchiveEntry entry = new TarArchiveEntry("test.txt");
      entry.setSize(content.length);
      tarOut.putArchiveEntry(entry);
      tarOut.write(content);
      tarOut.closeArchiveEntry();
    }

    forbiddenZip = Files.createTempFile("test-forbidden-", ".zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(forbiddenZip))) {
      ZipEntry entry = new ZipEntry("test.txt");
      zos.putNextEntry(entry);
      zos.write("Prohibited content".getBytes());
      zos.closeEntry();
    }

    try {
      Set<PosixFilePermission> perms = Files.getPosixFilePermissions(forbiddenZip);
      perms.remove(PosixFilePermission.OWNER_READ);
      Files.setPosixFilePermissions(forbiddenZip, perms);
    } catch (UnsupportedOperationException e) {
      assumeTrue(false, "Test skipped: POSIX permissions not supported");
    }

  }

  @AfterEach
  void cleanup() throws IOException {
    Files.walk(tempDir)
        .sorted(Comparator.reverseOrder())
        .forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

    Files.deleteIfExists(tempFileZip);
    Files.deleteIfExists(tempFileTarGz);
    Files.deleteIfExists(forbiddenZip);
  }

  @Test
  void testExtractZipFile() throws IOException {
    FileExtractor.extractFile(tempFileZip.toString(), tempDir.toString());

    Path extractedFile = tempDir.resolve("parentDir/subDir/test.txt");

    Path extractedDir = tempDir.resolve("parentDir");
    assertTrue(Files.exists(extractedDir), "Directory 'parentDir' was not extracted");

    Path extractedSubDir = extractedDir.resolve("subDir");
    assertTrue(Files.exists(extractedSubDir), "Subdirectory 'subDir' was not extracted");


    byte[] expectedContent = "Content test".getBytes();
    byte[] actualContent = Files.readAllBytes(extractedFile);

    assertTrue(Files.exists(extractedFile), "The file was not extracted correctly");
    assertArrayEquals(expectedContent, actualContent);
  }

  @Test
  void testExtractTarGzFile() throws IOException {
    FileExtractor.extractFile(tempFileTarGz.toString(), tempDir.toString());

    Path extractedFile = tempDir.resolve("test.txt");

    byte[] expectedContent = "Content test".getBytes();
    byte[] actualContent = Files.readAllBytes(extractedFile);


    assertTrue(Files.exists(extractedFile), "The file was not extracted correctly");
    assertArrayEquals(expectedContent, actualContent);
  }


  @Test
  void testExtractFileUnsupportedFormat() {
    String filePath = "test.dock";
    String extractTo = tempDir.toString();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      FileExtractor.extractFile(filePath, extractTo);
    });

    assertEquals("Archive format unsupported. Only use .zip or .tar.gz.", exception.getMessage());
  }




  @Test
  void testExtractFileWithoutReadPermission() throws IOException {

    Exception ex = assertThrows(IOException.class, () -> {
      FileExtractor.extractFile(forbiddenZip.toString(), tempDir.toString());
    });

    assertTrue(ex.getMessage().contains("Permission denied") ||
            ex.getMessage().contains("access denied"));

  }
}
