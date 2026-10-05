package dev.buildcli.infrastructure.tui;

import dev.buildcli.infrastructure.JGitAccess;
import dev.buildcli.ports.GitAccess;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only things the user asks to look at from the chat: git diff/status/log and a file. These are the user's own
 * actions, like typing the command in a shell, so they do not go through an agent's permissions; they are still
 * bounded (time, size) and cannot be turned into writes (no user-supplied git options).
 */
final class LocalViews {
    private static final long MAX_FILE_BYTES = 512L * 1024;
    private static final int MAX_LINES = 20_000;

    private LocalViews() {}

    private static final GitAccess GIT = new JGitAccess();
    private static final int LOG_ENTRIES = 30;

    /** @param arg "", "--staged" or a path; anything else that looks like an option is refused */
    static List<String> diff(Path cwd, String arg) throws IOException {
        GitAccess.Scope scope = GitAccess.Scope.WORKTREE;
        String a = arg.strip();
        if (a.equals("--staged") || a.equals("--cached") || a.startsWith("--staged ") || a.startsWith("--cached ")) {
            scope = GitAccess.Scope.STAGED;
            a = a.substring(a.indexOf(' ') < 0 ? a.length() : a.indexOf(' ')).strip();
        }
        if (a.startsWith("-")) {
            throw new IOException("only --staged and a path are supported: /diff [--staged] [path]");
        }
        return capped(GIT.diff(cwd, scope, a.isEmpty() ? List.of() : List.of(a)).lines().toList());
    }

    static List<String> status(Path cwd) throws IOException {
        return capped(GIT.status(cwd, true, List.of()).lines().toList());
    }

    static List<String> log(Path cwd) throws IOException {
        return capped(GIT.log(cwd, LOG_ENTRIES, true).lines().toList());
    }

    /** A text file for the viewer. Refuses binaries and very large files rather than freezing the screen. */
    static List<String> file(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("no such file: " + path);
        }
        if (Files.size(path) > MAX_FILE_BYTES) {
            throw new IOException(path.getFileName() + " is " + Files.size(path) / 1024 + " KB; the viewer opens files up to "
                    + MAX_FILE_BYTES / 1024 + " KB");
        }
        byte[] bytes = Files.readAllBytes(path);
        for (int i = 0; i < Math.min(bytes.length, 4096); i++) {
            if (bytes[i] == 0) {
                throw new IOException(path.getFileName() + " looks like a binary file");
            }
        }
        return capped(new String(bytes, StandardCharsets.UTF_8).lines().toList());
    }

    private static List<String> capped(List<String> lines) {
        if (lines.size() <= MAX_LINES) {
            return lines;
        }
        List<String> out = new ArrayList<>(lines.subList(0, MAX_LINES));
        out.add("… " + (lines.size() - MAX_LINES) + " more lines not shown");
        return out;
    }
}
