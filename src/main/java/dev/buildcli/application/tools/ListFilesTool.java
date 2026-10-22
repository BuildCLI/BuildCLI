package dev.buildcli.application.tools;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import dev.buildcli.ports.ToolSpec.Param;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class ListFilesTool implements Tool {
    private static final int MAX_ENTRIES = 200;

    @Override
    public String name() {
        return "list_files";
    }

    @Override
    public String capability() {
        return Capability.FILESYSTEM_READ;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(name(), "List the entries of a directory in the workspace (directories end with '/').",
                List.of(new Param("path", "Directory relative to the workspace root; defaults to the root", false, false)));
    }

    @Override
    public boolean returnsExternalContent() {
        return true;
    }

    @Override
    public String execute(ToolContext ctx, Agent agent, ToolCall call) throws Exception {
        String rel = ToolContext.optional(call, "path", ".");
        Path dir = ctx.resolve(rel);
        if (!Files.isDirectory(dir)) {
            return "ERROR: not a directory: " + rel;
        }
        List<String> readGlobs = agent.permissions().readGlobs();
        try (Stream<Path> entries = Files.list(dir)) {
            List<String> names = entries.sorted()
                    .filter(p -> !ctx.isProtected(p))
                    .filter(p -> Files.isDirectory(p)
                            ? ToolContext.matches(readGlobs, ctx.relative(p) + "/x")
                            : ToolContext.matches(readGlobs, ctx.relative(p)))
                    .limit(MAX_ENTRIES)
                    .map(p -> p.getFileName() + (Files.isDirectory(p) ? "/" : ""))
                    .toList();
            return names.isEmpty() ? "(empty or not readable by " + agent.name() + ")" : String.join("\n", names);
        }
    }
}
