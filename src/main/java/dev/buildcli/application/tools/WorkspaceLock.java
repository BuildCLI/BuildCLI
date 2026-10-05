package dev.buildcli.application.tools;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Lets agents work in parallel without trampling each other: any number of them may read the workspace at once, but
 * changing it (writing a file, running a command, committing) is exclusive. The lock is fair, so a writer is not
 * starved by a stream of readers. User approvals happen outside the lock, so nobody holds it while a person decides.
 */
public final class WorkspaceLock {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private volatile String holder;

    public <T> T shared(Callable<T> work) throws Exception {
        lock.readLock().lock();
        try {
            return work.call();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Runs {@code work} as the only agent changing the workspace.
     *
     * @param onWait told who holds the lock when {@code agent} has to wait for it
     */
    public <T> T exclusive(String agent, Consumer<String> onWait, Callable<T> work) throws Exception {
        if (!lock.writeLock().tryLock()) {
            onWait.accept(holder == null ? "a teammate" : holder);
            lock.writeLock().lockInterruptibly();
        }
        String previous = holder;
        holder = agent;
        try {
            return work.call();
        } finally {
            holder = previous;
            lock.writeLock().unlock();
        }
    }
}
