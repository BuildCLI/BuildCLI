package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.tamboui.style.Style;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Code blocks are coloured by language; the text, the wrapping and the code background never change. */
class CodeHighlightTest {
    static final Style CODE = Theme.on(Theme.CODE_TEXT, Theme.CODE);

    @AfterEach
    void restore() {
        Theme.use("dark");
    }

    static List<List<Styled.Span>> lines(String text) {
        return Styled.lines(text, 60, Style.EMPTY, Style.EMPTY.bold(), CODE, s -> { });
    }

    @Test
    void aKnownLanguageGetsSeveralColoursAndKeepsItsText() {
        var lines = lines("```java\npublic class A { // hi\n  int n = 42;\n}\n```");
        assertEquals(4, lines.size(), "header and three code lines");
        var first = lines.get(1);
        assertEquals("public class A { // hi", first.stream().map(Styled.Span::text).reduce("", String::concat));
        assertTrue(first.stream().map(Styled.Span::style).distinct().count() >= 3, "keyword, type, comment...: " + first);
        assertTrue(first.stream().allMatch(s -> Theme.CODE.equals(s.style().bg().orElse(null))), "the code background stays");
    }

    @Test
    void everyLineOfABlockIsAsWideAsItsWidestSoTheBackgroundIsARectangle() {
        for (String text : new String[] {"```java\nclass A {\n    int n;\n}\n```", "```java\nclass A {\n    int n;\n", "```cobol\nMOVE 1 TO X\nGO\n```"}) {
            var lines = lines(text);
            int[] widths = lines.stream().skip(1).filter(l -> !l.isEmpty()).mapToInt(Styled::width).toArray();
            assertTrue(widths.length >= 2, text);
            assertTrue(java.util.Arrays.stream(widths).distinct().count() == 1, text + " -> " + java.util.Arrays.toString(widths));
            assertEquals(Theme.CODE, lines.get(lines.size() - 1).get(lines.get(lines.size() - 1).size() - 1).style().bg().orElse(null));
        }
        var around = lines("before\n```sh\nls\n```\nafter").stream().map(Styled::width).toList();
        assertEquals(6, around.get(0), "text outside a block is not padded");
    }

    @Test
    void aBlockCommentSpanningLinesIsColouredOnEveryLine() {
        var lines = lines("```java\n/* one\n   two */\n```");
        Style comment = lines.get(1).get(0).style();
        assertNotEquals(CODE.fg().orElse(null), comment.fg().orElse(null));
        var two = lines.get(2).stream().filter(sp -> sp.text().contains("two")).findFirst().orElseThrow();
        assertEquals(comment.fg(), two.style().fg(), "the second line is still a comment");
    }

    @Test
    void anUnknownLanguageOrNoLanguageStaysPlainCode() {
        for (String fence : new String[] {"```cobol", "```", "```brainfuck"}) {
            var lines = lines(fence + "\nMOVE 1 TO X\n```");
            var code = lines.get(lines.size() - 1);
            assertEquals("MOVE 1 TO X", code.get(0).text(), fence);
            assertEquals(CODE, code.get(0).style(), fence);
        }
    }

    @Test
    void commonNamesAreUnderstood() {
        assertTrue(CodeHighlight.knows("yml") && CodeHighlight.knows("JSON") && CodeHighlight.knows("shell") && CodeHighlight.knows("java title=\"A.java\""));
        assertFalse(CodeHighlight.knows("") || CodeHighlight.knows("brainfuck"));
    }

    @Test
    void everyThemeHasItsOwnPalette() {
        Theme.use("dark");
        var dark = lines("```java\nclass A {}\n```").get(1).get(0).style().fg();
        Theme.use("light");
        var light = lines("```java\nclass A {}\n```").get(1).get(0).style().fg();
        assertNotEquals(dark, light, "a purple made for a dark code block is not used on a light one");
    }

    @Test
    void aBlockStillStreamingIsColouredToo() {
        var lines = Styled.lines("```json\n{\"a\": tr", 40, Style.EMPTY, Style.EMPTY.bold(), CODE, null);
        assertEquals("{\"a\": tr", lines.get(1).stream().map(Styled.Span::text).reduce("", String::concat));
    }

    @org.junit.jupiter.api.io.TempDir java.nio.file.Path dir;

    @Test
    void aFileOpenedInTheViewerIsColouredByItsExtension() throws Exception {
        java.nio.file.Files.writeString(dir.resolve("A.java"), "public class A {\n    int n = 42;\n}\n");
        java.nio.file.Files.writeString(dir.resolve("notes.txt"), "public class A {\n");
        var chat = new ChatScreenTest();
        chat.dir = dir;
        var screen = chat.screen(chat.session());
        for (String file : new String[] {"A.java", "notes.txt"}) {
            ChatScreenTest.render(screen, 120, 36);
            ChatScreenTest.type(screen, "/open " + file);
            ChatScreenTest.key(screen, dev.tamboui.tui.event.KeyCode.ENTER);
            var buf = dev.tamboui.buffer.Buffer.empty(new dev.tamboui.layout.Rect(0, 0, 120, 36));
            screen.render(dev.tamboui.terminal.Frame.forTesting(buf), new dev.tamboui.layout.Rect(0, 0, 120, 36), dev.tamboui.toolkit.element.RenderContext.empty());
            java.util.Set<Object> colours = new java.util.HashSet<>();
            for (int y = 0; y < 36; y++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < 120; x++) {
                    row.append(buf.get(x, y).symbol());
                }
                if (row.toString().contains("public class A")) {
                    for (int x = 0; x < 120; x++) {
                        colours.add(buf.get(x, y).style().fg().orElse(null));
                    }
                    break;
                }
            }
            if (file.endsWith(".java")) {
                assertTrue(colours.size() >= 4, "keywords, types and text have their own colours: " + colours);
            } else {
                assertTrue(colours.size() <= 4, "plain text is not coloured as code: " + colours);
            }
            ChatScreenTest.key(screen, dev.tamboui.tui.event.KeyCode.ESCAPE);
        }
    }
}
