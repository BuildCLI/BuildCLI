package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.domain.AgentUsage;
import dev.buildcli.domain.ChatEntry;
import dev.buildcli.domain.Event;
import dev.buildcli.domain.FileChange;
import dev.buildcli.domain.RunInfo;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.infrastructure.StateStore;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The state database behaves the same on every engine and with writes applied at once or in batches by a writer thread:
 * SQLite, H2 in a file and H2 in memory.
 */
class StateBackendsTest {
    static final String MODES = "sqlite,sqlite+batched,h2,h2+batched,memory,memory+batched";
    static final AtomicInteger NAMES = new AtomicInteger();

    @TempDir Path dir;

    StateStore open(String mode) {
        boolean batched = mode.endsWith("+batched");
        String engine = mode.replace("+batched", "");
        return switch (engine) {
            case "sqlite" -> new StateStore("jdbc:sqlite:" + dir.resolve("s.db"), batched);
            case "h2" -> StateStore.open(dir.resolve("state.db"), StateStore.Backend.H2, batched);
            default -> new StateStore("jdbc:h2:mem:t" + NAMES.incrementAndGet() + ";DB_CLOSE_DELAY=-1", batched);
        };
    }

    static ChatEntry entry(long id, String thread, String text, long position) {
        return new ChatEntry(id, thread, "USER", "you", text, Instant.parse("2026-10-01T10:00:00Z"), "DONE", List.of(), position);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sqlite", "sqlite+batched", "h2", "h2+batched", "memory", "memory+batched"})
    void runsTasksEventsAndUsage(String mode) throws Exception {
        try (var store = open(mode)) {
            assertEquals(StateStore.SCHEMA_VERSION, store.schemaVersion());
            store.startRun(new RunInfo("r1", "t", "do it", Instant.parse("2026-01-01T00:00:00Z"), null, "RUNNING", null));
            store.startRun(new RunInfo("r2", "t", "later", Instant.parse("2026-02-01T00:00:00Z"), null, "RUNNING", null));
            Task t = new Task(1, null, "user", "ana", "objective", "brief");
            store.saveTask("r1", t);
            t.status = TaskStatus.RUNNING;
            store.saveTask("r1", t);
            t.status = TaskStatus.DONE;
            t.result = "all good";
            t.attempts(2);
            t.tokens(123);
            store.saveTask("r1", t);
            Task child = new Task(2, 1, "ana", "bruno", "child", "");
            store.saveTask("r1", child);
            store.append("r1", new Event(Instant.now(), "AgentInvoked", 1, "ana", "step=1", 100, 10));
            store.append("r1", new Event(Instant.now(), "AgentInvoked", 1, "ana", "step=2", 200, 20));
            store.append("r1", new Event(Instant.now(), "ToolCalled", 1, "ana", "read_file"));
            store.finishRun("r1", "DONE", "summary");

            List<Task> tasks = store.listTasks("r1");
            assertEquals(2, tasks.size(), "saving a task again updates it");
            assertEquals(TaskStatus.DONE, tasks.get(0).status);
            assertEquals("all good", tasks.get(0).result);
            assertEquals(123, tasks.get(0).tokens());
            assertEquals(null, tasks.get(0).parentId);
            assertEquals(1, tasks.get(1).parentId);
            assertEquals(List.of("step=1", "step=2", "read_file"), store.list("r1").stream().map(Event::payload).toList(), "in the order written");
            assertEquals(List.of(new AgentUsage("ana", 2, 300, 30)), store.usage("r1"));
            assertEquals(List.of("r2", "r1"), store.listRuns(10).stream().map(RunInfo::id).toList(), "newest first");
            assertEquals("DONE", store.listRuns(10).get(1).status());
            assertEquals("summary", store.listRuns(10).get(1).summary());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"sqlite", "sqlite+batched", "h2", "h2+batched", "memory", "memory+batched"})
    void chatHistoryAndChanges(String mode) throws Exception {
        try (var store = open(mode)) {
            store.save(entry(1, "ana", "first", 1));
            store.save(entry(2, "ana", "second", 2));
            store.save(entry(3, "", "in the group", 3));
            store.save(entry(2, "ana", "second, edited", 4)); // same id: replaced, and moved to where the conversation is
            assertEquals(List.of("first", "in the group", "second, edited"), store.recent(10).stream().map(ChatEntry::text).toList());
            assertEquals(List.of("in the group", "second, edited"), store.recent(2).stream().map(ChatEntry::text).toList(), "the newest two, oldest first");

            store.saveChanges(2, List.of(new FileChange("ana", "src/A.java", false, null, "class A {}"),
                    new FileChange("ana", ".env", true, "A=1", "A=2")));
            var kept = store.changes(2);
            assertEquals(2, kept.size());
            assertEquals("class A {}", kept.get(0).after());
            assertEquals(null, kept.get(1).after(), "a file that may hold secrets is never written to disk");
            for (long id = 10; id < 70; id++) {
                store.saveChanges(id, List.of(new FileChange("ana", "f" + id, false, null, "x")));
            }
            assertTrue(store.changes(2).isEmpty(), "only the newest 50 change sets are kept");
            assertEquals(1, store.changes(69).size());

            store.clear("ana");
            assertEquals(List.of("in the group"), store.recent(10).stream().map(ChatEntry::text).toList());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"sqlite+batched", "h2+batched", "memory+batched"})
    void manyWritersLoseNothingAndAReadSeesEveryWriteBeforeIt(String mode) throws Exception {
        int writers = 32;
        int each = 500;
        try (var store = open(mode)) {
            var done = new CountDownLatch(writers);
            for (int w = 0; w < writers; w++) {
                int id = w;
                Thread.ofVirtual().start(() -> {
                    for (int i = 0; i < each; i++) {
                        store.append("run" + id, new Event(Instant.now(), "ToolCalled", i, "a", "n=" + i));
                    }
                    done.countDown();
                });
            }
            done.await();
            for (int w = 0; w < writers; w++) {
                List<Event> events = store.list("run" + w);
                assertEquals(each, events.size(), "run" + w);
                List<String> expected = new ArrayList<>();
                for (int i = 0; i < each; i++) {
                    expected.add("n=" + i);
                }
                assertEquals(expected, events.stream().map(Event::payload).toList(), "one writer's events keep their order");
            }
            store.append("late", new Event(Instant.now(), "X", 1, "a", "just now"));
            assertEquals(1, store.list("late").size(), "a read right after a write sees it");
        }
    }

    @Test
    void closingWaitsForWritesStillQueued() throws Exception {
        Path file = dir.resolve("nested/state.db");
        try (var store = StateStore.open(file, StateStore.Backend.SQLITE, true)) {
            for (int i = 0; i < 5000; i++) {
                store.append("r", new Event(Instant.now(), "E", i, "a", "p" + i));
            }
        }
        try (var store = StateStore.open(file)) {
            assertEquals(5000, store.list("r").size());
        }
        Path h2 = dir.resolve("h2/state.db");
        try (var store = StateStore.open(h2, StateStore.Backend.H2, true)) {
            for (int i = 0; i < 5000; i++) {
                store.append("r", new Event(Instant.now(), "E", i, "a", "p" + i));
            }
        }
        try (var store = StateStore.open(h2, StateStore.Backend.H2, false)) {
            assertEquals(5000, store.list("r").size(), "H2 keeps it across a restart");
        }
        assertTrue(java.nio.file.Files.exists(dir.resolve("h2/state.mv.db")));
    }

    @Test
    void aBatchedWriteThatFailsIsReportedNotSwallowed() throws Exception {
        var store = open("sqlite+batched");
        store.startRun(new RunInfo("dup", "t", "r", Instant.now(), null, "RUNNING", null));
        store.startRun(new RunInfo("dup", "t", "r", Instant.now(), null, "RUNNING", null)); // primary key violation, found later
        store.list("dup"); // a read applies everything queued before it
        var ex = assertThrows(IllegalStateException.class, () -> store.append("dup", new Event(Instant.now(), "E", 1, "a", "p")));
        assertTrue(ex.getMessage().contains("an earlier write"), ex.getMessage());
        store.startRun(new RunInfo("dup", "t", "r", Instant.now(), null, "RUNNING", null));
        assertThrows(SQLException.class, store::close);
    }

    @Test
    void h2InAFileCanBeOpenedAgainInTheSameProcessAndEngineNamesParse() throws Exception {
        Path file = dir.resolve("state.db");
        try (var first = StateStore.open(file, StateStore.Backend.H2, false)) {
            assertFalse(!first.listRuns(1).isEmpty());
            // the same process may open it again; another process cannot (checked by hand, see docs)
            try (var again = StateStore.open(file, StateStore.Backend.H2, false)) {
                assertEquals(0, again.listRuns(1).size());
            }
        }
        assertEquals(StateStore.Backend.H2, StateStore.Backend.parse("H2"));
        assertEquals(StateStore.Backend.MEMORY, StateStore.Backend.parse("h2-memory"));
        assertEquals(StateStore.Backend.SQLITE, StateStore.Backend.parse(null));
        assertThrows(IllegalArgumentException.class, () -> StateStore.Backend.parse("postgres"));
    }

    @Test
    void theEngineComesFromTheEnvironmentThenTheSettingsThenSqlite() throws Exception {
        Path project = java.nio.file.Files.createDirectories(dir.resolve("project"));
        Path home = java.nio.file.Files.createDirectories(dir.resolve("home"));
        var out = new java.io.PrintStream(new java.io.ByteArrayOutputStream());
        java.util.function.Function<java.util.Map<String, String>, dev.buildcli.cli.CliContext> ctx = env -> new dev.buildcli.cli.CliContext(project, home,
                env, out, out, new java.io.BufferedReader(new java.io.StringReader("")), false, (r, x) -> null, (session, models, services) -> { });
        assertEquals(StateStore.Backend.SQLITE, ctx.apply(java.util.Map.of()).storageBackend());
        var settings = new dev.buildcli.application.Settings(new dev.buildcli.infrastructure.FileSettingsStore(
                ctx.apply(java.util.Map.of()).globalDir(), ctx.apply(java.util.Map.of()).projectStateDir()));
        settings.set(dev.buildcli.ports.SettingsStore.Scope.GLOBAL, dev.buildcli.application.Settings.STORAGE, "h2");
        assertEquals(StateStore.Backend.H2, ctx.apply(java.util.Map.of()).storageBackend());
        assertEquals(StateStore.Backend.MEMORY, ctx.apply(java.util.Map.of("BUILDCLI_STORAGE", "memory")).storageBackend(), "the environment wins");
        var h2 = ctx.apply(java.util.Map.of("BUILDCLI_STORAGE", "h2"));
        assertFalse(h2.hasState(), "nothing yet");
        h2.openState().close();
        assertTrue(h2.hasState(), "H2 keeps state.mv.db, not state.db: `runs` and `usage` must look for it");
        assertFalse(ctx.apply(java.util.Map.of("BUILDCLI_STORAGE", "memory")).hasState());
        assertTrue(dev.buildcli.application.Settings.DEFINITIONS.stream().anyMatch(d -> d.key().equals(dev.buildcli.application.Settings.STORAGE)
                && d.choices().equals(List.of("sqlite", "h2", "memory"))));
    }
}
