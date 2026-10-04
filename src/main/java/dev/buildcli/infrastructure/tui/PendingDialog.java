package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;
import static dev.buildcli.infrastructure.tui.Draw.clean;
import static dev.buildcli.infrastructure.tui.Draw.fill;
import static dev.buildcli.infrastructure.tui.Draw.put;
import static dev.buildcli.infrastructure.tui.Draw.st;

import dev.buildcli.application.ChatSession;
import dev.buildcli.infrastructure.tui.Styled.Span;
import dev.buildcli.ports.EscalationChoice;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * What an agent waits for you to answer: approving a write or a command, what to do with a task that keeps failing, or a
 * question with options. It draws the box over the chat and takes the keys while one is open.
 */
final class PendingDialog {
    private final ChatSession session;
    private final InputEditor input;
    private final BiConsumer<Rect, Runnable> hit;
    private final Function<String, String> chatTitle;
    /** The question being asked, which option is highlighted, and whether you are typing your own answer instead. */
    private ChatSession.Pending.Question asked;
    private int choice;
    private boolean typingAnswer;

    /** @param input the chat's input box, where an open question or "something else" is answered; @param chatTitle a thread's name */
    PendingDialog(ChatSession session, InputEditor input, BiConsumer<Rect, Runnable> hit, Function<String, String> chatTitle) {
        this.session = session;
        this.input = input;
        this.hit = hit;
        this.chatTitle = chatTitle;
    }

    /** The keys while something is pending. @return null when the key is not for the dialog and the chat should handle it */
    EventResult key(KeyEvent key) {
        ChatSession.Pending p = session.pending();
        if (p == null) {
            return null;
        }
        boolean ctrl = key.hasCtrl();
        boolean alt = key.hasAlt();
        KeyCode code = key.code();
        char ch = code == KeyCode.CHAR ? Character.toLowerCase(key.character()) : 0;
        if (p instanceof ChatSession.Pending.Question q) {
            if (q != asked) {
                asked = q;
                choice = 0;
                typingAnswer = false;
            }
            return questionKey(q, key);
        }
        if (code == KeyCode.CHAR && (input.isEmpty() || ctrl)) {
            if (p instanceof ChatSession.Pending.Approval a && (ch == 'y' || ch == 'n')) {
                a.answer().complete(ch == 'y');
                return EventResult.HANDLED;
            }
            if (p instanceof ChatSession.Pending.Approval a && ch == 'a' && a.request().grantKey() != null) {
                session.approveAlways(a);
                return EventResult.HANDLED;
            }
            if (p instanceof ChatSession.Pending.Escalation e && "rsa".indexOf(ch) >= 0) {
                e.answer().complete(ch == 'r' ? EscalationChoice.RETRY : ch == 's' ? EscalationChoice.SKIP : EscalationChoice.ABORT);
                return EventResult.HANDLED;
            }
            if (input.isEmpty() && !ctrl && !alt) {
                // a letter that answers nothing must not start a message: it would make Y and N stop working
                return EventResult.HANDLED;
            }
        }
        return null;
    }

    void draw(Buffer buf, Rect r) {
        ChatSession.Pending p = session.pending();
        if (p == null) {
            return; // answered from another thread since the screen decided to draw the dialog
        }
        int w = Math.min(r.width() - 4, 100);
        Style base = st(Theme.TEXT, Theme.DIALOG);
        List<List<Span>> body = new ArrayList<>();
        String title;
        List<String[]> buttons = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        List<Integer> questionRows = new ArrayList<>();
        if (p instanceof ChatSession.Pending.Approval a) {
            title = t("{0} asks for approval", clean(a.request().agent())) + where(p);
            body.add(List.of(new Span(clean(a.request().summary()), base.bold())));
            body.add(List.of());
            int limit = Math.max(3, r.height() - 12);
            List<String> lines = clean(a.request().detail()).lines().toList();
            for (int i = 0; i < Math.min(limit, lines.size()); i++) {
                String l = lines.get(i);
                Color c = l.startsWith("+++") || l.startsWith("---") ? Theme.DIM : l.startsWith("@@") ? Theme.BLUE
                        : l.startsWith("+") ? Theme.ADD_FG : l.startsWith("-") ? Theme.DEL_FG : Theme.TEXT;
                body.add(List.of(new Span(CharWidth.substringByWidth(l.replace("\t", "    "), w - 4), st(c, Theme.DIALOG))));
            }
            if (lines.size() > limit) {
                body.add(List.of(new Span(t("… {0} more lines", lines.size() - limit), st(Theme.DIM, Theme.DIALOG))));
            }
            buttons.add(new String[] {" " + t("Approve") + "  Y ", "primary"});
            actions.add(() -> a.answer().complete(true));
            if (a.request().grantKey() != null) {
                buttons.add(new String[] {" " + t("Always here") + "  A ", "plain"});
                actions.add(() -> session.approveAlways(a));
                body.add(List.of());
                for (String line : Wrap.lines(t("A: {0}. Until you close BuildCLI; /revoke takes it back.", clean(a.request().grantLabel())), w - 4)) {
                    body.add(List.of(new Span(line, st(Theme.DIM, Theme.DIALOG))));
                }
            }
            buttons.add(new String[] {" " + t("Deny") + "  N ", "plain"});
            actions.add(() -> a.answer().complete(false));
        } else if (p instanceof ChatSession.Pending.Question q) {
            title = t("{0} asks you", clean(q.agent())) + where(p);
            body.addAll(Styled.lines(clean(q.question()), w - 4, base, base.bold(), base));
            body.add(List.of());
            if (!q.options().isEmpty() && !typingAnswer) {
                for (int i = 0; i <= q.options().size(); i++) {
                    boolean other = i == q.options().size();
                    String label = other ? t("Something else…") : clean(q.options().get(i));
                    Style os = i == choice ? st(Theme.BG, Theme.TEXT).bold() : other ? st(Theme.DIM, Theme.DIALOG) : base;
                    int row = body.size();
                    questionRows.add(row);
                    body.add(List.of(new Span((i == choice ? " ❯ " : "   ") + (i + 1) + ". " + label + " ", os)));
                }
                body.add(List.of());
                body.add(List.of(new Span(t("↑↓ or a number to choose · Enter confirms · Esc skips"), st(Theme.DIM, Theme.DIALOG))));
            } else {
                body.add(List.of(new Span(typingAnswer && !q.options().isEmpty() ? t("Type your answer below · Enter sends · Esc goes back")
                        : t("Type your answer below · Enter sends · Esc skips"), st(Theme.DIM, Theme.DIALOG))));
            }
        } else {
            var e = (ChatSession.Pending.Escalation) p;
            title = t("{0} is stuck (task #{1})", clean(e.agent()), e.taskId()) + where(p);
            body.addAll(Styled.lines(clean(e.objective()), w - 4, base, base.bold(), base));
            body.add(List.of());
            body.addAll(Styled.lines(t("It failed after the automatic retries: {0}", clean(e.reason())), w - 4, st(Theme.RED, Theme.DIALOG),
                    st(Theme.RED, Theme.DIALOG).bold(), base));
            buttons.add(new String[] {" " + t("Retry") + "  R ", "primary"});
            actions.add(() -> e.answer().complete(EscalationChoice.RETRY));
            buttons.add(new String[] {" " + t("Skip task") + "  S ", "plain"});
            actions.add(() -> e.answer().complete(EscalationChoice.SKIP));
            buttons.add(new String[] {" " + t("Stop everything") + "  A ", "danger"});
            actions.add(() -> e.answer().complete(EscalationChoice.ABORT));
        }
        int h = body.size() + (buttons.isEmpty() ? 3 : 5);
        int x = r.x() + (r.width() - w) / 2;
        int y = r.y() + Math.max(1, (r.height() - h) / 2);
        fill(buf, new Rect(x, y, w, h), base);
        Style border = st(Theme.FAINT, Theme.DIALOG);
        put(buf, x, y, "╭" + "─".repeat(w - 2) + "╮", border, x + w);
        for (int i = 1; i < h - 1; i++) {
            put(buf, x, y + i, "│", border, x + w);
            put(buf, x + w - 1, y + i, "│", border, x + w);
        }
        put(buf, x, y + h - 1, "╰" + "─".repeat(w - 2) + "╯", border, x + w);
        put(buf, x + 2, y, " " + title + " ", base.bold(), x + w - 2);
        for (int i = 0; i < body.size(); i++) {
            Draw.spans(buf, x + 2, y + 1 + i, body.get(i), x + w - 2, hit);
        }
        for (int i = 0; i < questionRows.size(); i++) {
            int index = i;
            hit.accept(new Rect(x + 2, y + 1 + questionRows.get(i), w - 4, 1), () -> chooseAnswer(index));
        }
        int bx = x + 2;
        for (int i = 0; i < buttons.size(); i++) {
            Style bs = switch (buttons.get(i)[1]) {
                case "primary" -> st(Theme.BG, Theme.TEXT).bold();
                case "danger" -> st(Theme.TEXT, Theme.DANGER).bold();
                default -> st(Theme.TEXT, Theme.FIELD);
            };
            int bw = put(buf, bx, y + h - 2, buttons.get(i)[0], bs, x + w - 1);
            hit.accept(new Rect(bx, y + h - 2, bw, 1), actions.get(i));
            bx += bw + 2;
        }
    }

    boolean typingOwnAnswer() {
        return session.pending() instanceof ChatSession.Pending.Question q && (typingAnswer || q.options().isEmpty());
    }

    /** The keys while an agent asks you something; null lets the key edit the answer in the input box. */
    private EventResult questionKey(ChatSession.Pending.Question q, KeyEvent key) {
        KeyCode code = key.code();
        if (key.hasCtrl()) {
            return null;
        }
        if (typingAnswer || q.options().isEmpty()) {
            if (code == KeyCode.ENTER && !key.hasShift() && !key.hasAlt()) {
                String text = input.text().strip();
                if (!text.isEmpty()) {
                    input.clear();
                    q.answer().complete(text);
                }
                return EventResult.HANDLED;
            }
            if (code == KeyCode.ESCAPE) {
                if (q.options().isEmpty()) {
                    q.answer().complete("");
                } else {
                    typingAnswer = false;
                }
                return EventResult.HANDLED;
            }
            return null;
        }
        int n = q.options().size() + 1;
        switch (code) {
            case UP -> choice = (choice - 1 + n) % n;
            case DOWN, TAB -> choice = (choice + 1) % n;
            case ENTER -> chooseAnswer(choice);
            case ESCAPE -> q.answer().complete("");
            case CHAR -> {
                int d = key.character() - '1';
                if (d >= 0 && d < n) {
                    chooseAnswer(d);
                }
            }
            default -> { }
        }
        return EventResult.HANDLED;
    }

    private void chooseAnswer(int index) {
        if (!(session.pending() instanceof ChatSession.Pending.Question q)) {
            return;
        }
        if (index >= q.options().size()) {
            typingAnswer = true;
            input.clear();
        } else {
            q.answer().complete(q.options().get(index));
        }
    }

    private String where(ChatSession.Pending p) {
        String chat = chatTitle.apply(p.thread());
        int n = session.pendingCount();
        return " · " + t("{0} chat", chat) + (n > 1 ? " · " + t("1 of {0}", n) : "");
    }
}
