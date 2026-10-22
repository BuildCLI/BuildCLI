package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Chat;
import dev.buildcli.domain.Limits;
import dev.buildcli.domain.Permissions;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.domain.Roster;
import dev.buildcli.infrastructure.FileChatStore;
import dev.buildcli.ports.ChatStore;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Chats work like WhatsApp groups: members, admins, mentions, agents talking to each other, within limits. */
@Timeout(20)
class GroupChatTest {
    @TempDir Path dir;

    static Agent agent(String name) {
        return new Agent(name, "role of " + name, "", Set.of(), Permissions.none());
    }

    static final Roster ROSTER = new Roster("backend", "ana", List.of(agent("ana"), agent("bruno"), agent("carla")), Limits.defaults());
    static final List<Agent> CONTACTS = List.of(agent("ana"), agent("bruno"), agent("carla"), agent("dan"));

    final List<String> calls = new CopyOnWriteArrayList<>();

    static Task done(String me, String result) {
        Task t = new Task(1, null, "user", me, "", "");
        t.status = TaskStatus.DONE;
        t.result = result;
        return t;
    }

    static void awaitIdle(ChatSession s) throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        Thread.sleep(40);
        while (s.busy() && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        assertFalse(s.busy());
    }

    static void waitFor(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean() && System.nanoTime() < end) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), "timed out");
    }

    ChatSession session(ChatSession.Executor executor, ChatStore store, int hops) {
        return new ChatSession(ROSTER, CONTACTS, executor, store, () -> hops);
    }

    ChatSession echo(ChatStore store, int hops) {
        return session((team, request, ui, cancelled, d) -> {
            calls.add(request.target() + ":" + request.text().lines().reduce((a, b) -> b).orElse(""));
            return done(request.target(), request.target() + " answered");
        }, store, hops);
    }

    static List<String> notes(ChatSession s) {
        return s.messages().stream().filter(m -> m.kind() == ChatSession.Kind.SYSTEM).map(ChatSession.Message::text).toList();
    }

    @Test
    void severalMentionsReachSeveralMembersInParallel() throws Exception {
        CountDownLatch bothStarted = new CountDownLatch(2);
        var s = session((team, request, ui, cancelled, d) -> {
            bothStarted.countDown();
            assertTrue(bothStarted.await(5, TimeUnit.SECONDS), "both worked at the same time");
            return done(request.target(), "ok from " + request.target());
        }, ChatStore.NONE, 6);
        long id = s.submit("@bruno and @carla, please look", List.of(), ChatSession.MAIN);
        awaitIdle(s);
        var texts = s.messages().stream().filter(m -> m.kind() == ChatSession.Kind.AGENT).map(ChatSession.Message::text).toList();
        assertTrue(texts.contains("ok from bruno") && texts.contains("ok from carla"), texts.toString());
        assertEquals(ChatSession.State.DONE, s.messages().stream().filter(m -> m.id() == id).findFirst().orElseThrow().state());
    }

    @Test
    void aMessageWithoutMentionGoesToAFreeAdmin() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        var s = session((team, request, ui, cancelled, d) -> {
            calls.add(request.target());
            if (request.text().equals("long job")) {
                release.await(5, TimeUnit.SECONDS);
            }
            return done(request.target(), "ok");
        }, ChatStore.NONE, 6);
        s.setAdmin(ChatSession.MAIN, "bruno", true);
        s.submit("long job");
        waitFor(() -> calls.size() == 1);
        s.submit("quick question");
        waitFor(() -> calls.size() == 2);
        assertEquals(List.of("ana", "bruno"), calls, "ana was busy, so the other admin answered");
        release.countDown();
        awaitIdle(s);
    }

    @Test
    void mentioningSomeoneOutsideTheGroupExplainsItInsteadOfSendingIt() throws Exception {
        var s = echo(ChatStore.NONE, 6);
        s.submit("@dan can you help?");
        awaitIdle(s);
        assertTrue(notes(s).get(0).startsWith("dan is not in this group"), notes(s).toString());
        assertEquals(List.of("ana:@dan can you help?"), calls, "the admin got it instead");
    }

    @Test
    void agentsTalkToEachOtherByMentionAndPauseAtTheLimit() throws Exception {
        var s = session((team, request, ui, cancelled, d) -> {
            String me = request.target();
            calls.add(me);
            String other = me.equals("ana") ? "bruno" : "ana";
            return done(me, "@" + other + " what do you think?");
        }, ChatStore.NONE, 3);
        s.submit("discuss the design");
        awaitIdle(s);
        assertEquals(List.of("ana", "bruno", "ana", "bruno"), calls, "the user's message plus three agent-to-agent messages");
        assertTrue(notes(s).stream().anyMatch(n -> n.startsWith("The agents paused after 3 messages")), notes(s).toString());
        assertTrue(s.messages().stream().filter(m -> m.kind() == ChatSession.Kind.AGENT).allMatch(m -> m.thread().equals(ChatSession.MAIN)));
    }

    @Test
    void groupsKeepAnAdminAndSurviveARestart() {
        var store = new FileChatStore(dir);
        var s = echo(store, 6);
        String id = s.createGroup("Frontend squad", List.of("carla", "dan"));
        assertEquals("#frontend-squad", id);
        assertEquals(List.of("carla"), s.group(id).admins(), "the first member starts as admin");
        s.addMember(id, "bruno");
        assertThrows(IllegalArgumentException.class, () -> s.setAdmin(id, "carla", false), "the only admin cannot step down");
        s.setAdmin(id, "dan", true);
        s.setAdmin(id, "carla", false);
        s.removeMember(id, "dan");
        assertEquals(List.of("carla", "bruno"), s.group(id).members());
        assertEquals(List.of("carla"), s.group(id).admins(), "removing the last admin makes the first member admin");
        s.removeMember(ChatSession.MAIN, "carla");

        var again = echo(new FileChatStore(dir), 6);
        Chat g = again.group(id);
        assertEquals("Frontend squad", g.name());
        assertEquals(List.of("carla", "bruno"), g.members());
        assertFalse(again.group(ChatSession.MAIN).has("carla"), "changes to the team's own group are kept too");
        assertFalse(again.deleteGroup(ChatSession.MAIN));
        assertTrue(again.deleteGroup(id));
    }

    @Test
    void agentsInNoChatAreReported() {
        var s = echo(ChatStore.NONE, 6);
        assertEquals(List.of("dan"), s.idleContacts());
        s.openDirect("dan");
        assertEquals(List.of(), s.idleContacts());
    }
}
