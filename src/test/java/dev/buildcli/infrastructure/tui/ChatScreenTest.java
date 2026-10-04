package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Limits;
import dev.buildcli.domain.Permissions;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.domain.Roster;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.PasteEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Drives the chat screen without a terminal: render into a buffer, send keys, clicks and pastes, read the screen back. */
class ChatScreenTest {
    @TempDir Path dir;

    static final Roster ROSTER = new Roster("backend", "ana", List.of(
            new Agent("ana", "architect", "", Set.of(), Permissions.none()),
            new Agent("bruno", "developer", "", Set.of(), Permissions.none())), Limits.defaults());

    final List<String> requests = new ArrayList<>();
    final List<String> targets = new ArrayList<>();
    boolean quit;

    ChatSession session() {
        return new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            requests.add(request.text());
            targets.add(request.target());
            Task t = new Task(1, null, "user", request.target() == null ? "ana" : request.target(), request.text(), "");
            t.status = TaskStatus.DONE;
            t.result = "reply to " + request.text();
            return t;
        });
    }

    ChatScreen screen(ChatSession s) {
        return new ChatScreen(s, Map.of("ana", "ollama/qwen", "bruno", "ollama/qwen"), dir, () -> quit = true);
    }

    static String render(ChatScreen screen, int w, int h) {
        Buffer buf = Buffer.empty(new Rect(0, 0, w, h));
        screen.render(Frame.forTesting(buf), new Rect(0, 0, w, h), RenderContext.empty());
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                sb.append(buf.get(x, y).symbol());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    static void type(ChatScreen screen, String text) {
        text.codePoints().forEach(cp -> screen.handleKeyEvent(KeyEvent.ofChar(cp), true));
    }

    static void key(ChatScreen screen, KeyCode code) {
        screen.handleKeyEvent(KeyEvent.ofKey(code), true);
    }

    static void idle(ChatSession s) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        Thread.sleep(30);
        while ((s.busy() || s.queued() > 0) && System.nanoTime() < end) {
            Thread.sleep(10);
        }
    }

    @Test
    void showsTheChatListAndTheTeamChatWithSuggestions() {
        var screen = screen(session());
        String out = render(screen, 130, 36);
        assertTrue(out.contains("BuildCLI"), out);
        assertTrue(out.contains("backend"));
        assertTrue(out.contains("ana, bruno, you"), "the group's members, in its header");
        assertTrue(out.contains("Start a conversation with backend"), out);
        assertTrue(out.contains("Type a message"));
    }

    @Test
    void typingAndEnterSendsToTheTeamAndTheReplyAppearsAsABubble() throws Exception {
        var s = session();
        var screen = screen(s);
        render(screen, 120, 36);
        type(screen, "add a health endpoint");
        key(screen, KeyCode.ENTER);
        idle(s);
        String out = render(screen, 120, 36);
        assertEquals(List.of("add a health endpoint"), requests);
        assertTrue(out.contains("add a health endpoint"), out);
        assertTrue(out.contains("reply to add a health endpoint"), out);
        assertTrue(out.contains("✓✓"), "delivered and answered");
        assertTrue(out.contains("TODAY"));
    }

    @Test
    void theAtSignOpensTheMentionMenuAndEnterCompletesIt() {
        var screen = screen(session());
        render(screen, 120, 36);
        type(screen, "ask @b");
        String out = render(screen, 120, 36);
        assertTrue(out.contains("Mention an agent"), out);
        assertTrue(out.contains("@bruno"));
        assertFalse(out.contains("@ana "), "filtered by what was typed");
        key(screen, KeyCode.ENTER);
        assertEquals("ask @bruno ", screen.inputForTest().text());
    }

    @Test
    void theSlashOpensTheCommandMenuFilteredAsYouType() {
        var screen = screen(session());
        render(screen, 120, 36);
        type(screen, "/");
        String out = render(screen, 120, 36);
        assertTrue(out.contains("Commands") && out.contains("/diff") && out.contains("/open"), out);
        type(screen, "st");
        out = render(screen, 120, 36);
        assertTrue(out.contains("/status") && out.contains("/stop"));
        assertFalse(out.contains("/diff"));
    }

    @Test
    void aDirectChatOpensFromTheNewChatListAndMessagesGoToThatAgent() throws Exception {
        var s = session();
        var screen = screen(s);
        String out = render(screen, 120, 36);
        int plus = out.lines().findFirst().orElseThrow().indexOf("＋");
        screen.handleMouseEvent(MouseEvent.press(MouseButton.LEFT, plus, 0));
        out = render(screen, 120, 36);
        assertTrue(out.contains("New chat") && out.contains("New group"), out);
        int row = out.lines().toList().indexOf(out.lines().filter(l -> l.contains("bruno") && l.contains("Message")).findFirst().orElseThrow());
        type(screen, "");
        for (int i = 0; i < 2; i++) {
            key(screen, KeyCode.DOWN);
        }
        key(screen, KeyCode.ENTER);
        assertEquals("bruno", screen.selectedForTest(), "row " + row);
        render(screen, 120, 36);
        type(screen, "hi");
        key(screen, KeyCode.ENTER);
        idle(s);
        assertEquals("bruno", targets.get(0));
        out = render(screen, 120, 36);
        assertTrue(out.contains("reply to hi"), out);
        assertTrue(out.lines().limit(18).anyMatch(l -> l.contains("bruno") && l.indexOf("bruno") < 20), "the direct chat is now in the list");
    }

    @Test
    void messagesTypedWhileTheTeamWorksAreQueued() throws Exception {
        var gate = new java.util.concurrent.CountDownLatch(1);
        var s = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            gate.await(5, TimeUnit.SECONDS);
            Task t = new Task(1, null, "user", "ana", request.text(), "");
            t.status = TaskStatus.DONE;
            t.result = "ok";
            return t;
        });
        var screen = screen(s);
        render(screen, 120, 36);
        type(screen, "first");
        key(screen, KeyCode.ENTER);
        Thread.sleep(100);
        type(screen, "second");
        key(screen, KeyCode.ENTER);
        String out = render(screen, 120, 36);
        assertTrue(out.contains("1 queued"), "the second message shows as waiting:\n" + out);
        assertTrue(out.contains("1 queued"));
        gate.countDown();
        idle(s);
        assertFalse(render(screen, 120, 36).contains("queued"));
    }

    @Test
    void pastingThePathOfAnImageAttachesItAndSendsItAlong() throws Exception {
        var img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        Path file = dir.resolve("shot.png");
        ImageIO.write(img, "png", file.toFile());
        var screen = screen(session());
        render(screen, 120, 36);
        screen.handlePasteEvent(new PasteEvent("'" + file + "'"));
        assertEquals(1, screen.attachmentsForTest().size());
        assertTrue(render(screen, 120, 36).contains("shot.png"));
        screen.handlePasteEvent(new PasteEvent("just some text"));
        assertEquals("just some text", screen.inputForTest().text());
    }

    @Test
    void slashOpenShowsAFileInTheViewerAndEscClosesIt() throws Exception {
        java.nio.file.Files.writeString(dir.resolve("notes.md"), "line one\nline two\n");
        var screen = screen(session());
        render(screen, 120, 36);
        type(screen, "/open notes.md");
        key(screen, KeyCode.ENTER);
        assertTrue(screen.viewerOpenForTest());
        String out = render(screen, 120, 36);
        assertTrue(out.contains("notes.md") && out.contains("line two"), out);
        key(screen, KeyCode.ESCAPE);
        assertFalse(screen.viewerOpenForTest());
    }

    @Test
    void shiftOrAltEnterAddsANewLineInsteadOfSending() {
        var screen = screen(session());
        render(screen, 120, 36);
        type(screen, "a");
        screen.handleKeyEvent(KeyEvent.ofKey(KeyCode.ENTER, KeyModifiers.ALT), true);
        type(screen, "b");
        assertEquals("a\nb", screen.inputForTest().text());
        assertTrue(requests.isEmpty());
    }

    @Test
    void ctrlCClearsTheInputFirstAndQuitsWhenEmpty() {
        var screen = screen(session());
        type(screen, "draft");
        screen.handleKeyEvent(KeyEvent.ofChar('c', KeyModifiers.CTRL), true);
        assertEquals("", screen.inputForTest().text());
        assertFalse(quit);
        screen.handleKeyEvent(KeyEvent.ofChar('c', KeyModifiers.CTRL), true);
        assertTrue(quit);
    }

    @Test
    void settingsOpenFromTheCommandAndChangeTheThemeLive() {
        var screen = screen(session());
        render(screen, 120, 36);
        type(screen, "/settings");
        key(screen, KeyCode.ENTER);
        assertTrue(screen.settingsOpenForTest());
        String out = render(screen, 120, 36);
        assertTrue(out.contains("Settings") && out.contains("Appearance") && out.contains("Providers"), out);
        type(screen, "2");
        key(screen, KeyCode.ENTER);
        render(screen, 120, 36);
        assertEquals("light", Theme.current());
        key(screen, KeyCode.ESCAPE);
        assertFalse(screen.settingsOpenForTest());
        Theme.use("dark");
    }

    @Test
    void withEnterSendsOffEnterAddsALineAndAltEnterSends() throws Exception {
        var s = session();
        var screen = screen(s);
        render(screen, 120, 36);
        type(screen, "/settings");
        key(screen, KeyCode.ENTER);
        key(screen, KeyCode.ENTER); // General > Enter sends: on -> off
        key(screen, KeyCode.ESCAPE);
        type(screen, "a");
        key(screen, KeyCode.ENTER);
        type(screen, "b");
        assertEquals("a\nb", screen.inputForTest().text());
        screen.handleKeyEvent(KeyEvent.ofKey(KeyCode.ENTER, KeyModifiers.ALT), true);
        idle(s);
        assertEquals(List.of("a\nb"), requests);
    }

    @Test
    void newGroupCommandCreatesAGroupThatShowsInTheListAndGetsTheMessages() throws Exception {
        var s = session();
        var screen = screen(s);
        render(screen, 120, 36);
        type(screen, "/newgroup Reviewers @bruno");
        key(screen, KeyCode.ENTER);
        String out = render(screen, 120, 36);
        assertTrue(out.contains("Reviewers") && out.contains("bruno, you"), out);
        assertEquals("#reviewers", screen.selectedForTest());
        type(screen, "please review");
        key(screen, KeyCode.ENTER);
        idle(s);
        assertEquals(List.of("bruno"), targets, "the only member, and so the admin, answers");
    }

    @Test
    void groupInfoLetsYouMakeSomeoneAdminWithOneKey() {
        var s = session();
        var screen = screen(s);
        render(screen, 120, 36);
        type(screen, "/info");
        key(screen, KeyCode.ENTER);
        String out = render(screen, 120, 36);
        assertTrue(out.contains("Group info") && out.contains("Make admin"), out);
        key(screen, KeyCode.DOWN);
        type(screen, "a");
        assertTrue(s.group(ChatSession.MAIN).isAdmin("bruno"));
        key(screen, KeyCode.ESCAPE);
    }

    @Test
    void withNoAgentsTheScreenOffersToCreateOneAndOpensItsChatOnceItExists() {
        var s = new ChatSession(List.of(), List.of(), Limits.defaults(), (team, request, ui, cancelled, d) -> null,
                dev.buildcli.ports.ChatStore.NONE, () -> 6, dev.buildcli.ports.ChatLog.NONE);
        var screen = screen(s);
        String out = render(screen, 120, 36);
        assertTrue(out.contains("No agents yet") && out.contains("Add a ready-made team") && out.contains("Create your own agent")
                && out.contains("Connect a model and provider"), out);
        int[] button = CopyAndFindScreenTest.find(out, "Add a ready-made team", 0);
        screen.handleMouseEvent(MouseEvent.press(MouseButton.LEFT, button[0] + 2, button[1]));
        String toast = "";
        for (int i = 0; i < 50 && !toast.contains("Could not add them"); i++) {
            toast = render(screen, 120, 36);
        }
        assertTrue(toast.contains("Could not add them: not available"), "the demo has no files to write, and says so:\n" + toast);
        s.addContact(new Agent("lia", "helper", "", Set.of(), Permissions.none()));
        out = render(screen, 120, 36);
        assertEquals("lia", screen.selectedForTest());
        assertTrue(out.contains("Start a conversation with lia"), out);
    }

    @Test
    void aTinyTerminalGetsAMessageNotACrash() {
        assertTrue(render(screen(session()), 30, 8).contains("Terminal too small"));
    }
}
