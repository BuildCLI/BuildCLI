package dev.buildcli.cli;

import dev.buildcli.domain.RunInfo;
import dev.buildcli.infrastructure.StateStore;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "runs", description = "List the recent runs of this project")
final class RunsCommand implements Callable<Integer> {
    private final CliContext ctx;

    @Option(names = "--limit", description = "How many runs to show (default: ${DEFAULT-VALUE})", defaultValue = "20")
    int limit;

    RunsCommand(CliContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Integer call() throws Exception {
        if (!ctx.hasState()) {
            ctx.out.println("No runs yet for this project. Try: buildcli run --group <name> \"<what to do>\"");
            return 0;
        }
        try (StateStore store = ctx.openState()) {
            List<RunInfo> runs = store.listRuns(limit);
            if (runs.isEmpty()) {
                ctx.out.println("No runs yet for this project.");
                return 0;
            }
            Tables.print(ctx.out, List.of("RUN", "STATUS", "GROUP", "STARTED", "REQUEST"), runs.stream()
                    .map(r -> List.of(r.id(), r.status(), r.group(), r.startedAt().toString().replace('T', ' ').substring(0, 19),
                            Tables.cut(r.request(), 60))).toList());
        }
        return 0;
    }
}
