package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;
import static dev.buildcli.infrastructure.tui.Draw.fill;
import static dev.buildcli.infrastructure.tui.Draw.putSafe;
import static dev.buildcli.infrastructure.tui.Draw.st;

import dev.buildcli.infrastructure.ModelCatalog;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The small modal boxes the settings screen opens for its flows: a one-line question, a list to tick, a yes/no and a model
 * picker. One is open at a time; while it is, it takes every key. The screen behind only says what to do with the answer.
 */
final class SettingsDialogs {
    /** A one-line question with an input, answered with Enter. */
    record Prompt(String title, String hint, InputEditor editor, Consumer<String> onSubmit, Runnable back) {}

    /** Several things to tick, for a step of a flow; Esc goes back one step. */
    static final class Checklist {
        final String title;
        final List<String> options;
        final List<String> notes;
        final java.util.Set<String> checked = new java.util.LinkedHashSet<>();
        final Consumer<List<String>> onDone;
        final Runnable back;
        int index;

        Checklist(String title, List<String> options, List<String> notes, java.util.Collection<String> initial, Consumer<List<String>> onDone,
                  Runnable back) {
            this.title = title;
            this.options = options;
            this.notes = notes;
            this.checked.addAll(initial);
            this.onDone = onDone;
            this.back = back;
        }
    }

    record Confirm(String text, Runnable yes) {}

    static final class Picker {
        final String title;
        final Consumer<String> onPick;
        final InputEditor filter = new InputEditor();
        final List<CompletableFuture<ModelCatalog.Result>> sources = new ArrayList<>();
        int index;
        int first;

        Picker(String title, Consumer<String> onPick) {
            this.title = title;
            this.onPick = onPick;
        }
    }

    private final BiConsumer<Rect, Runnable> hit;
    private final Consumer<String> fail;
    private Prompt prompt;
    private Checklist checklist;
    private Confirm confirm;
    private Picker picker;

    /** @param hit registers a clickable area; @param fail shows a problem on the screen behind */
    SettingsDialogs(BiConsumer<Rect, Runnable> hit, Consumer<String> fail) {
        this.hit = hit;
        this.fail = fail;
    }

    boolean active() {
        return picker != null || prompt != null || confirm != null || checklist != null;
    }

    /** @param back what Esc does: the previous step of a flow; null cancels the whole flow */
    void ask(String title, String hint, String initial, Consumer<String> onSubmit, Runnable back) {
        InputEditor e = new InputEditor();
        e.set(initial);
        prompt = new Prompt(title, hint, e, onSubmit, back);
    }

    void confirm(String text, Runnable yes) {
        confirm = new Confirm(text, yes);
    }

    void tick(String title, List<String> options, List<String> notes, java.util.Collection<String> initial, Consumer<List<String>> onDone,
              Runnable back) {
        checklist = new Checklist(title, options, notes, initial, onDone, back);
    }

    void pick(String title, Consumer<String> onPick, List<CompletableFuture<ModelCatalog.Result>> sources) {
        Picker p = new Picker(title, onPick);
        p.sources.addAll(sources);
        picker = p;
    }

    void render(Buffer buf, Rect r) {
        if (picker != null) {
            drawPicker(buf, r);
        } else if (checklist != null) {
            drawChecklist(buf, r);
        } else if (prompt != null) {
            drawPrompt(buf, r);
        } else if (confirm != null) {
            drawConfirm(buf, r);
        }
    }

    private Rect box(Rect r, int w, int h) {
        int bw = Math.min(r.width() - 4, w);
        int bh = Math.min(r.height() - 2, h);
        return new Rect(r.x() + (r.width() - bw) / 2, r.y() + Math.max(1, (r.height() - bh) / 2), bw, bh);
    }

    private void frame(Buffer buf, Rect b, String title) {
        fill(buf, b, st(Theme.TEXT, Theme.DIALOG));
        Style border = st(Theme.FAINT, Theme.DIALOG);
        putSafe(buf, b.x(), b.y(), "╭" + "─".repeat(b.width() - 2) + "╮", border, b.right());
        for (int y = b.y() + 1; y < b.bottom() - 1; y++) {
            putSafe(buf, b.x(), y, "│", border, b.right());
            putSafe(buf, b.right() - 1, y, "│", border, b.right());
        }
        putSafe(buf, b.x(), b.bottom() - 1, "╰" + "─".repeat(b.width() - 2) + "╯", border, b.right());
        putSafe(buf, b.x() + 2, b.y(), " " + title + " ", st(Theme.TEXT, Theme.DIALOG).bold(), b.right() - 2);
    }

    private void drawPrompt(Buffer buf, Rect r) {
        Rect b = box(r, 70, 7);
        frame(buf, b, prompt.title());
        putSafe(buf, b.x() + 2, b.y() + 1, prompt.hint(), st(Theme.DIM, Theme.DIALOG), b.right() - 2);
        Rect field = new Rect(b.x() + 2, b.y() + 3, b.width() - 4, 1);
        fill(buf, field, st(Theme.TEXT, Theme.FIELD));
        String t = prompt.editor().text();
        int avail = field.width() - 2;
        String shown = CharWidth.of(t) > avail ? CharWidth.substringByWidthFromEnd(t, avail) : t;
        putSafe(buf, field.x() + 1, field.y(), shown + "▏", st(Theme.TEXT, Theme.FIELD), field.right());
        putSafe(buf, b.x() + 2, b.bottom() - 2, prompt.back() == null ? t("Enter next · Esc cancel") : t("Enter next · Esc back"), st(Theme.DIM, Theme.DIALOG), b.right() - 2);
    }

    private void drawChecklist(Buffer buf, Rect r) {
        Rect b = box(r, 76, checklist.options.size() + 6);
        frame(buf, b, checklist.title);
        for (int i = 0; i < checklist.options.size(); i++) {
            String opt = checklist.options.get(i);
            boolean on = i == checklist.index;
            Style st = on ? st(Theme.TEXT, Theme.FIELD) : st(Theme.TEXT, Theme.DIALOG);
            int y = b.y() + 2 + i;
            int row = i;
            Rect line = new Rect(b.x() + 1, y, b.width() - 2, 1);
            fill(buf, line, st);
            putSafe(buf, b.x() + 2, y, (checklist.checked.contains(opt) ? "[x] " : "[ ] ") + opt, st, b.right() - 2);
            putSafe(buf, b.x() + 26, y, checklist.notes.get(i), on ? st : st(Theme.DIM, Theme.DIALOG), b.right() - 2);
            hit.accept(line, () -> {
                checklist.index = row;
                toggle(checklist, opt);
            });
        }
        putSafe(buf, b.x() + 2, b.bottom() - 2, t("↑↓ move · Space tick · Enter next · Esc back"), st(Theme.DIM, Theme.DIALOG), b.right() - 2);
    }

    private void drawConfirm(Buffer buf, Rect r) {
        List<String> lines = Wrap.lines(confirm.text(), 60);
        Rect b = box(r, 66, lines.size() + 5);
        frame(buf, b, t("Are you sure?"));
        for (int i = 0; i < lines.size(); i++) {
            putSafe(buf, b.x() + 2, b.y() + 1 + i, lines.get(i), st(Theme.TEXT, Theme.DIALOG), b.right() - 2);
        }
        int y = b.bottom() - 2;
        int w1 = putSafe(buf, b.x() + 2, y, " " + t("Yes") + "  Y ", st(Theme.TEXT, Theme.DANGER).bold(), b.right());
        hit.accept(new Rect(b.x() + 2, y, w1, 1), this::confirmYes);
        int w2 = putSafe(buf, b.x() + 4 + w1, y, " " + t("No") + "  N ", st(Theme.TEXT, Theme.FIELD), b.right());
        hit.accept(new Rect(b.x() + 4 + w1, y, w2, 1), () -> confirm = null);
    }

    private void confirmYes() {
        Runnable yes = confirm.yes();
        confirm = null;
        yes.run();
    }

    private void drawPicker(Buffer buf, Rect r) {
        Rect b = box(r, 90, r.height() - 4);
        frame(buf, b, picker.title);
        Rect field = new Rect(b.x() + 2, b.y() + 1, b.width() - 4, 1);
        fill(buf, field, st(Theme.TEXT, Theme.FIELD));
        String f = picker.filter.text();
        putSafe(buf, field.x() + 1, field.y(), f.isEmpty() ? t("Search models, or type provider:model and press Enter") + "▏" : f + "▏",
                st(f.isEmpty() ? Theme.DIM : Theme.TEXT, Theme.FIELD), field.right());
        List<ModelCatalog.Model> items = pickerItems();
        int rows = b.height() - 5;
        picker.index = Math.max(0, Math.min(picker.index, items.size() - 1));
        if (picker.index < picker.first) {
            picker.first = picker.index;
        } else if (picker.index >= picker.first + rows) {
            picker.first = picker.index - rows + 1;
        }
        int loading = 0;
        List<String> problems = new ArrayList<>();
        for (var src : picker.sources) {
            ModelCatalog.Result res = src.getNow(null);
            if (res == null) {
                loading++;
            } else if (res.problem() != null) {
                problems.add(res.problem());
            }
        }
        for (int i = 0; i < rows && picker.first + i < items.size(); i++) {
            ModelCatalog.Model m = items.get(picker.first + i);
            boolean sel = picker.first + i == picker.index;
            Color bg = sel ? Theme.SELECTED : Theme.DIALOG;
            Rect row = new Rect(b.x() + 1, b.y() + 3 + i, b.width() - 2, 1);
            fill(buf, row, st(Theme.TEXT, bg));
            int w = putSafe(buf, row.x() + 1, row.y(), m.ref(), st(m.tools() ? Theme.TEXT : Theme.DIM, bg), row.right() - 2);
            putSafe(buf, row.x() + 3 + w, row.y(), m.note(), st(m.free() ? Theme.ACCENT : Theme.DIM, bg), row.right() - 1);
            int idx = picker.first + i;
            hit.accept(row, () -> {
                picker.index = idx;
                pickSelected();
            });
        }
        String info = loading > 0 ? t("Loading models from {0} provider(s)…", loading)
                : t("{0} models", items.size()) + (problems.isEmpty() ? "" : " · " + String.join(" · ", problems));
        putSafe(buf, b.x() + 2, b.bottom() - 2, info + "   ·  " + t("↑↓ Enter pick · Esc cancel"), st(Theme.DIM, Theme.DIALOG), b.right() - 2);
    }

    private List<ModelCatalog.Model> pickerItems() {
        List<ModelCatalog.Model> all = new ArrayList<>();
        String f = picker.filter.text().strip().toLowerCase(Locale.ROOT);
        for (var src : picker.sources) {
            ModelCatalog.Result r = src.getNow(null);
            if (r != null) {
                for (ModelCatalog.Model m : r.models()) {
                    if (f.isEmpty() || m.ref().toLowerCase(Locale.ROOT).contains(f) || m.note().toLowerCase(Locale.ROOT).contains(f)) {
                        all.add(m);
                    }
                }
            }
        }
        return all;
    }

    private void pickSelected() {
        List<ModelCatalog.Model> items = pickerItems();
        String typed = picker.filter.text().strip();
        String choice = typed.contains(":") && (items.isEmpty() || items.stream().noneMatch(m -> m.ref().equals(typed)) && picker.index == 0
                && !items.get(0).ref().toLowerCase(Locale.ROOT).contains(typed.toLowerCase(Locale.ROOT)))
                ? typed : items.isEmpty() ? null : items.get(picker.index).ref();
        if (choice == null) {
            fail.accept(t("Type provider:model, e.g. openrouter:openrouter/free"));
            return;
        }
        Consumer<String> onPick = picker.onPick;
        picker = null;
        onPick.accept(choice);
    }

    /** @return false when no dialog is open, so the screen behind handles the key */
    boolean key(KeyEvent key) {
        KeyCode code = key.code();
        char ch = code == KeyCode.CHAR ? Character.toLowerCase(key.character()) : 0;
        if (picker != null) {
            switch (code) {
                case ESCAPE -> picker = null;
                case UP -> picker.index = Math.max(0, picker.index - 1);
                case DOWN -> picker.index++;
                case PAGE_DOWN -> picker.index += 10;
                case PAGE_UP -> picker.index = Math.max(0, picker.index - 10);
                case ENTER -> pickSelected();
                default -> edit(picker.filter, key);
            }
            if (code == KeyCode.CHAR || code == KeyCode.BACKSPACE) {
                picker.index = 0;
                picker.first = 0;
            }
            return true;
        }
        if (checklist != null) {
            Checklist c = checklist;
            switch (code) {
                case ESCAPE -> {
                    checklist = null;
                    c.back.run();
                }
                case UP -> c.index = Math.max(0, c.index - 1);
                case DOWN -> c.index = Math.min(c.options.size() - 1, c.index + 1);
                case ENTER -> {
                    checklist = null;
                    c.onDone.accept(c.options.stream().filter(c.checked::contains).toList());
                }
                case CHAR -> {
                    if (ch == ' ') {
                        toggle(c, c.options.get(c.index));
                    }
                }
                default -> { }
            }
            return true;
        }
        if (prompt != null) {
            switch (code) {
                case ESCAPE -> {
                    Runnable back = prompt.back();
                    prompt = null;
                    if (back != null) {
                        back.run();
                    }
                }
                case ENTER -> {
                    Prompt p = prompt;
                    prompt = null;
                    p.onSubmit().accept(p.editor().text());
                }
                default -> edit(prompt.editor(), key);
            }
            return true;
        }
        if (confirm != null) {
            if (ch == 'y' || code == KeyCode.ENTER) {
                confirmYes();
            } else if (ch == 'n' || code == KeyCode.ESCAPE) {
                confirm = null;
            }
            return true;
        }
        return false;
    }

    void paste(String text) {
        if (picker != null) {
            picker.filter.insert(text.strip());
        } else if (prompt != null) {
            prompt.editor().insert(text.replace('\n', ' '));
        }
    }

    /** The wheel over a model picker. @return whether one is open */
    boolean scroll(int direction) {
        if (picker == null) {
            return false;
        }
        picker.index = Math.max(0, picker.index + direction * 3);
        return true;
    }

    private static void toggle(Checklist c, String option) {
        if (!c.checked.remove(option)) {
            c.checked.add(option);
        }
    }

    private static void edit(InputEditor e, KeyEvent key) {
        switch (key.code()) {
            case BACKSPACE -> e.backspace();
            case DELETE -> e.delete();
            case LEFT -> e.left();
            case RIGHT -> e.right();
            case HOME -> e.home();
            case END -> e.end();
            case CHAR -> {
                if (key.hasCtrl() && Character.toLowerCase(key.character()) == 'u') {
                    e.clear();
                } else if (!key.hasCtrl() && !key.hasAlt() && key.character() >= ' ') {
                    e.insert(key.string());
                }
            }
            default -> { }
        }
    }
}
