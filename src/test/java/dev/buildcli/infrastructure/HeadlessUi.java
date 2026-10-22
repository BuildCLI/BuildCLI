package dev.buildcli.infrastructure;

import dev.buildcli.domain.Event;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.EscalationChoice;
import dev.buildcli.ports.UserInterface;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/** Scripted user for tests and benchmarks: no terminal involved. */
public final class HeadlessUi implements UserInterface {
    private final Predicate<ApprovalRequest> approver;
    private final EscalationChoice escalation;
    private final boolean print;
    public final List<ApprovalRequest> approvals = new CopyOnWriteArrayList<>();
    public final List<Event> events = new CopyOnWriteArrayList<>();
    public volatile int escalations;

    public HeadlessUi(Predicate<ApprovalRequest> approver, EscalationChoice escalation, boolean print) {
        this.approver = approver;
        this.escalation = escalation;
        this.print = print;
    }

    @Override
    public boolean approve(ApprovalRequest request) {
        approvals.add(request);
        return approver.test(request);
    }

    @Override
    public EscalationChoice escalate(int taskId, String agent, String objective, String reason) {
        escalations++;
        return escalation;
    }

    @Override
    public void onEvent(Event e) {
        events.add(e);
        if (print) {
            System.out.printf("%-18s #%d %-6s %s%n", e.type(), e.taskId(), e.agent(), e.payload());
        }
    }
}
