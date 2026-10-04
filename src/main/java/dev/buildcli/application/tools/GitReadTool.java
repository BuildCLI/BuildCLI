package dev.buildcli.application.tools;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.ports.GitAccess;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import dev.buildcli.ports.ToolSpec.Param;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Read-only git: status, diff and log. The model chooses an operation, never git options, and the work is done by JGit
 * in the process, so no program is started and nothing the repository configures (an external diff, a textconv filter,
 * an fsmonitor) can run.
 */
public final class GitReadTool implements Tool {
    private static final int LOG_ENTRIES = 20;

    @Override
    public String name() {
        return "git_read";
    }

    @Override
    public String capability() {
        return Capability.GIT_READ;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(name(), "Read-only git: operation is one of status, diff (uncommitted changes) or log (last 20 commits).",
                List.of(new Param("operation", "status | diff | log", false, true),
                        new Param("path", "Optional path to limit a diff to", false, false)));
    }

    @Override
    public boolean returnsExternalContent() {
        return true;
    }

    @Override
    public String execute(ToolContext ctx, Agent agent, ToolCall call) throws Exception {
        String operation = ToolContext.required(call, "operation").toLowerCase();
        String out;
        try {
            switch (operation) {
                case "status" -> out = ctx.git().status(ctx.workspace(), true, List.of());
                case "log" -> out = ctx.git().log(ctx.workspace(), LOG_ENTRIES, false);
                case "diff" -> {
                    String path = ToolContext.optional(call, "path", null);
                    List<String> paths = List.of();
                    if (path != null) {
                        Path file = ctx.resolve(path);
                        String rel = ctx.relative(file);
                        if (!ToolContext.matches(agent.permissions().readGlobs(), rel)) {
                            return "DENIED: " + agent.name() + " may not read '" + rel + "'";
                        }
                        paths = List.of(rel);
                    }
                    out = ctx.git().diff(ctx.workspace(), GitAccess.Scope.WORKTREE, paths);
                }
                default -> {
                    return "ERROR: operation must be one of status, diff, log";
                }
            }
        } catch (IOException e) {
            return "ERROR: git: " + e.getMessage();
        }
        return out.isBlank() ? "(no output)" : ToolContext.cap(out, ToolContext.MAX_OUTPUT);
    }
}
