package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;
import static dev.buildcli.infrastructure.tui.Draw.fill;
import static dev.buildcli.infrastructure.tui.Draw.put;
import static dev.buildcli.infrastructure.tui.Draw.st;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.ChatSession.Kind;
import dev.buildcli.application.ChatSession.Message;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Marking messages, like a messenger: right-click a message (or Alt+M for the newest) to start, click others to add or remove,
 * then copy, forward or delete the marked ones. Marked messages are tinted; a bar over the chat says how many and offers the
 * actions. Esc ends it.
 */
final class MessageMarks {
    private final ChatSession session;
    private final BiConsumer<Rect, Runnable> hit;
    private final Consumer<String> copy;
    private final Supplier<String> chat;
    private final Function<String, String> chatTitle;
    private final Set<Long> marked = new LinkedHashSet<>();
    private boolean confirmDelete;
    private boolean forwarding;

    /** @param copy puts text on the clipboard; @param chat the open chat; @param chatTitle a thread's name */
    MessageMarks(ChatSession session, BiConsumer<Rect, Runnable> hit, Consumer<String> copy, Supplier<String> chat, Function<String, String> chatTitle) {
        this.session = session;
        this.hit = hit;
        this.copy = copy;
        this.chat = chat;
        this.chatTitle = chatTitle;
    }

    boolean active() {
        return !marked.isEmpty();
    }

    boolean isMarked(long id) {
        return marked.contains(id);
    }

    boolean forwarding() {
        return forwarding && active();
    }

    /** One row of height while something is marked: where the bar goes. */
    int height() {
        return active() ? 1 : 0;
    }

    void clear() {
        marked.clear();
        confirmDelete = false;
        forwarding = false;
    }

    private Message find(long id) {
        for (Message m : session.messages()) {
            if (m.id() == id) {
                return m;
            }
        }
        return null;
    }

    /** What can be marked: what was said or reported, not the cards of changed files nor the small activity lines. */
    private static boolean markable(Message m) {
        return m.kind() == Kind.USER || m.kind() == Kind.AGENT || m.kind() == Kind.SYSTEM || m.kind() == Kind.ERROR;
    }

    /** Marks the message, or unmarks it if it was. */
    void toggle(long id) {
        Message m = find(id);
        if (m == null || !markable(m)) {
            return;
        }
        confirmDelete = false;
        if (!marked.remove(id)) {
            marked.add(id);
        }
        if (marked.isEmpty()) {
            clear();
        }
    }

    /** The messages of the open chat that can be marked, oldest first. */
    private List<Message> candidates() {
        List<Message> out = new ArrayList<>();
        for (Message m : session.messages()) {
            if (markable(m) && (m.thread().equals(chat.get()) || m.thread().equals(ChatSession.EVERYWHERE))) {
                out.add(m);
            }
        }
        return out;
    }

    /** The marked messages, oldest first. */
    private List<Message> selection() {
        List<Message> out = new ArrayList<>();
        for (Message m : session.messages()) {
            if (marked.contains(m.id())) {
                out.add(m);
            }
        }
        return out;
    }

    private static String who(Message m) {
        return m.kind() == Kind.USER ? t("You") : m.author().isEmpty() ? "BuildCLI" : m.author();
    }

    private String text() {
        StringBuilder sb = new StringBuilder();
        for (Message m : selection()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(m.text());
        }
        return sb.toString();
    }

    private void copySelected() {
        String t = text();
        clear();
        copy.accept(t);
    }

    private void delete() {
        int asked = marked.size();
        int gone = session.deleteMessages(new ArrayList<>(marked));
        clear();
        if (gone < asked) {
            session.system(t("{0} could not be deleted: a message still being answered, or a card of changed files.", asked - gone));
        }
    }

    /** The user picked a chat while forwarding: send the marked messages there as one message from you. */
    void forwardTo(String thread) {
        List<Message> picked = selection();
        String from = chatTitle.apply(chat.get());
        StringBuilder sb = new StringBuilder(t("Forwarded from {0}:", from));
        for (Message m : picked) {
            sb.append("\n\n").append(who(m)).append(": ").append(m.text());
        }
        clear();
        session.submit(sb.toString(), List.of(), thread);
    }

    /** The bar over the chat. */
    void draw(Buffer buf, Rect r) {
        Style base = st(Theme.TEXT, Theme.PANEL);
        fill(buf, r, base);
        int x = r.x() + 1;
        String close = " ✕ ";
        put(buf, x, r.y(), close, st(Theme.DIM, Theme.PANEL), r.right());
        hit.accept(new Rect(x, r.y(), 3, 1), this::clear);
        x += 4;
        String label = forwarding ? t("Forward to… click a chat in the list · Esc cancels")
                : confirmDelete ? (marked.size() == 1 ? t("Delete 1 message? The agents forget it too.") : t("Delete {0} messages? The agents forget them too.", marked.size()))
                : t("{0} selected", marked.size());
        x += put(buf, x, r.y(), label, base.bold(), r.right()) + 2;
        if (forwarding) {
            return;
        }
        if (confirmDelete) {
            x += button(buf, x, r, " " + t("Delete") + "  Y ", st(Theme.TEXT, Theme.DANGER).bold(), this::delete);
            button(buf, x + 1, r, " " + t("Cancel") + "  N ", st(Theme.TEXT, Theme.FIELD), () -> confirmDelete = false);
            return;
        }
        x += button(buf, x, r, " " + t("Copy") + "  C ", st(Theme.TEXT, Theme.FIELD), this::copySelected) + 1;
        x += button(buf, x, r, " " + t("Forward") + "  F ", st(Theme.TEXT, Theme.FIELD), () -> forwarding = true) + 1;
        button(buf, x, r, " " + t("Delete") + "  D ", st(Theme.TEXT, Theme.FIELD), () -> confirmDelete = true);
    }

    private int button(Buffer buf, int x, Rect r, String label, Style style, Runnable action) {
        int w = put(buf, x, r.y(), label, style, r.right());
        hit.accept(new Rect(x, r.y(), w, 1), action);
        return w;
    }

    /** Alt+M: starts marking with the newest message of the chat. */
    void start() {
        List<Message> all = candidates();
        if (!all.isEmpty() && !active()) {
            marked.add(all.get(all.size() - 1).id());
        }
    }

    /** Keys while something is marked. @return null if the key is not for marking */
    EventResult key(KeyEvent key) {
        if (!active() || key.hasCtrl() || key.hasAlt()) {
            return null;
        }
        char ch = key.code() == KeyCode.CHAR ? Character.toLowerCase(key.character()) : 0;
        if (key.code() == KeyCode.ESCAPE) {
            clear();
            return EventResult.HANDLED;
        }
        if (forwarding) {
            return null;
        }
        if (confirmDelete) {
            if (ch == 'y' || key.code() == KeyCode.ENTER) {
                delete();
            } else if (ch == 'n') {
                confirmDelete = false;
            }
            return EventResult.HANDLED;
        }
        switch (ch) {
            case 'c' -> copySelected();
            case 'f' -> forwarding = true;
            case 'd' -> confirmDelete = true;
            default -> {
                return extend(key);
            }
        }
        return EventResult.HANDLED;
    }

    /** Up marks the message before the oldest marked one; Down unmarks the oldest, like extending a selection by lines. */
    private EventResult extend(KeyEvent key) {
        List<Message> all = candidates();
        List<Message> picked = selection();
        if (key.code() == KeyCode.UP && !picked.isEmpty()) {
            int at = all.indexOf(picked.get(0));
            if (at > 0) {
                marked.add(all.get(at - 1).id());
            }
            return EventResult.HANDLED;
        }
        if (key.code() == KeyCode.DOWN && picked.size() > 1) {
            marked.remove(picked.get(0).id());
            return EventResult.HANDLED;
        }
        return null;
    }
}
