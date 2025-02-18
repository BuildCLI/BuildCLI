package org.buildcli.commands;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


class ChangelogCommandTest {

    private ChangelogCommand command;

    @TempDir
    Path tempDir;

    private Git git;

    private File file;

    private Map<String, Map<String, List<String>>> data;

    private Map<String, List<String>> sections;


    @BeforeEach
    public void setUp() throws Exception {
        // It initializes a real git repository in the temporary directory to isolate tests
        git   = Git.init().setDirectory(tempDir.toFile()).call();
        file  = new File(tempDir.toFile(), "test.txt");
        file.createNewFile();

        data     = new LinkedHashMap<>();
        sections = new LinkedHashMap<>();

        command  = new ChangelogCommand();

        setPrivateField(command, "version", "1.0.0");
        setPrivateField(command, "format", "markdown");
        setPrivateField(command, "outputFile", tempDir.resolve("CHANGELOG.md").toString());
        setPrivateField(command, "includeTypes", List.of("feat", "fix"));
        setPrivateField(command, "repositoryDir", tempDir.toFile());
    }

    @Test
    @DisplayName("Format output file with extension")
    void testFormatOutputFileWithExtension() {
        String result = command.formatOutputFile("teste.md", "markdown");
        assertEquals("teste.md", result, "Expected file name with markdown extension remains unchanged");
    }

    @Test
    @DisplayName("Format output file without extension for json format")
    void testFormatOutputFileWithoutExtension() {
        String result = command.formatOutputFile("teste", "json");
        assertEquals("teste.json", result, "Expected output file name should have .json extension");
    }

    @Test
    @DisplayName("Generate output markdown")
    void testGenerateOutputMarkdown() {

        sections.put("Added", List.of("- feat: new feature"));
        data.put("1.0.0", sections);

        String result = command.generateOutput(data, "markdown");
        assertAll("Markdown output",
                () -> assertTrue(result.contains("## [1.0.0]"), "Should contain version header"),
                () -> assertTrue(result.contains("### Added"), "Should contain section header 'Added'"),
                () -> assertTrue(result.contains("- feat: new feature"), "Should contain commit message")
        );
    }

    @Test
    @DisplayName("Generate output HTML")
    void testGenerateOutputHtml() {

        sections.put("Added", List.of("- feat: new feature"));
        data.put("1.0.0", sections);

        String result = command.generateOutput(data, "html");
        assertAll("HTML output",
                () -> assertTrue(result.contains("<h2>[1.0.0]"), "Should contain version header"),
                () -> assertTrue(result.contains("<h3>Added</h3>"), "Should contain section header 'Added'"),
                () -> assertTrue(result.contains("<li>- feat: new feature</li>"), "Should contain commit message in list item")
        );
    }

    @Test
    @DisplayName("Generate output JSON")
    void testGenerateOutputJson() {

        sections.put("Added", List.of("- feat: new feature"));
        data.put("1.0.0", sections);

        String result = command.generateOutput(data, "json");
        assertAll("JSON output",
                () -> assertTrue(result.contains("\"version\": \"1.0.0\""), "Should contain version field"),
                () -> assertTrue(result.contains("\"Added\": ["), "Should contain 'Added' section"),
                () -> assertTrue(result.contains("\"- feat: new feature\""), "Should contain commit message")
        );
    }

    @Test
    @DisplayName("Generate ChangeLog should include feat commit in Added section")
    void generateChangeLog_shouldIncludeFeatCommitInAddedSection() throws Exception {
        file = new File(tempDir.toFile(), "test.md");
        git  = Git.open(tempDir.toFile());
        git.add().addFilepattern("test.md").call();
        git.commit().setMessage("feat(core): add new feature").call();

        String outputFile = tempDir.resolve("CHANGELOG.md").toString();
        command.generateChangeLog("1.0.0", outputFile, List.of("all"));

        String content = Files.readString(tempDir.resolve("CHANGELOG.md"));
        assertAll("Generated changelog",
                () -> assertTrue(content.contains("### Added"), "Should contain 'Added' section"),
                () -> assertTrue(content.contains("All notable changes to this project will be documented in this file."),
                        "Should contain changelog description")
        );
    }

    @Test
    @DisplayName("Run Command Fails with Invalid Output Path and Missing Version")
    void run_shouldFailWithInvalidOutputPathAndMissingVersion() throws Exception {

        git.add().addFilepattern("test.txt").call();
        git.commit().setMessage("Initial commit").call();

        CommandLine cmd = new CommandLine(command);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        setPrivateField(command, "outputFile", "/invalid/path/CHANGELOG.md");
        setPrivateField(command, "version", null);

        Exception exception = assertThrows(RuntimeException.class, () -> command.run());
        assertTrue(exception.getCause() instanceof FileNotFoundException, "The cause should be FileNotFoundException");
        assertTrue(sw.toString().contains("Failed to generate changelog."), "The error message must be present in the output");

    }

    @Test
    @DisplayName("Run Command Uses Unreleased When No Version or Tags")
    void run_shouldUseUnreleasedWhenNoVersionOrTags() throws Exception {

        git.add().addFilepattern("test.txt").call();
        git.commit().setMessage("Initial commit").call();

        CommandLine cmd = new CommandLine(command);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        setPrivateField(command, "version", null);

        command.run();

        assertTrue(sw.toString().contains("Changelog generated successfully."), "Success message should be present");
        String changelogContent = Files.readString(tempDir.resolve("CHANGELOG.md"));
        assertTrue(changelogContent.contains("## [Unreleased]"), "Changelog should use 'Unreleased' when no version or tags are present");
    }

    @Test
    @DisplayName("getLatestGitTag returns latest tag when repository has tag")
    void testGetLatestGitTagWithTags() throws Exception {

        Files.writeString(file.toPath(), "test content");

        git.add().addFilepattern("test.txt").call();
        git.commit().setMessage("Initial Commit").call();

        git.tag().setName("v1.0.0").call();
        git.tag().setName("v1.0.1").call();

        Optional<String> latestTag = command.getLatestGitTag();
        assertTrue(latestTag.isPresent(), "Latest tag should be present");
        assertEquals("v1.0.1", latestTag.get(), "Latest tag should be 'v1.0.1'");

    }

    @Test
    @DisplayName("getLatestGitTag returns empty Optional when repository has no tags")
    void testGetLatestGitTagNoTags() throws Exception {

        Files.writeString(file.toPath(), "test content");
        git.add().addFilepattern("test.txt").call();
        git.commit().setMessage("Initial commit").call();

        Optional<String> latestTag = command.getLatestGitTag();
        assertTrue(latestTag.isEmpty(), "Expected no tags in the repository");
    }

    @Test
    @DisplayName("writeToFile writes content to file successfully")
    void testWriteToFileSucess() throws IOException {
        File outputFile = tempDir.resolve("output.txt").toFile();
        String content  = "File writing test";

        command.writeToFile(content, outputFile.getAbsolutePath());

        String readContent = Files.readString(outputFile.toPath());
        assertEquals(content, readContent, "The file content should match the written content");
    }

    @Test
    @DisplayName("writeFile throws IOException for invalid file path")
    void testWriteToFileInvalidPath(){
        String content     = "Test Content";
        String invalidPath = "/invalid/path/output.txt";

        assertThrows(IOException.class, () -> command.writeToFile(content, invalidPath),
                "writeToFile should throw IOException for an invalid output path");
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}