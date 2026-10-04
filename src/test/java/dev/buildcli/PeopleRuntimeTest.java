package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.tools.ToolContext;
import dev.buildcli.application.tools.WorkspaceLock;
import dev.buildcli.application.tools.WriteFileTool;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Limits;
import dev.buildcli.domain.Permissions;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.domain.Roster;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.ToolCall;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Agents behave like people: one conversation at a time each, in parallel with each other, without deadlocks or lost writes. */
@Timeout(20)
class PeopleRuntimeTest {
    @TempDir Path workspace;

    static final Roster ROSTER = new Roster("backend", "ana", List.of(
            new Agent("ana", "architect", "", Set.of(), Permissions.none()),
            new Agent("bruno", "developer", "", Set.of(), Permissions.none())), Limits.defaults());

    static Task done(String to, String result) {
        Task t = new Task(1, null, "user", to, "", "");
        t.status = TaskStatus.DONE;
        t.result = result;
        return t;
    }

    static void awaitIdle(ChatSession s) throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        Thread.sleep(30);
        while (s.busy() && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        assertFalse(s.busy(), "still busy");
    }

    static List<String> agentTexts(ChatSession s) {
        return s.messages().stream().filter(m -> m.kind() == ChatSession.Kind.AGENT).map(ChatSession.Message::text).toList();
    }

    @Test
    void differentAgentsAnswerInParallel() throws Exception {
        CountDownLatch anaMayFinish = new CountDownLatch(1);
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            String me = request.target() == null ? "ana" : request.target();
            if (me.equals("ana")) {
                anaMayFinish.await(10, TimeUnit.SECONDS);
            }
            return done(me, me + " answered");
        });
        session.submit("team question");
        session.submit("quick one", List.of(), "bruno");
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!agentTexts(session).contains("bruno answered") && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        assertEquals(List.of("bruno answered"), agentTexts(session), "bruno answered while ana was still busy");
        assertTrue(session.isActive(ChatSession.MAIN));
        anaMayFinish.countDown();
        awaitIdle(session);
        assertEquals(2, agentTexts(session).size());
    }

    @Test
    void anAgentNeverWorksInTwoChatsAtOnce() throws Exception {
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maxInside = new AtomicInteger();
        List<String> order = new CopyOnWriteArrayList<>();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch firstMayFinish = new CountDownLatch(1);
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            maxInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
            order.add(request.text());
            if (request.text().equals("in the team chat")) {
                firstStarted.countDown();
                firstMayFinish.await(10, TimeUnit.SECONDS);
            } else {
                Thread.sleep(20);
            }
            inside.decrementAndGet();
            return done("ana", "ok " + request.text());
        });
        session.submit("in the team chat");
        session.submit("in ana's direct chat", List.of(), "ana");
        session.submit("team again");
        assertTrue(firstStarted.await(10, TimeUnit.SECONDS));
        assertTrue(session.queued() >= 1, "ana has not read the later messages yet");
        assertEquals(ChatSession.MAIN, session.agentThread("ana"), "busy in the team chat");
        firstMayFinish.countDown();
        awaitIdle(session);
        assertEquals(1, maxInside.get(), "one conversation at a time");
        assertEquals(List.of("in the team chat", "in ana's direct chat", "team again"), order, "in the order received");
    }

    @Test
    void streamedTextStaysInTheChatItWasWrittenFor() throws Exception {
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            // the HTTP client delivers streamed text on its own thread, not on the agent's
            Thread t = new Thread(() -> {
                ui.onText(1, "ana", "secret for the direct chat");
                ui.onEvent(new dev.buildcli.domain.Event(java.time.Instant.now(), "ToolCalled", 1, "ana", "read_file {path=a.txt}"));
            });
            t.start();
            t.join();
            ui.onEvent(new dev.buildcli.domain.Event(java.time.Instant.now(), "AgentReplied", 1, "ana", "done"));
            return done("ana", "done");
        });
        session.submit("hello", List.of(), "ana");
        awaitIdle(session);
        var seen = session.messages().stream().filter(m -> m.text().contains("secret") || m.text().contains("read a.txt")).toList();
        assertFalse(seen.isEmpty(), "the text was shown somewhere");
        assertTrue(seen.stream().allMatch(m -> m.thread().equals("ana")), "only in ana's chat: " + seen);
    }

    @Test
    void aHandoffThatWouldDeadlockIsRefusedInsteadOfHanging() throws Exception {
        CountDownLatch brunoBusy = new CountDownLatch(1);
        CountDownLatch anaHandedOff = new CountDownLatch(1);
        Map<String, String> results = new ConcurrentHashMap<>();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            String me = request.target() == null ? "ana" : request.target();
            String other = me.equals("ana") ? "bruno" : "ana";
            if (me.equals("bruno")) {
                brunoBusy.countDown();
                anaHandedOff.await(10, TimeUnit.SECONDS);
                Thread.sleep(100); // ana is now waiting for bruno
            } else {
                brunoBusy.await(10, TimeUnit.SECONDS);
            }
            String refusal = dispatcher.refusal(me, other);
            String result;
            if (refusal != null) {
                result = "refused: " + refusal;
            } else {
                if (me.equals("ana")) {
                    anaHandedOff.countDown();
                }
                result = dispatcher.run(me, other, () -> other + " did it");
            }
            results.put(me, result);
            return done(me, result);
        });
        session.submit("bruno, ask ana", List.of(), "bruno");
        session.submit("ana, ask bruno");
        awaitIdle(session);
        assertTrue(results.get("bruno").startsWith("refused: ana is waiting for you"), results.toString());
        assertEquals("bruno did it", results.get("ana"), "ana's handoff ran once bruno was free");
    }

    @Test
    void severalAgentsCanAskForApprovalAtOnceAndAreAnsweredInOrder() throws Exception {
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            String me = request.target() == null ? "ana" : request.target();
            boolean ok = ui.approve(new ApprovalRequest(me, "write", "Write " + me + ".txt", ""));
            return done(me, me + (ok ? " approved" : " denied"));
        });
        session.submit("ana, write");
        session.submit("bruno, write", List.of(), "bruno");
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (session.pendingCount() < 2 && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        assertEquals(2, session.pendingCount());
        var first = (ChatSession.Pending.Approval) session.pending();
        first.answer().complete(true);
        end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (session.pendingCount() != 1 && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        var second = (ChatSession.Pending.Approval) session.pending();
        assertFalse(first.agent().equals(second.agent()));
        second.answer().complete(false);
        awaitIdle(session);
        assertTrue(agentTexts(session).contains(first.agent() + " approved"));
        assertTrue(agentTexts(session).contains(second.agent() + " denied"));
    }

    @Test
    void aFileChangedWhileTheWriteWaitedForApprovalIsNotOverwritten() throws Exception {
        Path file = workspace.resolve("out/a.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "v1");
        Agent bruno = new Agent("bruno", "developer", "", Set.of("filesystem.write"),
                new Permissions(List.of("out/**"), List.of(), Duration.ofSeconds(5)));
        var ctx = new ToolContext(workspace, request -> {
            try {
                Files.writeString(file, "carla's version"); // someone else writes while bruno waits for approval
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return true;
        });
        String result = new WriteFileTool().execute(ctx, bruno, new ToolCall("1", "write_file", Map.of("path", "out/a.txt", "content", "v2")));
        assertTrue(result.startsWith("ERROR: out/a.txt was changed by someone else"), result);
        assertEquals("carla's version", Files.readString(file), "the other change survives");
    }

    @Test
    void workspaceChangesAreExclusiveButReadsAreShared() throws Exception {
        WorkspaceLock lock = new WorkspaceLock();
        AtomicInteger writers = new AtomicInteger();
        AtomicInteger overlap = new AtomicInteger();
        List<String> waitedFor = new CopyOnWriteArrayList<>();
        Runnable write = () -> {
            try {
                lock.exclusive(Thread.currentThread().getName(), waitedFor::add, () -> {
                    if (writers.incrementAndGet() > 1) {
                        overlap.incrementAndGet();
                    }
                    Thread.sleep(50);
                    writers.decrementAndGet();
                    return null;
                });
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        Thread a = Thread.ofVirtual().name("ana").start(write);
        Thread b = Thread.ofVirtual().name("bruno").start(write);
        a.join();
        b.join();
        assertEquals(0, overlap.get());
        assertEquals(1, waitedFor.size(), "the second writer was told who it waited for");
        CountDownLatch bothIn = new CountDownLatch(2);
        Runnable read = () -> {
            try {
                lock.shared(() -> {
                    bothIn.countDown();
                    return bothIn.await(2, TimeUnit.SECONDS);
                });
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        Thread r1 = Thread.ofVirtual().start(read);
        Thread r2 = Thread.ofVirtual().start(read);
        r1.join();
        r2.join();
        assertEquals(0, bothIn.getCount(), "two readers were inside at the same time");
        assertNotNull(lock);
    }
}
