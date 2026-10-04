package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;
import static dev.buildcli.infrastructure.tui.Draw.clean;
import static dev.buildcli.infrastructure.tui.Draw.fill;
import static dev.buildcli.infrastructure.tui.Draw.put;
import static dev.buildcli.infrastructure.tui.Draw.st;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.ChatSession.State;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The full-screen viewer over the chat: a diff, a file, the task list or the help text, scrolled with the keyboard. When it
 * shows the files a card says changed, it also offers to put them back.
 */
final class ViewerPane {
    /** What it shows. {@code changes} is the id of the card whose files these are, or -1. */
    record View(String title, List<String> lines, boolean diff, boolean numbered, long changes, boolean confirmUndo) {
        View(String title, List<String> lines, boolean diff, boolean numbered) {
            this(title, lines, diff, numbered, -1, false);
        }
    }

    private final ChatSession session;
    private final BiConsumer<Rect, Runnable> hit;
    /** Asks the screen to show the files of a card (second argument: to confirm undoing them). */
    private final BiConsumer<Long, Boolean> review;
    private View view;
    private int scroll;
    private int height = 10;

    ViewerPane(ChatSession session, BiConsumer<Rect, Runnable> hit, BiConsumer<Long, Boolean> review) {
        this.session = session;
        this.hit = hit;
        this.review = review;
    }

    boolean isOpen() {
        return view != null;
    }

    void close() {
        view = null;
    }

    /** The wheel: positive scrolls up. */
    void wheel(int d) {
        scroll -= d;
    }

    void open(View v) {
        view = v;
        scroll = 0;
        coloured = null;
        colouredFor = null;
    }

    /** Files up to this long are coloured by language; a bigger one is shown plain rather than slow down every frame. */
    private static final int HIGHLIGHT_LINES = 3000;
    private List<Style[]> coloured;
    private String colouredFor;

    /** Colours of each character of a file view, by the language of its name; null for diffs, unknown names and huge files. */
    private List<Style[]> colours(Style base) {
        String key = Theme.current();
        if (!key.equals(colouredFor)) {
            colouredFor = key;
            coloured = null;
            if (!view.diff() && view.lines().size() <= HIGHLIGHT_LINES) {
                String text = view.lines().stream().map(l -> clean(l).replace("\t", "    ")).collect(java.util.stream.Collectors.joining("\n"));
                coloured = CodeHighlight.styles(text, CodeHighlight.extension(view.title()), base);
            }
        }
        return coloured;
    }

    void draw(Buffer buf, Rect r) {
        Style base = st(Theme.TEXT, Theme.BG);
        Style bar = st(Theme.TEXT, Theme.SIDEBAR);
        fill(buf, new Rect(r.x(), r.y(), r.width(), 1), bar);
        put(buf, r.x() + 2, r.y(), clean(view.title()), bar.bold(), r.right() - 12);
        String close = " ✕ Esc ";
        int cx = r.right() - Wrap.width(close) - 1;
        put(buf, cx, r.y(), close, st(Theme.DIM, Theme.SIDEBAR), r.right());
        hit.accept(new Rect(cx, r.y(), Wrap.width(close), 1), () -> view = null);
        height = Math.max(1, r.height() - 2);
        List<String> lines = view.lines();
        int max = Math.max(0, lines.size() - height);
        scroll = Math.max(0, Math.min(scroll, max));
        List<Style[]> colours = colours(base);
        int gutter = view.numbered() ? Integer.toString(lines.size()).length() + 2 : 0;
        for (int i = 0; i < height && scroll + i < lines.size(); i++) {
            int n = scroll + i;
            String l = clean(lines.get(n)).replace("\t", "    ");
            int y = r.y() + 1 + i;
            int x = r.x() + 2;
            if (view.numbered()) {
                String num = String.format("%" + (gutter - 1) + "d ", n + 1);
                put(buf, x, y, num, st(Theme.FAINT, Theme.BG), r.right());
                x += gutter;
            }
            Style s = base;
            if (view.diff()) {
                if (l.startsWith("diff --git")) {
                    fill(buf, new Rect(r.x(), y, r.width(), 1), st(Theme.TEXT, Theme.SELECTED));
                    s = st(Theme.TEXT, Theme.SELECTED).bold();
                } else if (l.startsWith("+++") || l.startsWith("---") || l.startsWith("index ")) {
                    s = st(Theme.DIM, Theme.BG);
                } else if (l.startsWith("@@")) {
                    s = st(Theme.BLUE, Theme.BG);
                } else if (l.startsWith("+")) {
                    fill(buf, new Rect(r.x(), y, r.width(), 1), st(Theme.TEXT, Theme.ADD_BG));
                    s = st(Theme.ADD_FG, Theme.ADD_BG);
                } else if (l.startsWith("-")) {
                    fill(buf, new Rect(r.x(), y, r.width(), 1), st(Theme.TEXT, Theme.DEL_BG));
                    s = st(Theme.DEL_FG, Theme.DEL_BG);
                }
            }
            if (colours != null && n < colours.size() && colours.get(n).length == l.length()) {
                putColoured(buf, x, y, l, colours.get(n), r.right() - 1);
            } else {
                put(buf, x, y, l, s, r.right() - 1);
            }
        }
        String foot = lines.isEmpty() ? t("empty") : t("{0}–{1} of {2}", scroll + 1, Math.min(lines.size(), scroll + height), lines.size())
                + "   " + t("↑↓ PgUp PgDn scroll") + (view.diff() ? " · " + t("[ ] previous/next file") : "") + " · " + t("Esc close");
        fill(buf, new Rect(r.x(), r.bottom() - 1, r.width(), 1), bar);
        put(buf, r.x() + 2, r.bottom() - 1, foot, st(Theme.DIM, Theme.SIDEBAR), r.right());
        if (view.changes() >= 0) {
            long id = view.changes();
            Rect b = new Rect(r.x(), r.bottom() - 2, r.width(), 1);
            fill(buf, b, st(Theme.TEXT, Theme.PANEL));
            int x = r.x() + 2;
            if (view.confirmUndo()) {
                x += put(buf, x, b.y(), t("Files you or another agent changed since are left alone.") + "  ", st(Theme.DIM, Theme.PANEL), r.right());
                String undo = " " + t("Undo") + "  Y ";
                int uw = put(buf, x, b.y(), undo, st(Theme.TEXT, Theme.DANGER).bold(), r.right());
                hit.accept(new Rect(x, b.y(), uw, 1), () -> undoChanges(id));
                x += uw;
                String cancel = " " + t("Cancel") + "  N ";
                int cw = put(buf, x + 1, b.y(), cancel, st(Theme.TEXT, Theme.FIELD), r.right());
                hit.accept(new Rect(x + 1, b.y(), cw, 1), () -> view = null);
            } else if (session.canUndo() && !undone(id)) {
                int w = put(buf, x, b.y(), " " + t("Undo these changes") + "  U ", st(Theme.TEXT, Theme.FIELD), r.right());
                hit.accept(new Rect(x, b.y(), w, 1), () -> review.accept(id, true));
            }
            height = Math.max(1, height - 1);
        }
    }

    /** Draws {@code text} in runs of equal style. */
    private static void putColoured(Buffer buf, int x, int y, String text, Style[] styles, int limit) {
        int cx = x;
        int from = 0;
        for (int i = 1; i <= text.length(); i++) {
            if (i == text.length() || styles[i] != styles[from]) {
                cx += put(buf, cx, y, text.substring(from, i), styles[from], limit);
                from = i;
            }
        }
    }

    private boolean undone(long id) {
        return session.messages().stream().anyMatch(m -> m.id() == id && m.state() == State.UNDONE);
    }

    private void undoChanges(long id) {
        view = null;
        session.undo(id);
    }

    EventResult key(KeyEvent key) {
        KeyCode code = key.code();
        char ch = code == KeyCode.CHAR ? key.character() : 0;
        if (view.changes() >= 0 && code == KeyCode.CHAR && !key.hasCtrl()) {
            char c = Character.toLowerCase(ch);
            if (view.confirmUndo() && c == 'y') {
                undoChanges(view.changes());
                return EventResult.HANDLED;
            }
            if (view.confirmUndo() && c == 'n') {
                view = null;
                return EventResult.HANDLED;
            }
            if (!view.confirmUndo() && c == 'u' && session.canUndo() && !undone(view.changes())) {
                review.accept(view.changes(), true);
                return EventResult.HANDLED;
            }
        }
        switch (code) {
            case ESCAPE -> view = null;
            case UP -> scroll--;
            case DOWN -> scroll++;
            case PAGE_UP -> scroll -= height - 1;
            case PAGE_DOWN -> scroll += height - 1;
            case HOME -> scroll = 0;
            case END -> scroll = Integer.MAX_VALUE / 2;
            case CHAR -> {
                switch (ch) {
                    case 'q' -> view = null;
                    case ' ', 'j' -> scroll += ch == ' ' ? height - 1 : 1;
                    case 'k' -> scroll--;
                    case ']' -> jumpFile(1);
                    case '[' -> jumpFile(-1);
                    default -> { }
                }
            }
            default -> { }
        }
        return EventResult.HANDLED;
    }

    private void jumpFile(int dir) {
        List<String> lines = view.lines();
        for (int i = scroll + dir; i >= 0 && i < lines.size(); i += dir) {
            if (lines.get(i).startsWith("diff --git")) {
                scroll = i;
                return;
            }
        }
    }
}
