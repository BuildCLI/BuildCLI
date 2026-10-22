package dev.buildcli.domain;

import java.time.Instant;
import java.util.List;

/**
 * A chat message as it is stored. {@code kind} and {@code state} are the names of the chat's own enums, kept as text
 * so the stored form does not depend on application classes. {@code position} is where it sits in the conversation:
 * a message moves down when it is read, so the order is not the order of ids.
 */
public record ChatEntry(long id, String thread, String kind, String author, String text, Instant at, String state,
                        List<Attachment> attachments, long position) {

    public ChatEntry {
        attachments = List.copyOf(attachments);
    }
}
