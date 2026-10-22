package dev.buildcli.application.tools;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.GitAccess;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import dev.buildcli.ports.ToolSpec.Param;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Commits specific paths. Always requires the user's approval, shown with the status and diff of exactly those paths.
 * The agent must be able to read every path it commits. Repository hooks still run, as they would for the user.
 * The paths are literal: a name that happens to look like a pattern is a file name, never a glob.
 */
public final class GitCommitTool implements Tool {
    private static final int MAX_MESSAGE = 2000;
    private static final int MAX_REVIEW = 100_000;

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
        String reviewed;
        try {
            reviewed = review(ctx, message, paths);
        } catch (IOException e) {
            return "ERROR: git: " + e.getMessage();
        }
        if (reviewed.length() > MAX_REVIEW) {
            return "ERROR: the change is too large to review (" + reviewed.length() + " characters); commit fewer paths";
        }
        if (!ctx.approve(new ApprovalRequest(agent.name(), "git_commit", "Commit " + paths.size() + " path(s): "
                + message.lines().findFirst().orElse(""), reviewed))) {
            return "DENIED: the user rejected the commit";
        }
        return ctx.exclusive(() -> {
            try {
                // the user approved what they saw: anything written since is not covered by that answer
                if (!reviewed.equals(review(ctx, message, paths))) {
                    return "ERROR: the files changed while the commit waited for approval; nothing was committed, ask again";
                }
                return "OK: " + ToolContext.cap(ctx.git().commit(ctx.workspace(), message, paths), ToolContext.MAX_OUTPUT);
            } catch (IOException e) {
                return "ERROR: git commit: " + e.getMessage();
            }
        });
    }

    /** What the user is asked to approve: the message, the status and the diff of exactly the paths, new files included. */
    private static String review(ToolContext ctx, String message, List<String> paths) throws IOException {
        String status = ctx.git().status(ctx.workspace(), false, paths).strip();
        String diff = ctx.git().diff(ctx.workspace(), GitAccess.Scope.COMMIT, paths).strip();
        return "Message: " + message + "\n\n" + status + (diff.isBlank() ? "" : "\n" + diff);
    }
}
