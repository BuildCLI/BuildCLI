package dev.buildcli;

import static dev.buildcli.PeopleRuntimeTest.ROSTER;
import static dev.buildcli.PeopleRuntimeTest.awaitIdle;
import static dev.buildcli.PeopleRuntimeTest.done;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ApprovalMode;
import dev.buildcli.application.ChatSession;
import dev.buildcli.ports.ApprovalRequest;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Manual, edits and auto: which questions are answered for the user, and which never are. */
@Timeout(20)
class ApprovalModeTest {
    @Test
    void eachModeAnswersOnlyItsOwnKindsAndNeverTheTrustQuestion() {
        for (String kind : List.of("write", "command", "git_commit", "trust")) {
            assertFalse(ApprovalMode.MANUAL.approves(kind), kind);
        }
        assertTrue(ApprovalMode.EDITS.approves("write"));
        assertFalse(ApprovalMode.EDITS.approves("command") || ApprovalMode.EDITS.approves("git_commit") || ApprovalMode.EDITS.approves("trust"));
        assertTrue(ApprovalMode.AUTO.approves("write") && ApprovalMode.AUTO.approves("command") && ApprovalMode.AUTO.approves("git_commit"));
        assertFalse(ApprovalMode.AUTO.approves("trust"), "trusting a project is never skipped");
        assertEquals(ApprovalMode.MANUAL, ApprovalMode.AUTO.next());
        assertEquals(ApprovalMode.MANUAL, ApprovalMode.parse("nonsense"), "anything unknown is the safe mode");
        assertEquals(ApprovalMode.AUTO, ApprovalMode.parse(" Auto "));
    }

    @Test
    void autoApprovesAWriteWithoutAskingAndManualWaitsForYou() throws Exception {
        AtomicReference<Boolean> answer = new AtomicReference<>();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            answer.set(ui.approve(new ApprovalRequest("ana", "write", "Write a.txt", "+hello")));
            return done("ana", "ok");
        });
        session.approvalMode(ApprovalMode.AUTO);
        session.submit("write a file", List.of(), "ana");
        awaitIdle(session);
        assertEquals(Boolean.TRUE, answer.get());
        assertEquals(0, session.pendingCount());

        answer.set(null);
        session.approvalMode(ApprovalMode.MANUAL);
        session.submit("again", List.of(), "ana");
        long end = System.nanoTime() + 5_000_000_000L;
        while (session.pending() == null && System.nanoTime() < end) {
            Thread.onSpinWait();
        }
        assertTrue(session.pending() != null, "manual asks");
        session.stop("ana");
        awaitIdle(session);
    }
}
