package dev.buildcli.application.tools;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import dev.buildcli.ports.ToolSpec.Param;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Read-only git: status, diff and log, each with a fixed argv. The model chooses an operation, never git flags, and
 * configuration that could run repository-defined programs (fsmonitor, external diff, textconv) is switched off.
 */
public final class GitReadTool implements Tool {
    static final Duration TIMEOUT = Duration.ofSeconds(30);
    static final Map<String, String> ENV = Map.of("GIT_PAGER", "cat", "GIT_TERMINAL_PROMPT", "0", "GIT_OPTIONAL_LOCKS", "0");

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
        List<String> argv = new ArrayList<>(List.of("git", "-c", "core.fsmonitor=false"));
        switch (ToolContext.required(call, "operation").toLowerCase()) {
            case "status" -> argv.addAll(List.of("status", "--short", "--branch"));
            case "log" -> argv.addAll(List.of("log", "--oneline", "--no-color", "-n", "20"));
            case "diff" -> {
                argv.addAll(List.of("diff", "--no-color", "--no-ext-diff", "--no-textconv"));
                String path = ToolContext.optional(call, "path", null);
                if (path != null) {
                    Path file = ctx.resolve(path);
                    String rel = ctx.relative(file);
                    if (!ToolContext.matches(agent.permissions().readGlobs(), rel)) {
                        return "DENIED: " + agent.name() + " may not read '" + rel + "'";
                    }
                    argv.addAll(List.of("--", rel));
                }
            }
            default -> {
                return "ERROR: operation must be one of status, diff, log";
            }
        }
        ProcessRunner.Result r = ProcessRunner.run(argv, ctx.workspace(), TIMEOUT, ENV);
        if (r.timedOut()) {
            return "ERROR: git timed out";
        }
        if (r.exitCode() != 0) {
            return "ERROR: git exited with " + r.exitCode() + ": " + ToolContext.cap(r.output(), ToolContext.MAX_OUTPUT);
        }
        return r.output().isBlank() ? "(no output)" : ToolContext.cap(r.output(), ToolContext.MAX_OUTPUT);
    }
}
