package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Permissions;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseEvent;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** With more chats than fit, the list scrolls with the wheel and follows the chat you choose. */
class ChatListScrollTest {
    @TempDir Path dir;

    ChatScreen screen() {
        var session = new ChatSession(ChatScreenTest.ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            Task t = new Task(1, null, "user", "ana", request.text(), "");
            t.status = TaskStatus.DONE;
            t.result = "ok";
            return t;
        });
        for (int i = 1; i <= 15; i++) {
            session.addContact(new Agent("agent" + i, "role" + i, "", Set.of(), Permissions.none()));
            session.openDirect("agent" + i);
        }
        var screen = new ChatScreen(session, Map.of(), dir, () -> { });
        ChatScreenTest.render(screen, 130, 30);
        return screen;
    }

    @Test
    void theWheelScrollsTheListAndTellsHowManyAreHidden() {
        var screen = screen();
        String out = ChatScreenTest.render(screen, 130, 30);
        assertTrue(out.contains("↓") && out.contains("more"), out);
        assertFalse(out.contains("agent15"), "it does not fit:\n" + out);
        for (int i = 0; i < 12; i++) {
            screen.handleMouseEvent(MouseEvent.scrollDown(5, 10));
        }
        out = ChatScreenTest.render(screen, 130, 30);
        assertTrue(out.contains("agent15"), out);
        assertTrue(out.contains("↑"), "and some are above now:\n" + out);
    }

    @Test
    void choosingAChatBelowTheFoldBringsItIntoView() {
        var screen = screen();
        screen.handleKeyEvent(KeyEvent.ofKey(KeyCode.UP, KeyModifiers.ALT), true);
        String last = screen.threadsForTest().get(screen.threadsForTest().size() - 1);
        assertTrue(screen.selectedForTest().equals(last), "Alt+Up from the first chat wraps to the last: " + screen.selectedForTest());
        String out = ChatScreenTest.render(screen, 130, 30);
        assertTrue(out.contains(last), "the chosen chat is visible:\n" + out);
    }
}
