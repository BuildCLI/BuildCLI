package dev.buildcli.infrastructure.tui;

import java.util.ArrayList;
import java.util.List;

/**
 * The text being typed: a cursor over a string, with word movement, multi-line editing, wrap-aware up/down and a
 * history of what was sent. No TamboUI here, so it is tested on its own.
 */
final class InputEditor {
    private static final int HISTORY_LIMIT = 100;

    private final StringBuilder text = new StringBuilder();
    private int cursor;
    /** What was sent, per chat: Up in one chat never brings back what you wrote in another. */
    private final java.util.Map<String, List<String>> histories = new java.util.HashMap<>();
    private List<String> history = histories.computeIfAbsent("", k -> new ArrayList<>());
    private String scope = "";
    private int historyPos = -1;
    private String draft = "";
    /** Column to return to when moving up and down across short lines. */
    private int wantedColumn = -1;

    String text() {
        return text.toString();
    }

    int cursor() {
        return cursor;
    }

    boolean isEmpty() {
        return text.length() == 0;
    }

    void clear() {
        text.setLength(0);
        cursor = 0;
        historyPos = -1;
        wantedColumn = -1;
    }

    void set(String value) {
        text.setLength(0);
        text.append(value);
        cursor = text.length();
        wantedColumn = -1;
    }

    void insert(String s) {
        String clean = s.replace("\r\n", "\n").replace('\r', '\n').replace("\u0000", "");
        text.insert(cursor, clean);
        cursor += clean.length();
        historyPos = -1;
        wantedColumn = -1;
    }

    void backspace() {
        if (cursor > 0) {
            int from = text.offsetByCodePoints(cursor, -1);
            text.delete(from, cursor);
            cursor = from;
        }
        wantedColumn = -1;
    }

    void delete() {
        if (cursor < text.length()) {
            text.delete(cursor, text.offsetByCodePoints(cursor, 1));
        }
        wantedColumn = -1;
    }

    void left() {
        if (cursor > 0) {
            cursor = text.offsetByCodePoints(cursor, -1);
        }
        wantedColumn = -1;
    }

    void right() {
        if (cursor < text.length()) {
            cursor = text.offsetByCodePoints(cursor, 1);
        }
        wantedColumn = -1;
    }

    void wordLeft() {
        while (cursor > 0 && Character.isWhitespace(text.charAt(cursor - 1))) {
            cursor--;
        }
        while (cursor > 0 && !Character.isWhitespace(text.charAt(cursor - 1))) {
            cursor--;
        }
        wantedColumn = -1;
    }

    void wordRight() {
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
        while (cursor < text.length() && !Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
        wantedColumn = -1;
    }

    /** Deletes the word before the cursor (Ctrl+W). */
    void deleteWordBefore() {
        int end = cursor;
        wordLeft();
        text.delete(cursor, end);
    }

    void home() {
        int nl = cursor == 0 ? -1 : text.lastIndexOf("\n", cursor - 1);
        cursor = nl + 1;
        wantedColumn = -1;
    }

    void end() {
        int nl = text.indexOf("\n", cursor);
        cursor = nl < 0 ? text.length() : nl;
        wantedColumn = -1;
    }

    /** Moves up one visual line at the given width; returns false if already on the first line (the caller may use history). */
    boolean up(int width) {
        return vertical(width, -1);
    }

    boolean down(int width) {
        return vertical(width, 1);
    }

    private boolean vertical(int width, int dir) {
        List<Wrap.Segment> segs = Wrap.layout(text(), width);
        int row = rowOf(segs, cursor);
        int target = row + dir;
        if (target < 0 || target >= segs.size()) {
            return false;
        }
        if (wantedColumn < 0) {
            wantedColumn = Wrap.width(text.substring(segs.get(row).start(), cursor));
        }
        Wrap.Segment s = segs.get(target);
        int col = 0;
        int i = s.start();
        while (i < s.end()) {
            int cp = text.codePointAt(i);
            int w = Wrap.width(new String(Character.toChars(cp)));
            if (col + w > wantedColumn) {
                break;
            }
            col += w;
            i += Character.charCount(cp);
        }
        int keep = wantedColumn;
        cursor = i;
        wantedColumn = keep;
        return true;
    }

    static int rowOf(List<Wrap.Segment> segs, int cursor) {
        for (int r = 0; r < segs.size(); r++) {
            Wrap.Segment s = segs.get(r);
            boolean last = r == segs.size() - 1;
            boolean wrapsIntoNext = !last && segs.get(r + 1).start() == s.end();
            if (cursor < s.end() || (cursor == s.end() && !wrapsIntoNext) || last) {
                if (cursor >= s.start() || r == 0) {
                    return r;
                }
            }
        }
        return segs.size() - 1;
    }

    /** {row, column} of the cursor when the text is wrapped to {@code width}. */
    int[] cursorPosition(int width) {
        List<Wrap.Segment> segs = Wrap.layout(text(), width);
        int row = rowOf(segs, cursor);
        int from = segs.get(row).start();
        return new int[] {row, Wrap.width(text.substring(from, Math.max(from, Math.min(cursor, text.length()))))};
    }

    /** Moves the cursor to the character under a click at visual {@code row}, {@code column}. */
    void moveTo(int width, int row, int column) {
        List<Wrap.Segment> segs = Wrap.layout(text(), width);
        Wrap.Segment s = segs.get(Math.max(0, Math.min(row, segs.size() - 1)));
        int col = 0;
        int i = s.start();
        while (i < s.end()) {
            int cp = text.codePointAt(i);
            int w = Wrap.width(new String(Character.toChars(cp)));
            if (col + w > column) {
                break;
            }
            col += w;
            i += Character.charCount(cp);
        }
        cursor = i;
        wantedColumn = -1;
    }

    // ---- mentions ----

    /** The {@code @partial} being typed right before the cursor (without the @), or null. */
    String mentionPrefix() {
        int i = cursor;
        while (i > 0 && (Character.isLetterOrDigit(text.charAt(i - 1)) || text.charAt(i - 1) == '_' || text.charAt(i - 1) == '-')) {
            i--;
        }
        if (i > 0 && text.charAt(i - 1) == '@' && (i - 1 == 0 || Character.isWhitespace(text.charAt(i - 2)))) {
            return text.substring(i, cursor);
        }
        return null;
    }

    /** Replaces the {@code @partial} before the cursor with {@code @name }. */
    void completeMention(String name) {
        String prefix = mentionPrefix();
        if (prefix == null) {
            insert("@" + name + " ");
            return;
        }
        int from = cursor - prefix.length() - 1;
        text.delete(from, cursor);
        cursor = from;
        insert("@" + name + " ");
    }

    // ---- history ----

    /** Switches to the history of another chat. */
    void scope(String chat) {
        if (!chat.equals(scope)) {
            scope = chat;
            history = histories.computeIfAbsent(chat, k -> new ArrayList<>());
            historyPos = -1;
        }
    }

    void remember(String sent) {
        if (!sent.isBlank() && (history.isEmpty() || !history.get(history.size() - 1).equals(sent))) {
            history.add(sent);
            if (history.size() > HISTORY_LIMIT) {
                history.remove(0);
            }
        }
        historyPos = -1;
    }

    /** Older entry (Up on the first line). */
    boolean historyPrevious() {
        if (history.isEmpty() || historyPos == 0) {
            return false;
        }
        if (historyPos < 0) {
            draft = text();
            historyPos = history.size();
        }
        historyPos--;
        set(history.get(historyPos));
        return true;
    }

    boolean historyNext() {
        if (historyPos < 0) {
            return false;
        }
        historyPos++;
        if (historyPos >= history.size()) {
            historyPos = -1;
            set(draft);
        } else {
            set(history.get(historyPos));
        }
        return true;
    }
}
