package dev.buildcli.infrastructure;

import dev.buildcli.domain.Event;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.EscalationChoice;
import dev.buildcli.ports.UserInterface;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Plain-text UserInterface for {@code --headless} runs, pipes and CI: events are printed as they happen and decisions come
 * from a policy. With {@link Policy#ASK} the user is prompted on the console; the other policies never block, so a
 * script always finishes. Anything not explicitly allowed is denied.
 */
public final class ConsoleUi implements UserInterface {

    /** How approvals are decided without a terminal UI. */
    public enum Policy {
        /** Ask on the console. */
        ASK,
        /** Deny everything that needs approval (the safe default for scripts). */
        NONE,
        /** Approve file writes; deny commands, commits and trust requests. */
        WRITES,
        /** Approve everything. Only for throwaway workspaces. */
        ALL
    }

    private final PrintStream out;
    private final BufferedReader in;
    private final Policy policy;

    public ConsoleUi(PrintStream out, BufferedReader in, Policy policy) {
        this.out = out;
        this.in = in;
        this.policy = policy;
    }

    @Override
    public boolean approve(ApprovalRequest request) {
        out.println();
        out.println("[approval requested] " + request.agent() + ": " + request.summary());
        request.detail().lines().limit(40).forEach(l -> out.println("    " + l));
        boolean granted = switch (policy) {
            case ALL -> true;
            case WRITES -> request.kind().equals("write");
            case NONE -> false;
            case ASK -> askYesNo("Approve? [y/N] ");
        };
        out.println("[" + (granted ? "approved" : "denied") + (policy == Policy.ASK ? "" : " by --approve " + policy.name().toLowerCase()) + "]");
        return granted;
    }

    @Override
    public EscalationChoice escalate(int taskId, String agent, String objective, String reason) {
        out.println();
        out.println("[task #" + taskId + " needs you] " + agent + " failed after the automatic retries: " + reason);
        out.println("    objective: " + objective);
        if (policy != Policy.ASK) {
            out.println("[aborting: cannot ask without a terminal; use --approve ask on a terminal]");
            return EscalationChoice.ABORT;
        }
        while (true) {
            out.print("[r]etry, [s]kip this task, [a]bort the run? ");
            out.flush();
            String line = readLine();
            if (line == null) {
                return EscalationChoice.ABORT;
            }
            switch (line.strip().toLowerCase()) {
                case "r", "retry" -> {
                    return EscalationChoice.RETRY;
                }
                case "s", "skip" -> {
                    return EscalationChoice.SKIP;
                }
                case "a", "abort" -> {
                    return EscalationChoice.ABORT;
                }
                default -> out.println("please answer r, s or a");
            }
        }
    }

    @Override
    public void onEvent(Event e) {
        switch (e.type()) {
            case "RunStarted", "TaskCreated", "HandoffCreated", "ToolCalled", "ToolCompleted", "TaskCompleted", "TaskFailed",
                    "TaskRetried", "TaskEscalated", "TaskSkipped", "LimitReached", "AgentNudged" ->
                out.printf("%-16s #%d %-8s %s%n", e.type(), e.taskId(), e.agent(), e.payload().replace('\n', ' '));
            default -> { }
        }
    }

    private boolean askYesNo(String prompt) {
        out.print(prompt);
        out.flush();
        String line = readLine();
        return line != null && (line.strip().equalsIgnoreCase("y") || line.strip().equalsIgnoreCase("yes"));
    }

    private String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
