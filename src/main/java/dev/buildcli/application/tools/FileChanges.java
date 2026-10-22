package dev.buildcli.application.tools;

import dev.buildcli.domain.FileChange;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a run changed, per file, and undoing it. Undo restores a file only if it still holds exactly what the agent
 * wrote: if you or another agent changed it since, it is left alone and reported, so undo never throws away work.
 */
public final class FileChanges {
    private FileChanges() {}

    /** One file after all of a run's writes to it: its first "before" and its last "after". */
    public record Net(String path, List<String> agents, boolean existed, String before, String after) {
        public boolean restorable() {
            return after != null && (!existed || before != null);
        }

        /** Lines added and removed, for a "+12 −3" summary. */
        public int[] counts() {
            if (after == null) {
                return new int[] {0, 0};
            }
            List<String> old = before == null ? List.of() : before.lines().toList();
            List<String> now = after.lines().toList();
            var patch = com.github.difflib.DiffUtils.diff(old, now);
            int add = 0;
            int del = 0;
            for (var d : patch.getDeltas()) {
                add += d.getTarget().size();
                del += d.getSource().size();
            }
            return new int[] {add, del};
        }
    }

    public record UndoResult(List<String> restored, List<String> deleted, List<String> skipped) {
        public String summary() {
            List<String> parts = new ArrayList<>();
            if (!restored.isEmpty()) {
                parts.add("restored " + String.join(", ", restored));
            }
            if (!deleted.isEmpty()) {
                parts.add("deleted " + String.join(", ", deleted) + " (created by the agent)");
            }
            if (!skipped.isEmpty()) {
                parts.add("left " + String.join("; ", skipped));
            }
            return parts.isEmpty() ? "nothing to undo" : String.join("; ", parts);
        }
    }

    public static List<Net> net(List<FileChange> changes) {
        Map<String, Net> byPath = new LinkedHashMap<>();
        for (FileChange c : changes) {
            Net prev = byPath.get(c.path());
            if (prev == null) {
                byPath.put(c.path(), new Net(c.path(), List.of(c.agent()), c.existed(), c.before(), c.after()));
            } else {
                List<String> agents = new ArrayList<>(prev.agents());
                if (!agents.contains(c.agent())) {
                    agents.add(c.agent());
                }
                byPath.put(c.path(), new Net(c.path(), List.copyOf(agents), prev.existed(), prev.before(), c.after()));
            }
        }
        return List.copyOf(byPath.values());
    }

    /** The unified diff of every file, as the review screen shows it. */
    public static String diff(List<FileChange> changes) {
        StringBuilder sb = new StringBuilder();
        for (Net n : net(changes)) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(n.restorable() ? Diffs.unified(n.path(), n.existed() ? n.before() : null, n.after())
                    : "(" + n.path() + ": a copy was not kept, because the file is large or may hold secrets)");
        }
        return sb.toString();
    }

    /** Puts every file back as it was before the run, under the exclusive workspace lock. */
    public static UndoResult undo(Path workspace, WorkspaceLock lock, List<FileChange> changes) throws Exception {
        Path root = workspace.toAbsolutePath().normalize();
        return lock.exclusive("you", who -> { }, () -> {
            List<String> restored = new ArrayList<>();
            List<String> deleted = new ArrayList<>();
            List<String> skipped = new ArrayList<>();
            for (Net n : net(changes)) {
                Path file = root.resolve(n.path()).normalize();
                if (!file.startsWith(root)) {
                    skipped.add(n.path() + " (outside the workspace)");
                    continue;
                }
                if (n.after() == null) {
                    skipped.add(n.path() + " (no copy kept: it is large or may hold secrets)");
                    continue;
                }
                String now = Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.UTF_8) : null;
                if (now == null || !now.equals(n.after())) {
                    skipped.add(n.path() + (now == null ? " (deleted since)" : " (changed since)"));
                } else if (!n.existed()) {
                    Files.delete(file);
                    deleted.add(n.path());
                } else if (n.before() == null) {
                    skipped.add(n.path() + " (too large to have been kept)");
                } else {
                    Files.writeString(file, n.before(), StandardCharsets.UTF_8);
                    restored.add(n.path());
                }
            }
            return new UndoResult(restored, deleted, skipped);
        });
    }
}
