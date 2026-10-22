package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Event;
import dev.buildcli.domain.Limits;
import dev.buildcli.domain.Permissions;
import dev.buildcli.domain.RunInfo;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.domain.Roster;
import dev.buildcli.infrastructure.StateStore;
import dev.buildcli.ports.ChatStore;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** Chats survive a restart: messages, their states and who said what, kept in the project's state database. */
@Timeout(20)
class ChatHistoryTest {
    @TempDir Path dir;

    static final Roster ROSTER = new Roster("backend", "ana", List.of(
            new Agent("ana", "architect", "", Set.of(), Permissions.none()),
            new Agent("bruno", "developer", "", Set.of(), Permissions.none())), Limits.defaults());

    static Task done(String me, String result) {
        Task t = new Task(1, null, "user", me, "", "");
        t.status = TaskStatus.DONE;
        t.result = result;
        return t;
    }

    ChatSession open(StateStore db, ChatSession.Executor executor) {
        return new ChatSession(ROSTER, ROSTER.agents(), executor, ChatStore.NONE, () -> 6, db);
    }

    ChatSession.Executor answering() {
        return (team, request, ui, cancelled, d) -> done(request.target(), "answer from " + request.target()
                + (request.history().isEmpty() ? "" : " (I remember: " + request.history().lines().count() + " lines)"));
    }

    static void awaitIdle(ChatSession s) throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        Thread.sleep(40);
        while (s.busy() && System.nanoTime() < end) {
            Thread.sleep(10);
        }
    }

    @Test
    void messagesAreThereAgainAfterARestartAndAgentsRememberThem() throws Exception {
        Path file = dir.resolve("state.db");
        List<String> seen;
        try (StateStore db = StateStore.open(file)) {
            ChatSession s = open(db, answering());
            s.submit("hello team");
            s.submit("hi bruno", List.of(), "bruno");
            awaitIdle(s);
            seen = s.messages().stream().map(m -> m.thread() + "|" + m.kind() + "|" + m.text() + "|" + m.state()).toList();
            s.close();
        }
        try (StateStore db = StateStore.open(file)) {
            ChatSession s = open(db, answering());
            var texts = s.messages().stream().map(m -> m.thread() + "|" + m.kind() + "|" + m.text() + "|" + m.state()).toList();
            assertEquals(seen, texts, "the chat reopens exactly as it was left");
            assertEquals(4, texts.size());
            assertTrue(texts.contains("bruno|AGENT|answer from bruno|NONE") && texts.contains("|USER|hello team|DONE"));
            long newId = s.submit("and now?");
            assertTrue(s.messages().stream().allMatch(m -> m.id() == newId || m.id() < newId), "ids continue after the stored ones");
            awaitIdle(s);
            assertTrue(s.messages().stream().anyMatch(m -> m.text().startsWith("answer from ana (I remember: 2 lines)")),
                    "the earlier conversation is context for the agent");
            s.close();
        }
    }

    @Test
    void workInterruptedByClosingComesBackAsNotSentAndCanBeRetried() throws Exception {
        Path file = dir.resolve("state.db");
        CountDownLatch never = new CountDownLatch(1);
        try (StateStore db = StateStore.open(file)) {
            ChatSession s = open(db, (team, request, ui, cancelled, d) -> {
                never.await(3, TimeUnit.SECONDS);
                throw new InterruptedException("closed");
            });
            s.submit("long work");
            Thread.sleep(150);
            s.close();
        }
        try (StateStore db = StateStore.open(file)) {
            ChatSession s = open(db, answering());
            var m = s.messages().get(0);
            assertEquals(ChatSession.State.FAILED, m.state());
            assertEquals(m.id(), s.lastFailedMessage());
            assertTrue(s.retry(m.id()));
            awaitIdle(s);
            assertEquals(ChatSession.State.DONE, s.messages().stream().filter(x -> x.id() == m.id()).findFirst().orElseThrow().state());
            s.close();
        }
    }

    @Test
    void secretsAreScrubbedOnDiskButShownAsTypedOnScreen() throws Exception {
        Path file = dir.resolve("state.db");
        try (StateStore db = StateStore.open(file)) {
            ChatSession s = open(db, answering());
            s.submit("use api_key = sk-live-abcdef1234567890 please");
            awaitIdle(s);
            assertTrue(s.messages().get(0).text().contains("sk-live-abcdef1234567890"), "the screen shows what was typed");
            s.close();
            var stored = db.recent(10).get(0).text();
            assertFalse(stored.contains("sk-live-abcdef1234567890"), stored);
        }
    }

    @Test
    void clearingAChatDeletesOnlyThatChat() throws Exception {
        Path file = dir.resolve("state.db");
        try (StateStore db = StateStore.open(file)) {
            ChatSession s = open(db, answering());
            s.submit("team message");
            s.submit("direct message", List.of(), "bruno");
            awaitIdle(s);
            s.clearChat("bruno");
            s.close();
            assertEquals(List.of("team message", "answer from ana"), db.recent(10).stream().map(e -> e.text()).toList());
        }
    }

    @Test
    void anOlderDatabaseIsUpgradedWithoutLosingItsRuns() throws Exception {
        Path file = dir.resolve("old.db");
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var st = c.createStatement()) {
            st.execute("CREATE TABLE runs (id TEXT PRIMARY KEY, team TEXT NOT NULL, request TEXT NOT NULL, started_at TEXT NOT NULL,"
                    + " finished_at TEXT, status TEXT NOT NULL, summary TEXT)");
            st.execute("CREATE TABLE tasks (run_id TEXT NOT NULL, id INTEGER NOT NULL, parent_id INTEGER, from_agent TEXT NOT NULL,"
                    + " to_agent TEXT NOT NULL, objective TEXT NOT NULL, brief TEXT NOT NULL, status TEXT NOT NULL, result TEXT,"
                    + " attempts INTEGER NOT NULL, tokens INTEGER NOT NULL, updated_at TEXT NOT NULL, PRIMARY KEY (run_id, id))");
            st.execute("CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, run_id TEXT NOT NULL, ts TEXT NOT NULL, type TEXT NOT NULL,"
                    + " task_id INTEGER NOT NULL, agent TEXT, payload TEXT, input_tokens INTEGER NOT NULL DEFAULT 0,"
                    + " output_tokens INTEGER NOT NULL DEFAULT 0)");
            st.execute("INSERT INTO runs VALUES ('r1', 'backend', 'old request', '2026-09-01T10:00:00Z', null, 'DONE', null)");
            st.execute("PRAGMA user_version=1");
        }
        try (StateStore db = StateStore.open(file)) {
            assertEquals(StateStore.SCHEMA_VERSION, db.schemaVersion());
            assertEquals(3, StateStore.SCHEMA_VERSION, "a version-1 database goes through every later migration");
            assertEquals("old request", db.listRuns(10).get(0).request());
            assertTrue(db.recent(10).isEmpty());
            assertTrue(db.changes(1).isEmpty(), "the file_changes table exists after the upgrade");
            db.append("r1", new Event(Instant.now(), "RunStarted", 0, "user", "x"));
            db.startRun(new RunInfo("r2", "backend", "new", Instant.now(), null, "RUNNING", null));
        }
    }
}
