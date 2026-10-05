package dev.buildcli.application;

import static dev.buildcli.application.I18n.t;
import dev.buildcli.application.ChatSession.Kind;
import dev.buildcli.application.ChatSession.Message;
import dev.buildcli.application.ChatSession.State;
import dev.buildcli.application.tools.FileChanges;
import dev.buildcli.application.tools.WorkspaceLock;
import dev.buildcli.domain.FileChange;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The "changed N files" card an agent leaves in the chat after a run, and putting those files back. The card keeps what was
 * written, so it can be reviewed and undone, also after a restart.
 */
final class ChangeCards {
    private final MessageStore transcript;
    /** Writes a note in a chat. */
    private final BiConsumer<String, String> note;
    private volatile Path workspace;
    private volatile WorkspaceLock workspaceLock;

    ChangeCards(MessageStore transcript, BiConsumer<String, String> note) {
        this.transcript = transcript;
        this.note = note;
    }

    /** Where undo writes, and the lock the agents share; without it undo is not offered. */
    void workspace(Path root, WorkspaceLock lock) {
        this.workspace = root;
        this.workspaceLock = lock;
    }

    boolean canUndo() {
        return workspace != null;
    }

    /** Posts the card for the files a run wrote, if it wrote any. */
    void post(Run run) {
        if (run.changes.isEmpty()) {
            return;
        }
        List<FileChange> files = List.copyOf(run.changes);
        List<String> paths = FileChanges.net(files).stream().map(FileChanges.Net::path).toList();
        long id = transcript.nextId();
        transcript.keepChanges(id, files);
        transcript.add(new Message(id, Kind.CHANGES, run.me,
                (paths.size() == 1 ? t("Changed 1 file: {0}", String.join(", ", paths)) : t("Changed {0} files: {1}", paths.size(), String.join(", ", paths))), Instant.now(), State.DONE,
                List.of(), run.thread));
    }

    /**
     * Puts back the files of a card, except those changed since by you or another agent. The agents see in the conversation
     * that it was undone. @return what happened, in one line (also written in the chat)
     */
    String undo(long id) {
        Message m = transcript.find(id);
        if (m == null || m.kind() != Kind.CHANGES) {
            return t("Nothing to undo.");
        }
        if (m.state() == State.UNDONE) {
            return t("Already undone.");
        }
        if (workspace == null) {
            return t("Undo is not available here.");
        }
        List<FileChange> files = transcript.changes(id);
        if (files.isEmpty()) {
            return t("These changes were not kept, so they cannot be undone.");
        }
        String text;
        try {
            var r = FileChanges.undo(workspace, workspaceLock, files);
            if (!r.restored().isEmpty() || !r.deleted().isEmpty()) {
                transcript.replace(id, State.UNDONE);
            }
            text = t("Undid {0}'s changes: {1}.", m.author(), r.summary());
        } catch (Exception e) {
            text = t("Undo failed: {0}", e.getMessage());
        }
        note.accept(m.thread(), text);
        return text;
    }

    /** The newest card of a chat that can still be undone, or -1. */
    long last(String thread) {
        List<Message> all = transcript.snapshot();
        for (int i = all.size() - 1; i >= 0; i--) {
            Message m = all.get(i);
            if (m.kind() == Kind.CHANGES && m.thread().equals(thread) && m.state() == State.DONE) {
                return m.id();
            }
        }
        return -1;
    }
}
