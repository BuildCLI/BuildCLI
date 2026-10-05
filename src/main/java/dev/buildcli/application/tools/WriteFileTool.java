package dev.buildcli.application.tools;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import dev.buildcli.ports.ToolSpec.Param;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes are checked against the agent's write globs first, then shown to the user as a unified diff. */
public final class WriteFileTool implements Tool {
    private static final long MAX_DIFFABLE_BYTES = 2_000_000;

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public String capability() {
        return Capability.FILESYSTEM_WRITE;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(name(), "Create or overwrite a text file in the workspace. Requires user approval.",
                List.of(new Param("path", "Path relative to the workspace root", false, true),
                        new Param("content", "Full new file content", false, true)));
    }

    @Override
    public boolean returnsExternalContent() {
        return false;
    }

    @Override
    public String execute(ToolContext ctx, Agent agent, ToolCall call) throws Exception {
        Object raw = call.args().get("content");
        if (raw == null) {
            throw new IllegalArgumentException("missing argument 'content'");
        }
        String content = raw.toString(); // an empty file is a legitimate write
        Path file = ctx.resolve(ToolContext.required(call, "path"));
        String rel = ctx.relative(file);
        if (!ToolContext.matches(agent.permissions().writeGlobs(), rel)) {
            return "DENIED: " + agent.name() + " may not write '" + rel + "' (allowed: " + agent.permissions().writeGlobs() + ")";
        }
        boolean exists = Files.isRegularFile(file);
        String before = exists && Files.size(file) <= MAX_DIFFABLE_BYTES ? Files.readString(file, StandardCharsets.UTF_8) : null;
        long beforeSize = exists ? Files.size(file) : -1;
        String diff;
        if (exists && Files.size(file) > MAX_DIFFABLE_BYTES) {
            diff = "(the existing file is " + Files.size(file) + " bytes, too large to diff; it will be replaced by "
                    + content.length() + " characters)";
        } else {
            diff = Diffs.unified(rel, before, content);
        }
        if (!ctx.approve(new ApprovalRequest(agent.name(), "write", "Write " + rel, diff, "write",
                "let " + agent.name() + " edit files in this chat (you can still undo)"))) {
            return "DENIED: the user rejected the write to " + rel;
        }
        // the user approved this exact diff; if someone changed the file meanwhile, writing would silently undo their work
        return ctx.exclusive(() -> {
            boolean nowExists = Files.isRegularFile(file);
            boolean unchanged = nowExists == exists && (!exists || (before != null
                    ? before.equals(Files.readString(file, StandardCharsets.UTF_8)) : Files.size(file) == beforeSize));
            if (!unchanged) {
                return "ERROR: " + rel + " was changed by someone else while you waited for approval. Read it again and redo "
                        + "your change on top of the new content.";
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
            ctx.changed(new dev.buildcli.domain.FileChange(agent.name(), rel, exists, before, content));
            return "OK: wrote " + content.length() + " chars to " + rel;
        });
    }
}
