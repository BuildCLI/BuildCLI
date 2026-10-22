package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.buildcli.application.Events;
import dev.buildcli.application.Orchestrator;
import dev.buildcli.application.ToolRuntime;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Limits;
import dev.buildcli.domain.Permissions;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.Roster;
import dev.buildcli.infrastructure.HeadlessUi;
import dev.buildcli.infrastructure.RecordingGateway;
import dev.buildcli.infrastructure.ReplayGateway;
import dev.buildcli.infrastructure.ScriptedGateway;
import dev.buildcli.infrastructure.StateStore;
import dev.buildcli.ports.EscalationChoice;
import dev.buildcli.ports.LlmGateway;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordReplayTest {
    @TempDir Path dir;

    Agent ana = new Agent("ana", "architect", "", Set.of("agent.handoff"), Permissions.none());
    Agent bruno = new Agent("bruno", "developer", "", Set.of("filesystem.write"),
            new Permissions(List.of("out/**"), List.of(), Duration.ofSeconds(5)));

    HeadlessUi run(LlmGateway llm, Path workspace) throws Exception {
        var ui = new HeadlessUi(r -> true, EscalationChoice.ABORT, false);
        try (var store = new StateStore(StateStore.IN_MEMORY)) {
            Events events = new Events(store, "r", ui);
            Task root = new Orchestrator(new Roster("t", "ana", List.of(ana, bruno), Limits.defaults()), llm,
                    new ToolRuntime(workspace, ui, events), ui, events).run("go");
            assertEquals("all done", root.result);
        }
        return ui;
    }

    @Test
    void aRecordedSessionReplaysTheSameRunWithoutAModel() throws Exception {
        Path recording = dir.resolve("session.jsonl");
        var real = new ScriptedGateway()
                .call("ana", "handoff", Map.of("to", "bruno", "objective", "write", "brief", "b"))
                .call("bruno", "write_file", Map.of("path", "out/a.txt", "content", "hello"))
                .say("bruno", "written")
                .say("ana", "all done");
        Path ws1 = Files.createDirectory(dir.resolve("ws1"));
        HeadlessUi first = run(new RecordingGateway(real, recording), ws1);

        Path ws2 = Files.createDirectory(dir.resolve("ws2"));
        HeadlessUi second = run(new ReplayGateway(recording), ws2);

        assertEquals(first.events.stream().map(e -> e.type() + ":" + e.taskId() + ":" + e.agent()).toList(),
                second.events.stream().map(e -> e.type() + ":" + e.taskId() + ":" + e.agent()).toList());
        assertEquals("hello", Files.readString(ws2.resolve("out/a.txt")));
    }

    @Test
    void replayingMoreThanWasRecordedFailsLoudly() throws Exception {
        Path recording = dir.resolve("short.jsonl");
        Files.writeString(recording, "");
        var gateway = new ReplayGateway(recording);
        var ex = assertThrows(IllegalStateException.class, () -> gateway.chat(ana, List.of(), List.of()));
        assertEquals("the recording has no more replies for agent ana", ex.getMessage());
    }

    @Test
    void recordedProviderErrorsAreReplayedAsErrors() throws Exception {
        Path recording = dir.resolve("err.jsonl");
        LlmGateway failing = (agent, messages, tools) -> {
            throw new IllegalStateException("provider down");
        };
        var recorder = new RecordingGateway(failing, recording);
        assertThrows(IllegalStateException.class, () -> recorder.chat(ana, List.of(), List.of()));
        var replay = new ReplayGateway(recording);
        var ex = assertThrows(IllegalStateException.class, () -> replay.chat(ana, List.of(), List.of()));
        assertEquals("provider down", ex.getMessage());
    }
}
