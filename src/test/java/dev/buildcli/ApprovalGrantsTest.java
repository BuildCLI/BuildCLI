package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Limits;
import dev.buildcli.domain.Permissions;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.domain.Roster;
import dev.buildcli.ports.ApprovalRequest;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** "Always here": a permission for one agent, one kind of request and one chat; never for a commit; taken back by /revoke. */
class ApprovalGrantsTest {
    static final Roster ROSTER = new Roster("backend", "ana", List.of(
            new Agent("ana", "architect", "", Set.of(), Permissions.none()),
            new Agent("bruno", "developer", "", Set.of(), Permissions.none())), Limits.defaults());

    final List<String> results = new CopyOnWriteArrayList<>();

    ChatSession session(List<ApprovalRequest> asks) {
        return new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            String me = request.target() == null ? "ana" : request.target();
            for (ApprovalRequest ask : asks) {
                if (ask.agent().equals(me)) {
                    results.add(me + ":" + ask.kind() + ":" + ui.approve(ask));
                }
            }
            Task t = new Task(1, null, "user", me, request.text(), "");
            t.status = TaskStatus.DONE;
            t.result = "done";
            return t;
        });
    }

    static ChatSession.Pending.Approval awaitPending(ChatSession s) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < end) {
            if (s.pending() instanceof ChatSession.Pending.Approval a) {
                return a; // read once: the request can be answered and removed a moment later
            }
            Thread.sleep(5);
        }
        return null;
    }

    static void idle(ChatSession s) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        Thread.sleep(30);
        while ((s.busy() || s.pending() != null) && System.nanoTime() < end) {
            Thread.sleep(10);
        }
    }

    static ApprovalRequest write(String agent) {
        return new ApprovalRequest(agent, "write", "Write A.java", "diff", "write", "let " + agent + " edit files in this chat");
    }

    @Test
    void alwaysAnswersTheNextSameRequestWithoutAskingAgain() throws Exception {
        var s = session(List.of(write("ana"), write("ana")));
        s.submit("edit");
        var first = awaitPending(s);
        assertNotNull(first);
        s.approveAlways(first);
        idle(s);
        assertNull(s.pending(), "the second write did not ask");
        assertEquals(List.of("ana:write:true", "ana:write:true"), results);
        assertEquals(List.of("let ana edit files in this chat (you can still undo)".replace(" (you can still undo)", "")), s.grants(ChatSession.MAIN));
    }

    @Test
    void theGrantIsForThatAgentThatKindOfRequestAndThatChatOnly() throws Exception {
        var cmd = new ApprovalRequest("ana", "command", "Run x", "d", "command:mvn\u0000verify", "let ana run exactly: mvn verify");
        var otherCmd = new ApprovalRequest("ana", "command", "Run y", "d", "command:rm\u0000-rf", "let ana run exactly: rm -rf");
        var s = session(List.of(cmd, otherCmd));
        s.submit("go");
        s.approveAlways(awaitPending(s));
        ChatSession.Pending.Approval second;
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        do {
            second = awaitPending(s);
        } while (second != null && second.request().summary().equals("Run x") && System.nanoTime() < end);
        assertEquals("Run y", second.request().summary(), "a different command is still asked");
        second.answer().complete(false);
        idle(s);
        assertEquals(List.of("ana:command:true", "ana:command:false"), results);
        s.close();
    }

    @Test
    void aGrantInOneChatDoesNotApplyInAnother() throws Exception {
        var s = session(List.of(write("ana")));
        s.submit("in the group");
        s.approveAlways(awaitPending(s));
        idle(s);
        results.clear();
        s.submit("in a direct chat", List.of(), "ana");
        var asked = awaitPending(s);
        assertNotNull(asked, "the direct chat asks again");
        asked.answer().complete(false);
        idle(s);
        assertEquals(List.of("ana:write:false"), results);
        s.close();
    }

    @Test
    void aRequestWithoutAGrantKeyIsAlwaysAsked() throws Exception {
        var commit = new ApprovalRequest("ana", "git_commit", "Commit 1 path(s)", "d");
        var s = session(List.of(commit, commit));
        s.submit("commit");
        var first = awaitPending(s);
        s.approveAlways(first);
        ChatSession.Pending.Approval second;
        do {
            second = awaitPending(s);
        } while (second == first);
        assertNotNull(second, "approving with A does not turn a commit into an automatic one");
        assertTrue(s.grants(ChatSession.MAIN).isEmpty());
        second.answer().complete(true);
        idle(s);
        s.close();
    }

    @Test
    void readingThePendingQuestionWhileItIsAnsweredNeverFails() throws Exception {
        var asks = new java.util.ArrayList<ApprovalRequest>();
        for (int i = 0; i < 300; i++) {
            asks.add(new ApprovalRequest("ana", "git_commit", "Commit " + i, "d"));
        }
        var s = session(asks);
        s.submit("many questions");
        // the screen reads pending() on every frame while answers arrive from another thread
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        int answered = 0;
        while ((s.busy() || s.pending() != null) && System.nanoTime() < end) {
            var p = s.pending();
            if (p instanceof ChatSession.Pending.Approval a) {
                a.answer().complete(true);
                answered++;
            }
        }
        assertEquals(300, results.size());
        assertTrue(answered >= 1);
        s.close();
    }

    @Test
    void revokeMakesItAskAgain() throws Exception {
        var s = session(List.of(write("ana")));
        s.submit("one");
        s.approveAlways(awaitPending(s));
        idle(s);
        assertEquals(1, s.revokeGrants(ChatSession.MAIN));
        assertEquals(0, s.revokeGrants(ChatSession.MAIN));
        results.clear();
        s.submit("two");
        var again = awaitPending(s);
        assertNotNull(again);
        again.answer().complete(false);
        idle(s);
        assertEquals(List.of("ana:write:false"), results);
        s.close();
    }
}
