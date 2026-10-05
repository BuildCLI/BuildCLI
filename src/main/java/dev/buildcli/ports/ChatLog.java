package dev.buildcli.ports;

import dev.buildcli.domain.ChatEntry;
import dev.buildcli.domain.FileChange;
import java.util.List;

/** The conversation history of one project, so chats survive a restart. */
public interface ChatLog {
    /** The newest {@code limit} messages, oldest first. */
    List<ChatEntry> recent(int limit);

    /** Inserts the message, or replaces the one with the same id (its state or text changed). */
    void save(ChatEntry entry);

    /** Deletes every message of one chat, and the file changes attached to them. */
    void clear(String thread);

    /** Deletes some messages (and the file changes attached to them). */
    default void delete(java.util.Collection<Long> ids) {}

    /** Keeps the files one message's run changed, so they can be reviewed and undone after a restart. */
    default void saveChanges(long messageId, List<FileChange> changes) {}

    default List<FileChange> changes(long messageId) {
        return List.of();
    }

    ChatLog NONE = new ChatLog() {
        @Override
        public List<ChatEntry> recent(int limit) {
            return List.of();
        }

        @Override
        public void save(ChatEntry entry) {
            // nothing kept
        }

        @Override
        public void clear(String thread) {
            // nothing kept
        }
    };
}
