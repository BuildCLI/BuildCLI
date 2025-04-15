package dev.buildcli.core.actions.changelog;

import java.util.Map;
import java.util.regex.Pattern;

public final class ChangelogConstants {
    public static final Pattern COMMIT_PATTERN = Pattern.compile("^(\\w+)(?:\\(([^)]+)\\))?\\s*:\\s*(.*)$");

    public static final Map<String, String> TYPE_TO_KEEP_SECTION = Map.ofEntries(
            Map.entry("feat", "Added"),
            Map.entry("docs", "Added"),
            Map.entry("fix", "Fixed"),
            Map.entry("refactor", "Changed"),
            Map.entry("perf", "Changed"),
            Map.entry("chore", "Changed"),
            Map.entry("style", "Changed"),
            Map.entry("ci", "Changed"),
            Map.entry("build", "Changed"),
            Map.entry("test", "Changed"),
            Map.entry("deprecated", "Deprecated"),
            Map.entry("remove", "Removed"),
            Map.entry("revert", "Removed"),
            Map.entry("security", "Security")
    );
}
