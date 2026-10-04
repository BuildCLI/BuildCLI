package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.Settings;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.ports.SettingsStore.Scope;
import dev.tamboui.tui.event.KeyCode;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** /model: change the default model, or one agent's, without opening the settings. */
class ModelCommandTest {
    @TempDir Path dir;

    final InAppKeyTest fake = new InAppKeyTest();

    ChatScreen screen() {
        var session = new ChatSession(ChatScreenTest.ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            Task t = new Task(1, null, "user", "ana", request.text(), "");
            t.status = TaskStatus.DONE;
            t.result = "ok";
            return t;
        });
        var screen = new ChatScreen(session, Map.of("ana", "x:y", "bruno", "x:y"), dir, () -> { }, fake.services());
        ChatScreenTest.render(screen, 130, 36);
        return screen;
    }

    String run(ChatScreen screen, String command) {
        ChatScreenTest.type(screen, command);
        ChatScreenTest.key(screen, KeyCode.ENTER);
        return ChatScreenTest.render(screen, 130, 36);
    }

    @Test
    void aModelOnItsOwnBecomesTheProjectDefault() {
        var screen = screen();
        String out = run(screen, "/model deepseek:deepseek-chat");
        assertEquals("deepseek:deepseek-chat", fake.settings.stored(Scope.PROJECT, Settings.DEFAULT_MODEL));
        assertNull(fake.settings.stored(Scope.GLOBAL, Settings.DEFAULT_MODEL), "only this project");
        assertTrue(out.contains("Default model: deepseek:deepseek-chat (this project)"), out);
    }

    @Test
    void anAgentCanHaveItsOwnModel() {
        var screen = screen();
        String out = run(screen, "/model @bruno deepseek:deepseek-reasoner");
        assertEquals("deepseek:deepseek-reasoner", fake.settings.stored(Scope.PROJECT, Settings.AGENT_MODEL + "bruno"));
        assertNull(fake.settings.stored(Scope.PROJECT, Settings.DEFAULT_MODEL), "the default is untouched");
        assertTrue(out.contains("bruno now uses deepseek:deepseek-reasoner"), out);
    }

    @Test
    void mistakesAreExplainedAndNothingIsSaved() {
        var screen = screen();
        assertTrue(run(screen, "/model deepseek").contains("written provider:model"));
        assertTrue(run(screen, "/model nope:thing").contains("Unknown provider 'nope'"));
        assertTrue(run(screen, "/model @zed deepseek:x").contains("There is no agent called zed"));
        assertTrue(run(screen, "/model one two three").contains("Use /model provider:model"));
        assertNull(fake.settings.stored(Scope.PROJECT, Settings.DEFAULT_MODEL));
        assertNull(fake.settings.stored(Scope.PROJECT, Settings.AGENT_MODEL + "zed"));
    }

    @Test
    void withoutArgumentsItOpensTheModelsSection() {
        var screen = screen();
        ChatScreenTest.type(screen, "/model");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        String out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(screen.settingsOpenForTest(), out);
        assertTrue(out.contains("▌ Models") && out.contains("Default model"), "the Models section is the one shown:\n" + out);
    }
}
