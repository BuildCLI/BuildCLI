package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Attachment;
import dev.buildcli.domain.Limits;
import dev.buildcli.domain.Permissions;
import dev.buildcli.domain.Roster;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessageMarksTest {
    @TempDir Path dir;

    @Test
    void forwardingAMessageKeepsItsAttachments() throws Exception {
        Agent ana = new Agent("ana", "architect", "You coordinate.", Set.of("filesystem.read"), Permissions.none());
        var session = new ChatSession(new Roster("backend", "ana", List.of(ana), Limits.defaults()),
                (team, request, ui, cancelled, dispatcher) -> {
                    throw new AssertionError("notes are not sent to an agent");
                });
        Path png = Files.write(dir.resolve("shot.png"), new byte[] {1, 2, 3});
        Attachment shot = new Attachment(Attachment.Kind.IMAGE, png, "shot.png", "image/png", 3);
        long id = session.submit("look at this", List.of(shot), ChatSession.NOTES);

        var marks = new MessageMarks(session, (rect, action) -> { }, text -> { }, () -> ChatSession.NOTES, thread -> "Notes");
        marks.toggle(id);
        marks.forwardTo(ChatSession.NOTES);

        var forwarded = session.messages().stream().filter(m -> m.id() != id && m.text().contains("look at this")).findFirst().orElseThrow();
        assertEquals(List.of(shot), forwarded.attachments(), "the image goes with the forwarded text");
        assertFalse(marks.active(), "the marks are cleared after forwarding");
    }
}
