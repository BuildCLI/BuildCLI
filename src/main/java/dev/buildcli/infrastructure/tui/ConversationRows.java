package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;
import static dev.buildcli.infrastructure.tui.Draw.DAY;
import static dev.buildcli.infrastructure.tui.Draw.TIME;
import static dev.buildcli.infrastructure.tui.Draw.clean;
import static dev.buildcli.infrastructure.tui.Draw.st;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.I18n;
import dev.buildcli.application.ChatSession.Message;
import dev.buildcli.application.ChatSession.State;
import dev.buildcli.domain.Attachment;
import dev.buildcli.infrastructure.tui.Styled.Span;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The rows of the open chat: day separators, bubbles with ticks and code blocks, activity lines, the "changed N files" card,
 * the typing dots, and what an empty chat shows (start a conversation, or create the first agent). A row is a list of
 * styled spans, some of them clickable; the screen decides where and how to draw them.
 */
final class ConversationRows {

    /** One line of the conversation: where it starts and what it is made of. */
    /** One screen line of the conversation; {@code message} is the id of the message it belongs to, or -1 (date pills, welcome texts...). */
    record Row(int x, List<Span> spans, long message) {
        Row(int x, List<Span> spans) {
            this(x, spans, -1);
        }
    }

    /** What a click on a button in the conversation does; the screen owns all of it. */
    interface Host {
        String selected();

        void review(long changesId, boolean undo);

        void copy(String text);

        void addSampleAgents();

        void createAgent();

        void openFather();

        void openConnect(String why);

        void openSettings();

        boolean runCommand(String text);

        void setInput(String text);

        /** Whether the folder is a git repository, where "my changes" and "tests" mean something. */
        boolean codeFolder();
    }

    private final ChatSession session;
    private final SettingsServices services;
    private final ChatListView chatList;
    private final Host host;
    /** True while an image or audio preview is still being prepared: the screen redraws until it is done. */
    private volatile boolean loadingPreviews;

    ConversationRows(ChatSession session, SettingsServices services, ChatListView chatList, Host host) {
        this.session = session;
        this.services = services;
        this.chatList = chatList;
        this.host = host;
    }

    boolean loadingPreviews() {
        return loadingPreviews;
    }

    void previewsRendered() {
        loadingPreviews = false;
    }

    List<Row> build(int width, List<Message> msgs) {
        List<Row> rows = new ArrayList<>();
        if (!chatList.exists(host.selected())) {
            onboarding(rows, width);
            return rows;
        }
        if (msgs.isEmpty() && !session.isActive(host.selected())) {
            welcome(rows, width);
            return rows;
        }
        boolean group = session.group(host.selected()) != null || ChatSession.isAgentChat(host.selected());
        long failed = session.lastFailedMessage();
        java.time.LocalDate day = null;
        Message prev = null;
        rows.add(new Row(0, List.of()));
        for (Message m : msgs) {
            java.time.LocalDate d = m.at().atZone(ZoneId.systemDefault()).toLocalDate();
            if (!d.equals(day)) {
                day = d;
                String label = d.equals(java.time.LocalDate.now()) ? t("TODAY") : d.equals(java.time.LocalDate.now().minusDays(1)) ? t("YESTERDAY")
                        : DAY.format(d).toUpperCase(Locale.ROOT);
                centred(rows, width, " " + label + " ", st(Theme.DIM, Theme.PILL));
                rows.add(new Row(0, List.of()));
                prev = null;
            }
            boolean sameAuthor = prev != null && prev.kind() == m.kind() && prev.author().equals(m.author())
                    && (m.kind() == ChatSession.Kind.USER || m.kind() == ChatSession.Kind.AGENT);
            if (m.kind() == ChatSession.Kind.ACTIVITY && !services.settings().flag(dev.buildcli.application.Settings.SHOW_ACTIVITY)) {
                continue;
            }
            boolean bothQuiet = prev != null && isQuiet(prev) && isQuiet(m);
            boolean bothBubbles = prev != null && isBubble(prev) && isBubble(m); // their rounded edges already leave a gap
            if (prev != null && !sameAuthor && !bothQuiet && !bothBubbles && !services.settings().flag(dev.buildcli.application.Settings.COMPACT)) {
                rows.add(new Row(0, List.of()));
            }
            int firstRow = rows.size();
            switch (m.kind()) {
                case USER -> userBubble(rows, width, m, m.id() == failed);
                case AGENT -> agentBubble(rows, width, group && !sameAuthor ? clean(m.author()) : null, m.text(), TIME.format(m.at()), false);
                case ACTIVITY -> activity(rows, width, m);
                case CHANGES -> changesCard(rows, width, m);
                case SYSTEM -> centred(rows, width, " " + clean(m.text()).replace('\n', ' ') + " ", st(Theme.DIM, Theme.PILL));
                case ERROR -> problem(rows, width, m.text(), failed);
                default -> { }
            }
            for (int i = firstRow; i < rows.size(); i++) {
                rows.set(i, new Row(rows.get(i).x(), rows.get(i).spans(), m.id()));
            }
            prev = m;
        }
        if (session.isActive(host.selected())) {
            rows.add(new Row(0, List.of()));
            ChatSession.Live live = session.live(host.selected());
            if (live != null) {
                agentBubble(rows, width, group ? clean(live.agent()) : null, live.text() + " ▍", "", true);
            } else if (session.pending() == null) {
                String who = chatList.busyAgentIn(host.selected());
                typingDots(rows, group ? who : "", who.isEmpty() ? "" : session.agentState(who));
            }
        }
        rows.add(new Row(0, List.of()));
        return rows;
    }

    private static boolean isBubble(Message m) {
        return m.kind() == ChatSession.Kind.USER || m.kind() == ChatSession.Kind.AGENT;
    }

    private static boolean isQuiet(Message m) {
        return m.kind() == ChatSession.Kind.ACTIVITY || m.kind() == ChatSession.Kind.SYSTEM || m.kind() == ChatSession.Kind.CHANGES;
    }

    /** "ana changed 2 files", with buttons to see exactly what changed and to put it back. */
    private void changesCard(List<Row> rows, int width, Message m) {
        boolean undone = m.state() == State.UNDONE;
        var nets = dev.buildcli.application.tools.FileChanges.net(session.changes(m.id()));
        int add = 0;
        int del = 0;
        for (var n : nets) {
            int[] c = n.counts();
            add += c[0];
            del += c[1];
        }
        String stat = nets.isEmpty() ? "" : "  +" + add + " −" + del;
        String text = (undone ? "↶ " : "✎ ") + clean(m.author()) + " " + clean(m.text()).replaceFirst("^Changed", "changed");
        int room = Math.max(10, Math.min(width - 14 - Wrap.width(stat), 76));
        String shown = CharWidth.truncateWithEllipsis(text, room, CharWidth.TruncatePosition.END);
        Style pill = st(undone ? Theme.DIM : Theme.TEXT, Theme.PILL);
        int w = Wrap.width(shown) + Wrap.width(stat) + 2;
        rows.add(new Row(Math.max(0, (width - w) / 2), List.of(new Span(" " + shown, pill),
                new Span(stat + " ", st(undone ? Theme.DIM : Theme.ACCENT, Theme.PILL)))));
        List<Span> buttons = new ArrayList<>();
        buttons.add(new Span(" " + t("Review") + " ", st(Theme.TEXT, Theme.FIELD), () -> host.review(m.id(), false)));
        if (undone) {
            buttons.add(new Span("  " + t("undone"), st(Theme.DIM, Theme.BG).italic()));
        } else if (session.canUndo()) {
            buttons.add(new Span(" ", st(Theme.TEXT, Theme.BG)));
            buttons.add(new Span(" " + t("Undo") + " ", st(Theme.TEXT, Theme.FIELD), () -> host.review(m.id(), true)));
        }
        int bw = Styled.width(buttons);
        rows.add(new Row(Math.max(0, (width - bw) / 2), buttons));
    }

    private static void centred(List<Row> rows, int width, String text, Style style) {
        for (String line : Wrap.lines(text, Math.max(10, width - 10))) {
            String l = line.startsWith(" ") ? line : " " + line;
            l = l.endsWith(" ") ? l : l + " ";
            rows.add(new Row(Math.max(0, (width - Wrap.width(l)) / 2), List.of(new Span(l, style))));
        }
    }

    private void typingDots(List<Row> rows, String who, String state) {
        int phase = (int) (System.currentTimeMillis() / 300 % 3);
        StringBuilder dots = new StringBuilder(" ");
        for (int i = 0; i < 3; i++) {
            dots.append(i == phase ? "●" : "•").append(' ');
        }
        List<Span> spans = new ArrayList<>();
        spans.add(new Span(" ", st(Theme.TEXT, Theme.THEM)));
        if (!who.isEmpty()) {
            spans.add(new Span(clean(who) + " ", st(Theme.agentColor(who), Theme.THEM).bold()));
        }
        spans.add(new Span(dots.toString(), st(Theme.DIM, Theme.THEM)));
        if (state.startsWith("waiting") || state.equals("reading") || state.equals("working")) {
            spans.add(new Span(I18n.state(state) + " ", st(Theme.DIM, Theme.THEM).italic()));
        }
        rows.add(new Row(2, spans));
    }

    /** No agents yet: BuildCLI is about agents, so the first thing to do is create one. */
    private void onboarding(List<Row> rows, int width) {
        centred(rows, width, t("No agents yet"), st(Theme.TEXT, Theme.BG).bold());
        rows.add(new Row(0, List.of()));
        centred(rows, width, t("Agents are the people you chat with: each has a role and a model, and does only what you allow."),
                st(Theme.DIM, Theme.BG));
        rows.add(new Row(0, List.of()));
        rows.add(new Row(0, List.of()));
        int boxW = Math.min(width - 4, 56);
        int x = Math.max(0, (width - boxW) / 2);
        String[][] actions = {{t("Add a ready-made team: software, writing or office"), "samples"}, {t("Create your own agent"), "agent"},
            {t("Talk to AgentFather: it creates agents for you"), "father"}, {t("Connect a model and provider"), "connect"}};
        for (String[] a : actions) {
            Runnable act = switch (a[1]) {
                case "samples" -> host::addSampleAgents;
                case "agent" -> host::createAgent;
                case "father" -> host::openFather;
                default -> () -> {
                    if (services.canConnect()) {
                        host.openConnect(null);
                    } else {
                        host.openSettings();
                    }
                };
            };
            String label = "  " + a[0];
            rows.add(new Row(x, List.of(new Span(label + " ".repeat(Math.max(1, boxW - Wrap.width(label) - 2)) + "› ", st(Theme.TEXT, Theme.PANEL), act))));
            rows.add(new Row(0, List.of()));
        }
        centred(rows, width, t("The sample teams are only a start: you can change them, or make your own. Every write asks you first, and you can undo it."), st(Theme.FAINT, Theme.BG));
    }

    private void welcome(List<Row> rows, int width) {
        if (chatList.isSpecial(host.selected())) {
            boolean notes = ChatSession.NOTES.equals(host.selected());
            centred(rows, width, notes ? t("Notes to yourself") : t("Nothing here yet"), st(Theme.TEXT, Theme.BG).bold());
            rows.add(new Row(0, List.of()));
            centred(rows, width, notes ? t("Write anything you want to keep. Only you can read this chat: no agent sees it.")
                    : t("Agents write here when you ask one of them to write to the other. You can read it, but not write in it."),
                    st(Theme.DIM, Theme.BG));
            return;
        }
        var g = session.group(host.selected());
        boolean isGroup = g != null;
        String who = isGroup ? clean(g.name()) : clean(host.selected());
        centred(rows, width, t("Start a conversation with {0}", who), st(Theme.TEXT, Theme.BG).bold());
        rows.add(new Row(0, List.of()));
        String sub = isGroup ? (g.admins().isEmpty() ? t("No admin yet. Type @ to talk to someone, or mention two people to ask both.")
                : t("{0} (admin) answers messages that mention nobody. Type @ to talk to someone, or mention two people to ask both.", clean(String.join(", ", g.admins()))))
                : t("{0}. Messages here go straight to {1}.", clean(chatList.roleOf(host.selected())), clean(host.selected()));
        centred(rows, width, sub, st(Theme.DIM, Theme.BG));
        rows.add(new Row(0, List.of()));
        rows.add(new Row(0, List.of()));
        String[][] ideas;
        if (host.codeFolder()) {
            ideas = isGroup
                    ? new String[][] {{t("Review my uncommitted changes"), t("Review my uncommitted changes and point out risks.")},
                        {t("Explain this project"), t("Explain how this project is organised and where to start.")},
                        {t("Add missing tests"), t("Find important code without tests and add unit tests for it.")},
                        {t("Show my changes"), "/diff"}}
                    : new String[][] {{t("What can you do?"), t("What can you do for me here, with your tools and permissions?")},
                        {t("Look at my changes"), t("Look at my uncommitted changes and tell me what you think.")}};
        } else {
            ideas = isGroup
                    ? new String[][] {{t("Summarize what is in this folder"), t("Look at the files in this folder and summarize what each one is about.")},
                        {t("Draft a document from my notes"), t("Read my notes in this folder and draft a clear document from them.")},
                        {t("Find mistakes and unclear parts"), t("Read the documents in this folder and point out mistakes and unclear parts.")},
                        {t("Plan next steps"), t("Read what is in this folder and propose a short list of next steps.")}}
                    : new String[][] {{t("What can you do?"), t("What can you do for me here, with your tools and permissions?")},
                        {t("Summarize this folder"), t("Look at the files in this folder and summarize what each one is about.")}};
        }
        int boxW = Math.min(width - 4, 56);
        int x = Math.max(0, (width - boxW) / 2);
        for (String[] idea : ideas) {
            Runnable act = idea[1].startsWith("/") ? () -> host.runCommand(idea[1]) : () -> host.setInput(idea[1]);
            String label = "  " + idea[0];
            rows.add(new Row(x, List.of(new Span(label + " ".repeat(Math.max(1, boxW - Wrap.width(label) - 2)) + "› ", st(Theme.TEXT, Theme.PANEL), act))));
            rows.add(new Row(0, List.of()));
        }
    }

    private int maxBody(int width) {
        return Math.max(16, Math.min(width - 10, Math.max(40, (int) (width * 0.72))));
    }

    private void userBubble(List<Row> rows, int width, Message m, boolean retryable) {
        Style base = st(Theme.ON_ME, Theme.ME);
        Style code = st(Theme.CODE_TEXT, Theme.CODE);
        int max = maxBody(width);
        List<List<Span>> body = new ArrayList<>();
        for (Attachment a : m.attachments()) {
            attachmentLines(body, a, base, max);
        }
        if (!m.text().isEmpty()) {
            body.addAll(Styled.lines(clean(m.text()), max, base, base.bold(), code));
        }
        List<Span> footer = new ArrayList<>();
        Style meta = st(Theme.ON_ME_DIM, Theme.ME);
        footer.add(new Span(TIME.format(m.at()) + " ", meta));
        switch (m.state()) {
            case QUEUED -> footer.add(new Span("✓", meta));
            case RUNNING -> footer.add(new Span("✓✓", st(Theme.TICK, Theme.ME)));
            case DONE -> footer.add(new Span("✓✓", st(Theme.TICK, Theme.ME)));
            case FAILED -> {
                footer.add(new Span("! not sent ", st(Theme.DANGER, Theme.ME).bold()));
                if (retryable) {
                    footer.add(new Span(" Retry ", st(Theme.BG, Theme.AMBER).bold(), () -> session.retry(m.id())));
                    if (services.canConnect()) {
                        footer.add(new Span(" ", meta));
                        footer.add(new Span(" Change model ", st(Theme.TEXT, Theme.FIELD), () -> host.openConnect(null)));
                    }
                }
            }
            default -> { }
        }
        bubble(rows, width, true, null, null, body, footer, base, code);
    }

    private void agentBubble(List<Row> rows, int width, String author, String text, String time, boolean live) {
        Style base = st(Theme.TEXT, Theme.THEM);
        Style code = st(Theme.CODE_TEXT, Theme.CODE);
        List<List<Span>> body = Styled.lines(clean(text), maxBody(width), base, base.bold(), code, live ? null : host::copy);
        List<Span> footer = time.isEmpty() ? List.of() : List.of(new Span(time, st(Theme.DIM, Theme.THEM)));
        bubble(rows, width, false, author, author == null ? null : Theme.agentColor(author), body, footer, base, code);
    }

    private void problem(List<Row> rows, int width, String text, long failed) {
        Style base = st(Theme.TEXT, Theme.ERROR_BG);
        List<List<Span>> body = Styled.lines(clean(text), maxBody(width), base, base.bold(), base);
        List<Span> footer = new ArrayList<>();
        if (failed >= 0) {
            footer.add(new Span(" Retry ", st(Theme.BG, Theme.AMBER).bold(), () -> session.retry(failed)));
        }
        if (services.canConnect()) {
            footer.add(new Span(" ", base));
            footer.add(new Span(" Change model ", st(Theme.TEXT, Theme.FIELD), () -> host.openConnect("The last message failed: " + text)));
        }
        bubble(rows, width, false, "Something went wrong", Theme.RED, body, footer, base, base);
    }

    private void activity(List<Row> rows, int width, Message m) {
        Color c = switch (m.state()) {
            case DONE -> Theme.ACCENT;
            case FAILED -> Theme.RED;
            default -> Theme.AMBER;
        };
        String glyph = switch (m.state()) {
            case DONE -> "✓";
            case FAILED -> "✗";
            default -> "◌";
        };
        String text = clean(m.author()) + " " + clean(m.text()).replace('\n', ' ');
        String shown = CharWidth.truncateWithEllipsis(text, Math.max(10, Math.min(width - 14, 76)), CharWidth.TruncatePosition.END);
        int w = Wrap.width(shown) + 4;
        rows.add(new Row(Math.max(0, (width - w) / 2), List.of(new Span(" " + glyph + " ", st(c, Theme.PILL)),
                new Span(shown + " ", st(m.state() == State.FAILED ? Theme.RED : Theme.DIM, Theme.PILL)))));
    }

    /** A bubble: optional author line, body, a footer aligned right (time, ticks), with one column of padding. */
    private void bubble(List<Row> rows, int width, boolean mine, String author, Color authorColor, List<List<Span>> body,
            List<Span> footer, Style base, Style code) {
        int inner = 0;
        for (List<Span> line : body) {
            inner = Math.max(inner, Styled.width(line));
        }
        if (author != null) {
            inner = Math.max(inner, Wrap.width(author));
        }
        // the time sits on the last line when it fits, as in WhatsApp; otherwise on its own line
        int footW = Styled.width(footer);
        List<Span> lastLine = body.isEmpty() ? List.of() : body.get(body.size() - 1);
        boolean inline = !footer.isEmpty() && Styled.width(lastLine) + 2 + footW <= Math.max(inner, maxBody(width)) && !isCode(lastLine, code);
        inner = Math.max(inner, inline ? Styled.width(lastLine) + 2 + footW : footW);
        inner = Math.max(inner, 6);
        int x = mine ? Math.max(0, width - inner - 4) : 2;
        int firstRow = rows.size();
        if (author != null) {
            rows.add(line(x, List.of(new Span(author, st(authorColor, base.bg().orElse(Theme.THEM)).bold())), inner, base));
        }
        for (int i = 0; i < body.size(); i++) {
            List<Span> l = body.get(i);
            if (inline && i == body.size() - 1) {
                List<Span> withTime = new ArrayList<>(l);
                withTime.add(new Span(" ".repeat(inner - Styled.width(l) - footW), base));
                withTime.addAll(footer);
                rows.add(line(x, withTime, inner, base));
            } else {
                rows.add(line(x, l, inner, isCode(l, code) ? code : base));
            }
        }
        if (!inline && !footer.isEmpty()) {
            List<Span> f = new ArrayList<>();
            f.add(new Span(" ".repeat(Math.max(0, inner - footW)), base));
            f.addAll(footer);
            rows.add(line(x, f, inner, base));
        }
        // a half-height edge above and below, one cell shorter on each side: the corners come out rounded, as in a messenger
        Style edge = Style.create().fg(base.bg().orElse(Theme.THEM)).bg(Theme.BG);
        rows.add(firstRow, new Row(x, List.of(new Span(" ", edge), new Span("▄".repeat(inner), edge), new Span(" ", edge))));
        rows.add(new Row(x, List.of(new Span(" ", edge), new Span("▀".repeat(inner), edge), new Span(" ", edge))));
    }


    private static boolean isCode(List<Span> line, Style code) {
        if (line.isEmpty()) {
            return false;
        }
        for (Span s : line) {
            if (!s.style().equals(code)) {
                return false;
            }
        }
        return true;
    }

    /** One bubble row; {@code fillStyle} pads the content (code lines keep their darker background to the edge). */
    private static Row line(int x, List<Span> content, int inner, Style fillStyle) {
        List<Span> spans = new ArrayList<>();
        Style edge = Style.create().fg(fillStyle.fg().orElse(Theme.TEXT)).bg(fillStyle == null ? Theme.THEM : fillStyle.bg().orElse(Theme.THEM));
        spans.add(new Span(" ", edge));
        spans.addAll(content);
        spans.add(new Span(" ".repeat(Math.max(0, inner - Styled.width(content))) + " ", edge));
        return new Row(x, spans);
    }

    private void attachmentLines(List<List<Span>> body, Attachment a, Style base, int max) {
        if (Previews.loading(a)) {
            loadingPreviews = true;
        }
        String size = a.size() >= 1024 * 1024 ? a.size() / 1024 / 1024 + " MB" : Math.max(1, a.size() / 1024) + " KB";
        if (a.kind() == Attachment.Kind.IMAGE) {
            Previews.Image img = Previews.image(a);
            if (img != null) {
                int cols = Math.min(img.width(), Math.min(max, 40));
                int pixRows = Math.max(1, Math.round((float) img.height() * cols / img.width()));
                for (int py = 0; py < pixRows; py += 2) {
                    List<Span> line = new ArrayList<>();
                    for (int cx = 0; cx < cols; cx++) {
                        int sx = Math.min(img.width() - 1, cx * img.width() / cols);
                        int top = img.pixels()[Math.min(img.height() - 1, py * img.height() / pixRows) * img.width() + sx];
                        int bottom = img.pixels()[Math.min(img.height() - 1, (py + 1) * img.height() / pixRows) * img.width() + sx];
                        line.add(new Span("▀", Style.create().fg(Color.rgb(top >> 16 & 255, top >> 8 & 255, top & 255))
                                .bg(Color.rgb(bottom >> 16 & 255, bottom >> 8 & 255, bottom & 255))));
                    }
                    body.add(line);
                }
            }
            body.add(List.of(new Span("▣ " + clean(a.name()) + " · " + size, st(Theme.ON_ME_DIM, Theme.ME))));
        } else {
            Previews.Audio audio = Previews.audio(a);
            String d = audio == null ? "" : Previews.duration(audio.seconds());
            List<Span> l = new ArrayList<>();
            l.add(new Span("▶ ", st(Theme.ON_ME, Theme.ME).bold()));
            l.add(new Span(audio != null && !audio.waveform().isEmpty() ? audio.waveform() : "━━━━━━━━━━━━━━━━━━", st(Theme.BLUE, Theme.ME)));
            l.add(new Span("  " + (d.isEmpty() ? size : d), st(Theme.ON_ME_DIM, Theme.ME)));
            body.add(l);
            body.add(List.of(new Span("♪ " + clean(a.name()), st(Theme.ON_ME_DIM, Theme.ME))));
        }
    }
}
