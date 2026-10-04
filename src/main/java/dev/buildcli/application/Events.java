package dev.buildcli.application;

import dev.buildcli.domain.Event;
import dev.buildcli.domain.RunInfo;
import dev.buildcli.domain.Task;
import dev.buildcli.ports.RunStore;
import dev.buildcli.ports.UserInterface;
import java.time.Instant;

/**
 * Records everything that happens in one run: appends to the event log, snapshots tasks and notifies the UI.
 * One log feeds persistence, audit, usage and display. Payloads are scrubbed of secrets before they are stored.
 */
public final class Events {
    private final RunStore store;
    private final String runId;
    private final UserInterface ui;

    public Events(RunStore store, String runId, UserInterface ui) {
        this.store = store;
        this.runId = runId;
        this.ui = ui;
    }

    public void emit(String type, int taskId, String agent, String payload) {
        emit(type, taskId, agent, payload, 0, 0);
    }

    /** For events that consume tokens (the model was called). */
    public void emit(String type, int taskId, String agent, String payload, int inputTokens, int outputTokens) {
        Event e = new Event(Instant.now(), type, taskId, agent, payload == null ? "" : Redactor.redact(payload), inputTokens, outputTokens);
        store.append(runId, e);
        ui.onEvent(e);
    }

    public void startRun(String group, String request) {
        store.startRun(new RunInfo(runId, group, Redactor.redact(request), Instant.now(), null, "RUNNING", null));
    }

    public void finishRun(String status, String summary) {
        store.finishRun(runId, status, Redactor.redact(summary));
    }

    /** Persists the current state of a task. Call after every status change. */
    public void taskChanged(Task task) {
        // the stored copy is scrubbed; the live task (what the UI reads) is left alone
        Task stored = new Task(task.id, task.parentId, task.from, task.to, Redactor.redact(task.objective), Redactor.redact(task.brief));
        stored.status = task.status;
        stored.result = Redactor.redact(task.result);
        stored.attempts(task.attempts());
        stored.tokens(task.tokens());
        store.saveTask(runId, stored);
        ui.onTaskChanged(task);
    }

}
