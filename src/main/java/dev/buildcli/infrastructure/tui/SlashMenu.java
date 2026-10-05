package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;
import static dev.buildcli.infrastructure.tui.Draw.clean;
import static dev.buildcli.infrastructure.tui.Draw.fill;
import static dev.buildcli.infrastructure.tui.Draw.put;
import static dev.buildcli.infrastructure.tui.Draw.putFit;
import static dev.buildcli.infrastructure.tui.Draw.st;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Agent;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.tui.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The menu above the input box: commands while you type "/word", agents while you type "@name". Up and Down choose, Tab
 * fills the choice in, Enter runs or completes it, Esc hides it until the text changes.
 */
final class SlashMenu {
    private static final int MAX_ROWS = 8;

    private final InputEditor input;
    private final ChatSession session;
    private final Supplier<String> selected;
    /** Runs a command typed in any chat; false if it is not one of those (AgentFather's own are answered by its chat). */
    private final Predicate<String> runCommand;
    private final BiConsumer<Rect, Runnable> hit;
    private int index;
    private String dismissedFor;

    SlashMenu(InputEditor input, ChatSession session, Supplier<String> selected, Predicate<String> runCommand, BiConsumer<Rect, Runnable> hit) {
        this.input = input;
        this.session = session;
        this.selected = selected;
        this.runCommand = runCommand;
        this.hit = hit;
    }

    /** The text changed or a new chat was opened: start at the first choice, and a menu hidden with Esc may show again. */
    void reset() {
        index = 0;
        dismissedFor = null;
    }

    /** @return true if the key was for the menu; false lets the input box handle it (Enter on a finished @name, for one) */
    boolean key(KeyEvent key) {
        List<MenuItem> items = items();
        if (items.isEmpty()) {
            return false;
        }
        switch (key.code()) {
            case UP -> {
                index = (index - 1 + items.size()) % items.size();
                return true;
            }
            case DOWN -> {
                index = (index + 1) % items.size();
                return true;
            }
            case TAB -> {
                items.get(Math.min(index, items.size() - 1)).fill().run();
                return true;
            }
            case ENTER -> {
                String typed = input.mentionPrefix();
                boolean complete = typed != null && items.size() == 1 && items.get(0).label().equalsIgnoreCase("@" + typed);
                if (!(key.hasAlt() || key.hasShift()) && !complete) {
                    items.get(Math.min(index, items.size() - 1)).accept().run();
                    return true;
                }
                return false;
            }
            case ESCAPE -> {
                dismissedFor = input.text().startsWith("/") ? input.text() : "@" + input.mentionPrefix();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    record MenuItem(String label, String detail, String right, Runnable accept, Runnable fill) {
        MenuItem(String label, String detail, String right, Runnable accept) {
            this(label, detail, right, accept, accept);
        }
    }

    /** The command menu while typing "/word", or the mention menu while typing "@name"; empty when neither. */
    private List<MenuItem> items() {
        String text = input.text();
        String key;
        List<MenuItem> items = new ArrayList<>();
        if (text.startsWith("/") && !text.contains(" ") && !text.contains("\n") && input.cursor() == text.length()) {
            key = text;
            String typed = text.substring(1).toLowerCase(Locale.ROOT);
            for (ChatCommands.Command c : commandsHere()) {
                if (c.name().startsWith(typed)) {
                    items.add(new MenuItem("/" + c.name() + (c.arg().isEmpty() ? "" : " " + c.arg()), c.description(), c.shortcut(),
                            () -> acceptCommand(c), () -> input.set("/" + c.name() + " ")));
                }
            }
        } else {
            String prefix = input.mentionPrefix();
            if (prefix == null) {
                return List.of();
            }
            key = "@" + prefix;
            var g = session.group(selected.get());
            List<Agent> ordered = new ArrayList<>();
            for (Agent a : session.contacts()) {
                if (g == null || g.has(a.name())) {
                    ordered.add(a);
                }
            }
            for (Agent a : session.contacts()) {
                if (g != null && !g.has(a.name())) {
                    ordered.add(a);
                }
            }
            for (Agent a : ordered) {
                if (a.name().toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                    String tag = g == null ? "" : !g.has(a.name()) ? t("not in this group") : g.isAdmin(a.name()) ? t("admin") : "";
                    items.add(new MenuItem("@" + a.name(), a.role(), tag, () -> completeMention(a.name())));
                }
            }
        }
        return key.equals(dismissedFor) ? List.of() : items;
    }

    /** The commands the menu offers in the open chat: AgentFather's own, in its chat, and the ones every chat has. */
    private List<ChatCommands.Command> commandsHere() {
        if (!ChatSession.FATHER.equals(selected.get())) {
            return ChatCommands.LIST;
        }
        List<ChatCommands.Command> all = new ArrayList<>(AgentFather.COMMANDS);
        all.addAll(ChatCommands.LIST);
        return all;
    }

    private void acceptCommand(ChatCommands.Command c) {
        if (c.arg().isEmpty() || c.arg().startsWith("[")) {
            input.clear();
            if (!runCommand.test("/" + c.name())) {
                // not one of the commands every chat has: the chat itself answers it (AgentFather's)
                session.submit("/" + c.name(), List.of(), selected.get());
            }
        } else {
            input.set("/" + c.name() + " ");
        }
    }

    void draw(Buffer buf, Rect box) {
        List<MenuItem> items = items();
        if (items.isEmpty()) {
            return;
        }
        index = Math.max(0, Math.min(index, items.size() - 1));
        int h = Math.min(items.size(), MAX_ROWS);
        int first = Math.max(0, Math.min(index - h + 1, items.size() - h));
        int w = box.width();
        int y0 = box.y() - h - 1;
        Style bg = st(Theme.TEXT, Theme.DIALOG);
        fill(buf, new Rect(box.x(), y0, w, h + 1), bg);
        put(buf, box.x() + 2, y0, items.get(0).label().startsWith("/") ? t("Commands") : t("Mention an agent"), st(Theme.DIM, Theme.DIALOG), box.right());
        boolean commands = items.get(0).label().startsWith("/");
        String hint = (commands ? t("↑↓ choose · Tab fills · Enter runs") : t("↑↓ choose · Tab or Enter completes")) + (items.size() > h ? " · " + t("{0} matches", items.size()) : "");
        put(buf, box.right() - 2 - Wrap.width(hint), y0, hint, st(Theme.FAINT, Theme.DIALOG), box.right() - 1);
        int labelW = 0;
        for (MenuItem it : items) {
            labelW = Math.max(labelW, Wrap.width(it.label()));
        }
        for (int i = 0; i < h; i++) {
            MenuItem it = items.get(first + i);
            boolean sel = first + i == index;
            Color rowBg = sel ? Theme.SELECTED : Theme.DIALOG;
            Rect row = new Rect(box.x(), y0 + 1 + i, w, 1);
            fill(buf, row, st(Theme.TEXT, rowBg));
            put(buf, row.x() + 2, row.y(), it.label(), st(Theme.TEXT, rowBg).bold(), row.right() - 1);
            putFit(buf, row.x() + 4 + labelW, row.y(), clean(t(it.detail())), st(Theme.DIM, rowBg), row.right() - 10);
            put(buf, row.right() - 2 - Wrap.width(it.right()), row.y(), it.right(), st(Theme.DIM, rowBg), row.right() - 1);
            int idx = first + i;
            hit.accept(row, () -> {
                index = idx;
                it.accept().run();
            });
        }
    }

    private void completeMention(String name) {
        input.completeMention(name);
        dismissedFor = null;
    }
}
