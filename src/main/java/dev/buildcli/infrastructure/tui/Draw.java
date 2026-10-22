package dev.buildcli.infrastructure.tui;

import dev.buildcli.infrastructure.TerminalText;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** What every screen needs to paint on a terminal buffer: fill an area, put clipped text, pick a style. */
final class Draw {
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private Draw() {}

    static void fill(Buffer buf, Rect r, Style style) {
        if (r.width() > 0 && r.height() > 0) {
            buf.fill(r, new Cell(" ", style));
        }
    }

    /** Draws text clipped at column {@code limit} (exclusive) and returns the columns used. */
    static int put(Buffer buf, int x, int y, String text, Style style, int limit) {
        if (x >= limit || text.isEmpty()) {
            return 0;
        }
        String clipped = CharWidth.substringByWidth(text, limit - x);
        buf.setString(x, y, clipped, style);
        return CharWidth.of(clipped);
    }

    /** Like {@link #put}, but text that does not fit ends in "…" instead of being cut off mid-word. */
    static int putFit(Buffer buf, int x, int y, String text, Style style, int limit) {
        int room = limit - x;
        if (room <= 1 || text.isEmpty() || CharWidth.of(text) <= room) {
            return put(buf, x, y, text, style, limit);
        }
        return put(buf, x, y, CharWidth.substringByWidth(text, room - 1) + "…", style, limit);
    }

    /** Like {@link #put}, for text that may come from a model or a file: control characters are made harmless first. */
    static int putSafe(Buffer buf, int x, int y, String text, Style style, int limit) {
        return put(buf, x, y, x >= limit || text.isEmpty() ? text : TerminalText.sanitize(text), style, limit);
    }

    /** Draws styled pieces side by side; a piece with an action becomes clickable through {@code hit}. */
    static void spans(Buffer buf, int x, int y, java.util.List<Styled.Span> spans, int limit, java.util.function.BiConsumer<Rect, Runnable> hit) {
        int cx = x;
        for (Styled.Span s : spans) {
            int w = put(buf, cx, y, s.text(), s.style(), limit);
            if (s.action() != null && w > 0) {
                hit.accept(new Rect(cx, y, w, 1), s.action());
            }
            cx += w;
        }
    }

    static Style st(Color fg, Color bg) {
        return Theme.on(fg, bg);
    }

    /** Text from outside made safe to draw. */
    static String clean(String s) {
        return TerminalText.sanitize(s);
    }

    /** 1234 as "1.2k". */
    static String tokens(long n) {
        return n >= 1000 ? String.format(Locale.ROOT, "%.1fk", n / 1000.0) : Long.toString(n);
    }
}
