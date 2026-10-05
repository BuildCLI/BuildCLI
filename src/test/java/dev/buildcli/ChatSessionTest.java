package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.ChatSession.Kind;
import dev.buildcli.application.ChatSession.State;
import dev.buildcli.application.Events;
import dev.buildcli.application.Orchestrator;
import dev.buildcli.application.RunAborted;
import dev.buildcli.application.ToolRuntime;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Event;
import dev.buildcli.domain.Limits;
import dev.buildcli.domain.Permissions;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.Roster;
import dev.buildcli.infrastructure.ScriptedGateway;
import dev.buildcli.infrastructure.StateStore;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.LlmGateway;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChatSessionTest {
    @TempDir Path workspace;

    static final Agent ANA = new Agent("ana", "architect", "You coordinate.", Set.of("filesystem.read", "agent.handoff"), Permissions.none());
    static final Agent BRUNO = new Agent("bruno", "developer", "You implement.", Set.of("filesystem.read"), Permissions.none());
    static final Roster ROSTER = new Roster("backend", "ana", List.of(ANA, BRUNO), Limits.defaults());

    /** Runs each request through a real orchestrator with the given model. */
    ChatSession.Executor orchestrated(LlmGateway llm, List<Orchestrator.Request> seen) {
        return (team, request, ui, cancelled, dispatcher) -> {
            seen.add(request);
            try (StateStore store = new StateStore(StateStore.IN_MEMORY)) {
                Events events = new Events(store, "run", ui);
                Orchestrator o = new Orchestrator(team, llm, new ToolRuntime(workspace, ui, events), ui, events);
                o.cancelWhen(cancelled);
                o.dispatchWith(dispatcher);
                return o.run(request);
            }
        };
    }

    static void awaitIdle(ChatSession s) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        Thread.sleep(50);
        while ((s.busy() || s.queued() > 0) && System.nanoTime() < end) {
            Thread.sleep(20);
        }
        assertFalse(s.busy() || s.queued() > 0, "the session did not become idle");
    }

    static List<String> texts(ChatSession s, Kind kind) {
        return s.messages().stream().filter(m -> m.kind() == kind).map(ChatSession.Message::text).toList();
    }

    @Test
    void messagesSentWhileTheTeamIsBusyWaitInOrderAndAreAllAnswered() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        List<String> order = new java.util.concurrent.CopyOnWriteArrayList<>();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            order.add(request.text());
            if (request.text().equals("first")) {
                release.await(10, TimeUnit.SECONDS);
            }
            Task t = new Task(1, null, "user", "ana", request.text(), "");
            t.status = dev.buildcli.domain.TaskStatus.DONE;
            t.result = "answer to " + request.text();
            return t;
        });
        long first = session.submit("first");
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (order.isEmpty() && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        long second = session.submit("second");
        long third = session.submit("third");

        assertTrue(session.busy());
        assertEquals(2, session.queued());
        var byId = session.messages().stream().collect(java.util.stream.Collectors.toMap(ChatSession.Message::id, m -> m));
        assertEquals(State.RUNNING, byId.get(first).state());
        assertEquals(State.QUEUED, byId.get(second).state());

        release.countDown();
        awaitIdle(session);
        assertEquals(List.of("first", "second", "third"), order);
        assertEquals(List.of("answer to first", "answer to second", "answer to third"), texts(session, Kind.AGENT));
        assertTrue(session.messages().stream().filter(m -> m.kind() == Kind.USER).allMatch(m -> m.state() == State.DONE));
        assertEquals(third, session.messages().stream().filter(m -> m.kind() == Kind.USER).reduce((a, b) -> b).orElseThrow().id());
    }

    @Test
    void aMentionSendsTheMessageStraightToThatAgent() throws Exception {
        List<Orchestrator.Request> seen = new ArrayList<>();
        var llm = new ScriptedGateway().say("bruno", "I can help with that.");
        var session = new ChatSession(ROSTER, orchestrated(llm, seen));
        session.submit("hey @Bruno, what do you think?");
        awaitIdle(session);

        assertEquals("bruno", seen.get(0).target());
        assertEquals(List.of("I can help with that."), texts(session, Kind.AGENT));
        assertEquals("bruno", session.messages().stream().filter(m -> m.kind() == Kind.AGENT).findFirst().orElseThrow().author());
        assertEquals(List.of(), session.mentioned("mail me at someone@ana.example"), "an address is not a mention");
        assertEquals(List.of(), session.mentioned("@nobody hello"));
        assertEquals(List.of("ana"), session.mentioned("ask @ana"));
    }

    @Test
    void aFailureIsShownAndTheSessionKeepsWorking() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("connection refused: localhost:11434");
            }
            Task t = new Task(1, null, "user", "ana", request.text(), "");
            t.status = dev.buildcli.domain.TaskStatus.DONE;
            t.result = "fine";
            return t;
        });
        long bad = session.submit("one");
        awaitIdle(session);
        assertTrue(texts(session, Kind.ERROR).get(0).contains("connection refused"), texts(session, Kind.ERROR).toString());
        assertTrue(texts(session, Kind.ERROR).get(0).contains("/retry"), "says how to try again");
        assertEquals(State.FAILED, session.messages().get(0).state());
        assertEquals(bad, session.lastFailedMessage());

        assertTrue(session.retry(bad));
        awaitIdle(session);
        assertEquals(List.of("fine"), texts(session, Kind.AGENT));
        assertEquals(-1, session.lastFailedMessage(), "a retry resends the same message");
        assertEquals(1, session.messages().stream().filter(m -> m.kind() == Kind.USER).count());
        var resent = session.messages().stream().filter(m -> m.kind() == Kind.USER).findFirst().orElseThrow();
        assertEquals(State.DONE, resent.state());
        assertEquals(resent.id(), session.messages().get(session.messages().size() - 2).id(), "a resent message moves to the end");
    }

    @Test
    void laterMessagesCarryTheConversationSoFarAsContext() throws Exception {
        List<Orchestrator.Request> seen = new ArrayList<>();
        var llm = new ScriptedGateway().say("ana", "The port is 8080.").say("ana", "Yes, 8080.");
        var session = new ChatSession(ROSTER, orchestrated(llm, seen));
        session.submit("what port do we use?");
        awaitIdle(session);
        session.submit("and is that the default?");
        awaitIdle(session);

        assertEquals("", seen.get(0).history());
        assertTrue(seen.get(1).history().contains("user: what port do we use?"), seen.get(1).history());
        assertTrue(seen.get(1).history().contains("ana: The port is 8080."), seen.get(1).history());
        assertFalse(seen.get(1).history().contains("is that the default"), "the current message is not its own history");
    }

    @Test
    void aDirectChatGoesToItsAgentAndKeepsItsOwnThreadAndHistory() throws Exception {
        List<Orchestrator.Request> seen = new ArrayList<>();
        var llm = new ScriptedGateway().say("ana", "Roster answer.").say("bruno", "Direct answer.").say("bruno", "Again.");
        var session = new ChatSession(ROSTER, orchestrated(llm, seen));
        session.submit("hello team");
        awaitIdle(session);
        session.submit("hi bruno", List.of(), "bruno");
        awaitIdle(session);
        session.submit("more", List.of(), "bruno");
        awaitIdle(session);

        assertEquals("ana", seen.get(0).target(), "the team chat without a mention goes to its admin");
        assertEquals("bruno", seen.get(1).target());
        assertEquals("", seen.get(1).history(), "the team conversation is not part of the direct chat");
        assertTrue(seen.get(2).history().contains("bruno: Direct answer."));
        var threads = session.messages().stream().map(m -> m.kind() + ":" + m.thread()).toList();
        assertEquals(List.of("USER:", "AGENT:", "USER:bruno", "AGENT:bruno", "USER:bruno", "AGENT:bruno"), threads);
        session.system("note");
        assertEquals(ChatSession.EVERYWHERE, session.messages().get(6).thread(), "local notes show in every chat");
    }

    @Test
    void stopEndsTheRunAndDeniesAnOpenQuestion() throws Exception {
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            boolean ok = ui.approve(new ApprovalRequest("bruno", "write", "Write out/a.txt", "+hi"));
            assertFalse(ok, "stopping answers 'no'");
            if (cancelled.getAsBoolean()) {
                throw new RunAborted("stopped by the user");
            }
            throw new IllegalStateException("should have been cancelled");
        });
        session.submit("do it");
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (session.pending() == null && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        assertTrue(session.pending() instanceof ChatSession.Pending.Approval, "the UI can see the question");
        session.stop();
        awaitIdle(session);
        assertTrue(texts(session, Kind.SYSTEM).get(0).startsWith("Stopped"));
        assertEquals(State.FAILED, session.messages().get(0).state());
    }

    @Test
    void toolEventsBecomeShortActivityLinesWithoutDumpingFileContents() {
        var session = new ChatSession(ROSTER, (tm, r, u, c, d) -> null);
        session.onEvent(new Event(Instant.now(), "ToolCalled", 2, "bruno", "write_file {path=out/a.txt, content=SECRET-BODY-OF-THE-FILE}"));
        session.onEvent(new Event(Instant.now(), "ToolCompleted", 2, "bruno", "ok: OK: wrote 20 chars"));
        session.onEvent(new Event(Instant.now(), "ToolCalled", 2, "bruno", "run_command {argv=[rm, -rf, out]}"));
        session.onEvent(new Event(Instant.now(), "ToolCompleted", 2, "bruno", "denied: DENIED: not allowed"));
        var lines = session.messages();
        assertEquals("wrote out/a.txt", lines.get(0).text());
        assertEquals(State.DONE, lines.get(0).state());
        assertEquals("ran rm -rf out (denied: DENIED: not allowed)", lines.get(1).text());
        assertEquals(State.FAILED, lines.get(1).state());
    }
}
