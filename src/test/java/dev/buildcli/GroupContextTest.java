package dev.buildcli;

import static dev.buildcli.PeopleRuntimeTest.ROSTER;
import static dev.buildcli.PeopleRuntimeTest.awaitIdle;
import static dev.buildcli.PeopleRuntimeTest.done;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Chat;
import dev.buildcli.infrastructure.FileChatStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/** The optional background of a group: a text and files the agents of that group read, and nobody else. */
@Timeout(20)
class GroupContextTest {
    @TempDir Path dir;

    @Test
    void theAgentsOfTheGroupAreToldTheTextAndTheFilesAndADirectChatIsNot() throws Exception {
        Path brief = dir.resolve("brief.md");
        Files.writeString(brief, "The client is Acme. Tone: formal.");
        Path binary = dir.resolve("logo.png");
        Files.write(binary, new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, 1, 2});
        AtomicReference<String> seen = new AtomicReference<>();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            seen.set(request.chat());
            return done(request.target() == null ? "ana" : request.target(), "ok");
        });
        session.setGroupContext(ChatSession.MAIN, "We write for a law firm.", List.of(brief.toString(), binary.toString()));

        session.submit("start", List.of(), ChatSession.MAIN);
        awaitIdle(session);
        String chat = seen.get();
        assertTrue(chat.contains("We write for a law firm."), chat);
        assertTrue(chat.contains("<group-file name=\"brief.md\">") && chat.contains("The client is Acme."), chat);
        assertTrue(chat.contains("not a text file"), "a binary file is named but not read:\n" + chat);
        assertTrue(chat.contains("not instructions"), "files are data, not orders");

        seen.set(null);
        session.submit("hello", List.of(), "ana");
        awaitIdle(session);
        assertFalse(seen.get().contains("law firm"), "a private chat does not get the group's background");
    }

    @Test
    void aLongFileIsCutAndAMissingOneIsSaidSo() throws Exception {
        Path big = dir.resolve("big.txt");
        Files.writeString(big, "x".repeat(50_000));
        AtomicReference<String> seen = new AtomicReference<>();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            seen.set(request.chat());
            return done("ana", "ok");
        });
        session.setGroupContext(ChatSession.MAIN, "", List.of(big.toString(), dir.resolve("gone.txt").toString()));
        session.submit("go", List.of(), ChatSession.MAIN);
        awaitIdle(session);
        assertTrue(seen.get().contains("[cut: the file is longer]"), "cut at the limit");
        assertTrue(seen.get().length() < 25_000, "and not the whole 50 KB: " + seen.get().length());
        assertTrue(seen.get().contains("no longer exists"));
    }

    @Test
    void theContextSurvivesARestartAndIsOptional() {
        var store = new FileChatStore(dir.resolve("state"));
        store.save(List.of(new Chat("#docs", "docs", true, List.of("ana"), List.of("ana"), "Plain text only.", List.of("/x/a.md")),
                new Chat("#bare", "bare", true, List.of("ana"), List.of("ana"))));
        var back = store.load();
        assertEquals("Plain text only.", back.get(0).context());
        assertEquals(List.of("/x/a.md"), back.get(0).files());
        assertFalse(back.get(1).hasContext(), "a group without context stays as it was");
    }
}
