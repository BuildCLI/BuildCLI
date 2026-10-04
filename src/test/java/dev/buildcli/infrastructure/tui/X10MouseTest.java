package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The Windows console reports the mouse as X10; the UI library reads SGR. Everything else must pass through untouched. */
class X10MouseTest {

    /** What the UI library would read from these console bytes; a script stands in for the console, so no terminal or thread is involved. */
    static String read(String console) throws Exception {
        var bytes = new java.util.ArrayDeque<Integer>();
        console.chars().forEach(bytes::add);
        X10Mouse mouse = new X10Mouse(timeout -> bytes.isEmpty() ? -2 : bytes.poll());
        StringBuilder sb = new StringBuilder();
        for (int c = mouse.read(0); c >= 0; c = mouse.read(0)) {
            sb.appendCodePoint(c);
        }
        return sb.toString().replace("\u001b", "<ESC>");
    }

    /** An X10 report as the Windows console writes it: ESC [ M, then button, column and row each plus 32. */
    static String x10(int code, int column, int row) {
        return "\u001b[M" + (char) (32 + code) + (char) (32 + column) + (char) (32 + row);
    }

    @Test
    void aClickBecomesAPressAndARelease() throws Exception {
        assertEquals("<ESC>[<0;10;5M<ESC>[<0;10;5m", read(x10(0, 10, 5) + x10(3, 10, 5)));
    }

    @Test
    void theWheelBecomesWheelButtons() throws Exception {
        assertEquals("<ESC>[<64;3;4M<ESC>[<65;3;4M", read(x10(64, 3, 4) + x10(65, 3, 4)));
    }

    @Test
    void movingWithAButtonHeldIsADragAndTheRightButtonKeepsItsNumber() throws Exception {
        assertEquals("<ESC>[<0;2;2M<ESC>[<32;2;3M<ESC>[<32;2;4M<ESC>[<0;2;4m", read(x10(0, 2, 2) + x10(0, 2, 3) + x10(0, 2, 4) + x10(3, 2, 4)));
        assertEquals("<ESC>[<1;7;7M<ESC>[<0;7;7m", read(x10(1, 7, 7) + x10(3, 7, 7)));
    }

    @Test
    void aReleaseWithNothingHeldIsIgnoredInsteadOfBecomingAKey() throws Exception {
        assertEquals("ab", read("a" + x10(3, 1, 1) + "b"));
    }

    @Test
    void keysAndOtherEscapeSequencesPassThroughUnchanged() throws Exception {
        assertEquals("hi<ESC>[A<ESC>[1;5C<ESC>x", read("hi\u001b[A\u001b[1;5C\u001bx"));
        assertEquals("<ESC>[200~pasted<ESC>[201~", read("\u001b[200~pasted\u001b[201~"));
    }

    @Test
    void mouseReportsInTheMiddleOfTypingDoNotLoseOrReorderCharacters() throws Exception {
        assertEquals("ab<ESC>[<0;1;1M<ESC>[<0;1;1mcd", read("ab" + x10(0, 1, 1) + x10(3, 1, 1) + "cd"));
    }
}
