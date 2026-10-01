package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.ports.ApprovalRequest;
import dev.tamboui.tui.event.KeyCode;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The approval dialog offers "Always here" only where it is safe, and /revoke takes it back. */
class AlwaysHereScreenTest {
    @TempDir Path dir;

    final List<Boolean> answers = new CopyOnWriteArrayList<>();

    ChatSession session(ApprovalRequest ask) {
        return new ChatSession(ChatScreenTest.ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            answers.add(ui.approve(ask));
            Task t = new Task(1, null, "user", "ana", request.text(), "");
            t.status = TaskStatus.DONE;
            t.result = "ok";
            return t;
        });
    }

    static void waitForDialog(ChatSession s) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (s.pending() == null && System.nanoTime() < end) {
            Thread.sleep(10);
        }
    }

    @Test
    void aWriteOffersAlwaysHereAndRevokeAsksAgain() throws Exception {
        var s = session(new ApprovalRequest("ana", "write", "Write A.java", "+x", "write", "let ana edit files in this chat"));
        var screen = new ChatScreen(s, Map.of("ana", "m", "bruno", "m"), dir, () -> { });
        ChatScreenTest.render(screen, 130, 36);
        ChatScreenTest.type(screen, "edit");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        waitForDialog(s);
        String out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("Always here  A"), out);
        assertTrue(out.contains("let ana edit files in this chat"), out);
        ChatScreenTest.type(screen, "a");
        ChatScreenTest.idle(s);
        assertEquals(List.of(true), answers);

        ChatScreenTest.type(screen, "/revoke");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        String after = ChatScreenTest.render(screen, 130, 36);
        assertTrue(after.contains("Asking again from now on. Was allowed: let ana edit files in this chat"), after);
        ChatScreenTest.type(screen, "/revoke");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        assertTrue(ChatScreenTest.render(screen, 130, 36).contains("Nothing is approved automatically in this chat."));
    }

    @Test
    void aCommitNeverOffersIt() throws Exception {
        var s = session(new ApprovalRequest("ana", "git_commit", "Commit 1 path(s)", "d"));
        var screen = new ChatScreen(s, Map.of("ana", "m", "bruno", "m"), dir, () -> { });
        ChatScreenTest.render(screen, 130, 36);
        ChatScreenTest.type(screen, "commit");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        waitForDialog(s);
        String out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("Approve  Y"), out);
        assertFalse(out.contains("Always here"), out);
        ChatScreenTest.type(screen, "a");
        assertTrue(s.pending() != null || answers.isEmpty(), "A does nothing on a commit");
        ChatScreenTest.type(screen, "n");
        ChatScreenTest.idle(s);
        assertEquals(List.of(false), answers);
    }
}
