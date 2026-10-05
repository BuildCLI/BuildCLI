package dev.buildcli.domain;

import java.util.List;

/**
 * A conversation. A group has members and admins, like a WhatsApp group: a message nobody is mentioned in goes to an
 * admin, a mention goes to that member. A direct chat is with one agent, and its id is that agent's name.
 */
public record Chat(String id, String name, boolean group, List<String> members, List<String> admins, String context, List<String> files) {

    /** A longest context text, so one group cannot fill every agent's prompt. */
    public static final int MAX_CONTEXT = 8000;

    public Chat {
        members = List.copyOf(members);
        admins = List.copyOf(admins);
        context = context == null ? "" : context.strip();
        files = files == null ? List.of() : List.copyOf(files);
    }

    /** A chat with no context: the text and files are optional. */
    public Chat(String id, String name, boolean group, List<String> members, List<String> admins) {
        this(id, name, group, members, admins, "", List.of());
    }

    /** Background the user gave for this group (a text and files); empty if none. */
    public boolean hasContext() {
        return !context.isBlank() || !files.isEmpty();
    }

    public Chat withContext(String text, List<String> paths) {
        return new Chat(id, name, group, members, admins, text, paths);
    }

    public boolean has(String agent) {
        return members.contains(agent);
    }

    public boolean isAdmin(String agent) {
        return admins.contains(agent);
    }

    public Chat withMembers(List<String> m, List<String> a) {
        return new Chat(id, name, group, m, a, context, files);
    }
}
