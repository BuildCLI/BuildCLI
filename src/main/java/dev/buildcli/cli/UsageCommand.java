package dev.buildcli.cli;

import dev.buildcli.domain.AgentUsage;
import dev.buildcli.domain.RunInfo;
import dev.buildcli.infrastructure.StateStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Token usage per agent, derived from the event log. Tokens only: no prices are assumed. */
@Command(name = "usage", description = "Token usage per agent for a run (default: the latest)")
final class UsageCommand implements Callable<Integer> {
    private final CliContext ctx;

    @Option(names = "--run", description = "Run id (see 'buildcli runs')")
    String run;

    @Option(names = "--json", description = "Machine-readable output")
    boolean json;

    UsageCommand(CliContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Integer call() throws Exception {
        if (!ctx.hasState()) {
            ctx.out.println(json ? "{\"runs\":[]}" : "No runs yet for this project.");
            return 0;
        }
        try (StateStore store = ctx.openState()) {
            RunInfo r = TaskCommand.pickRun(ctx, store, run);
            if (r == null) {
                return run == null ? 0 : 2;
            }
            List<AgentUsage> usage = store.usage(r.id());
            long in = usage.stream().mapToLong(AgentUsage::inputTokens).sum();
            long out = usage.stream().mapToLong(AgentUsage::outputTokens).sum();
            int calls = usage.stream().mapToInt(AgentUsage::calls).sum();
            if (json) {
                List<String> rows = new ArrayList<>();
                for (AgentUsage u : usage) {
                    rows.add("{\"agent\":\"" + u.agent() + "\",\"calls\":" + u.calls() + ",\"inputTokens\":" + u.inputTokens()
                            + ",\"outputTokens\":" + u.outputTokens() + "}");
                }
                ctx.out.println("{\"run\":\"" + r.id() + "\",\"status\":\"" + r.status() + "\",\"agents\":[" + String.join(",", rows)
                        + "],\"totals\":{\"calls\":" + calls + ",\"inputTokens\":" + in + ",\"outputTokens\":" + out + "}}");
                return 0;
            }
            ctx.out.println("Run " + r.id() + "  " + r.status() + "  group " + r.group());
            List<List<String>> rows = new ArrayList<>();
            for (AgentUsage u : usage) {
                rows.add(List.of(u.agent(), String.valueOf(u.calls()), String.valueOf(u.inputTokens()), String.valueOf(u.outputTokens())));
            }
            rows.add(List.of("total", String.valueOf(calls), String.valueOf(in), String.valueOf(out)));
            Tables.print(ctx.out, List.of("AGENT", "CALLS", "INPUT", "OUTPUT"), rows);
        }
        return 0;
    }
}
