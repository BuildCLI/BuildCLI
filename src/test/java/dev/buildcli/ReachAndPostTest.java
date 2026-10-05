package dev.buildcli;

import static dev.buildcli.PeopleRuntimeTest.ROSTER;
import static dev.buildcli.PeopleRuntimeTest.awaitIdle;
import static dev.buildcli.PeopleRuntimeTest.done;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.infrastructure.FileChatStore;
import dev.buildcli.ports.ChatStore;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** An agent writing for the user in a group or to a teammate, who may contact whom, and the user's own notes. */
@Timeout(20)
class ReachAndPostTest {
    @TempDir Path dir;

    static List<ChatSession.Message> in(ChatSession s, String thread) {
        return s.messages().stream().filter(m -> m.thread().equals(thread) && m.kind() == ChatSession.Kind.AGENT).toList();
    }

    @Test
    void anAgentWritesInAGroupAsItselfNotInTheChatItWasAskedIn() throws Exception {
        AtomicReference<String> result = new AtomicReference<>();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            result.set(dispatcher.post("bruno", "backend", "Hello everyone, from bruno"));
            return done("bruno", "I told them");
        });
        session.submit("tell the group hello", List.of(), "bruno");
        awaitIdle(session);
        assertTrue(result.get().startsWith("Posted in the group 'backend'"), result.get());
        assertEquals(List.of("Hello everyone, from bruno"), in(session, ChatSession.MAIN).stream().map(ChatSession.Message::text).toList());
        assertEquals(List.of("I told them"), in(session, "bruno").stream().map(ChatSession.Message::text).toList(), "the answer to the user stays private");
    }

    @Test
    void anAgentCannotWriteInAGroupItIsNotIn() throws Exception {
        AtomicReference<String> result = new AtomicReference<>();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            result.set(dispatcher.post("bruno", "backend", "hi"));
            return done("bruno", "x");
        });
        session.removeMember(ChatSession.MAIN, "bruno");
        session.submit("go", List.of(), "bruno");
        awaitIdle(session);
        String r = result.get();
        assertTrue(r.startsWith("ERROR") && r.contains("not a member"), r);
        assertEquals(0, in(session, ChatSession.MAIN).size());
    }

    @Test
    void aPrivateChatBetweenAgentsStartsOnlyWhenOneWritesToTheOtherAndTheUserCannotWriteInIt() throws Exception {
        List<String> seen = new CopyOnWriteArrayList<>();
        var session = new ChatSession(ROSTER, ROSTER.agents(), (team, request, ui, cancelled, dispatcher) -> {
            String me = request.target();
            seen.add(me + " <- " + request.text() + " | " + request.chat());
            if (me.equals("bruno") && request.chat().contains("private chat with ana")) {
                return done("bruno", "Sure, I will look at it.");
            }
            if (me.equals("ana") && request.chat().contains("private chat with bruno")) {
                return done("ana", "Thanks.");
            }
            dispatcher.post("ana", "bruno", "Can you check the build?");
            return done("ana", "I asked bruno.");
        }, ChatStore.NONE, () -> 2);
        assertEquals(List.of(), session.agentChats(), "nothing until someone is asked");
        session.submit("ask bruno to check the build", List.of(), "ana");
        awaitIdle(session);
        assertEquals(List.of("ana~bruno"), session.agentChats());
        assertEquals(List.of("Can you check the build?", "Sure, I will look at it.", "Thanks."),
                in(session, "ana~bruno").stream().map(ChatSession.Message::text).toList(), "replies come back, until the hop limit");
        assertEquals(List.of("I asked bruno."), in(session, "ana").stream().map(ChatSession.Message::text).toList());

        assertEquals(-1, session.submit("hello you two", List.of(), "ana~bruno"));
        assertTrue(session.messages().stream().anyMatch(m -> m.text().contains("You can read it, not write in it")));
        assertTrue(session.messages().stream().noneMatch(m -> m.kind() == ChatSession.Kind.USER && m.text().equals("hello you two")));
    }

    @Test
    void whenAnAgentIsHandedWorkInAPrivateChatTheirExchangeHappensInAChatOfTheirOwn() throws Exception {
        AtomicReference<String> insideRun = new AtomicReference<>();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            dispatcher.run("ana", "bruno", "check the build", () -> {
                insideRun.set("done");
                return "build is green";
            });
            return done("ana", "bruno says it is green");
        });
        session.submit("ask bruno to check the build", List.of(), "ana");
        awaitIdle(session);
        assertEquals("done", insideRun.get());
        assertEquals(List.of("ana~bruno"), session.agentChats(), "the handoff opened their chat");
        assertEquals(List.of("check the build"), in(session, "ana~bruno").stream().map(ChatSession.Message::text).toList());
        assertEquals(List.of("bruno says it is green"), in(session, "ana").stream().map(ChatSession.Message::text).toList(),
                "the chat with the user only gets the answer");
    }

    @Test
    void anAgentAskedByAnotherToTalkToYouWritesInItsOwnPrivateChatWithYouEvenWithoutChatPost() throws Exception {
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            // ana was asked by the user to get bruno to talk to them; bruno answers the handoff by writing to the person
            dispatcher.run("ana", "bruno", "talk to the user about the budget", () -> dispatcher.post("bruno", "you", "Hi, it is Bruno. About the budget..."));
            return done("ana", "I asked bruno to write to you.");
        });
        session.submit("ask bruno to talk to me", List.of(), "ana");
        awaitIdle(session);
        assertEquals(List.of("Hi, it is Bruno. About the budget..."), in(session, "bruno").stream().map(ChatSession.Message::text).toList(),
                "the message is in bruno's own chat with you");
        assertTrue(session.directChats().contains("bruno"), "and that chat is in your list");
        assertTrue(session.messages().stream().anyMatch(m -> m.thread().equals("ana") && m.text().contains("bruno wrote to you in your private chat")),
                "where you asked, you are told where to look");
    }

    @Test
    void anAgentTheUserBlockedCannotBeContactedAndTheAgentIsToldSo() throws Exception {
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Boolean> sees = new AtomicReference<>();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            result.set(dispatcher.post("bruno", "ana", "psst"));
            sees.set(dispatcher.sees("bruno", "ana"));
            return done("bruno", "ok");
        });
        session.setReach("bruno", "ana", false);
        assertFalse(session.canReach("bruno", "ana"));
        assertTrue(session.canReach("ana", "bruno"), "the other direction is not affected");
        assertEquals(List.of("bruno -> ana"), session.blockedPairs());
        session.submit("go", List.of(), "bruno");
        awaitIdle(session);
        assertTrue(result.get().startsWith("ERROR: you cannot contact ana"), result.get());
        assertFalse(sees.get(), "ana is not even on bruno's list of teammates");
        assertEquals(List.of(), session.agentChats());
        session.setReach("bruno", "ana", true);
        assertTrue(session.canReach("bruno", "ana"));
        assertEquals(List.of(), session.blockedPairs());
    }

    @Test
    void whoMayContactWhomIsRemembered() {
        var store = new FileChatStore(dir);
        var first = new ChatSession(ROSTER, ROSTER.agents(), (team, request, ui, cancelled, dispatcher) -> done("ana", "x"), store, () -> 6);
        first.setReach("bruno", "ana", false);
        var second = new ChatSession(ROSTER, ROSTER.agents(), (team, request, ui, cancelled, dispatcher) -> done("ana", "x"), store, () -> 6);
        assertFalse(second.canReach("bruno", "ana"));
        assertTrue(second.canReach("ana", "bruno"));
        assertEquals(Map.of("bruno", List.of("ana")), store.loadBlocked());
    }

    @Test
    void yourOwnChatKeepsNotesAndNoAgentReadsThem() throws Exception {
        List<String> ran = new CopyOnWriteArrayList<>();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            ran.add(request.text());
            return done("ana", "x");
        });
        long id = session.submit("remember to rotate the key", List.of(), ChatSession.NOTES);
        assertTrue(id > 0);
        awaitIdle(session);
        assertEquals(List.of(), ran, "no agent was asked");
        var note = session.messages().stream().filter(m -> m.thread().equals(ChatSession.NOTES)).toList();
        assertEquals(1, note.size());
        assertEquals(ChatSession.State.DONE, note.get(0).state());
    }
}
