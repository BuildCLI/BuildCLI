package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ApprovalMode;
import dev.tamboui.tui.event.KeyCode;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The approval mode chosen in Settings applies at once; it used to wait for the next start. */
class ApprovalModeSettingTest {
    @TempDir Path dir;

    @Test
    void choosingAutoInSettingsSwitchesTheRunningChat() {
        var chat = new ChatScreenTest();
        chat.dir = dir;
        var session = chat.session();
        var screen = new ChatScreen(session, Map.of("ana", "ollama/qwen", "bruno", "ollama/qwen"), dir, () -> { }, ChatScreen.basicServices(session, Map.of()));
        ChatScreenTest.render(screen, 130, 40);
        assertEquals(ApprovalMode.MANUAL, session.approvalMode());
        ChatScreenTest.type(screen, "/settings");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        String out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("Approval mode"), out);
        // the General section: Enter sends, Mouse, Bell, Agent-to-agent, Language, Approval mode
        for (int i = 0; i < 5; i++) {
            ChatScreenTest.key(screen, KeyCode.DOWN);
        }
        ChatScreenTest.key(screen, KeyCode.RIGHT);
        assertEquals(ApprovalMode.EDITS, session.approvalMode());
        ChatScreenTest.key(screen, KeyCode.RIGHT);
        assertEquals(ApprovalMode.AUTO, session.approvalMode(), ChatScreenTest.render(screen, 130, 40));
    }
}
