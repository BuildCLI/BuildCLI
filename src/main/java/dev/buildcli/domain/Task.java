package dev.buildcli.domain;

import java.util.concurrent.atomic.AtomicInteger;

/** A unit of execution. A handoff is simply a child task owned by another agent. */
public final class Task {
    public final int id;
    public final Integer parentId;
    public final String from;
    public final String to;
    public final String objective;
    public final String brief;
    public volatile TaskStatus status = TaskStatus.PENDING;
    public volatile String result;
    private final AtomicInteger attempts = new AtomicInteger();
    private final AtomicInteger tokens = new AtomicInteger();

    public Task(int id, Integer parentId, String from, String to, String objective, String brief) {
        this.id = id;
        this.parentId = parentId;
        this.from = from;
        this.to = to;
        this.objective = objective;
        this.brief = brief;
    }

    public int attempts() {
        return attempts.get();
    }

    public void attempts(int value) {
        attempts.set(value);
    }

    /** Counts one more try of this task and returns the new count. */
    public int nextAttempt() {
        return attempts.incrementAndGet();
    }

    public int tokens() {
        return tokens.get();
    }

    public void tokens(int value) {
        tokens.set(value);
    }

    /** Adds the tokens of one model call and returns the total so far. */
    public int addTokens(int more) {
        return tokens.addAndGet(more);
    }
}
