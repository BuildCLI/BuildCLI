package dev.buildcli.infrastructure.tui;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Puts text on the system clipboard from a terminal program. It uses the tool of the system (clip.exe, pbcopy, wl-copy,
 * xclip, xsel), which is the reliable way, and falls back to the OSC 52 escape sequence, which most modern terminals
 * obey and which works over SSH. The text goes only through the tool's standard input, never through a shell.
 */
final class Clipboard {
    /** OSC 52 payloads above this are dropped by many terminals, so a bigger text is not sent that way. */
    static final int OSC52_MAX_BYTES = 90_000;
    private static final long TOOL_TIMEOUT_MS = 3_000;

    private Clipboard() {}

    /** A command that reads the text on its standard input, and the encoding it expects. */
    record Tool(List<String> argv, boolean utf16WithBom) {}

    /**
     * Copies {@code text}.
     *
     * @param rawOutput writes bytes straight to the terminal (for OSC 52), or null when that is not possible
     * @return how it was copied, or null if it was not
     */
    static String copy(String text, Consumer<String> rawOutput) {
        for (Tool tool : candidates(System.getProperty("os.name", ""), isWsl(), name -> onPath(name))) {
            if (run(tool, text)) {
                return tool.argv().get(0);
            }
        }
        if (rawOutput != null && text.getBytes(StandardCharsets.UTF_8).length <= OSC52_MAX_BYTES) {
            rawOutput.accept(osc52(text));
            return "terminal";
        }
        return null;
    }

    /** The sequence that asks the terminal to put {@code text} on the clipboard. */
    static String osc52(String text) {
        return "\u001b]52;c;" + Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)) + "\u0007";
    }

    /** The tools to try on this system, best first. */
    static List<Tool> candidates(String osName, boolean wsl, java.util.function.Predicate<String> available) {
        String os = osName.toLowerCase(Locale.ROOT);
        List<Tool> out = new ArrayList<>();
        if (os.contains("win") || wsl) {
            // clip.exe reads UTF-16 with a byte order mark correctly; plain UTF-8 turns accents into garbage
            if (available.test("clip.exe") || available.test("clip")) {
                out.add(new Tool(List.of(available.test("clip.exe") ? "clip.exe" : "clip"), true));
            }
        }
        if (os.contains("mac")) {
            out.add(new Tool(List.of("pbcopy"), false));
        }
        if (!os.contains("win") && !os.contains("mac")) {
            if (available.test("wl-copy")) {
                out.add(new Tool(List.of("wl-copy"), false));
            }
            if (available.test("xclip")) {
                out.add(new Tool(List.of("xclip", "-selection", "clipboard"), false));
            }
            if (available.test("xsel")) {
                out.add(new Tool(List.of("xsel", "--clipboard", "--input"), false));
            }
        }
        return out;
    }

    private static boolean run(Tool tool, String text) {
        try {
            ProcessBuilder pb = new ProcessBuilder(tool.argv());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.environment().put("LC_CTYPE", "UTF-8");
            Process p = pb.start();
            try (OutputStream in = p.getOutputStream()) {
                if (tool.utf16WithBom()) {
                    in.write(new byte[] {(byte) 0xFF, (byte) 0xFE});
                    in.write(text.getBytes(StandardCharsets.UTF_16LE));
                } else {
                    in.write(text.getBytes(StandardCharsets.UTF_8));
                }
            }
            if (!p.waitFor(TOOL_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                // xclip and wl-copy stay alive to serve the selection: that is a success, not a hang
                return p.isAlive();
            }
            return p.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean onPath(String name) {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(java.io.File.pathSeparator)) {
            if (!dir.isBlank() && Files.isExecutable(Path.of(dir, name))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWsl() {
        try {
            return Files.readString(Path.of("/proc/version")).toLowerCase(Locale.ROOT).contains("microsoft");
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
