package dev.buildcli.application;

import static dev.buildcli.application.I18n.t;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Chat;
import dev.buildcli.ports.ChatStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Who you can talk to and where: the agents (contacts), the groups with their members and admins, the direct chats you
 * opened, and who may contact whom. Groups and the contact rules are saved when they change.
 */
final class ChatDirectory {
    private static final Pattern MENTION = Pattern.compile("(?<![\\w@])@([A-Za-z][A-Za-z0-9_-]*)");

    private final Map<String, Agent> contacts = new ConcurrentSkipListMap<>();
    /** Groups by id. Guarded by {@code this}. */
    private final Map<String, Chat> groups = new LinkedHashMap<>();
    private final Set<String> directs = new LinkedHashSet<>();
    /** Agent -> the agents it may not contact, set by the user. Guarded by {@code this}. */
    private final Map<String, Set<String>> blocked = new LinkedHashMap<>();
    private final ChatStore store;
    private final Runnable changed;
    private final Consumer<String> onError;
    /** Writes a note in a chat: who joined, who left. */
    private final BiConsumer<String, String> note;

    ChatDirectory(List<Agent> agents, List<Chat> saved, ChatStore store, Runnable changed, Consumer<String> onError,
            BiConsumer<String, String> note) {
        this.store = store;
        this.changed = changed;
        this.onError = onError;
        this.note = note;
        agents.forEach(a -> contacts.putIfAbsent(a.name(), a));
        for (Chat g : saved) {
            List<String> members = g.members().stream().filter(contacts::containsKey).distinct().toList();
            List<String> admins = g.admins().stream().filter(members::contains).toList();
            groups.put(g.id(), new Chat(g.id(), g.name(), true, members, admins.isEmpty() && !members.isEmpty() ? List.of(members.get(0)) : admins,
                    g.context(), g.files()));
        }
        store.loadBlocked().forEach((from, tos) -> blocked.put(from, new LinkedHashSet<>(tos)));
    }

    // ---- contacts ----

    List<Agent> contacts() {
        return List.copyOf(contacts.values());
    }

    Agent contact(String name) {
        return contacts.get(name);
    }

    boolean hasContact(String name) {
        return contacts.containsKey(name);
    }

    Set<String> contactNames() {
        return contacts.keySet();
    }

    void addContact(Agent agent) {
        changed.run();
        contacts.put(agent.name(), agent);
    }

    /** A deleted agent leaves every group; what it already said stays in the chats. */
    void removeContact(String name) {
        for (Chat g : groups()) {
            if (g.has(name)) {
                removeMember(g.id(), name);
            }
        }
        contacts.remove(name);
    }

    /** Every contact @mentioned in the text, in order, once each. */
    List<String> mentioned(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = MENTION.matcher(text);
        while (m.find()) {
            for (String name : contacts.keySet()) {
                if (name.equalsIgnoreCase(m.group(1)) && !out.contains(name)) {
                    out.add(name);
                }
            }
        }
        return out;
    }

    // ---- chats ----

    synchronized Chat group(String id) {
        return groups.get(id);
    }

    synchronized List<Chat> groups() {
        return List.copyOf(groups.values());
    }

    /** The chat to open first: the first group, else a direct chat with the first agent; null when there are no agents. */
    String defaultChat() {
        List<Chat> gs = groups();
        if (!gs.isEmpty()) {
            return gs.get(0).id();
        }
        return contacts.isEmpty() ? null : contacts.keySet().iterator().next();
    }

    synchronized void openDirect(String agent) {
        changed.run();
        if (contacts.containsKey(agent)) {
            directs.add(agent);
        }
    }

    /** Direct chats that were opened or that have messages (the threads in {@code withMessages}). */
    synchronized List<String> directChats(Set<String> withMessages) {
        Set<String> out = new LinkedHashSet<>(directs);
        for (String thread : withMessages) {
            if (contacts.containsKey(thread)) {
                out.add(thread);
            }
        }
        return List.copyOf(out);
    }

    /** Agents in no group and with no direct chat: loaded, but nobody can reach them. */
    List<String> idleContacts(List<String> directChats) {
        List<String> out = new ArrayList<>();
        List<Chat> all = groups();
        for (String name : contacts.keySet()) {
            if (all.stream().noneMatch(g -> g.has(name)) && !directChats.contains(name)) {
                out.add(name);
            }
        }
        return out;
    }

    // ---- who may contact whom ----

    /** Whether {@code from} may write to {@code to}. Everyone may, until the user says otherwise. */
    synchronized boolean canReach(String from, String to) {
        return !blocked.getOrDefault(from, Set.of()).contains(to);
    }

    void setReach(String from, String to, boolean allowed) {
        changed.run();
        synchronized (this) {
            if (allowed) {
                var set = blocked.get(from);
                if (set != null) {
                    set.remove(to);
                    if (set.isEmpty()) {
                        blocked.remove(from);
                    }
                }
            } else {
                blocked.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
            }
        }
        saveBlocked();
    }

    /** Who cannot contact whom, as "bruno -> ana" lines. */
    synchronized List<String> blockedPairs() {
        List<String> out = new ArrayList<>();
        blocked.forEach((from, tos) -> tos.forEach(to -> out.add(from + " -> " + to)));
        return out;
    }

    private void saveBlocked() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        synchronized (this) {
            blocked.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        }
        try {
            store.saveBlocked(copy);
        } catch (RuntimeException e) {
            onError.accept(t("Could not save who can contact whom: {0}", e.getMessage()));
        }
    }

    // ---- groups ----

    /** @return the new group's id */
    String createGroup(String name, List<String> members) {
        String clean = name == null || name.isBlank() ? "group" : name.strip();
        String base = "#" + clean.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(?:^-)|(?:-$)", "");
        List<String> known = members.stream().filter(contacts::containsKey).distinct().toList();
        String id;
        synchronized (this) {
            id = base;
            for (int i = 2; groups.containsKey(id); i++) {
                id = base + "-" + i;
            }
            groups.put(id, new Chat(id, clean, true, known, known.isEmpty() ? List.of() : List.of(known.get(0))));
        }
        saveGroups();
        return id;
    }

    void renameGroup(String id, String name) {
        changeGroup(id, g -> new Chat(g.id(), name.strip(), true, g.members(), g.admins(), g.context(), g.files()));
    }

    /** Replaces the background the agents of this group are given: a text and the files to read. */
    void setContext(String id, String text, List<String> files) {
        changeGroup(id, g -> g.withContext(text.length() > Chat.MAX_CONTEXT ? text.substring(0, Chat.MAX_CONTEXT) : text, files));
    }

    /** @return false if there was no such group */
    boolean removeGroup(String id) {
        boolean removed;
        synchronized (this) {
            removed = groups.remove(id) != null;
        }
        saveGroups();
        return removed;
    }

    void addMember(String id, String agent) {
        if (!contacts.containsKey(agent)) {
            throw new IllegalArgumentException("no agent named " + agent);
        }
        changeGroup(id, g -> {
            if (g.has(agent)) {
                return g;
            }
            List<String> m = new ArrayList<>(g.members());
            m.add(agent);
            return g.withMembers(m, g.admins().isEmpty() ? List.of(agent) : g.admins());
        });
        note.accept(id, agent + " was added");
    }

    /** Removing the last admin makes the next member admin, so a group always has someone to answer it. */
    void removeMember(String id, String agent) {
        changeGroup(id, g -> {
            List<String> m = new ArrayList<>(g.members());
            m.remove(agent);
            List<String> a = new ArrayList<>(g.admins());
            a.remove(agent);
            if (a.isEmpty() && !m.isEmpty()) {
                a.add(m.get(0));
            }
            return g.withMembers(m, a);
        });
        note.accept(id, agent + " was removed");
    }

    /** Makes a member an admin, or dismisses one. A group keeps at least one admin while it has members. */
    void setAdmin(String id, String agent, boolean admin) {
        Chat before = group(id);
        if (before == null || !before.has(agent)) {
            throw new IllegalArgumentException(agent + " is not in this group");
        }
        if (!admin && before.admins().size() == 1 && before.isAdmin(agent)) {
            throw new IllegalArgumentException(agent + " is the only admin; make someone else admin first");
        }
        changeGroup(id, g -> {
            List<String> a = new ArrayList<>(g.admins());
            a.remove(agent);
            if (admin) {
                a.add(agent);
            }
            return g.withMembers(g.members(), a);
        });
        note.accept(id, agent + (admin ? " is now an admin" : " is no longer an admin"));
    }

    private void changeGroup(String id, UnaryOperator<Chat> change) {
        synchronized (this) {
            Chat g = groups.get(id);
            if (g == null) {
                throw new IllegalArgumentException("no such group");
            }
            groups.put(id, change.apply(g));
        }
        changed.run();
        saveGroups();
    }

    private void saveGroups() {
        try {
            store.save(groups());
        } catch (RuntimeException e) {
            onError.accept(t("Could not save the groups: {0}", e.getMessage()));
        }
    }
}
