package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The chats that are not a group or an agent: your notes, and the private chats between agents. They must show their messages. */
class SpecialChatsScreenTest {
    final ChatScreenTest base = new ChatScreenTest();

    @Test
    void yourNotesShowWhatYouWroteAndNotTheEmptyChatScreen() throws Exception {
        base.dir = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"));
        ChatSession session = base.session();
        ChatScreen screen = base.screen(session);
        ChatScreenTest.render(screen, 130, 36);
        ChatListSearchTest.ctrlK(screen);
        ChatScreenTest.type(screen, "notes");
        ChatScreenTest.key(screen, dev.tamboui.tui.event.KeyCode.ENTER);
        assertTrue(screen.selectedForTest().equals(ChatSession.NOTES), "selected: [" + screen.selectedForTest() + "]\n" + ChatScreenTest.render(screen, 130, 36));
        ChatScreenTest.type(screen, "rotate the key on friday");
        ChatScreenTest.key(screen, dev.tamboui.tui.event.KeyCode.ENTER);
        String out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("A note to yourself: no agent reads it"), "the pane is the notes chat, not the default one:\n" + out);
        assertTrue(out.contains("only you can read this"), "the header explains it:\n" + out);
        assertTrue(out.contains("rotate the key on friday "), "the note is shown in the conversation (the list preview is cut shorter):\n" + out);
        assertFalse(out.contains("No agents yet") || out.contains("Welcome"), "not the empty-chat screen:\n" + out);
        assertTrue(out.contains("You (notes)"), "the header names the chat:\n" + out);
    }

    @Test
    void aPrivateChatBetweenAgentsShowsItsMessagesAndSaysItIsReadOnly() throws Exception {
        base.dir = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"));
        ChatSession session = new ChatSession(ChatScreenTest.ROSTER, ChatScreenTest.ROSTER.agents(), (roster, request, ui, cancelled, dispatcher) -> {
            if (request.target().equals("ana") && request.chat().contains("direct chat")) {
                dispatcher.post("ana", "bruno", "Can you check the build?");
            }
            var t = new dev.buildcli.domain.Task(1, null, "user", request.target(), request.text(), "");
            t.status = dev.buildcli.domain.TaskStatus.DONE;
            t.result = "ok from " + request.target();
            return t;
        }, dev.buildcli.ports.ChatStore.NONE, () -> 2);
        ChatScreen screen = base.screen(session);
        session.submit("ask bruno to check the build", List.of(), "ana");
        ChatScreenTest.idle(session);
        assertTrue(session.agentChats().contains("ana~bruno"), session.agentChats().toString());
        ChatScreenTest.render(screen, 130, 36);
        ChatListSearchTest.ctrlK(screen);
        ChatScreenTest.type(screen, "bruno");
        String found = ChatScreenTest.render(screen, 130, 36);
        assertTrue(found.contains("ana ↔ bruno"), "it can be found in the list:\n" + found);
        ChatScreenTest.key(screen, dev.tamboui.tui.event.KeyCode.ENTER);
        assertTrue(screen.selectedForTest().equals("ana~bruno"), "selected: [" + screen.selectedForTest() + "]\n" + ChatScreenTest.render(screen, 130, 36));
        String out = ChatScreenTest.render(screen, 130, 36);
        assertTrue(out.contains("Can you check the build?"), "the message is shown:\n" + out);
        assertTrue(out.contains("Read only"), "and the composer says it cannot be written in:\n" + out);
        assertFalse(out.contains("No agents yet") || out.contains("Welcome"), out);
    }
}
