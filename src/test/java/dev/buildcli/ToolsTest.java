package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.tools.Diffs;
import dev.buildcli.application.tools.GitCommitTool;
import dev.buildcli.application.tools.GitReadTool;
import dev.buildcli.application.tools.ListFilesTool;
import dev.buildcli.application.tools.SafeEnvironment;
import dev.buildcli.application.tools.SearchTool;
import dev.buildcli.application.tools.Tool;
import dev.buildcli.application.tools.ToolContext;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.domain.Permissions;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.ToolCall;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToolsTest {
    @TempDir Path ws;
    final List<ApprovalRequest> approvals = new ArrayList<>();

    static Agent agent(List<String> readGlobs, String... capabilities) {
        return new Agent("bruno", "developer", "", Set.of(capabilities),
                new Permissions(readGlobs, List.of(), List.of(), Duration.ofSeconds(30)));
    }

    ToolContext ctx(boolean approve) {
        return new ToolContext(ws, request -> {
            approvals.add(request);
            return approve;
        });
    }

    static ToolCall call(String name, Map<String, Object> args) {
        return new ToolCall("id", name, args);
    }

    String run(Tool tool, Agent agent, boolean approve, Map<String, Object> args) throws Exception {
        return tool.execute(ctx(approve), agent, call(tool.name(), args));
    }

    void write(String rel, String content) throws IOException {
        Path f = ws.resolve(rel);
        Files.createDirectories(f.getParent());
        Files.writeString(f, content);
    }

    void git(String... args) throws Exception {
        List<String> argv = new ArrayList<>(List.of("git"));
        argv.addAll(List.of(args));
        Process p = new ProcessBuilder(argv).directory(ws.toFile()).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        assertEquals(0, p.waitFor(), "git " + String.join(" ", args) + " failed: " + out);
    }

    void initRepo() throws Exception {
        git("init", "-q");
        git("config", "user.name", "Test");
        git("config", "user.email", "test@example.com");
        git("config", "commit.gpgsign", "false");
    }

    // ---- diffs ----

    @Test
    void aNewFileDiffShowsEveryLineAsAdded() {
        String d = Diffs.unified("a.txt", null, "one\ntwo");
        assertTrue(d.startsWith("--- /dev/null\n+++ b/a.txt"));
        assertTrue(d.contains("+one\n+two"));
    }

    @Test
    void aModificationDiffIsAUnifiedDiffWithContext() {
        String d = Diffs.unified("a.txt", "one\ntwo\nthree\nfour", "one\nTWO\nthree\nfour");
        assertTrue(d.contains("--- a/a.txt") && d.contains("+++ b/a.txt"), d);
        assertTrue(d.contains("@@"), d);
        assertTrue(d.contains("-two") && d.contains("+TWO") && d.contains(" one"), d);
    }

    @Test
    void anIdenticalWriteIsReportedAsNoChange() {
        assertTrue(Diffs.unified("a.txt", "same", "same").startsWith("(no changes"));
    }

    // ---- list_files and search ----

    @Test
    void listFilesMarksDirectoriesAndHidesWhatTheAgentMayNotRead() throws Exception {
        write("docs/a.md", "x");
        write("secret.txt", "s");
        write("readme.md", "r");
        Agent reader = agent(List.of("docs/**", "*.md"), Capability.FILESYSTEM_READ);
        String out = run(new ListFilesTool(), reader, true, Map.of());
        assertEquals("docs/\nreadme.md", out);
        assertEquals("a.md", run(new ListFilesTool(), reader, true, Map.of("path", "docs")));
    }

    @Test
    void listFilesRefusesToEscapeTheWorkspace() {
        var ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> run(new ListFilesTool(), agent(List.of("**"), Capability.FILESYSTEM_READ), true, Map.of("path", "..")));
        assertTrue(ex.getMessage().contains("escapes the workspace"));
    }

    @Test
    void searchFindsMatchesCaseInsensitivelyWithLineNumbers() throws Exception {
        write("src/A.java", "class A {\n  // TODO fix\n}\n");
        write("src/B.java", "class B {}\n");
        String out = run(new SearchTool(), agent(List.of("**"), Capability.SEARCH), true, Map.of("pattern", "todo"));
        assertEquals("src/A.java:2: // TODO fix", out);
    }

    @Test
    void searchSkipsBuildOutputBinariesAndUnreadablePathsAndHonoursTheGlob() throws Exception {
        write("target/x.txt", "needle");
        write(".git/config", "needle");
        write("private/p.txt", "needle");
        write("src/ok.txt", "needle");
        write("src/ok.java", "needle");
        Files.write(ws.resolve("bin.dat"), new byte[] {'n', 'e', 'e', 'd', 'l', 'e', 0, 1});
        Agent a = agent(List.of("src/**", "*.dat"), Capability.SEARCH);
        assertEquals("src/ok.java:1: needle\nsrc/ok.txt:1: needle", run(new SearchTool(), a, true, Map.of("pattern", "needle")));
        assertEquals("src/ok.java:1: needle", run(new SearchTool(), a, true, Map.of("pattern", "needle", "glob", "**/*.java")));
        assertEquals("no matches", run(new SearchTool(), a, true, Map.of("pattern", "absent")));
    }

    // ---- git ----

    @Test
    void gitReadShowsStatusDiffAndLog() throws Exception {
        initRepo();
        write("a.txt", "one\n");
        git("add", "a.txt");
        git("commit", "-q", "-m", "first commit");
        write("a.txt", "one\ntwo\n");
        Agent a = agent(List.of("**"), Capability.GIT_READ);
        assertTrue(run(new GitReadTool(), a, true, Map.of("operation", "status")).contains("M a.txt"));
        assertTrue(run(new GitReadTool(), a, true, Map.of("operation", "diff")).contains("+two"));
        assertTrue(run(new GitReadTool(), a, true, Map.of("operation", "diff", "path", "a.txt")).contains("+two"));
        assertTrue(run(new GitReadTool(), a, true, Map.of("operation", "log")).contains("first commit"));
    }

    @Test
    void gitReadRejectsUnknownOperationsAndUnreadablePaths() throws Exception {
        initRepo();
        Agent a = agent(List.of("src/**"), Capability.GIT_READ);
        assertTrue(run(new GitReadTool(), a, true, Map.of("operation", "push")).startsWith("ERROR"));
        assertTrue(run(new GitReadTool(), a, true, Map.of("operation", "diff", "path", "secret.txt")).startsWith("DENIED"));
    }

    @Test
    void gitReadOutsideARepositoryIsAnError() throws Exception {
        String out = run(new GitReadTool(), agent(List.of("**"), Capability.GIT_READ), true, Map.of("operation", "status"));
        assertTrue(out.startsWith("ERROR: git exited with"), out);
    }

    @Test
    void gitCommitShowsTheDiffForApprovalAndCommitsOnlyThosePaths() throws Exception {
        initRepo();
        write("keep.txt", "k\n");
        git("add", "keep.txt");
        git("commit", "-q", "-m", "base");
        write("keep.txt", "k\nchanged\n");
        write("other.txt", "not committed\n");
        Agent a = agent(List.of("**"), Capability.GIT_COMMIT, Capability.GIT_READ);
        String out = run(new GitCommitTool(), a, true, Map.of("message", "Update keep", "paths", List.of("keep.txt")));

        assertTrue(out.startsWith("OK:"), out);
        assertEquals(1, approvals.size());
        assertEquals("git_commit", approvals.get(0).kind());
        assertTrue(approvals.get(0).detail().contains("+changed"), approvals.get(0).detail());
        assertTrue(run(new GitReadTool(), a, true, Map.of("operation", "log")).contains("Update keep"));
        assertTrue(run(new GitReadTool(), a, true, Map.of("operation", "status")).contains("?? other.txt"),
                "files that were not named stay uncommitted");
    }

    @Test
    void aRejectedCommitChangesNothing() throws Exception {
        initRepo();
        write("a.txt", "a\n");
        Agent a = agent(List.of("**"), Capability.GIT_COMMIT, Capability.GIT_READ);
        String out = run(new GitCommitTool(), a, false, Map.of("message", "nope", "paths", List.of("a.txt")));
        assertTrue(out.startsWith("DENIED"), out);
        assertTrue(run(new GitReadTool(), a, true, Map.of("operation", "status")).contains("?? a.txt"));
    }

    @Test
    void gitCommitValidatesItsArguments() throws Exception {
        initRepo();
        Agent a = agent(List.of("src/**"), Capability.GIT_COMMIT);
        assertTrue(run(new GitCommitTool(), a, true, Map.of("message", "m", "paths", List.of())).startsWith("ERROR"));
        assertTrue(run(new GitCommitTool(), a, true, Map.of("message", "m", "paths", "a.txt")).startsWith("ERROR"));
        assertTrue(run(new GitCommitTool(), a, true, Map.of("message", "m", "paths", List.of("secret.txt"))).startsWith("DENIED"));
        assertTrue(approvals.isEmpty(), "invalid requests never reach the user");
    }

    // ---- environment ----

    @Test
    void commandsDoNotInheritSecretsFromTheUsersShell() {
        Map<String, String> env = Map.of("PATH", "/usr/bin", "HOME", "/home/u", "OPENAI_API_KEY", "sk-secret",
                "GITHUB_TOKEN", "ghp_x", "AWS_SECRET_ACCESS_KEY", "s", "LC_ALL", "C", "SystemRoot", "C:\\Windows");
        Map<String, String> filtered = SafeEnvironment.filter(env);
        assertEquals(Set.of("PATH", "HOME", "LC_ALL", "SystemRoot"), filtered.keySet());
        assertFalse(filtered.containsKey("OPENAI_API_KEY"));
    }

    @Test
    void readingAHugeFileReturnsOnlyTheStartWithoutLoadingItAll() throws Exception {
        Path big = ws.resolve("big.log");
        try (var out = java.nio.file.Files.newBufferedWriter(big)) {
            for (int i = 0; i < 200_000; i++) {
                out.write("line " + i + " of a very large log file\n");
            }
        }
        long size = Files.size(big);
        assertTrue(size > 5_000_000, "the fixture is big: " + size);
        String out = run(new dev.buildcli.application.tools.ReadFileTool(), agent(List.of("**"), Capability.FILESYSTEM_READ), true,
                Map.of("path", "big.log"));
        assertTrue(out.startsWith("line 0 of a very large log file\nline 1"), out.substring(0, 40));
        assertTrue(out.length() < 5000, "the output is bounded: " + out.length());
        assertTrue(out.contains("truncated"), "the model is told it is partial");
    }

    @Test
    void overwritingAHugeFileDoesNotTryToDiffItInMemory() throws Exception {
        Path big = ws.resolve("out/huge.txt");
        Files.createDirectories(big.getParent());
        try (var out = Files.newOutputStream(big)) {
            byte[] chunk = new byte[1 << 20];
            java.util.Arrays.fill(chunk, (byte) 'x');
            for (int i = 0; i < 3; i++) {
                out.write(chunk);
            }
        }
        Agent writer = new Agent("bruno", "dev", "", Set.of(Capability.FILESYSTEM_WRITE),
                new dev.buildcli.domain.Permissions(List.of("**"), List.of("out/**"), List.of(), Duration.ofSeconds(5)));
        String out = run(new dev.buildcli.application.tools.WriteFileTool(), writer, true, Map.of("path", "out/huge.txt", "content", "small"));
        assertTrue(out.startsWith("OK"), out);
        assertTrue(approvals.get(0).detail().contains("too large to diff"), approvals.get(0).detail());
    }
}
