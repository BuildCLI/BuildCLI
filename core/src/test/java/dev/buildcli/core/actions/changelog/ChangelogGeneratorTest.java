package dev.buildcli.core.actions.changelog;

import dev.buildcli.core.domain.git.GitOperations;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ChangelogGeneratorTest {
    private File tempRepo;
    private Git git;
    private GitOperations gitOperations;

    @BeforeEach
    void setUp() throws GitAPIException, IOException {
        tempRepo = Files.createTempDirectory("testRepo").toFile();
        git = Git.init().setDirectory(tempRepo).call();
        gitOperations = new GitOperations();

        // Make sure the repository has a valid HEAD
        if (git.getRepository().getFullBranch().equals("refs/heads/master")) {
            // Initial commit to avoid NoHeadException
            makeCommit("chore: initial commit");
        }

        makeCommit("feat(api): add new endpoint");
        makeCommit("fix(auth): resolve login issue");
    }


    @AfterEach
    void tearDown() {
        git.close();
        deleteDirectory(tempRepo);
    }

    @Test
    void testGenerateChangeLogContents_withValidCommits() throws IOException, GitAPIException {
        List<String> includeTypes = Arrays.asList("feat", "fix");

        String result = ChangelogGenerator.generateChangeLogContents(
                gitOperations, "v1.0.0", tempRepo.getAbsolutePath(), includeTypes, "markdown");

        assertTrue(result.contains("feat(api): add new endpoint"));
        assertTrue(result.contains("fix(auth): resolve login issue"));
    }

    private void makeCommit(String message) throws GitAPIException, IOException {
        File newFile = new File(tempRepo, UUID.randomUUID().toString() + ".txt");
        Files.writeString(newFile.toPath(), "Test content\n");
        git.add().addFilepattern(".").call();
        git.commit()
           .setMessage(message)
           .setAllowEmpty(false)
           .setNoVerify(true)
           .call();
    }

    private void deleteDirectory(File file) {
        if (file.isDirectory()) {
            for (File sub : Objects.requireNonNull(file.listFiles())) {
                deleteDirectory(sub);
            }
        }
        file.delete();
    }
}
