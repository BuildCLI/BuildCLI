package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.tools.WorkspaceLock;
import dev.buildcli.domain.FileChange;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.tamboui.tui.event.KeyCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The changes card: seeing what an agent wrote, and undoing it only after seeing it. */
class ChangesScreenTest {
    @TempDir Path dir;

    ChatSession session() throws Exception {
        Files.writeString(dir.resolve("Hello.java"), "class Hello { int v = 2; }\n");
        var s = new ChatSession(ChatScreenTest.ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            ui.fileChanged(new FileChange("ana", "Hello.java", true, "class Hello { int v = 1; }\n", "class Hello { int v = 2; }\n"));
            Task t = new Task(1, null, "user", "ana", request.text(), "");
            t.status = TaskStatus.DONE;
            t.result = "changed it";
            return t;
        });
        s.workspace(dir, new WorkspaceLock());
        return s;
    }

    @Test
    void theCardShowsTheFilesAndUndoAsksBeforeTouchingAnything() throws Exception {
        var s = session();
        var screen = new ChatScreen(s, Map.of("ana", "m", "bruno", "m"), dir, () -> { });
        ChatScreenTest.render(screen, 130, 36);
        ChatScreenTest.type(screen, "set v to 2");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while ((s.busy() || s.lastChanges(ChatSession.MAIN) < 0) && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        String out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("ana changed 1 file: Hello.java"), out);
        assertTrue(out.contains("+1 −1"), out);
        assertTrue(out.contains("Review") && out.contains("Undo"), out);

        ChatScreenTest.type(screen, "/undo");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("Undo ana's changes?"), out);
        assertTrue(out.contains("-class Hello { int v = 2; }") && out.contains("+class Hello { int v = 1; }") || out.contains("int v = 1"), out);
        assertTrue(Files.readString(dir.resolve("Hello.java")).contains("v = 2"), "nothing changes until you confirm");

        ChatScreenTest.key(screen, KeyCode.ESCAPE);
        assertTrue(Files.readString(dir.resolve("Hello.java")).contains("v = 2"), "Esc cancels");

        ChatScreenTest.type(screen, "/undo");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        ChatScreenTest.type(screen, "y");
        assertTrue(Files.readString(dir.resolve("Hello.java")).contains("v = 1"), "confirmed: the old content is back");
        out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("Undid ana's changes: restored Hello.java"), out);
        assertTrue(out.contains("undone"), out);
        assertFalse(out.contains(" Undo "), "no second undo for the same changes:\n" + out);
        assertEquals(-1, s.lastChanges(ChatSession.MAIN));
    }

    @Test
    void reviewShowsTheDiffWithoutOfferingToChangeAnythingUntilUIsPressed() throws Exception {
        var s = session();
        var screen = new ChatScreen(s, Map.of("ana", "m", "bruno", "m"), dir, () -> { });
        ChatScreenTest.render(screen, 130, 36);
        ChatScreenTest.type(screen, "go");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while ((s.busy() || s.lastChanges(ChatSession.MAIN) < 0) && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        ChatScreenTest.type(screen, "/review");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        String out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("ana's changes"), out);
        assertTrue(out.contains("Undo these changes  U"), out);
        ChatScreenTest.type(screen, "u");
        out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("Undo ana's changes?"), out);
        assertTrue(out.contains("Undo  Y") && out.contains("Cancel  N"), out);
        ChatScreenTest.type(screen, "n");
        assertFalse(screen.viewerOpenForTest());
        assertTrue(Files.readString(dir.resolve("Hello.java")).contains("v = 2"));
    }
}
