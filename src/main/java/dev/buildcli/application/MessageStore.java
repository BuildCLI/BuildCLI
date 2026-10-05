package dev.buildcli.application;

import dev.buildcli.application.ChatSession.Kind;
import dev.buildcli.application.ChatSession.Message;
import dev.buildcli.application.ChatSession.State;
import dev.buildcli.domain.ChatEntry;
import dev.buildcli.domain.FileChange;
import dev.buildcli.ports.ChatLog;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The messages of the conversation: the newest ones kept on screen, all of them kept on disk by a writer thread (the UI
 * never waits for the disk), and the short history an agent is shown when it answers. Safe to call from any thread.
 */
final class MessageStore {
    /** Messages kept on screen; the history on disk keeps them all. */
    static final int MAX_MESSAGES = 2000;
    private static final int HISTORY_MESSAGES = 10;
    private static final int HISTORY_CHARS = 6000;

    private final ChatLog log;
    /** Told whenever something a screen could show changed. */
    private final Runnable changed;
    private final List<Message> messages = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong();
    /** Where each message sits in the conversation, for the history: it changes when a message moves down on being read. */
    private final Map<Long, Long> positions = new ConcurrentHashMap<>();
    private final AtomicLong nextPosition = new AtomicLong();
    /** Messages waiting to be written. */
    private final LinkedBlockingDeque<Message> writes = new LinkedBlockingDeque<>();
    private final Thread writer;
    private volatile boolean closed;
    /** The files behind each changes card, loaded from the log the first time a restored card asks. */
    private final Map<Long, List<FileChange>> changeSets = new ConcurrentHashMap<>();
    private final Set<Long> savedChanges = ConcurrentHashMap.newKeySet();

    MessageStore(ChatLog log, Runnable changed) {
        this.log = log;
        this.changed = changed;
        restore();
        this.writer = log == ChatLog.NONE ? null : Thread.ofVirtual().name("chat-history").start(this::writeLoop);
    }

    long nextId() {
        return ids.incrementAndGet();
    }

    /**
     * Loads the conversation kept from earlier. Work that was in progress when BuildCLI closed did not finish: those
     * messages come back as not sent, so they can be retried, and unfinished activity as failed.
     */
    private void restore() {
        List<ChatEntry> saved;
        try {
            saved = log.recent(MAX_MESSAGES);
        } catch (RuntimeException e) {
            messages.add(new Message(nextId(), Kind.ERROR, "", "Could not load the earlier messages: " + e.getMessage(), Instant.now(),
                    State.NONE, List.of(), ChatSession.EVERYWHERE));
            return;
        }
        long max = 0;
        for (ChatEntry e : saved) {
            Kind kind;
            State st;
            try {
                kind = Kind.valueOf(e.kind());
                st = State.valueOf(e.state());
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            boolean interrupted = st == State.QUEUED || st == State.RUNNING;
            Message m = new Message(e.id(), kind, e.author(), e.text(), e.at(), interrupted ? State.FAILED : st, e.attachments(), e.thread());
            messages.add(m);
            positions.put(m.id(), e.position());
            if (interrupted) {
                writes.add(m);
            }
            max = Math.max(max, e.id());
            nextPosition.set(Math.max(nextPosition.get(), e.position()));
        }
        ids.set(max);
    }

    // ---- reading ----

    List<Message> snapshot() {
        synchronized (messages) {
            return List.copyOf(messages);
        }
    }

    Message find(long id) {
        synchronized (messages) {
            return messages.stream().filter(m -> m.id() == id).findFirst().orElse(null);
        }
    }

    int queued() {
        int n = 0;
        for (Message m : snapshot()) {
            if (m.kind() == Kind.USER && m.state() == State.QUEUED) {
                n++;
            }
        }
        return n;
    }

    /** The newest user message that failed, or -1. */
    long lastFailed() {
        synchronized (messages) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message m = messages.get(i);
                if (m.kind() == Kind.USER && m.state() == State.FAILED) {
                    return m.id();
                }
            }
        }
        return -1;
    }

    // ---- changing ----

    void add(Message m) {
        synchronized (messages) {
            messages.add(m);
            positions.put(m.id(), nextPosition.incrementAndGet());
            if (messages.size() > MAX_MESSAGES) {
                messages.remove(0); // only from the screen: the history on disk keeps it
            }
            persist(m);
        }
    }

    void replace(long id, State s) {
        synchronized (messages) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                if (messages.get(i).id() == id) {
                    Message changedMessage = messages.get(i).withState(s);
                    messages.set(i, changedMessage);
                    persist(changedMessage);
                    return;
                }
            }
        }
    }

    /** An agent started on a user message: it moves to where the conversation is, as in a chat app. */
    void markRead(long id) {
        synchronized (messages) {
            for (int i = 0; i < messages.size(); i++) {
                Message m = messages.get(i);
                if (m.id() == id && m.state() == State.QUEUED) {
                    messages.remove(i);
                    Message read = m.withState(State.RUNNING);
                    messages.add(read);
                    positions.put(read.id(), nextPosition.incrementAndGet());
                    persist(read);
                    return;
                }
            }
        }
    }

    /** A failed message goes to the end of its chat again, queued. */
    void requeue(Message m) {
        synchronized (messages) {
            messages.removeIf(x -> x.id() == m.id());
            Message again = new Message(m.id(), m.kind(), m.author(), m.text(), Instant.now(), State.QUEUED, m.attachments(), m.thread());
            messages.add(again);
            positions.put(again.id(), nextPosition.incrementAndGet());
            persist(again);
        }
    }

    /** The newest running activity line of {@code agent} is finished, or failed with the reason. */
    void completeActivity(String agent, String payload) {
        boolean ok = payload.startsWith("ok");
        synchronized (messages) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message m = messages.get(i);
                if (m.kind() == Kind.ACTIVITY && m.author().equals(agent) && m.state() == State.RUNNING) {
                    Message done = new Message(m.id(), m.kind(), m.author(),
                            ok ? m.text() : m.text() + " (" + ToolRuntime.abbreviate(payload, 140) + ")", m.at(), ok ? State.DONE : State.FAILED,
                            m.attachments(), m.thread());
                    messages.set(i, done);
                    persist(done);
                    return;
                }
            }
        }
    }

    /** Deletes one chat's messages, on screen and on disk. @return why the saved ones could not be deleted, or null */
    String clearThread(String thread) {
        synchronized (messages) {
            messages.removeIf(m -> m.thread().equals(thread) || m.thread().equals(ChatSession.EVERYWHERE));
        }
        try {
            log.clear(thread);
            return null;
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    /**
     * Deletes these messages, on screen and on disk. A message still being worked on (queued or running) and a card of changed
     * files (it is how they are undone) stay. @return how many were deleted
     */
    int delete(java.util.Collection<Long> ids) {
        List<Long> gone = new ArrayList<>();
        synchronized (messages) {
            messages.removeIf(m -> {
                boolean wanted = ids.contains(m.id()) && m.kind() != Kind.CHANGES && m.state() != State.QUEUED && m.state() != State.RUNNING;
                if (wanted) {
                    gone.add(m.id());
                }
                return wanted;
            });
        }
        if (!gone.isEmpty()) {
            try {
                log.delete(gone);
            } catch (RuntimeException e) {
                // they are gone from the screen; the saved copy may come back after a restart
            }
            changed.run();
        }
        return gone.size();
    }

    // ---- the files behind a "changed N files" card ----

    /** Keeps the files a run wrote, so the card can be reviewed and undone, also after a restart. */
    void keepChanges(long messageId, List<FileChange> files) {
        changeSets.put(messageId, files);
    }

    /** The files behind a changes card, oldest write first; empty if they were not kept. */
    List<FileChange> changes(long id) {
        return changeSets.computeIfAbsent(id, k -> {
            try {
                return log.changes(k);
            } catch (RuntimeException e) {
                return List.of();
            }
        });
    }

    // ---- what an agent is shown of the conversation ----

    /** The last few messages of {@code thread}, as text: context for an agent, not instructions. */
    String history(long exceptMessageId, String thread) {
        List<Message> all = snapshot();
        List<String> lines = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && lines.size() < HISTORY_MESSAGES; i--) {
            Message m = all.get(i);
            if (m.id() == exceptMessageId || (m.kind() != Kind.USER && m.kind() != Kind.AGENT && m.kind() != Kind.CHANGES)
                    || !m.thread().equals(thread)) {
                continue;
            }
            if (m.kind() == Kind.CHANGES) {
                // so an agent knows what was written, and does not assume its files are still there after an undo
                lines.add(0, "(" + m.author() + " " + m.text().substring(0, 1).toLowerCase(Locale.ROOT) + m.text().substring(1)
                        + (m.state() == State.UNDONE ? "; the user undid these changes" : "") + ")");
                continue;
            }
            if (m.kind() == Kind.USER && m.state() != State.DONE) {
                continue;
            }
            lines.add(0, (m.kind() == Kind.USER ? "user" : m.author()) + ": " + ToolRuntime.abbreviate(m.text(), 1200));
        }
        int skip = 0;
        while (skip < lines.size() && lines.stream().skip(skip).mapToInt(String::length).sum() > HISTORY_CHARS) {
            skip++;
        }
        StringBuilder sb = new StringBuilder();
        lines.stream().skip(skip).forEach(l -> sb.append(l).append('\n'));
        return sb.toString().strip();
    }

    // ---- keeping on disk ----

    /** Keeps a new or changed message, except local notes (help, command errors) that belong to no chat. */
    private void persist(Message m) {
        changed.run();
        if (writer != null && !m.thread().equals(ChatSession.EVERYWHERE)) {
            writes.add(m);
        }
    }

    private void writeLoop() {
        while (true) {
            Message m;
            try {
                m = writes.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            write(m);
            if (closed && writes.isEmpty()) {
                return;
            }
        }
    }

    private void write(Message m) {
        try {
            // what is kept on disk is scrubbed of secrets, like the event log; the screen shows the original
            log.save(new ChatEntry(m.id(), m.thread(), m.kind().name(), m.author(), Redactor.redact(m.text()), m.at(), m.state().name(),
                    m.attachments(), positions.getOrDefault(m.id(), 0L)));
            List<FileChange> files = changeSets.get(m.id());
            if (m.kind() == Kind.CHANGES && files != null && savedChanges.add(m.id())) {
                log.saveChanges(m.id(), files);
            }
        } catch (RuntimeException e) {
            // a full disk or a locked database must not break the chat; the message stays on screen
        }
    }

    /** Writes what is still queued, so the last messages are there next time. */
    void close() {
        closed = true;
        if (writer != null) {
            List<Message> rest = new ArrayList<>();
            writes.drainTo(rest);
            writer.interrupt();
            rest.forEach(this::write);
        }
    }
}
