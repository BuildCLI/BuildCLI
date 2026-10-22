package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.infrastructure.JGitAccess;
import dev.buildcli.ports.GitAccess.Scope;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Git is done in the process by JGit; these pin what it has to do the way the git program did. */
class JGitAccessTest {
    @TempDir Path ws;
    final JGitAccess git = new JGitAccess();
    Git repo;

    @BeforeEach
    void init() throws Exception {
        repo = Git.init().setDirectory(ws.toFile()).setInitialBranch("main").call();
        var config = repo.getRepository().getConfig();
        config.setString("user", null, "name", "Test");
        config.setString("user", null, "email", "test@example.com");
        config.setBoolean("commit", null, "gpgsign", false);
        config.save();
    }

    @AfterEach
    void close() {
        repo.close();
    }

    void write(String rel, String content) throws IOException {
        Path f = ws.resolve(rel);
        Files.createDirectories(f.getParent());
        Files.writeString(f, content);
    }

    void commitAll(String message) throws Exception {
        repo.add().addFilepattern(".").call();
        repo.commit().setMessage(message).call();
    }

    @Test
    void aFolderThatIsNotARepositoryIsAnErrorThatSaysSo(@TempDir Path elsewhere) {
        assertTrue(assertThrows(IOException.class, () -> git.status(elsewhere, true, List.of())).getMessage().contains("not a git repository"));
    }

    @Test
    void statusIsTheShortFormWithTheBranchFirst() throws Exception {
        write("kept.txt", "k\n");
        write("gone.txt", "g\n");
        commitAll("base");
        write("kept.txt", "k\nchanged\n");
        Files.delete(ws.resolve("gone.txt"));
        write("new.txt", "n\n");
        repo.add().addFilepattern("new.txt").call();
        write("loose.txt", "l\n");

        List<String> lines = git.status(ws, true, List.of()).lines().toList();
        assertEquals("## main", lines.get(0));
        assertTrue(lines.contains(" M kept.txt"), lines.toString());
        assertTrue(lines.contains(" D gone.txt"), lines.toString());
        assertTrue(lines.contains("A  new.txt"), lines.toString());
        assertTrue(lines.contains("?? loose.txt"), lines.toString());
    }

    @Test
    void aRepositoryWithoutCommitsSaysSo() throws Exception {
        assertEquals("## No commits yet on main\n", git.status(ws, true, List.of()));
    }

    @Test
    void theDiffOfTheWorkingTreeLeavesOutStagedChangesAndNewFiles() throws Exception {
        write("a.txt", "one\n");
        commitAll("base");
        write("a.txt", "one\ntwo\n");
        write("untracked.txt", "never shown\n");
        String diff = git.diff(ws, Scope.WORKTREE, List.of());
        assertTrue(diff.contains("+two"), diff);
        assertFalse(diff.contains("never shown"), diff);

        repo.add().addFilepattern("a.txt").call();
        assertEquals("", git.diff(ws, Scope.WORKTREE, List.of()), "staged changes are not in the plain diff");
        assertTrue(git.diff(ws, Scope.STAGED, List.of()).contains("+two"));
        assertTrue(git.diff(ws, Scope.HEAD, List.of("a.txt")).contains("+two"));
    }

    @Test
    void aDiffCanBeLimitedToOnePath() throws Exception {
        write("a.txt", "a\n");
        write("b.txt", "b\n");
        commitAll("base");
        write("a.txt", "a\nA\n");
        write("b.txt", "b\nB\n");
        String diff = git.diff(ws, Scope.WORKTREE, List.of("b.txt"));
        assertTrue(diff.contains("+B") && !diff.contains("+A"), diff);
    }

    @Test
    void theLogIsOneLinePerCommitNewestFirstWithNamesWhenAsked() throws Exception {
        write("a.txt", "a\n");
        commitAll("first");
        write("a.txt", "a\nb\n");
        commitAll("second");
        repo.tag().setName("v1").call();

        List<String> plain = git.log(ws, 20, false).lines().toList();
        assertEquals(2, plain.size());
        assertTrue(plain.get(0).matches("[0-9a-f]{7} second"), plain.toString());
        assertTrue(plain.get(1).endsWith(" first"));
        String decorated = git.log(ws, 20, true).lines().findFirst().orElseThrow();
        assertTrue(decorated.contains("(HEAD -> main, tag: v1) second"), decorated);
        assertEquals(1, git.log(ws, 1, false).lines().count());
    }

    @Test
    void commitTakesOnlyTheNamedPathsAndStagesDeletions() throws Exception {
        write("keep.txt", "k\n");
        write("remove.txt", "r\n");
        write("other.txt", "o\n");
        commitAll("base");
        write("keep.txt", "k\nchanged\n");
        Files.delete(ws.resolve("remove.txt"));
        write("other.txt", "o\nnot committed\n");
        write("fresh.txt", "f\n");

        String made = git.commit(ws, "Update", List.of("keep.txt", "remove.txt", "fresh.txt"));
        assertTrue(made.matches("(?s)\\[main [0-9a-f]{7}] Update.*"), made);
        List<String> status = git.status(ws, false, List.of()).lines().toList();
        assertEquals(List.of(" M other.txt"), status, "what was not named stays as it was");
        assertEquals(List.of("Update", "base"), git.log(ws, 5, false).lines().map(l -> l.substring(8)).toList());
    }

    @Test
    void aPathThatLooksLikeAPatternIsAFileName() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"),
                "Windows does not allow * in a file name");
        write("a.txt", "a\n");
        write("*.txt", "star\n");
        commitAll("base");
        write("a.txt", "a\nchanged\n");
        write("*.txt", "star\nchanged\n");
        git.commit(ws, "Only the star", List.of("*.txt"));
        assertEquals(List.of(" M a.txt"), git.status(ws, false, List.of()).lines().toList());
    }

    @Test
    void committingNothingNewIsAnErrorAndSoIsAnUnknownPath() throws Exception {
        write("a.txt", "a\n");
        commitAll("base");
        assertTrue(assertThrows(IOException.class, () -> git.commit(ws, "again", List.of("a.txt"))).getMessage().contains("nothing to commit"));
        assertThrows(IOException.class, () -> git.commit(ws, "ghost", List.of("missing.txt")));
    }

    @Test
    void aFailingCommitHookRefusesTheCommit() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"),
                "hooks are shell scripts");
        write("a.txt", "a\n");
        commitAll("base");
        write("a.txt", "a\nb\n");
        Path hook = repo.getRepository().getDirectory().toPath().resolve("hooks/pre-commit");
        Files.createDirectories(hook.getParent());
        Files.writeString(hook, "#!/bin/sh\necho 'lint failed'\nexit 1\n");
        hook.toFile().setExecutable(true);
        String message = assertThrows(IOException.class, () -> git.commit(ws, "blocked", List.of("a.txt"))).getMessage();
        assertTrue(message.contains("hook"), message);
        assertEquals(1, git.log(ws, 5, false).lines().count());
    }

    @Test
    void aWorkspaceInsideTheRepositoryFindsTheRepositoryAndNamesPathsFromItself() throws Exception {
        write("sub/a.txt", "a\n");
        commitAll("base");
        write("sub/a.txt", "a\nchanged\n");
        Path sub = ws.resolve("sub");
        assertTrue(git.diff(sub, Scope.WORKTREE, List.of("a.txt")).contains("+changed"));
        git.commit(sub, "From a subfolder", List.of("a.txt"));
        assertEquals("From a subfolder", git.log(sub, 1, false).strip().substring(8));
    }
}
