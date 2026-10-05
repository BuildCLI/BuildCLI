package dev.buildcli;

import static dev.buildcli.PeopleRuntimeTest.ROSTER;
import static dev.buildcli.PeopleRuntimeTest.done;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.ports.ApprovalRequest;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** An agent waiting for an answer that is interrupted (it was removed, or the app is closing) says no and stays interrupted. */
@Timeout(20)
class InterruptedApprovalTest {
    @Test
    void interruptingAWaitingAgentDeniesAndKeepsTheInterruptFlag() throws Exception {
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> done("ana", "ok"));
        AtomicReference<Boolean> granted = new AtomicReference<>();
        AtomicBoolean stillInterrupted = new AtomicBoolean();
        Thread waiting = new Thread(() -> {
            granted.set(session.approve(new ApprovalRequest("ana", "write", "Write x", "diff")));
            stillInterrupted.set(Thread.currentThread().isInterrupted());
        });
        waiting.start();
        long end = System.nanoTime() + 5_000_000_000L;
        while (session.pendingCount() == 0 && System.nanoTime() < end) {
            Thread.onSpinWait();
        }
        assertEquals(1, session.pendingCount(), "the question is waiting");
        waiting.interrupt();
        waiting.join(5000);
        assertFalse(waiting.isAlive());
        assertEquals(Boolean.FALSE, granted.get(), "an interrupted question is a no");
        assertTrue(stillInterrupted.get(), "and whoever called can still see it was interrupted");
        assertEquals(0, session.pendingCount(), "the question is gone from the list");
    }
}
