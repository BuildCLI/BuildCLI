package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.tamboui.tui.event.KeyCode;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Creating an agent: every step can go back, and what the agent may do is ticked from a list, not typed. */
class AgentWizardTest {
    @TempDir Path dir;

    ChatScreen screen() {
        var session = new ChatSession(ChatScreenTest.ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            Task t = new Task(1, null, "user", "ana", request.text(), "");
            t.status = TaskStatus.DONE;
            t.result = "ok";
            return t;
        });
        return new ChatScreen(session, Map.of("ana", "ollama:x"), dir, () -> { }, ChatScreen.basicServices(session, Map.of("ana", "ollama:x")));
    }

    @Test
    void escGoesBackOneStepAndKeepsWhatWasTyped() {
        var screen = screen();
        ChatScreenTest.render(screen, 120, 36);
        screen.openNewAgentForTest();
        String out = ChatScreenTest.render(screen, 120, 36);
        assertTrue(out.contains("New agent: name (1/4)"), out);
        ChatScreenTest.type(screen, "rita");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        ChatScreenTest.type(screen, "x");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        out = ChatScreenTest.render(screen, 120, 36);
        assertTrue(out.contains("What rita may do (3/4)"), out);
        assertTrue(out.contains("[x] filesystem.read") && out.contains("[ ] git.commit"), "the capabilities are listed to tick:\n" + out);
        assertTrue(out.contains("create and change files"), "each says what it allows:\n" + out);

        ChatScreenTest.key(screen, KeyCode.ESCAPE);
        out = ChatScreenTest.render(screen, 120, 36);
        assertTrue(out.contains("Role of rita (2/4)") && out.contains("assistantx"), "back to the role, as typed:\n" + out);
        ChatScreenTest.key(screen, KeyCode.ESCAPE);
        out = ChatScreenTest.render(screen, 120, 36);
        assertTrue(out.contains("New agent: name (1/4)") && out.contains("rita"), out);
        ChatScreenTest.key(screen, KeyCode.ESCAPE);
        out = ChatScreenTest.render(screen, 120, 36);
        assertFalse(out.contains("New agent: name"), "Esc on the first step cancels:\n" + out);
        assertTrue(screen.settingsOpenForTest(), "and leaves you in Settings, not outside it");
    }

    @Test
    void ticksChangeWhatTheAgentMayDo() {
        var screen = screen();
        ChatScreenTest.render(screen, 120, 36);
        screen.openNewAgentForTest();
        ChatScreenTest.type(screen, "rita");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        ChatScreenTest.key(screen, KeyCode.ENTER);
        ChatScreenTest.key(screen, KeyCode.DOWN);
        ChatScreenTest.type(screen, " ");
        String out = ChatScreenTest.render(screen, 120, 36);
        assertTrue(out.contains("[x] chat.post") && out.contains("[ ] agent.handoff"), "Space ticks the highlighted line:\n" + out);
    }
}
