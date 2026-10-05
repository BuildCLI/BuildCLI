package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WrapAndEditorTest {

    @Test
    void wrapsAtWordsAndHardBreaksLongWordsAndKeepsNewlines() {
        assertEquals(List.of("hello", "world"), Wrap.lines("hello world", 7));
        assertEquals(List.of("abcd", "efgh", "ij"), Wrap.lines("abcdefghij", 4));
        assertEquals(List.of("a", "", "b"), Wrap.lines("a\n\nb", 10));
        assertEquals(List.of("one two", "three"), Wrap.lines("one two three", 8));
        assertEquals(List.of(""), Wrap.lines("", 10));
    }

    @Test
    void wideCharactersTakeTwoColumns() {
        assertEquals(List.of("你好", "世界"), Wrap.lines("你好世界", 4));
    }

    @Test
    void typingDeletingAndMovingTheCursor() {
        var e = new InputEditor();
        e.insert("hello world");
        e.wordLeft();
        assertEquals(6, e.cursor());
        e.insert("big ");
        assertEquals("hello big world", e.text());
        e.end();
        e.backspace();
        assertEquals("hello big worl", e.text());
        e.home();
        e.delete();
        assertEquals("ello big worl", e.text());
        e.deleteWordBefore();
        assertEquals("ello big worl", e.text(), "nothing before the cursor");
        e.end();
        e.deleteWordBefore();
        assertEquals("ello big ", e.text());
    }

    @Test
    void emojiAreDeletedWholeNotHalf() {
        var e = new InputEditor();
        e.insert("a😀");
        e.backspace();
        assertEquals("a", e.text());
    }

    @Test
    void upAndDownFollowTheWrappedLinesAndRememberTheColumn() {
        var e = new InputEditor();
        e.insert("abcdef ghijkl mn");
        // width 7 wraps to: "abcdef " / "ghijkl " / "mn"
        assertArrayEquals(new int[] {2, 2}, e.cursorPosition(7));
        assertTrue(e.up(7));
        assertArrayEquals(new int[] {1, 2}, e.cursorPosition(7));
        assertTrue(e.up(7));
        assertArrayEquals(new int[] {0, 2}, e.cursorPosition(7));
        assertFalse(e.up(7), "already on the first line");
        assertTrue(e.down(7));
        assertTrue(e.down(7));
        assertFalse(e.down(7));
    }

    @Test
    void homeAndEndWorkPerLine() {
        var e = new InputEditor();
        e.insert("one\ntwo");
        e.home();
        assertEquals(4, e.cursor());
        e.left();
        e.home();
        assertEquals(0, e.cursor());
        e.end();
        assertEquals(3, e.cursor());
    }

    @Test
    void mentionsAreDetectedAndCompleted() {
        var e = new InputEditor();
        e.insert("hey @br");
        assertEquals("br", e.mentionPrefix());
        e.completeMention("bruno");
        assertEquals("hey @bruno ", e.text());
        assertNull(e.mentionPrefix());
        var mail = new InputEditor();
        mail.insert("write to a@b");
        assertNull(mail.mentionPrefix(), "an address is not a mention");
        var bare = new InputEditor();
        bare.insert("@");
        assertEquals("", bare.mentionPrefix());
    }

    @Test
    void historyRecallsPreviousMessagesAndRestoresTheDraft() {
        var e = new InputEditor();
        e.remember("first");
        e.remember("second");
        e.insert("draft");
        assertTrue(e.historyPrevious());
        assertEquals("second", e.text());
        assertTrue(e.historyPrevious());
        assertEquals("first", e.text());
        assertFalse(e.historyPrevious());
        assertTrue(e.historyNext());
        assertEquals("second", e.text());
        assertTrue(e.historyNext());
        assertEquals("draft", e.text());
        assertFalse(e.historyNext());
    }

    @Test
    void pastedCarriageReturnsBecomeNewlines() {
        var e = new InputEditor();
        e.insert("a\r\nb\rc");
        assertEquals("a\nb\nc", e.text());
    }
}
