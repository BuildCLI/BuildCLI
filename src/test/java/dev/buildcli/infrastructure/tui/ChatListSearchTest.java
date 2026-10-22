package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The search box above the chat list: by name, by role, by something said in a chat, and to start a chat with an agent. */
class ChatListSearchTest {
    final ChatScreenTest base = new ChatScreenTest();

    ChatScreen screen(ChatSession session) {
        base.dir = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"));
        return base.screen(session);
    }

    static void ctrlK(ChatScreen screen) {
        screen.handleKeyEvent(KeyEvent.ofChar('k', KeyModifiers.CTRL), true);
    }

    @Test
    void findsAChatByNameByRoleAndByWhatWasSaidInIt() throws Exception {
        ChatSession session = base.session();
        ChatScreen screen = screen(session);
        session.submit("remember the deploy is on friday", List.of(), "ana");
        ChatScreenTest.idle(session);
        ChatScreenTest.render(screen, 130, 36);

        ctrlK(screen);
        assertTrue(screen.listSearchOpenForTest());
        ChatScreenTest.type(screen, "ARCH");
        String out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("⌕ ARCH"), "what is typed shows in the box:\n" + out);
        assertTrue(out.contains("ana") && out.contains("architect"), "the role matches, whatever the case:\n" + out);

        ChatScreenTest.key(screen, KeyCode.ESCAPE);
        assertFalse(screen.listSearchOpenForTest());
        ctrlK(screen);
        ChatScreenTest.type(screen, "friday");
        out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("the deploy is on friday") || out.contains("deploy is on friday"), "a match inside a message shows the message:\n" + out);

        ChatScreenTest.type(screen, "zzz");
        assertTrue(ChatScreenTest.render(screen, 130, 36).contains("No chat or agent matches"));
    }

    @Test
    void enterOpensTheChosenChatAndAnAgentWithoutOneStartsIt() throws Exception {
        ChatSession session = base.session();
        ChatScreen screen = screen(session);
        ChatScreenTest.render(screen, 130, 36);
        assertFalse(session.directChats().contains("bruno"), "no chat with bruno yet");

        ctrlK(screen);
        ChatScreenTest.type(screen, "bru");
        String out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("Start a chat with bruno"), "an agent with no chat is offered:\n" + out);
        ChatScreenTest.key(screen, KeyCode.ENTER);
        assertFalse(screen.listSearchOpenForTest(), "opening a chat closes the search");
        assertEquals("bruno", screen.selectedForTest());
        assertTrue(session.directChats().contains("bruno"));

        ctrlK(screen);
        ChatScreenTest.type(screen, "backend");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        assertEquals(ChatSession.MAIN, screen.selectedForTest(), "the group is found by its name too");
    }

    @Test
    void clickingTheBoxOpensItAndTheSlashCommandPrefillsIt() throws Exception {
        ChatSession session = base.session();
        ChatScreen screen = screen(session);
        ChatScreenTest.render(screen, 130, 36);
        ChatScreenTest.type(screen, "/chats ana");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        assertTrue(screen.listSearchOpenForTest());
        String out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("⌕ ana"), out);
        assertTrue(out.contains("↑↓ choose"), out);
    }
}
