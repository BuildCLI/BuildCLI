package dev.buildcli.application.tools;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import dev.buildcli.ports.ToolSpec.Param;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Commits specific paths. Always requires the user's approval, shown with the status and diff of exactly those paths.
 * The agent must be able to read every path it commits. Repository hooks still run, as they would for the user.
 */
public final class GitCommitTool implements Tool {
    private static final int MAX_MESSAGE = 2000;
    private static final List<String> GIT = List.of("git", "-c", "core.fsmonitor=false");

    @Override
    public String name() {
        return "git_commit";
    }

    @Override
    public String capability() {
        return Capability.GIT_COMMIT;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(name(), "Commit the given paths with a message. Always requires user approval.",
                List.of(new Param("message", "Commit message", false, true),
                        new Param("paths", "Paths to commit, relative to the workspace root", true, true)));
    }

    @Override
    public boolean returnsExternalContent() {
        return true;
    }

    @Override
    public String execute(ToolContext ctx, Agent agent, ToolCall call) throws Exception {
        String message = ToolContext.required(call, "message").replaceAll("[\\p{Cntrl}&&[^\n]]", "").strip();
        if (message.length() > MAX_MESSAGE) {
            return "ERROR: commit message is longer than " + MAX_MESSAGE + " characters";
        }
        if (!(call.args().get("paths") instanceof List<?> raw) || raw.isEmpty() || !raw.stream().allMatch(String.class::isInstance)) {
            return "ERROR: paths must be a non-empty array of strings";
        }
        List<String> paths = new ArrayList<>();
        for (Object o : raw) {
            Path file = ctx.resolve((String) o);
            String rel = ctx.relative(file);
            if (!ToolContext.matches(agent.permissions().readGlobs(), rel)) {
                return "DENIED: " + agent.name() + " may not access '" + rel + "'";
            }
            paths.add(rel);
        }
        String status = git(ctx, List.of("status", "--short", "--"), paths);
        String diff = git(ctx, List.of("diff", "--no-color", "--no-ext-diff", "--no-textconv", "HEAD", "--"), paths);
        String detail = "Message: " + message + "\n\n" + status + (diff.isBlank() ? "" : "\n" + diff);
        if (!ctx.approve(new ApprovalRequest(agent.name(), "git_commit", "Commit " + paths.size() + " path(s): "
                + message.lines().findFirst().orElse(""), detail))) {
            return "DENIED: the user rejected the commit";
        }
        return ctx.exclusive(() -> {
            ProcessRunner.Result add = run(ctx, List.of("add", "--"), paths);
            if (add.exitCode() != 0) {
                return "ERROR: git add failed: " + ToolContext.cap(add.output(), ToolContext.MAX_OUTPUT);
            }
            List<String> commit = new ArrayList<>(GIT);
            commit.addAll(List.of("commit", "-m", message, "--"));
            commit.addAll(paths);
            ProcessRunner.Result r = ProcessRunner.run(commit, ctx.workspace(), agent.permissions().commandTimeout(), GitReadTool.ENV);
            if (r.timedOut()) {
                return "ERROR: git commit timed out";
            }
            return (r.exitCode() == 0 ? "OK: " : "ERROR: git commit exited with " + r.exitCode() + ": ")
                    + ToolContext.cap(r.output(), ToolContext.MAX_OUTPUT);
        });
    }

    private static ProcessRunner.Result run(ToolContext ctx, List<String> args, List<String> paths) throws Exception {
        List<String> argv = new ArrayList<>(GIT);
        argv.addAll(args);
        argv.addAll(paths);
        return ProcessRunner.run(argv, ctx.workspace(), GitReadTool.TIMEOUT, GitReadTool.ENV);
    }

    private static String git(ToolContext ctx, List<String> args, List<String> paths) throws Exception {
        ProcessRunner.Result r = run(ctx, args, paths);
        return r.exitCode() == 0 ? r.output().strip() : "";
    }
}
