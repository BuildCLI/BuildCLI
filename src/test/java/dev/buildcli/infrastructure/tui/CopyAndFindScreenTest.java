package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Copying a code block and finding text in a chat, driven through the screen like a user. */
class CopyAndFindScreenTest {
    @TempDir Path dir;

    static final String ANSWER = "Here is the plan.\n\n```java\nclass A {\n    int x = 1;\n}\n```\n\nand then\n\n```sh\nmvn -q verify\n```\n\nDone with the banana plan.";

    final List<String> copied = new CopyOnWriteArrayList<>();

    ChatScreen screen(String answer) throws Exception {
        var session = new ChatSession(ChatScreenTest.ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            Task t = new Task(1, null, "user", "ana", request.text(), "");
            t.status = TaskStatus.DONE;
            t.result = request.text().startsWith("again") ? "second banana" : answer;
            return t;
        });
        var screen = new ChatScreen(session, Map.of("ana", "m", "bruno", "m"), dir, () -> { });
        screen.copier(text -> {
            copied.add(text);
            return "test";
        });
        ChatScreenTest.render(screen, 130, 40);
        ChatScreenTest.type(screen, "show me");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        ChatScreenTest.idle(session);
        return screen;
    }

    static void ctrl(ChatScreen screen, char c) {
        screen.handleKeyEvent(KeyEvent.ofChar(c, KeyModifiers.CTRL), true);
    }

    /** Column and row of the nth occurrence of {@code text} on the rendered screen, or null. */
    static int[] find(String out, String text, int nth) {
        String[] lines = out.split("\n");
        int seen = 0;
        for (int y = 0; y < lines.length; y++) {
            for (int x = lines[y].indexOf(text); x >= 0; x = lines[y].indexOf(text, x + 1)) {
                if (seen++ == nth) {
                    return new int[] {x, y};
                }
            }
        }
        return null;
    }

    static void waitFor(java.util.function.BooleanSupplier c) throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!c.getAsBoolean() && System.nanoTime() < end) {
            Thread.sleep(10);
        }
    }

    @Test
    void everyCodeBlockHasACopyButtonThatCopiesExactlyThatBlock() throws Exception {
        var screen = screen(ANSWER);
        String out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("java") && out.contains("⧉ copy"), out);
        int[] second = find(out, "⧉ copy", 1);
        assertNotNull(second, "two blocks, two buttons:\n" + out);
        screen.handleMouseEvent(MouseEvent.press(MouseButton.LEFT, second[0] + 1, second[1]));
        waitFor(() -> !copied.isEmpty());
        assertEquals(List.of("mvn -q verify"), copied);
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("Copied 1 line"));

        int[] first = find(out, "⧉ copy", 0);
        screen.handleMouseEvent(MouseEvent.press(MouseButton.LEFT, first[0] + 1, first[1]));
        waitFor(() -> copied.size() == 2);
        assertEquals("class A {\n    int x = 1;\n}", copied.get(1), "the indentation is kept");
    }

    @Test
    void slashCopyTakesTheLastBlockAndSlashCopyMessageTheWholeAnswer() throws Exception {
        var screen = screen(ANSWER);
        ChatScreenTest.type(screen, "/copy");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        waitFor(() -> copied.size() == 1);
        assertEquals("mvn -q verify", copied.get(0));
        ChatScreenTest.type(screen, "/copy message");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        waitFor(() -> copied.size() == 2);
        assertEquals(ANSWER, copied.get(1));
    }

    @Test
    void anAnswerWithoutCodeIsCopiedWhole() throws Exception {
        var screen = screen("just words");
        ChatScreenTest.type(screen, "/copy");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        waitFor(() -> copied.size() == 1);
        assertEquals("just words", copied.get(0));
    }

    @Test
    void whenNothingCanCopyItSaysHowToFixIt() throws Exception {
        var screen = screen(ANSWER);
        screen.copier(text -> null);
        ChatScreenTest.type(screen, "/copy");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        String out = "";
        while (!out.contains("Could not copy") && System.nanoTime() < end) {
            Thread.sleep(20);
            out = ChatScreenTest.render(screen, 130, 40);
        }
        assertTrue(out.contains("Could not copy: no clipboard tool found"), out);
    }

    @Test
    void ctrlFOpensTheSearchBoxAndTheNewestMatchIsCurrent() throws Exception {
        var screen = screen(ANSWER);
        ChatScreenTest.type(screen, "again");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        ChatScreenTest.idle(screen.sessionForTest());
        ctrl(screen, 'f');
        ChatScreenTest.type(screen, "banana");
        String out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("Find in this chat: banana"), out);
        assertTrue(out.contains("2 of 2"), "two rows mention banana, and the newest is current:\n" + out);

        ChatScreenTest.key(screen, KeyCode.UP);
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("1 of 2"));
        ChatScreenTest.key(screen, KeyCode.DOWN);
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("2 of 2"));
        ChatScreenTest.key(screen, KeyCode.ENTER);
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("1 of 2"), "Enter goes round to the next one");
    }

    @Test
    void searchIsCaseInsensitiveReportsNoMatchesAndEscClosesIt() throws Exception {
        var screen = screen(ANSWER);
        ctrl(screen, 'f');
        ChatScreenTest.type(screen, "PLAN");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("of 2"), "found despite the capitals");
        ChatScreenTest.type(screen, "xyz");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("no matches"));
        ChatScreenTest.key(screen, KeyCode.ESCAPE);
        String out = ChatScreenTest.render(screen, 130, 40);
        assertFalse(out.contains("Find in this chat"), out);
        ChatScreenTest.type(screen, "hello");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("hello"), "typing goes to the message box again");
    }

    @Test
    void findScrollsToAnOlderMatchOutOfView() throws Exception {
        var long1 = new StringBuilder("needle at the very top\n\n");
        for (int i = 0; i < 80; i++) {
            long1.append("filler line ").append(i).append("\n\n");
        }
        var screen = screen(long1.toString());
        String bottom = ChatScreenTest.render(screen, 130, 30);
        assertFalse(bottom.contains("needle at the very top"), "the match starts out of view:\n" + bottom);
        ctrl(screen, 'f');
        ChatScreenTest.type(screen, "needle");
        String out = ChatScreenTest.render(screen, 130, 30);
        assertTrue(out.contains("needle at the very top"), "the view jumped to the match:\n" + out);
        assertTrue(out.contains("1 of 1"), out);
    }

    @Test
    void slashFindOpensTheBoxWithTheText() throws Exception {
        var screen = screen(ANSWER);
        ChatScreenTest.type(screen, "/find verify");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        String out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("Find in this chat: verify"), out);
        assertTrue(out.contains("1 of 1"), out);
    }
}
