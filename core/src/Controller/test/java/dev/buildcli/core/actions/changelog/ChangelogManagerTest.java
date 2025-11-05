package dev.buildcli.core.actions.changelog;

import dev.buildcli.core.domain.git.GitOperations;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChangelogManagerTest {

    private File tempRepo;
    private Git git;
    private GitOperations gitOperations;
    private ChangelogManager changelogManager;
    private CommandSpec spec;

    @BeforeEach
    void setUp() throws GitAPIException, IOException {
        CommandLine commandLine = new CommandLine(new DummyCommand());
        spec = commandLine.getCommandSpec();
        tempRepo = Files.createTempDirectory("testRepo").toFile();
        String repositoryDir = tempRepo.getAbsolutePath();

        changelogManager = new ChangelogManager(spec, repositoryDir);
        git = Git.init().setDirectory(tempRepo).call();
        gitOperations = new GitOperations();
    }

    @AfterEach
    void tearDown() throws IOException {
        git.close();
        deleteDirectory(tempRepo);
    }

    private void makeCommit(String message) throws GitAPIException, IOException {
        File newFile = new File(tempRepo, UUID.randomUUID().toString() + ".txt");
        Files.writeString(newFile.toPath(), "Test content");
        git.add().addFilepattern(".").call();
        git.commit().setMessage(message).call();
    }

    private void deleteDirectory(File file) {
        if (file.isDirectory()) {
            for (File sub : file.listFiles()) {
                deleteDirectory(sub);
            }
        }
        file.delete();
    }

    @Test
    void testGenerateChangelog_withValidInputs() throws IOException, GitAPIException {
        makeCommit("chore: initial commit");
        makeCommit("feat(api): add new endpoint");
        makeCommit("fix(auth): resolve login issue");

        String version = "v1.0.0";
        String outputFile = "changelog.md";
        String format = "markdown";
        List<String> includeTypes = Arrays.asList("feat", "fix");

        changelogManager.generateChangelog(version, outputFile, format, includeTypes);

        File output = new File(outputFile);
        assertTrue(output.exists(), "Output file should exist.");
        assertTrue(output.length() > 0, "Output file should not be empty.");
    }

    // Test 2: Generate changelog with null version (use latest tag)
    @Test
    void testGenerateChangelog_withNullVersion() throws IOException, GitAPIException {
        makeCommit("chore: initial commit");
        makeCommit("feat(api): add new endpoint");
        makeCommit("fix(auth): resolve login issue");

        String version = null;  // Version is null
        String outputFile = "changelog.md";
        String format = "markdown";
        List<String> includeTypes = Arrays.asList("feat", "fix");

        changelogManager.generateChangelog(version, outputFile, format, includeTypes);

        File output = new File(outputFile);
        assertTrue(output.exists(), "Output file should exist.");
        assertTrue(output.length() > 0, "Output file should not be empty.");
    }

    @Test
    void testGenerateChangelog_withInvalidRepoPath() {
        String version = "v1.0.0";
        String outputFile = "changelog.md";
        String format = "markdown";
        List<String> includeTypes = Arrays.asList("feat", "fix");
        String repositoryDir = "/invalid/path/to/repo";

        ChangelogManager invalidRepoChangelogManager = new ChangelogManager(spec, repositoryDir);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            invalidRepoChangelogManager.generateChangelog(version, outputFile, format, includeTypes);
        });

        assertTrue(exception.getMessage().contains("repository not found"));
    }

    // Dummy Command class to satisfy Picocli
    @CommandLine.Command
    static class DummyCommand {}
}
