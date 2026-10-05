package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.domain.AgentUsage;
import dev.buildcli.domain.Event;
import dev.buildcli.domain.RunInfo;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.infrastructure.StateStore;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StateStoreTest {
    @TempDir Path dir;

    static RunInfo run(String id) {
        return new RunInfo(id, "backend", "do it", Instant.now(), null, "RUNNING", null);
    }

    @Test
    void newDatabaseIsAtTheCurrentSchemaVersion() throws Exception {
        try (var store = new StateStore(StateStore.IN_MEMORY)) {
            assertEquals(StateStore.SCHEMA_VERSION, store.schemaVersion());
        }
    }

    @Test
    void stateSurvivesReopeningTheFile() throws Exception {
        Path file = dir.resolve("nested/state.db");
        try (var store = StateStore.open(file)) {
            store.startRun(run("r1"));
            Task t = new Task(1, null, "user", "ana", "objective", "brief");
            t.status = TaskStatus.DONE;
            t.result = "all good";
            t.attempts(2);
            t.tokens(123);
            store.saveTask("r1", t);
            store.append("r1", new Event(Instant.now(), "TaskCreated", 1, "ana", "payload"));
            store.finishRun("r1", "DONE", "summary");
        }
        try (var store = StateStore.open(file)) {
            RunInfo r = store.listRuns(10).get(0);
            assertEquals("r1", r.id());
            assertEquals("DONE", r.status());
            assertEquals("summary", r.summary());
            assertTrue(r.finishedAt() != null);
            Task t = store.listTasks("r1").get(0);
            assertEquals(TaskStatus.DONE, t.status);
            assertEquals("all good", t.result);
            assertEquals(2, t.attempts());
            assertEquals(123, t.tokens());
            assertNull(t.parentId);
            assertEquals(1, store.list("r1").size());
        }
    }

    @Test
    void savingATaskAgainUpdatesItInsteadOfDuplicatingIt() throws Exception {
        try (var store = new StateStore(StateStore.IN_MEMORY)) {
            store.startRun(run("r1"));
            Task t = new Task(1, null, "user", "ana", "o", "b");
            store.saveTask("r1", t);
            t.status = TaskStatus.RUNNING;
            store.saveTask("r1", t);
            t.status = TaskStatus.DONE;
            store.saveTask("r1", t);
            List<Task> tasks = store.listTasks("r1");
            assertEquals(1, tasks.size());
            assertEquals(TaskStatus.DONE, tasks.get(0).status);
        }
    }

    @Test
    void usageIsAggregatedPerAgentFromTheEvents() throws Exception {
        try (var store = new StateStore(StateStore.IN_MEMORY)) {
            store.startRun(run("r1"));
            store.append("r1", new Event(Instant.now(), "AgentInvoked", 1, "ana", "step=1", 100, 10));
            store.append("r1", new Event(Instant.now(), "AgentInvoked", 1, "ana", "step=2", 200, 20));
            store.append("r1", new Event(Instant.now(), "AgentInvoked", 2, "bruno", "step=1", 50, 5));
            store.append("r1", new Event(Instant.now(), "ToolCalled", 2, "bruno", "x", 0, 0));
            store.append("r2", new Event(Instant.now(), "AgentInvoked", 1, "ana", "step=1", 999, 999));
            assertEquals(List.of(new AgentUsage("ana", 2, 300, 30), new AgentUsage("bruno", 1, 50, 5)), store.usage("r1"));
        }
    }

    @Test
    void recentRunsComeFirst() throws Exception {
        try (var store = new StateStore(StateStore.IN_MEMORY)) {
            store.startRun(new RunInfo("old", "t", "r", Instant.parse("2026-01-01T00:00:00Z"), null, "DONE", null));
            store.startRun(new RunInfo("new", "t", "r", Instant.parse("2026-02-01T00:00:00Z"), null, "DONE", null));
            assertEquals("new", store.listRuns(10).get(0).id());
            assertEquals(1, store.listRuns(1).size());
        }
    }

    @Test
    void aDatabaseFromANewerVersionIsRefusedAndLeftUntouched() throws Exception {
        Path file = dir.resolve("future.db");
        String url = "jdbc:sqlite:" + file;
        try (Connection c = DriverManager.getConnection(url); Statement st = c.createStatement()) {
            st.execute("PRAGMA user_version=99");
            st.execute("CREATE TABLE marker (x INTEGER)");
        }
        var ex = assertThrows(IllegalStateException.class, () -> new StateStore(url));
        assertTrue(ex.getMessage().contains("schema version 99"), ex.getMessage());
        try (Connection c = DriverManager.getConnection(url); Statement st = c.createStatement();
                var rs = st.executeQuery("SELECT name FROM sqlite_master WHERE name = 'events'")) {
            assertTrue(!rs.next(), "the database must not have been migrated");
        }
    }
}
