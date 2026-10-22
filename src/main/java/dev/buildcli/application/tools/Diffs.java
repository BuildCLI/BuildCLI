package dev.buildcli.application.tools;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import java.util.List;

/** Unified diffs for the approval dialog, so the user sees exactly what a write changes. */
public final class Diffs {
    private static final int CONTEXT = 3;
    private static final int MAX_LINES = 400;

    private Diffs() {}

    /** @param old the current content, or null if the file does not exist yet */
    public static String unified(String path, String old, String now) {
        if (old == null) {
            return "--- /dev/null\n+++ b/" + path + "\n" + capLines(now.lines().map(l -> "+" + l).toList());
        }
        List<String> before = old.lines().toList();
        List<String> after = now.lines().toList();
        Patch<String> patch = DiffUtils.diff(before, after);
        if (patch.getDeltas().isEmpty()) {
            return "(no changes: the file already has this content)";
        }
        return capLines(UnifiedDiffUtils.generateUnifiedDiff("a/" + path, "b/" + path, before, patch, CONTEXT));
    }

    private static String capLines(List<String> lines) {
        if (lines.size() <= MAX_LINES) {
            return String.join("\n", lines);
        }
        return String.join("\n", lines.subList(0, MAX_LINES)) + "\n... [" + (lines.size() - MAX_LINES) + " more lines]";
    }
}
