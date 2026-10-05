package dev.buildcli.application.tools;

import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.ToolCall;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

/** What a tool may use while running one call: the confined workspace, path checks and the approval channel. */
public final class ToolContext {
    /** Longest tool output handed back to the model; small local models have small context windows. */
    public static final int MAX_OUTPUT = 4000;

    /** Asks the user to approve an action. Blocks until they decide. */
    public interface Approval {
        boolean request(ApprovalRequest request);
    }

    private final Path workspace;
    private final Approval approval;
    private final WorkspaceLock lock;
    private final String agent;
    private final java.util.function.Consumer<String> onWait;
    private java.util.function.Consumer<dev.buildcli.domain.FileChange> onChange = c -> { };
    private List<Path> protectedPaths = List.of();
    private dev.buildcli.ports.GitAccess git;

    public ToolContext(Path workspace, Approval approval) {
        this(workspace, approval, new WorkspaceLock(), "agent", who -> { });
    }

    public ToolContext(Path workspace, Approval approval, WorkspaceLock lock, String agent, java.util.function.Consumer<String> onWait) {
        this.workspace = workspace.toAbsolutePath().normalize();
        this.approval = approval;
        this.lock = lock;
        this.agent = agent;
        this.onWait = onWait;
    }

    /** Runs a change to the workspace while no other agent reads or changes it. Call it after any approval. */
    public <T> T exclusive(java.util.concurrent.Callable<T> work) throws Exception {
        return lock.exclusive(agent, onWait, work);
    }

    /**
     * Folders no tool may read or write even when they are inside the workspace (BuildCLI's own folder, which holds the
     * saved API keys and the trust decisions: an agent must not read the first or rewrite the second).
     */
    public ToolContext protect(List<Path> paths) {
        this.protectedPaths = paths.stream().map(p -> p.toAbsolutePath().normalize()).toList();
        return this;
    }

    /** The repository access the git tools use. */
    public ToolContext git(dev.buildcli.ports.GitAccess access) {
        this.git = access;
        return this;
    }

    public dev.buildcli.ports.GitAccess git() {
        if (git == null) {
            throw new IllegalStateException("git is not available in this run");
        }
        return git;
    }

    /** Where to report the files this call writes, so they can be reviewed and undone. */
    public ToolContext onChange(java.util.function.Consumer<dev.buildcli.domain.FileChange> listener) {
        this.onChange = listener;
        return this;
    }

    public void changed(dev.buildcli.domain.FileChange change) {
        onChange.accept(change);
    }

    public Path workspace() {
        return workspace;
    }

    public boolean approve(ApprovalRequest request) {
        return approval.request(request);
    }

    /**
     * Confines every path to the workspace root, including through symlinks: the deepest existing ancestor is
     * resolved to its real path and must still be inside the (real) workspace.
     */
    public Path resolve(String rel) throws IOException {
        Path p = workspace.resolve(rel).normalize();
        if (!p.startsWith(workspace)) {
            throw new IllegalArgumentException("path escapes the workspace: " + rel);
        }
        Path existing = p;
        while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        if (existing != null && !existing.toRealPath().startsWith(workspace.toRealPath())) {
            throw new IllegalArgumentException("path escapes the workspace through a symlink: " + rel);
        }
        if (isProtected(p)) {
            throw new IllegalArgumentException("that is BuildCLI's own folder (saved keys and trust decisions); tools cannot use it: " + rel);
        }
        return p;
    }

    /** True for BuildCLI's own folder and everything in it, also through a symlink. Walks of the workspace must skip it. */
    public boolean isProtected(Path path) {
        Path abs = path.toAbsolutePath().normalize();
        for (Path forbidden : protectedPaths) {
            if (abs.startsWith(forbidden)) {
                return true;
            }
            try {
                Path existing = abs;
                while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
                    existing = existing.getParent();
                }
                if (existing != null && Files.exists(forbidden) && existing.toRealPath().startsWith(forbidden.toRealPath())) {
                    return true;
                }
            } catch (IOException e) {
                return true; // cannot tell: do not risk it
            }
        }
        return false;
    }

    /** The workspace-relative path with forward slashes, the form permission globs are matched against. */
    public String relative(Path absolute) {
        String rel = workspace.relativize(absolute).toString().replace('\\', '/');
        return rel.isEmpty() ? "." : rel;
    }

    public static boolean matches(List<String> globs, String relativePath) {
        Path p = Path.of(relativePath);
        return globs.stream().anyMatch(g -> FileSystems.getDefault().getPathMatcher("glob:" + g).matches(p));
    }

    public static String required(ToolCall call, String key) {
        Object v = call.args().get(key);
        if (v == null || v.toString().isBlank()) {
            throw new IllegalArgumentException("missing argument '" + key + "'");
        }
        return v.toString();
    }

    public static String optional(ToolCall call, String key, String fallback) {
        Object v = call.args().get(key);
        return v == null || v.toString().isBlank() ? fallback : v.toString();
    }

    public static String cap(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "... [truncated " + (s.length() - max) + " chars]";
    }
}
