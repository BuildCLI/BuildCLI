package dev.buildcli.ports;

import dev.buildcli.domain.AgentUsage;
import dev.buildcli.domain.RunInfo;
import dev.buildcli.domain.Task;
import java.util.List;

/** Local operational state: runs, tasks and the event log. Never stored inside the project tree. */
public interface RunStore extends EventStore {
    void startRun(RunInfo run);

    /** Marks the run finished with a status (DONE, FAILED, ABORTED) and a short summary. */
    void finishRun(String runId, String status, String summary);

    /** Inserts or updates a snapshot of the task. */
    void saveTask(String runId, Task task);

    /** Most recent runs first. */
    List<RunInfo> listRuns(int limit);

    List<Task> listTasks(String runId);

    /** Per-agent usage, derived from the events of the run. */
    List<AgentUsage> usage(String runId);
}
