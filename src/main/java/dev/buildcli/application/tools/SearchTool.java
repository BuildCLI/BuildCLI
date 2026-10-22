package dev.buildcli.application.tools;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import dev.buildcli.ports.ToolSpec.Param;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/** Case-insensitive text search over readable files in the workspace. */
public final class SearchTool implements Tool {
    private static final int MAX_MATCHES = 30;
    private static final long MAX_FILE_BYTES = 1_000_000;
    private static final int MAX_LINE = 200;
    private static final Set<String> SKIPPED_DIRS = Set.of(".git", "target", "node_modules", "build", ".gradle", ".idea");

    @Override
    public String name() {
        return "search";
    }

    @Override
    public String capability() {
        return Capability.SEARCH;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(name(), "Search the workspace for a text (case-insensitive) and list matching lines as path:line: text.",
                List.of(new Param("pattern", "Text to look for", false, true),
                        new Param("glob", "Optional file filter, e.g. **/*.java", false, false)));
    }

    @Override
    public boolean returnsExternalContent() {
        return true;
    }

    @Override
    public String execute(ToolContext ctx, Agent agent, ToolCall call) throws Exception {
        String needle = ToolContext.required(call, "pattern").toLowerCase(Locale.ROOT);
        String glob = ToolContext.optional(call, "glob", null);
        PathMatcher filter = glob == null ? null : FileSystems.getDefault().getPathMatcher("glob:" + glob);
        List<String> readGlobs = agent.permissions().readGlobs();
        List<String> matches = new ArrayList<>();
        walk(ctx, ctx.workspace(), needle, filter, readGlobs, matches);
        if (matches.isEmpty()) {
            return "no matches";
        }
        return ToolContext.cap(String.join("\n", matches), ToolContext.MAX_OUTPUT);
    }

    /** Depth-first in alphabetical order, so results (and the cut-off at MAX_MATCHES) are reproducible. */
    private static void walk(ToolContext ctx, Path dir, String needle, PathMatcher filter, List<String> readGlobs,
            List<String> matches) throws IOException {
        List<Path> children;
        try (Stream<Path> s = Files.list(dir)) {
            children = s.sorted().toList();
        }
        for (Path child : children) {
            if (matches.size() >= MAX_MATCHES) {
                return;
            }
            if (Files.isSymbolicLink(child) || ctx.isProtected(child)) {
                continue;
            }
            if (Files.isDirectory(child)) {
                if (!SKIPPED_DIRS.contains(child.getFileName().toString())) {
                    walk(ctx, child, needle, filter, readGlobs, matches);
                }
                continue;
            }
            String rel = ctx.relative(child);
            if (Files.isRegularFile(child) && Files.size(child) <= MAX_FILE_BYTES && ToolContext.matches(readGlobs, rel)
                    && (filter == null || filter.matches(Path.of(rel))) && isText(child)) {
                scan(child, rel, needle, matches);
            }
        }
    }

    private static void scan(Path file, String rel, String needle, List<String> out) throws IOException {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (java.nio.charset.MalformedInputException e) {
            return; // not UTF-8 text
        }
        for (int i = 0; i < lines.size() && out.size() < MAX_MATCHES; i++) {
            if (lines.get(i).toLowerCase(Locale.ROOT).contains(needle)) {
                out.add(rel + ":" + (i + 1) + ": " + ToolContext.cap(lines.get(i).strip(), MAX_LINE));
            }
        }
    }

    private static boolean isText(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] head = in.readNBytes(4096);
            for (byte b : head) {
                if (b == 0) {
                    return false;
                }
            }
            return true;
        }
    }
}
