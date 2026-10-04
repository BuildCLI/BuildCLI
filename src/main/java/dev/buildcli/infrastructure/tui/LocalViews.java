package dev.buildcli.infrastructure.tui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Read-only things the user asks to look at from the chat: git diff/status/log and a file. These are the user's own
 * actions, like typing the command in a shell, so they do not go through an agent's permissions; they are still
 * bounded (time, size) and cannot be turned into writes (no user-supplied git options).
 */
final class LocalViews {
    private static final long MAX_FILE_BYTES = 512L * 1024;
    private static final int MAX_LINES = 20_000;
    private static final long MAX_OUTPUT_BYTES = 2L * 1024 * 1024;

    private LocalViews() {}

    /** @param arg "", "--staged" or a path; anything else that looks like an option is refused */
    static List<String> diff(Path cwd, String arg) throws IOException {
        List<String> cmd = new ArrayList<>(List.of("git", "--no-pager", "diff", "--no-color", "--no-ext-diff", "--no-textconv"));
        String a = arg.strip();
        if (a.equals("--staged") || a.equals("--cached") || a.startsWith("--staged ") || a.startsWith("--cached ")) {
            cmd.add("--staged");
            a = a.substring(a.indexOf(' ') < 0 ? a.length() : a.indexOf(' ')).strip();
        }
        if (a.startsWith("-")) {
            throw new IOException("only --staged and a path are supported: /diff [--staged] [path]");
        }
        if (!a.isEmpty()) {
            cmd.add("--");
            cmd.add(a);
        }
        return run(cwd, cmd);
    }

    static List<String> status(Path cwd) throws IOException {
        return run(cwd, List.of("git", "--no-pager", "status", "--short", "--branch"));
    }

    static List<String> log(Path cwd) throws IOException {
        return run(cwd, List.of("git", "--no-pager", "log", "--oneline", "--decorate", "--no-color", "-30"));
    }

    static List<String> run(Path cwd, List<String> cmd) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(cwd.toFile()).redirectErrorStream(true);
        pb.environment().put("GIT_TERMINAL_PROMPT", "0");
        pb.environment().put("GIT_OPTIONAL_LOCKS", "0");
        Process p = pb.start();
        byte[] out = p.getInputStream().readNBytes((int) MAX_OUTPUT_BYTES);
        try {
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IOException("git took too long");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
            throw new IOException("interrupted");
        }
        String text = new String(out, StandardCharsets.UTF_8);
        if (p.exitValue() != 0) {
            throw new IOException(text.isBlank() ? "git failed" : text.strip());
        }
        return capped(text.lines().toList());
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
