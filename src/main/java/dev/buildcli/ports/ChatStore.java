package dev.buildcli.ports;

import dev.buildcli.domain.Chat;
import java.util.List;

/** The groups the user made, per project, kept outside the project tree. */
public interface ChatStore {
    List<Chat> load();

    void save(List<Chat> groups);

    /** Who may not contact whom: agent name -> the agents it cannot write to. Everyone else can. */
    default java.util.Map<String, List<String>> loadBlocked() {
        return java.util.Map.of();
    }

    default void saveBlocked(java.util.Map<String, List<String>> blocked) {
        // nothing to keep
    }

    ChatStore NONE = new ChatStore() {
        @Override
        public List<Chat> load() {
            return List.of();
        }

        @Override
        public void save(List<Chat> groups) {
            // nothing to keep
        }
    };
}
