package dev.buildcli;

import static dev.buildcli.PeopleRuntimeTest.ROSTER;
import static dev.buildcli.PeopleRuntimeTest.awaitIdle;
import static dev.buildcli.PeopleRuntimeTest.done;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** The front end hears about changes instead of asking for them. */
@Timeout(20)
class ChangeListenerTest {
    @Test
    void aListenerIsCalledWhenAnythingVisibleChangesAndNeverWhenNothingDoes() throws Exception {
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> done("ana", "ok"));
        AtomicInteger calls = new AtomicInteger();
        session.onChange(calls::incrementAndGet);
        Thread.sleep(100);
        assertEquals(0, calls.get(), "idle: silence");

        long before = session.version();
        session.submit("hello", List.of(), "ana");
        awaitIdle(session);
        assertTrue(calls.get() >= 2, "the message and the reply: " + calls.get());
        assertEquals(session.version() - before, calls.get(), "one call per change, so the listener and the counter agree");
    }
}
