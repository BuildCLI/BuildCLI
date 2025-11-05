package dev.buildcli.core.actions.changelog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChangelogFileUtilsTest {

    private ChangelogFileUtils changelogFileUtils;

    @BeforeEach
    public void setup() {
        this.changelogFileUtils = new ChangelogFileUtils();
    }

    @Test
    void testFormatOutputFile_withValidFileName() {
        String result = ChangelogFileUtils.formatOutputFile("report", "markdown");
        assertEquals("report.md", result);
    }

    @Test
    void testFormatOutputFile_withFileNameHavingExtension() {
        String result = ChangelogFileUtils.formatOutputFile("report.doc", "json");
        assertEquals("report.json", result);
    }

    @Test
    void testFormatOutputFile_withNullFileName() {
        String result = ChangelogFileUtils.formatOutputFile(null, "markdown");
        assertEquals("CHANGELOG.md", result);
    }

    @Test
    void testFormatOutputFile_withBlankFileName() {
        String result = ChangelogFileUtils.formatOutputFile(" ", "html");
        assertEquals("CHANGELOG.html", result);
    }

    @Test
    void testWriteToFile_createsFileWithContent() throws IOException {
        String content = "Hello, World!";
        Path tempFile = Files.createTempFile("testFile", ".txt");
        String filePath = tempFile.toString();

        ChangelogFileUtils.writeToFile(content, filePath);

        String fileContent = Files.readString(tempFile);
        assertEquals(content, fileContent);

        Files.deleteIfExists(tempFile);
    }
}
