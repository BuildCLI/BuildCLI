package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.tools.FileChanges;
import dev.buildcli.application.tools.WorkspaceLock;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.FileChange;
import dev.buildcli.domain.Limits;
import dev.buildcli.domain.Permissions;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.domain.Roster;
import dev.buildcli.infrastructure.StateStore;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** What an agent wrote is recorded per message, shown, and can be put back without throwing away anyone's later work. */
class UndoTest {
    @TempDir Path dir;

    static final Roster ROSTER = new Roster("backend", "ana", List.of(
            new Agent("ana", "architect", "", Set.of(), Permissions.none())), Limits.defaults());

    String read(String name) throws Exception {
        return Files.readString(dir.resolve(name), StandardCharsets.UTF_8);
    }

    @Test
    void undoRestoresOldContentAndDeletesFilesTheAgentCreated() throws Exception {
        Files.writeString(dir.resolve("a.txt"), "v2");
        Files.writeString(dir.resolve("new.txt"), "hello");
        var changes = List.of(new FileChange("ana", "a.txt", true, "v1", "v2"), new FileChange("ana", "new.txt", false, null, "hello"));
        var r = FileChanges.undo(dir, new WorkspaceLock(), changes);
        assertEquals("v1", read("a.txt"));
        assertFalse(Files.exists(dir.resolve("new.txt")));
        assertEquals(List.of("a.txt"), r.restored());
        assertEquals(List.of("new.txt"), r.deleted());
        assertTrue(r.skipped().isEmpty());
    }

    @Test
    void severalWritesToOneFileUndoToTheStateBeforeTheFirst() throws Exception {
        Files.writeString(dir.resolve("a.txt"), "v3");
        var changes = List.of(new FileChange("ana", "a.txt", true, "v1", "v2"), new FileChange("bruno", "a.txt", true, "v2", "v3"));
        assertEquals(1, FileChanges.net(changes).size());
        assertEquals(List.of("ana", "bruno"), FileChanges.net(changes).get(0).agents());
        FileChanges.undo(dir, new WorkspaceLock(), changes);
        assertEquals("v1", read("a.txt"));
    }

    @Test
    void aFileYouChangedSinceIsLeftAloneAndReported() throws Exception {
        Files.writeString(dir.resolve("mine.txt"), "I edited this after the agent");
        Files.writeString(dir.resolve("gone.txt"), "x");
        Files.delete(dir.resolve("gone.txt"));
        Files.writeString(dir.resolve("ok.txt"), "agent");
        var r = FileChanges.undo(dir, new WorkspaceLock(), List.of(new FileChange("ana", "mine.txt", true, "old", "agent"),
                new FileChange("ana", "gone.txt", true, "old", "x"), new FileChange("ana", "ok.txt", true, "old", "agent")));
        assertEquals("I edited this after the agent", read("mine.txt"));
        assertEquals("old", read("ok.txt"));
        assertEquals(2, r.skipped().size(), r.summary());
        assertTrue(r.summary().contains("mine.txt (changed since)"), r.summary());
        assertTrue(r.summary().contains("gone.txt (deleted since)"), r.summary());
    }

    @Test
    void undoNeverLeavesTheWorkspace() throws Exception {
        Path outside = dir.resolveSibling("outside-" + System.nanoTime() + ".txt");
        Files.writeString(outside, "secret");
        try {
            var r = FileChanges.undo(dir, new WorkspaceLock(), List.of(new FileChange("ana", "../" + outside.getFileName(), false, null, "secret")));
            assertTrue(Files.exists(outside), "a path escaping the workspace is never touched");
            assertEquals(1, r.skipped().size());
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void changesSurviveARestartExceptTheContentOfFilesThatMayHoldSecrets() throws Exception {
        try (StateStore store = new StateStore(StateStore.IN_MEMORY)) {
            store.saveChanges(7, List.of(new FileChange("ana", "src/A.java", true, "old", "new"),
                    new FileChange("ana", ".env", false, null, "API_KEY=abc"), new FileChange("ana", "config/db-password.txt", true, "a", "b")));
            var back = store.changes(7);
            assertEquals(3, back.size());
            assertEquals("new", back.get(0).after());
            assertTrue(back.get(0).restorable());
            assertNull(back.get(1).after(), "a .env is never written to the database");
            assertNull(back.get(2).before());
            assertNull(back.get(2).after());
            assertFalse(back.get(1).restorable());
        }
    }

    @Test
    void onlyTheNewestChangeSetsAreKeptAndClearingAChatDropsItsChanges() throws Exception {
        try (StateStore store = new StateStore(StateStore.IN_MEMORY)) {
            for (int i = 1; i <= 60; i++) {
                store.saveChanges(i, List.of(new FileChange("ana", "f" + i, false, null, "x")));
            }
            assertTrue(store.changes(1).isEmpty(), "the oldest were pruned");
            assertEquals(1, store.changes(60).size());
            store.save(new dev.buildcli.domain.ChatEntry(60, "#t", "CHANGES", "ana", "Changed 1 file", java.time.Instant.now(), "DONE", List.of(), 1));
            store.clear("#t");
            assertTrue(store.changes(60).isEmpty());
        }
    }

    @Test
    void writeFileReportsWhatItReplacedOnlyAfterTheWriteHappened() throws Exception {
        Files.writeString(dir.resolve("a.txt"), "v1");
        var agent = new Agent("ana", "dev", "", Set.of(dev.buildcli.domain.Capability.FILESYSTEM_WRITE), new Permissions(List.of("**"), List.of(), java.time.Duration.ofSeconds(5)));
        List<FileChange> seen = new java.util.ArrayList<>();
        boolean[] approve = {false};
        var ctx = new dev.buildcli.application.tools.ToolContext(dir, request -> approve[0]).onChange(seen::add);
        var tool = new dev.buildcli.application.tools.WriteFileTool();
        var call = new dev.buildcli.ports.ToolCall("1", "write_file", java.util.Map.of("path", "a.txt", "content", "v2"));
        assertTrue(tool.execute(ctx, agent, call).startsWith("DENIED"));
        assertTrue(seen.isEmpty(), "a write the user refused is not a change");
        approve[0] = true;
        assertTrue(tool.execute(ctx, agent, call).startsWith("OK"));
        assertEquals(List.of(new FileChange("ana", "a.txt", true, "v1", "v2")), seen);
        tool.execute(ctx, agent, new dev.buildcli.ports.ToolCall("2", "write_file", java.util.Map.of("path", "sub/new.txt", "content", "n")));
        assertEquals(new FileChange("ana", "sub/new.txt", false, null, "n"), seen.get(1));
    }

    @Test
    void aRunThatWritesFilesLeavesACardAndUndoPutsThemBack() throws Exception {
        Files.writeString(dir.resolve("a.txt"), "v1");
        try (StateStore store = new StateStore(StateStore.IN_MEMORY)) {
            var session = new ChatSession(ROSTER, List.of(ROSTER.agents().get(0)), (team, request, ui, cancelled, dispatcher) -> {
                Files.writeString(dir.resolve("a.txt"), "v2");
                ui.fileChanged(new FileChange("ana", "a.txt", true, "v1", "v2"));
                Files.writeString(dir.resolve("b.txt"), "b");
                ui.fileChanged(new FileChange("ana", "b.txt", false, null, "b"));
                Task t = new Task(1, null, "user", "ana", request.text(), "");
                t.status = TaskStatus.DONE;
                t.result = "done";
                return t;
            }, dev.buildcli.ports.ChatStore.NONE, () -> 6, store);
            session.workspace(dir, new WorkspaceLock());
            String thread = session.defaultChat();
            session.submit("change things");
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while ((session.busy() || session.lastChanges(thread) < 0) && System.nanoTime() < end) {
                Thread.sleep(10);
            }
            long card = session.lastChanges(thread);
            assertTrue(card > 0, "a changes card follows the run");
            var msg = session.messages().stream().filter(m -> m.id() == card).findFirst().orElseThrow();
            assertTrue(msg.text().contains("a.txt") && msg.text().contains("b.txt"), msg.text());
            Thread.sleep(100);
            assertEquals(2, store.changes(card).size(), "kept for after a restart");

            String said = session.undo(card);
            assertTrue(said.contains("restored a.txt") && said.contains("deleted b.txt"), said);
            assertEquals("v1", read("a.txt"));
            assertFalse(Files.exists(dir.resolve("b.txt")));
            assertEquals(-1, session.lastChanges(thread), "an undone card is not offered again");
            assertEquals("Already undone.", session.undo(card));
            session.close();
        }
    }
}
