package dev.buildcli.application.tools;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import dev.buildcli.ports.ToolSpec.Param;
import java.util.List;
import java.util.Map;

/**
 * Commands are structured argv, never shell strings: no pipes, redirects or expansion. A command whose argv starts with
 * an allow-list entry runs; anything else needs the user's approval. The allow list reduces accidents; it is not a
 * sandbox (an allowed {@code mvn test} still runs project code).
 */
public final class RunCommandTool implements Tool {
    @Override
    public String name() {
        return "run_command";
    }

    @Override
    public String capability() {
        return Capability.COMMAND_EXECUTE;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(name(),
                "Run a command in the workspace. Pass argv as an array of separate strings, never a shell string.",
                List.of(new Param("argv", "Command and arguments, e.g. [\"ls\", \"-la\"]", true, true)));
    }

    @Override
    public boolean returnsExternalContent() {
        return true;
    }

    @Override
    public String execute(ToolContext ctx, Agent agent, ToolCall call) throws Exception {
        Object raw = call.args().get("argv");
        if (!(raw instanceof List<?> list) || list.isEmpty() || !list.stream().allMatch(String.class::isInstance)) {
            return "ERROR: argv must be a non-empty array of strings (no shell strings, pipes or expansion)";
        }
        List<String> argv = list.stream().map(String.class::cast).toList();
        String refusal = CommandGuard.refusal(argv, CommandGuard.isWindows());
        if (refusal != null) {
            return "ERROR: " + refusal;
        }
        boolean allowed = agent.permissions().commandAllow().stream()
                .anyMatch(prefix -> argv.size() >= prefix.size() && argv.subList(0, prefix.size()).equals(prefix));
        if (!allowed && !ctx.approve(new ApprovalRequest(agent.name(), "command", "Run outside policy: " + String.join(" ", argv),
                "Not in " + agent.name() + "'s command allow list.",
                "command:" + String.join("\u0000", argv), "let " + agent.name() + " run exactly: " + String.join(" ", argv)))) {
            return "DENIED: command not allowed by policy and the user rejected it: " + String.join(" ", argv);
        }
        ProcessRunner.Result r = ctx.exclusive(() -> ProcessRunner.run(argv, ctx.workspace(), agent.permissions().commandTimeout(), Map.of()));
        if (r.timedOut()) {
            return "ERROR: command timed out after " + agent.permissions().commandTimeout();
        }
        return "exit=" + r.exitCode() + "\n" + ToolContext.cap(r.output(), ToolContext.MAX_OUTPUT);
    }
}
