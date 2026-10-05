package dev.buildcli.application;

import dev.buildcli.domain.FileChange;
import dev.buildcli.domain.Task;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One agent answering one message, with everything that happens for it (on any agent's thread). {@code hops}
 * counts messages agents sent each other since the user last wrote, so their conversation cannot run away.
 */
final class Run {
    /** The run the current thread is working for: set on an agent's own thread while it handles a job. */
    static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();

    final long messageId;
    final String thread;
    /** The chat the person wrote in; {@code thread} differs when a teammate was handed part of the work. */
    final String origin;
    final String me;
    final int hops;
    final AtomicBoolean stop;
    /** The tasks of this run (guarded by the map itself). */
    final Map<Integer, Task> tasks;
    final Set<String> handedOffTo;
    /** Files written during this run, by its agent or by teammates it handed work to. */
    final List<FileChange> changes;

    Run(long messageId, String thread, String me, int hops) {
        this(messageId, thread, thread, me, hops, new AtomicBoolean(), new LinkedHashMap<>(), ConcurrentHashMap.newKeySet(), new CopyOnWriteArrayList<>());
    }

    private Run(long messageId, String thread, String origin, String me, int hops, AtomicBoolean stop, Map<Integer, Task> tasks,
            Set<String> handedOffTo, List<FileChange> changes) {
        this.messageId = messageId;
        this.thread = thread;
        this.origin = origin;
        this.me = me;
        this.hops = hops;
        this.stop = stop;
        this.tasks = tasks;
        this.handedOffTo = handedOffTo;
        this.changes = changes;
    }

    /** The same run seen from a teammate who was handed part of it: what they say goes to {@code otherThread}, the rest is shared. */
    Run handedTo(String agent, String otherThread) {
        return new Run(messageId, otherThread, origin, agent, hops, stop, tasks, handedOffTo, changes);
    }
}
