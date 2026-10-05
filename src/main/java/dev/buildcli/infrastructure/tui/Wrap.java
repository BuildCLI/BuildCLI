package dev.buildcli.infrastructure.tui;

import dev.tamboui.text.CharWidth;
import java.util.ArrayList;
import java.util.List;

/**
 * Word wrapping by display width (CJK and emoji take two columns). One algorithm serves the transcript, which wraps
 * styled text, and the input box, which needs to map a cursor position to a screen position.
 */
final class Wrap {
    private Wrap() {}

    /** A visual line: the characters {@code [start, end)} of the source text. */
    record Segment(int start, int end) {}

    static int width(String s) {
        return CharWidth.of(s);
    }

    /**
     * Splits {@code text} into lines of at most {@code width} columns. A newline always ends a line; otherwise lines
     * break after the last space that fits, or hard in the middle of a word too long to fit.
     * Segments are contiguous except for the newline characters between them.
     */
    static List<Segment> layout(String text, int width) {
        int max = Math.max(1, width);
        List<Segment> out = new ArrayList<>();
        int start = 0;
        int i = 0;
        int col = 0;
        int lastSpaceEnd = -1;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '\n') {
                out.add(new Segment(start, i));
                i++;
                start = i;
                col = 0;
                lastSpaceEnd = -1;
                continue;
            }
            int cp = text.codePointAt(i);
            int len = Character.charCount(cp);
            int w = c == '\t' ? 4 : Math.max(0, CharWidth.of(cp));
            if (col + w > max && i > start) {
                int cut = lastSpaceEnd > start ? lastSpaceEnd : i;
                out.add(new Segment(start, cut));
                start = cut;
                i = cut;
                col = 0;
                lastSpaceEnd = -1;
                continue;
            }
            col += w;
            i += len;
            if (c == ' ') {
                lastSpaceEnd = i;
            }
        }
        out.add(new Segment(start, n));
        return out;
    }

    /** The plain lines of {@code text} wrapped to {@code width}, trailing spaces removed. */
    static List<String> lines(String text, int width) {
        List<String> out = new ArrayList<>();
        for (Segment s : layout(text, width)) {
            out.add(text.substring(s.start(), s.end()).stripTrailing().replace("\t", "    "));
        }
        return out;
    }
}
