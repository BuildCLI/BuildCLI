package dev.buildcli.ports;

import dev.buildcli.domain.Event;
import dev.buildcli.domain.FileChange;
import dev.buildcli.domain.Task;

public interface UserInterface {
    /** Blocks until the user decides. */
    boolean approve(ApprovalRequest request);

    /** Called after the automatic retries are exhausted. Blocks until the user decides. */
    EscalationChoice escalate(int taskId, String agent, String objective, String reason);

    default void onEvent(Event event) {}

    /** A task was created or changed status. The task is a live object: read what you need immediately. */
    default void onTaskChanged(Task task) {}

    /** Live text of an agent that is still generating (streaming). Called from the orchestrator thread. */
    default void onText(int taskId, String agent, String delta) {}

    /** An agent wrote a file. Called on the agent's thread, right after the write. */
    default void fileChanged(FileChange change) {}
}
