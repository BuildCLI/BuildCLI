package dev.buildcli.cli;

import dev.buildcli.domain.Event;
import dev.buildcli.domain.RunInfo;
import dev.buildcli.domain.Task;
import dev.buildcli.infrastructure.StateStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "task", description = "Inspect the tasks and handoffs of a run", subcommands = {TaskCommand.ListCmd.class, TaskCommand.ShowCmd.class})
final class TaskCommand implements Callable<Integer> {
    private final CliContext ctx;

    TaskCommand(CliContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Integer call() {
        ctx.out.println("Use one of: task list [--run ID] | task show <id> [--run ID]");
        return 2;
    }

    /** The requested run, or the latest one; null (after saying why) if there is none. */
    static RunInfo pickRun(CliContext ctx, StateStore store, String runId) {
        List<RunInfo> runs = store.listRuns(runId == null ? 1 : 1000);
        if (runId == null) {
            if (runs.isEmpty()) {
                ctx.out.println("No runs yet for this project.");
                return null;
            }
            return runs.get(0);
        }
        return runs.stream().filter(r -> r.id().equals(runId)).findFirst().orElseGet(() -> {
            ctx.err.println("error: no run '" + runId + "' in this project (see: buildcli runs)");
            return null;
        });
    }

    @Command(name = "list", description = "List the tasks of a run (default: the latest)")
    static final class ListCmd implements Callable<Integer> {
        private final CliContext ctx;

        @Option(names = "--run", description = "Run id (see 'buildcli runs')")
        String run;

        ListCmd(CliContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public Integer call() throws Exception {
            if (!ctx.hasState()) {
                ctx.out.println("No runs yet for this project.");
                return 0;
            }
            try (StateStore store = ctx.openState()) {
                RunInfo r = pickRun(ctx, store, run);
                if (r == null) {
                    return run == null ? 0 : 2;
                }
                ctx.out.println("Run " + r.id() + "  " + r.status() + "  group " + r.group());
                List<Task> tasks = store.listTasks(r.id());
                List<List<String>> rows = new ArrayList<>();
                for (Task t : tasks) {
                    rows.add(List.of("  ".repeat(depth(t, tasks)) + "#" + t.id, t.status.name(), t.from + " -> " + t.to,
                            String.valueOf(t.tokens()), Tables.cut(t.objective, 60)));
                }
                Tables.print(ctx.out, List.of("TASK", "STATUS", "FROM -> TO", "TOKENS", "OBJECTIVE"), rows);
            }
            return 0;
        }

        private static int depth(Task t, List<Task> all) {
            int d = 0;
            Integer parent = t.parentId;
            while (parent != null && d < 10) {
                int id = parent;
                Task up = all.stream().filter(x -> x.id == id).findFirst().orElse(null); // not map(): the root's parentId is null
                parent = up == null ? null : up.parentId;
                d++;
            }
            return d;
        }
    }

    @Command(name = "show", description = "Show one task and its events")
    static final class ShowCmd implements Callable<Integer> {
        private final CliContext ctx;

        @Parameters(index = "0", description = "Task number")
        int id;

        @Option(names = "--run", description = "Run id (default: the latest)")
        String run;

        ShowCmd(CliContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public Integer call() throws Exception {
            if (!ctx.hasState()) {
                ctx.out.println("No runs yet for this project.");
                return 0;
            }
            try (StateStore store = ctx.openState()) {
                RunInfo r = pickRun(ctx, store, run);
                if (r == null) {
                    return run == null ? 0 : 2;
                }
                Task t = store.listTasks(r.id()).stream().filter(x -> x.id == id).findFirst().orElse(null);
                if (t == null) {
                    ctx.err.println("error: run " + r.id() + " has no task #" + id);
                    return 2;
                }
                ctx.out.println("Task #" + t.id + " of run " + r.id() + "  " + t.status + (t.parentId == null ? "" : "  (handoff from #" + t.parentId + ")"));
                ctx.out.println("  from:      " + t.from + " -> " + t.to);
                ctx.out.println("  attempts:  " + t.attempts() + "   tokens: " + t.tokens());
                ctx.out.println("  objective: " + t.objective);
                if (!t.brief.isBlank()) {
                    ctx.out.println("  brief:     " + t.brief);
                }
                ctx.out.println("  result:    " + (t.result == null ? "(none)" : t.result));
                ctx.out.println();
                ctx.out.println("Events:");
                for (Event e : store.list(r.id())) {
                    if (e.taskId() == id) {
                        ctx.out.printf("  %s  %-17s %-8s %s%n", e.ts().toString().substring(11, 19), e.type(), e.agent(), Tables.cut(e.payload(), 110));
                    }
                }
            }
            return 0;
        }
    }
}
