package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;
import static dev.buildcli.infrastructure.tui.Draw.clean;
import static dev.buildcli.infrastructure.tui.Draw.fill;
import static dev.buildcli.infrastructure.tui.Draw.put;
import static dev.buildcli.infrastructure.tui.Draw.putFit;
import static dev.buildcli.infrastructure.tui.Draw.st;
import dev.buildcli.application.ChatSession;
import dev.buildcli.application.ChatSession.Message;
import dev.buildcli.application.I18n;
import dev.buildcli.domain.Attachment;
import dev.buildcli.infrastructure.tui.Styled.Span;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.tui.event.PasteEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The chat screen. It looks like WhatsApp Web: a list of chats on the left (your groups and a direct chat
 * with each agent, with previews, times, "typing…" and unread counts), and the open chat on the right with bubbles,
 * ticks and date separators. It works like ChatGPT: streamed replies with markdown and code blocks, suggestions, a
 * {@code /} command menu, an {@code @} mention menu, and a viewer for git diffs and files.
 * Drawn by hand on the terminal buffer; keyboard, mouse (click, wheel, scrollbar drag) and paste (a pasted file path
 * becomes an attachment) end up as calls on the {@link ChatSession}.
 */
final class ChatScreen implements Element {

    private record Hit(Rect rect, Runnable action) {}


    private static final int SIDEBAR_WIDTH = 34;
    private static final int SIDEBAR_MIN_TOTAL = 96;
    private static final int MAX_INPUT_ROWS = 8;
    private static final String SPINNER = "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏";

    private final ChatSession session;
    private final Map<String, String> models;
    private final Path cwd;
    /** Whether the folder is a git repository; if not, nothing about "changes" or "tests" is offered. */
    private final boolean gitFolder;
    private final Runnable quit;
    private final InputEditor input = new InputEditor();
    private final PendingDialog pending;
    private final List<Attachment> attachments = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();

    private boolean sidebar = true;
    /** True until the user toggles the chat list: then it follows their choice even on narrow terminals. */
    private boolean autoSidebar = true;
    private int sideWidth;
    private String selected = ChatSession.MAIN;
    /** Text typed in a chat and not sent, by chat. */
    private final Map<String, String> drafts = new java.util.HashMap<>();
    private int scrollOff;
    private int lastTotal;
    private int scrollMax;
    private final ViewerPane viewer;
    private final SlashMenu menu;
    private final MessageMarks marks;
    /** Where the last frame drew the conversation, to tell which message a click landed on. */
    private Rect convRect = Rect.ZERO;
    private int convFirst;
    private int convYOff;
    private List<ConversationRows.Row> convRows = List.of();
    private Rect area = Rect.ZERO;
    private Rect inputTextArea = Rect.ZERO;
    private Rect scrollTrack = Rect.ZERO;
    private boolean draggingScrollbar;
    private int inputFirstRow;
    private int inputWidth = 40;

    private final SettingsServices services;
    private final ChatListView chatList;
    private final ChatCommands commands;
    private final ConversationRows conversation;
    private final AgentFather father;
    private final SettingsView settingsView;
    private boolean settingsOpen;
    private ChatInfoView infoView;
    private boolean infoOpen;
    private final ConnectView connectView;
    private boolean connectOpen;
    /** Writes straight to the terminal (OSC 52 copy); set by the app once the terminal is up. */
    private volatile java.util.function.Consumer<String> rawOutput;
    private java.util.function.Function<String, String> copier = text -> Clipboard.copy(text, rawOutput);
    /** A short message at the bottom of the conversation ("Copied 12 lines"); it fades after a few seconds. */
    private volatile String toast = "";
    private volatile long toastUntil;
    /** Find in this chat: the query, which match is current and whether to scroll to it on the next frame. */
    private boolean searching;
    private final InputEditor findInput = new InputEditor();
    private List<Integer> findRows = List.of();
    private int findIndex = -1;
    private boolean findJump;
    private boolean findDirty;

    ChatScreen(ChatSession session, Map<String, String> models, Path cwd, Runnable quit) {
        this(session, models, cwd, quit, basicServices(session, models));
    }

    ChatScreen(ChatSession session, Map<String, String> models, Path cwd, Runnable quit, SettingsServices services) {
        this.session = session;
        this.gitFolder = java.nio.file.Files.exists(cwd.resolve(".git"));
        this.marks = new MessageMarks(session, (rect, action) -> hits.add(new Hit(rect, action)), this::copyText, () -> selected, this::pendingChatTitle);
        this.menu = new SlashMenu(input, session, () -> selected, this::runCommand, (rect, action) -> hits.add(new Hit(rect, action)));
        this.pending = new PendingDialog(session, input, (rect, action) -> hits.add(new Hit(rect, action)), this::pendingChatTitle);
        this.viewer = new ViewerPane(session, (rect, action) -> hits.add(new Hit(rect, action)), this::reviewChanges);
        // what you sent in earlier runs is still there to walk back through with Up
        for (var m : session.messages()) {
            if (m.kind() == ChatSession.Kind.USER) {
                input.scope(m.thread());
                input.remember(m.text());
            }
        }
        input.scope("");
        this.chatList = new ChatListView(session, new ChatListView.Host() {
            @Override
            public void hit(Rect rect, Runnable action) {
                hits.add(new Hit(rect, action));
            }

            @Override
            public String selected() {
                return selected;
            }

            @Override
            public String draft(String thread) {
                return thread.equals(selected) ? input.text() : drafts.getOrDefault(thread, "");
            }

            @Override
            public void select(String thread) {
                ChatScreen.this.select(thread);
            }

            @Override
            public void openSettings() {
                settingsOpen = true;
            }

            @Override
            public void openNewChat() {
                openInfo(ChatInfoView.Mode.NEW_CHAT);
            }

            @Override
            public void showSidebar() {
                sidebar = true;
            }
        });
        this.commands = new ChatCommands(session, services, cwd, new ChatCommands.Host() {
            @Override
            public String selected() {
                return selected;
            }

            @Override
            public void select(String thread) {
                ChatScreen.this.select(thread);
            }

            @Override
            public void openView(ChatCommands.ViewSpec v) {
                viewer.open(new ViewerPane.View(v.title(), v.lines(), v.diff(), v.numbered()));
            }

            @Override
            public void setInput(String text) {
                input.set(text);
            }

            @Override
            public void attach(Attachment attachment) {
                attachments.add(attachment);
            }

            @Override
            public void openSettings() {
                settingsOpen = true;
            }

            @Override
            public void showModels() {
                settingsOpen = true;
                settingsView.showModels();
            }

            @Override
            public void openConnect() {
                ChatScreen.this.openConnect(null);
            }

            @Override
            public void openInfo(ChatInfoView.Mode mode) {
                ChatScreen.this.openInfo(mode);
            }

            @Override
            public void toggleSidebar() {
                ChatScreen.this.toggleSidebar();
            }

            @Override
            public void openFind(String text) {
                openSearch(text);
            }

            @Override
            public void openChats(String text) {
                chatList.openSearch(text);
            }

            @Override
            public void copyLast(boolean wholeMessage) {
                ChatScreen.this.copyLast(wholeMessage);
            }

            @Override
            public void review(long changesId) {
                reviewChanges(changesId, false);
            }

            @Override
            public void undoLast() {
                ChatScreen.this.undoLast();
            }

            @Override
            public void quit() {
                ChatScreen.this.quit.run();
            }

            @Override
            public void say(String text) {
                ChatScreen.this.say(text);
            }

            @Override
            public String titleOf(String thread) {
                return chatList.title(thread);
            }
        });
        this.conversation = new ConversationRows(session, services, chatList, new ConversationRows.Host() {
            @Override
            public String selected() {
                return selected;
            }

            @Override
            public boolean codeFolder() {
                return gitFolder;
            }

            @Override
            public void review(long changesId, boolean undo) {
                reviewChanges(changesId, undo);
            }

            @Override
            public void copy(String text) {
                copyText(text);
            }

            @Override
            public void addSampleAgents() {
                ChatScreen.this.addSampleAgents();
            }

            @Override
            public void createAgent() {
                settingsOpen = true;
                settingsView.startNewAgent();
            }

            @Override
            public void openFather() {
                select(ChatSession.FATHER);
            }

            @Override
            public void openConnect(String why) {
                ChatScreen.this.openConnect(why);
            }

            @Override
            public void openSettings() {
                settingsOpen = true;
            }

            @Override
            public boolean runCommand(String text) {
                return commands.run(text);
            }

            @Override
            public void setInput(String text) {
                input.set(text);
            }
        });
        this.father = new AgentFather(session, services);
        session.father(father::handle);
        this.models = models;
        this.cwd = cwd;
        this.quit = quit;
        this.services = services;
        this.settingsView = new SettingsView(services, () -> settingsOpen = false, file -> {
            settingsOpen = false;
            runCommand("/open " + file);
        }, () -> openConnect(null), key -> {
            if (dev.buildcli.application.Settings.APPROVAL_MODE.equals(key)) {
                var mode = dev.buildcli.application.ApprovalMode.parse(settings().get(key));
                if (mode != session.approvalMode()) {
                    commands.setMode(mode);
                }
            }
        });
        this.infoView = new ChatInfoView(session, () -> selected, this::select, () -> infoOpen = false, this::modelLabel);
        this.connectView = new ConnectView(services, message -> {
            connectOpen = false;
            if (message != null) {
                session.system(message);
            }
        });
        I18n.use(settings().get(dev.buildcli.application.Settings.LANGUAGE));
        session.profile(() -> dev.buildcli.application.UserProfile.describe(settings()));
        session.approvalMode(dev.buildcli.application.ApprovalMode.parse(settings().get(dev.buildcli.application.Settings.APPROVAL_MODE)));
        ensureSelection();
        if (services.canConnect() && !session.contacts().isEmpty() && noModelAnywhere()) {
            openConnect(t("None of your agents has a model yet. Connect one to start chatting; it takes a minute."));
        }
    }

    /** Nothing to answer with: no default model, no agent with a model of its own, no --model on the command line. */
    private boolean noModelAnywhere() {
        return settings().defaultModel() == null && session.contacts().stream().allMatch(a -> modelLabel(a.name()).startsWith("no model"))
                && models.values().stream().allMatch(v -> v.startsWith("no model"));
    }

    /** The connect screen: choose a provider and a model, test it, make it the default. @param why shown on top, or null */
    private void openConnect(String why) {
        settingsOpen = false;
        infoOpen = false;
        viewer.close();
        connectView.open(why);
        connectOpen = true;
    }

    /** Settings kept in memory, no providers or agent files: for tests and the demo. */
    static SettingsServices basicServices(ChatSession session, Map<String, String> models) {
        dev.buildcli.application.Settings settings = dev.buildcli.application.Settings.defaults();
        return new SettingsServices() {
            @Override
            public dev.buildcli.application.Settings settings() {
                return settings;
            }

            @Override
            public List<Provider> providers() {
                return List.of();
            }

            @Override
            public void addProvider(String name, String url, String keyEnv) {
                throw new IllegalStateException("not available in the demo");
            }

            @Override
            public void removeProvider(String name) {
                throw new IllegalStateException("not available in the demo");
            }

            @Override
            public java.util.concurrent.CompletableFuture<String> test(String model) {
                return java.util.concurrent.CompletableFuture.completedFuture("not available in the demo");
            }

            @Override
            public java.util.concurrent.CompletableFuture<dev.buildcli.infrastructure.ModelCatalog.Result> models(String provider) {
                return java.util.concurrent.CompletableFuture.completedFuture(new dev.buildcli.infrastructure.ModelCatalog.Result(List.of(), null));
            }

            @Override
            public List<AgentInfo> agents() {
                return session.contacts().stream().map(a -> new AgentInfo(a.name(), a.role(), "built in", "", List.copyOf(a.capabilities()))).toList();
            }

            @Override
            public String createAgent(String name, String role, String instructions, List<String> capabilities, boolean global) {
                throw new IllegalStateException("not available in the demo");
            }

            @Override
            public void deleteAgent(String name) {
                throw new IllegalStateException("not available in the demo");
            }

        };
    }

    /** True while something on screen moves by itself: an agent working (spinner, dots) or a preview loading. */
    boolean animating() {
        return session.busy() || conversation.loadingPreviews() || connectOpen && connectView.animating() || System.currentTimeMillis() < toastUntil;
    }


    /** For the app: how to write bytes straight to the terminal. */
    void rawOutput(java.util.function.Consumer<String> writer) {
        this.rawOutput = writer;
    }

    /** For tests: replaces the system clipboard. The function returns how it copied, or null when it could not. */
    void copier(java.util.function.Function<String, String> copier) {
        this.copier = copier;
    }

    /** For the app: how to ask for a frame when something changes that the session does not know about. */
    void redraw(Runnable redraw) {
        this.redraw = redraw;
    }

    private volatile Runnable redraw = () -> { };

    /** A message at the bottom of the chat that goes away by itself. */
    void notice(String text) {
        say(text);
    }

    private void say(String text) {
        toast = text;
        toastUntil = System.currentTimeMillis() + 3_000; // animating() asks for the redraws until it expires
        redraw.run();
    }

    /** Copies on another thread, because a clipboard tool can take a moment, and says how it went. */
    private void copyText(String text) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            String how = copier.apply(text);
            long lines = text.lines().count();
            say(how == null ? t("Could not copy: no clipboard tool found (install xclip, xsel or wl-copy)")
                    : lines == 1 ? t("Copied 1 line") : t("Copied {0} lines", lines));
        });
    }

    private String modelLabel(String agent) {
        String own = services.settings().modelFor(agent);
        if (own != null) {
            return own;
        }
        String def = services.settings().defaultModel();
        return def != null ? def : models.getOrDefault(agent, t("no model: type /connect"));
    }

    private dev.buildcli.application.Settings settings() {
        return services.settings();
    }

    // ---- Element ----

    @Override
    public void render(Frame frame, Rect rect, RenderContext ctx) {
        I18n.use(settings().get(dev.buildcli.application.Settings.LANGUAGE));
        area = rect;
        hits.clear();
        ensureSelection();
        scrollTrack = Rect.ZERO;
        Theme.use(settings().get(dev.buildcli.application.Settings.THEME));
        Buffer buf = frame.buffer();
        fill(buf, rect, Theme.on(Theme.TEXT, Theme.BG));
        if (rect.width() < 44 || rect.height() < 12) {
            put(buf, rect.x() + 1, rect.y() + 1, "Terminal too small (need 44x12)", Theme.on(Theme.AMBER, Theme.BG), rect.right());
            frame.clearCursor();
            return;
        }
        int sideW = sidebar && (rect.width() >= SIDEBAR_MIN_TOTAL || !autoSidebar) ? Math.min(SIDEBAR_WIDTH, rect.width() / 2) : 0;
        sideWidth = sideW;
        List<Message> all = session.messages();
        if (sideW > 0) {
            chatList.draw(buf, new Rect(rect.x(), rect.y(), sideW, rect.height()), all);
            for (int y = rect.y(); y < rect.bottom(); y++) {
                put(buf, rect.x() + sideW, y, "│", st(Theme.LINE, Theme.BG), rect.right());
            }
        }
        Rect pane = new Rect(rect.x() + sideW + (sideW > 0 ? 1 : 0), rect.y(), rect.width() - sideW - (sideW > 0 ? 1 : 0), rect.height());
        if (connectOpen) {
            connectView.render(buf, pane);
            frame.clearCursor();
        } else if (settingsOpen) {
            settingsView.render(buf, pane);
            frame.clearCursor();
        } else if (infoOpen) {
            infoView.render(buf, pane);
            frame.clearCursor();
        } else if (viewer.isOpen()) {
            viewer.draw(buf, pane);
            frame.clearCursor();
        } else {
            drawPane(frame, buf, pane, all);
        }
        if (session.pending() != null) {
            pending.draw(buf, rect);
            frame.clearCursor();
        }
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return Size.UNKNOWN;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public String id() {
        return "chat";
    }

    @Override
    public Rect renderedArea() {
        return area;
    }

    // ---- drawing primitives ----

    private void drawSpans(Buffer buf, int x, int y, List<Span> spans, int limit) {
        Draw.spans(buf, x, y, spans, limit, (rect, action) -> hits.add(new Hit(rect, action)));
    }

    // ---- chat list (left) ----

    /** Keeps a chat open: the first group or agent, once there is one (for example right after the first agent is created). */
    private void ensureSelection() {
        if (chatList.exists(selected)) {
            return;
        }
        String first = session.defaultChat();
        if (first != null) {
            if (session.group(first) == null) {
                session.openDirect(first);
            }
            select(first);
        }
    }

    private void select(String thread) {
        if (marks.forwarding()) {
            marks.forwardTo(thread);
        } else if (!thread.equals(selected)) {
            marks.clear();
        }
        if (!thread.equals(selected)) {
            // what you typed and did not send waits in its chat, like in any messenger
            if (input.text().isEmpty()) {
                drafts.remove(selected);
            } else {
                drafts.put(selected, input.text());
            }
            input.set(drafts.getOrDefault(thread, ""));
        }
        selected = thread;
        // choosing a chat from the list leaves whatever screen was over the conversation (settings, connect, the chat's info)
        settingsOpen = false;
        infoOpen = false;
        connectOpen = false;
        searching = false;
        chatList.closeSearch();
        if (ChatSession.FATHER.equals(thread)) {
            father.greet();
        }
        scrollOff = 0;
        lastTotal = 0;
        menu.reset();
    }

    private void openInfo(ChatInfoView.Mode mode) {
        infoView.open(mode);
        infoOpen = true;
        settingsOpen = false;
    }

    // ---- the open chat (right) ----

    private void drawPane(Frame frame, Buffer buf, Rect pane, List<Message> all) {
        drawPaneHeader(buf, new Rect(pane.x(), pane.y(), pane.width(), 2));
        inputWidth = Math.max(10, pane.width() - 12);
        List<Wrap.Segment> segs = Wrap.layout(input.text(), inputWidth);
        int visibleRows = Math.min(segs.size(), MAX_INPUT_ROWS);
        int barH = visibleRows + 2;
        int barY = pane.bottom() - barH;
        int chipsY = attachments.isEmpty() ? barY : barY - 1;
        int findH = (searching ? 1 : 0) + marks.height();
        Rect content = new Rect(pane.x(), pane.y() + 2 + findH, pane.width(), Math.max(1, chipsY - pane.y() - 2 - findH));
        List<Message> msgs = chatList.inThread(all, selected);
        if (!msgs.isEmpty()) {
            chatList.markSeen(selected, msgs.get(msgs.size() - 1).id());
        }
        drawConversation(buf, content, msgs);
        if (searching) {
            drawFindBar(buf, new Rect(pane.x(), pane.y() + 2, pane.width(), 1));
        }
        if (marks.active()) {
            marks.draw(buf, new Rect(pane.x(), pane.y() + 2 + (searching ? 1 : 0), pane.width(), 1));
        }
        if (!attachments.isEmpty()) {
            drawChips(buf, new Rect(pane.x(), chipsY, pane.width(), 1));
        }
        Rect bar = new Rect(pane.x(), barY, pane.width(), barH);
        drawInputBar(frame, buf, bar, segs, visibleRows);
        if (searching) {
            frame.clearCursor(); // the cursor is in the search box
        }
        if (session.pending() == null) {
            menu.draw(buf, new Rect(pane.x() + 4, barY, Math.min(pane.width() - 8, 90), barH));
        }
    }

    private void drawPaneHeader(Buffer buf, Rect r) {
        Style base = st(Theme.TEXT, Theme.PANEL);
        fill(buf, r, base);
        int x = r.x() + 1;
        if (sideWidth == 0) {
            put(buf, x, r.y(), "‹", st(Theme.DIM, Theme.PANEL), r.right());
            hits.add(new Hit(new Rect(x, r.y(), 2, 2), () -> sidebar = true));
            x += 2;
        }
        var group = session.group(selected);
        boolean isGroup = group != null;
        String name = isGroup ? group.name() : chatList.exists(selected) ? chatList.title(selected) : t("Welcome");
        Color c = isGroup ? Theme.ACCENT : Theme.agentColor(selected);
        put(buf, x, r.y(), " " + (name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT)) + " ", st(Theme.BG, c).bold(), r.right());
        int nx = x + 4;
        put(buf, nx, r.y(), chatList.exists(selected) ? chatList.title(selected) : t("Welcome"), base.bold(), r.right() - 30);
        String sub;
        Style subStyle = st(Theme.DIM, Theme.PANEL);
        ChatSession.Live live = session.live(selected);
        String elsewhere = isGroup ? null : session.agentThread(selected);
        if (session.isActive(selected)) {
            String who = live != null ? live.agent() : chatList.busyAgentIn(selected);
            String state = live != null ? t("typing…") : who.isEmpty() ? t("working…") : I18n.state(session.agentState(who)) + "…";
            sub = isGroup && !who.isEmpty() ? t("{0} is {1}", clean(who), state) : state;
            subStyle = st(Theme.ACCENT, Theme.PANEL);
        } else if (elsewhere != null) {
            sub = t("busy in the {0} chat · will read your messages after", chatList.title(elsewhere));
            subStyle = st(Theme.AMBER, Theme.PANEL);
        } else if (isGroup) {
            StringBuilder sb = new StringBuilder();
            for (String m : group.members()) {
                sb.append(sb.isEmpty() ? "" : ", ").append(clean(m));
            }
            sb.append(sb.isEmpty() ? t("you") : ", " + t("you"));
            sub = sb + "   · " + t("click for group info");
        } else if (chatList.hasDescription(selected)) {
            sub = chatList.describe(selected);
        } else if (session.contact(selected) == null) {
            sub = t("create an agent to start");
        } else {
            sub = t("online") + " · " + clean(chatList.roleOf(selected)) + " · " + clean(modelLabel(selected));
        }
        putFit(buf, nx, r.y() + 1, sub, subStyle, r.right() - 30);
        hits.add(new Hit(new Rect(x, r.y(), Math.max(1, r.right() - 32 - x), 2), () -> openInfo(ChatInfoView.Mode.INFO)));
        int bx = r.right() - 1;
        String[][] buttons = gitFolder ? new String[][] {{" " + t("Help") + " ", "help"}, {" " + t("Tasks") + " ", "tasks"}, {" " + t("Changes") + " ", "diff"}}
                : new String[][] {{" " + t("Help") + " ", "help"}, {" " + t("Tasks") + " ", "tasks"}};
        for (String[] b : buttons) {
            bx -= Wrap.width(b[0]) + 1;
            put(buf, bx, r.y(), b[0], st(Theme.TEXT, Theme.FIELD), r.right());
            String cmd = b[1];
            hits.add(new Hit(new Rect(bx, r.y(), Wrap.width(b[0]), 1), () -> runCommand("/" + cmd)));
        }
        int px = r.right() - 1;
        if (session.isActive(selected)) {
            String stop = " ■ " + t("Stop") + " ";
            px -= Wrap.width(stop);
            put(buf, px, r.y() + 1, stop, st(Theme.TEXT, Theme.DANGER), r.right());
            hits.add(new Hit(new Rect(px, r.y() + 1, Wrap.width(stop), 1), () -> session.stop(selected)));
            px -= 1;
        }
        var mode = session.approvalMode();
        String pill = " " + t(mode.label()) + " ⇄ ";
        px -= Wrap.width(pill);
        Style pillStyle = switch (mode) {
            case MANUAL -> st(Theme.DIM, Theme.FIELD);
            case EDITS -> st(Theme.ON_ACCENT, Theme.ACCENT).bold();
            case AUTO -> st(Theme.TEXT, Theme.DANGER).bold();
        };
        put(buf, px, r.y() + 1, pill, pillStyle, r.right());
        hits.add(new Hit(new Rect(px, r.y() + 1, Wrap.width(pill), 1), () -> commands.setMode(session.approvalMode().next())));
    }

    /** The agent currently working in {@code thread}, preferring one that is not just waiting for a teammate. */
    private record RowsKey(long version, int width, String chat, String theme, boolean activity, boolean compact, long frame) {}

    private RowsKey rowsKey;
    private List<ConversationRows.Row> rowsCache = List.of();

    private void drawConversation(Buffer buf, Rect r, List<Message> msgs) {
        int width = r.width() - 1;
        // rebuilding every bubble is the costly part of a frame: reuse the rows until the chat changes or something moves
        boolean moving = session.isActive(selected) || conversation.loadingPreviews();
        var key = new RowsKey(session.version(), width, selected, Theme.current(), settings().flag(dev.buildcli.application.Settings.SHOW_ACTIVITY),
                settings().flag(dev.buildcli.application.Settings.COMPACT), moving ? System.currentTimeMillis() / 100 : 0);
        if (!key.equals(rowsKey)) {
            conversation.previewsRendered();
            rowsCache = conversation.build(width, msgs);
            rowsKey = key;
        }
        List<ConversationRows.Row> rows = rowsCache;
        int total = rows.size();
        int viewH = r.height();
        scrollMax = Math.max(0, total - viewH);
        if (scrollOff > 0 && total > lastTotal) {
            scrollOff += total - lastTotal; // keep reading where the user stopped
        }
        lastTotal = total;
        scrollOff = Math.max(0, Math.min(scrollOff, scrollMax));
        String query = searching ? findInput.text().strip().toLowerCase(Locale.ROOT) : "";
        if (searching) {
            findRows = matchingRows(rows, query);
            if (findDirty) {
                findIndex = findRows.size() - 1; // start at the newest match
                findJump = true;
                findDirty = false;
            }
            findIndex = findRows.isEmpty() ? -1 : Math.max(0, Math.min(findIndex, findRows.size() - 1));
            if (findJump && findIndex >= 0) {
                int firstWanted = Math.max(0, Math.min(findRows.get(findIndex) - viewH / 2, Math.max(0, total - viewH)));
                scrollOff = Math.max(0, Math.min(scrollMax, total - viewH - firstWanted));
                findJump = false;
            }
        }
        int first = Math.max(0, total - viewH - scrollOff);
        boolean empty = msgs.isEmpty() && !session.isActive(selected);
        int yOff = total < viewH ? (empty ? Math.max(0, (viewH - total) / 3) : viewH - total) : 0;
        convRect = r;
        convFirst = first;
        convYOff = yOff;
        convRows = rows;
        for (int i = 0; i < viewH && first + i < total; i++) {
            ConversationRows.Row row = rows.get(first + i);
            if (row.message() >= 0) {
                Rect line = new Rect(r.x(), r.y() + yOff + i, width, 1);
                if (marks.isMarked(row.message())) {
                    fill(buf, line, st(Theme.TEXT, Theme.SELECTED));
                    put(buf, r.x(), line.y(), "✓", st(Theme.ACCENT, Theme.SELECTED).bold(), r.x() + 1);
                }
                long id = row.message();
                hits.add(new Hit(line, () -> {
                    if (marks.active()) {
                        marks.toggle(id);
                    }
                }));
            }
            drawSpans(buf, r.x() + row.x(), r.y() + yOff + i, row.spans(), r.x() + width);
            if (!query.isEmpty()) {
                boolean current = findIndex >= 0 && findRows.get(findIndex) == first + i;
                highlight(buf, r.x() + row.x(), r.y() + yOff + i, row.spans(), query, current, r.x() + width);
            }
        }
        if (System.currentTimeMillis() < toastUntil && !toast.isEmpty()) {
            String t = " " + toast + " ";
            int tw = Wrap.width(t);
            put(buf, r.x() + Math.max(0, (width - tw) / 2), r.bottom() - 1, t, st(Theme.TEXT, Theme.PANEL).bold(), r.x() + width);
        }
        if (scrollMax > 0) {
            int trackX = r.right() - 1;
            scrollTrack = new Rect(trackX, r.y(), 1, viewH);
            int thumb = Math.max(1, viewH * viewH / total);
            int pos = (int) Math.round((double) (scrollMax - scrollOff) / scrollMax * (viewH - thumb));
            for (int i = pos; i < pos + thumb && i < viewH; i++) {
                put(buf, trackX, r.y() + i, "▐", st(Theme.FAINT, Theme.BG), trackX + 1);
            }
        }
        if (scrollOff > 0) {
            String pill = " ⌄ ";
            int px = r.right() - 6;
            int py = r.bottom() - 2;
            put(buf, px, py, pill, st(Theme.DIM, Theme.PANEL).bold(), r.right());
            hits.add(new Hit(new Rect(px, py, 3, 1), () -> scrollOff = 0));
        }
    }

    // ---- find in this chat ----

    private static String plainText(List<Span> spans) {
        StringBuilder sb = new StringBuilder();
        for (Span sp : spans) {
            sb.append(sp.text());
        }
        return sb.toString();
    }

    /** The rows whose text contains the (lower-cased) query, top to bottom. A phrase split over two rows is not found. */
    private static List<Integer> matchingRows(List<ConversationRows.Row> rows, String query) {
        if (query.isEmpty()) {
            return List.of();
        }
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (plainText(rows.get(i).spans()).toLowerCase(Locale.ROOT).contains(query)) {
                out.add(i);
            }
        }
        return out;
    }

    /** Draws the matches of {@code query} in one row again, over the text, in the highlight colours. */
    private void highlight(Buffer buf, int x, int y, List<Span> spans, String query, boolean current, int limit) {
        String plain = plainText(spans);
        String lower = plain.toLowerCase(Locale.ROOT);
        if (lower.length() != plain.length()) {
            return; // a letter that changes length when lower-cased would put the highlight in the wrong column
        }
        Style style = current ? st(Theme.BG, Theme.ACCENT).bold() : st(Theme.BG, Theme.AMBER);
        for (int at = lower.indexOf(query); at >= 0; at = lower.indexOf(query, at + query.length())) {
            put(buf, x + Wrap.width(plain.substring(0, at)), y, plain.substring(at, at + query.length()), style, limit);
        }
    }

    private void openSearch(String text) {
        searching = true;
        findInput.set(text);
        findIndex = -1;
        findDirty = true;
    }

    private void drawFindBar(Buffer buf, Rect r) {
        Style bar = st(Theme.TEXT, Theme.PANEL);
        fill(buf, r, bar);
        int x = r.x() + 2;
        x += put(buf, x, r.y(), t("Find in this chat:") + " ", st(Theme.DIM, Theme.PANEL), r.right());
        String text = findInput.text();
        x += put(buf, x, r.y(), text.isEmpty() ? t("type to search") + "▏" : text + "▏", st(text.isEmpty() ? Theme.DIM : Theme.TEXT, Theme.FIELD), r.right() - 40);
        String count = text.isBlank() ? "" : findRows.isEmpty() ? t("no matches") : t("{0} of {1}", findIndex + 1, findRows.size());
        String right = count + "   " + t("↑ older  ↓ newer  Esc close ✕") + " ";
        int rx = Math.max(x + 2, r.right() - Wrap.width(right));
        put(buf, rx, r.y(), right, st(findRows.isEmpty() && !text.isBlank() ? Theme.RED : Theme.DIM, Theme.PANEL), r.right());
        hits.add(new Hit(new Rect(r.right() - 3, r.y(), 3, 1), () -> searching = false));
    }

    private void findStep(int direction) {
        if (findRows.isEmpty()) {
            return;
        }
        findIndex = (Math.max(0, findIndex) + direction + findRows.size()) % findRows.size();
        findJump = true;
    }

    private EventResult searchKey(KeyEvent key) {
        KeyCode code = key.code();
        switch (code) {
            case ESCAPE -> searching = false;
            case ENTER -> findStep(key.hasShift() ? -1 : 1);
            case UP -> findStep(-1);
            case DOWN -> findStep(1);
            case PAGE_UP -> scroll(10);
            case PAGE_DOWN -> scroll(-10);
            case BACKSPACE -> {
                findInput.backspace();
                findDirty = true;
            }
            case LEFT -> findInput.left();
            case RIGHT -> findInput.right();
            case CHAR -> {
                if (key.hasCtrl() && Character.toLowerCase(key.character()) == 'u') {
                    findInput.clear();
                    findDirty = true;
                } else if (!key.hasCtrl() && !key.hasAlt() && key.character() >= ' ') {
                    findInput.insert(key.string());
                    findDirty = true;
                }
            }
            default -> { }
        }
        return EventResult.HANDLED;
    }

    /** /copy: the last code block of the last answer, or with "message" (or when it has no code) the whole answer. */
    private void copyLast(boolean wholeMessage) {
        List<Message> msgs = chatList.inThread(session.messages(), selected);
        for (int i = msgs.size() - 1; i >= 0; i--) {
            Message m = msgs.get(i);
            if (m.kind() != ChatSession.Kind.AGENT) {
                continue;
            }
            List<String> blocks = Styled.codeBlocks(m.text());
            copyText(wholeMessage || blocks.isEmpty() ? m.text() : blocks.get(blocks.size() - 1));
            return;
        }
        say(t("Nothing to copy yet"));
    }

    // ---- conversation rows ----

    /** Opens the files of a changes card as a diff. With {@code undo} it asks to confirm putting them back. */
    private void reviewChanges(long id, boolean undo) {
        var files = session.changes(id);
        if (files.isEmpty()) {
            session.system(t("These changes were not kept, so they cannot be shown."));
            return;
        }
        List<String> lines = new ArrayList<>();
        for (var n : dev.buildcli.application.tools.FileChanges.net(files)) {
            lines.add("diff --git a/" + n.path() + " b/" + n.path());
            lines.addAll(dev.buildcli.application.tools.FileChanges.diff(List.of(new dev.buildcli.domain.FileChange(
                    n.agents().get(0), n.path(), n.existed(), n.before(), n.after()))).lines().toList());
        }
        String who = files.get(0).agent();
        viewer.open(new ViewerPane.View(undo ? t("Undo {0}'s changes?", who) : t("{0}'s changes", who), lines, true, false, id, undo));
    }

    private void undoLast() {
        long id = session.lastChanges(selected);
        if (id < 0) {
            session.system(t("No changes to undo in this chat."));
        } else if (!session.canUndo()) {
            session.system(t("Undo is not available here."));
        } else {
            reviewChanges(id, true);
        }
    }

    /** The "add the sample agents" button of an empty chat: writes the agents, adds them live and opens their group. */
    private void addSampleAgents() {
        try {
            List<String> added = services.createSampleAgents();
            if (session.group("#maintainers") != null) {
                select("#maintainers");
            }
            say(t("Added {0}. Say something in the group to start.", String.join(", ", added)));
        } catch (Exception e) {
            say(t("Could not add them: {0}", e.getMessage()));
        }
    }

    // ---- chips and the input bar ----

    private void drawChips(Buffer buf, Rect r) {
        fill(buf, r, st(Theme.TEXT, Theme.PANEL));
        int x = r.x() + 2;
        for (int i = 0; i < attachments.size(); i++) {
            Attachment a = attachments.get(i);
            String label = (a.kind() == Attachment.Kind.IMAGE ? " ▣ " : " ♪ ") + clean(a.name()) + " ";
            int w = put(buf, x, r.y(), label, st(Theme.TEXT, Theme.FIELD), r.right());
            int idx = i;
            int cw = put(buf, x + w, r.y(), "✕ ", st(Theme.DIM, Theme.FIELD), r.right());
            hits.add(new Hit(new Rect(x + w, r.y(), cw, 1), () -> attachments.remove(idx)));
            x += w + cw + 1;
        }
    }

    private void drawInputBar(Frame frame, Buffer buf, Rect r, List<Wrap.Segment> segs, int visibleRows) {
        fill(buf, r, st(Theme.TEXT, Theme.PANEL));
        Style field = st(Theme.TEXT, Theme.FIELD);
        int fx = r.x() + 5;
        int fw = inputWidth + 2;
        fill(buf, new Rect(fx, r.y() + 1, fw, visibleRows), field);
        put(buf, r.x() + 2, r.y() + visibleRows, "+", st(Theme.DIM, Theme.PANEL).bold(), r.right());
        hits.add(new Hit(new Rect(r.x() + 1, r.y() + 1, 3, visibleRows), () -> input.set("/attach ")));
        String src = input.text();
        int[] cur = input.cursorPosition(inputWidth);
        inputFirstRow = Math.max(0, Math.min(cur[0] - visibleRows + 1, segs.size() - visibleRows));
        int tx = fx + 1;
        for (int i = 0; i < visibleRows; i++) {
            int y = r.y() + 1 + i;
            Wrap.Segment s = segs.get(inputFirstRow + i);
            if (src.isEmpty()) {
                String hint = ChatSession.FATHER.equals(selected) ? t("Ask AgentFather: /newagent, /agents, /editagent, /help")
                        : ChatSession.isAgentChat(selected) ? t("Read only: ask one of them, in their own chat, to write to the other")
                        : selected.equals(ChatSession.NOTES) ? t("A note to yourself: no agent reads it")
                        : session.busy() ? t("Type a message (it waits its turn)") : t("Type a message");
                put(buf, tx, y, hint, st(Theme.DIM, Theme.FIELD), tx + inputWidth);
            } else {
                String line = src.substring(s.start(), s.end()).stripTrailing();
                Style ls = line.startsWith("/") && i == 0 && inputFirstRow == 0 ? st(Theme.BLUE, Theme.FIELD) : field;
                put(buf, tx, y, line, ls, tx + inputWidth);
            }
        }
        boolean canSend = !src.isBlank() || !attachments.isEmpty();
        int sx = fx + fw + 1;
        put(buf, sx, r.y() + visibleRows, " ➤ ", canSend ? st(Theme.BG, Theme.ACCENT).bold() : st(Theme.DIM, Theme.PANEL), r.right());
        hits.add(new Hit(new Rect(sx, r.y() + visibleRows, 3, 1), this::submit));
        inputTextArea = new Rect(tx, r.y() + 1, inputWidth, visibleRows);
        if (session.pending() == null || session.pending() instanceof ChatSession.Pending.Question && pending.typingOwnAnswer()) {
            frame.setCursorPosition(tx + Math.min(cur[1], inputWidth), r.y() + 1 + (cur[0] - inputFirstRow));
        }
    }

    private String pendingChatTitle(String thread) {
        return thread.equals(ChatSession.EVERYWHERE) ? chatList.title(ChatSession.MAIN) : chatList.title(thread);
    }

    // ---- keyboard ----

    @Override
    public EventResult handleKeyEvent(KeyEvent key, boolean focused) {
        input.scope(selected);
        boolean ctrl = key.hasCtrl();
        boolean alt = key.hasAlt();
        KeyCode code = key.code();
        char ch = code == KeyCode.CHAR ? Character.toLowerCase(key.character()) : 0;

        if (connectOpen) {
            if (ctrl && ch == 'c') {
                connectOpen = false;
            } else {
                connectView.key(key);
            }
            return EventResult.HANDLED;
        }
        if (settingsOpen) {
            if (ctrl && ch == 'c') {
                settingsOpen = false;
            } else {
                settingsView.key(key);
            }
            return EventResult.HANDLED;
        }
        if (infoOpen) {
            if (ctrl && ch == 'c') {
                infoOpen = false;
            } else {
                infoView.key(key);
            }
            return EventResult.HANDLED;
        }
        if (code == KeyCode.F2) {
            settingsOpen = true;
            return EventResult.HANDLED;
        }
        if (ctrl && ch == 'c') {
            if (chatList.isSearching()) {
                chatList.closeSearch();
            } else if (searching) {
                searching = false;
            } else if (viewer.isOpen()) {
                viewer.close();
            } else if (!input.isEmpty()) {
                input.clear();
            } else {
                quit.run();
            }
            return EventResult.HANDLED;
        }
        if (ctrl && ch == 'x') {
            session.stop(selected);
            return EventResult.HANDLED;
        }
        if (ctrl && ch == 'k' && !viewer.isOpen()) {
            chatList.openSearch("");
            return EventResult.HANDLED;
        }
        if (chatList.isSearching() && !viewer.isOpen() && session.pending() == null) {
            return chatList.searchKey(key);
        }
        if (ctrl && ch == 'f' && !viewer.isOpen()) {
            openSearch(searching ? findInput.text() : "");
            return EventResult.HANDLED;
        }
        if (searching && !viewer.isOpen() && session.pending() == null) {
            return searchKey(key);
        }
        EventResult pendingKey = pending.key(key);
        if (pendingKey != null) {
            return pendingKey;
        }
        EventResult markKey = marks.key(key);
        if (markKey != null) {
            return markKey;
        }
        if (alt && ch == 'm' && !viewer.isOpen()) {
            marks.start();
            return EventResult.HANDLED;
        }
        if (ctrl && code == KeyCode.CHAR) {
            switch (ch) {
                case 'g' -> {
                    runCommand("/diff");
                    return EventResult.HANDLED;
                }
                case 'o' -> {
                    viewer.close();
                    input.set("/open ");
                    return EventResult.HANDLED;
                }
                case 't' -> {
                    runCommand("/tasks");
                    return EventResult.HANDLED;
                }
                case 'b' -> {
                    toggleSidebar();
                    return EventResult.HANDLED;
                }
                case 'l' -> {
                    scrollOff = 0; // redraw at the latest message; deleting a chat is /clear, which asks first
                    return EventResult.HANDLED;
                }
                default -> { }
            }
        }
        if (viewer.isOpen()) {
            return viewer.key(key);
        }
        if (switchChat(key)) {
            return EventResult.HANDLED;
        }
        if (menu.key(key)) {
            return EventResult.HANDLED;
        }
        switch (code) {
            case ENTER -> {
                boolean modified = alt || ctrl || key.hasShift();
                boolean enterSends = settings().flag(dev.buildcli.application.Settings.ENTER_SENDS);
                if (endsWithBackslash() && enterSends && !modified) {
                    input.backspace();
                    input.insert("\n");
                } else if (enterSends != modified) {
                    submit();
                } else {
                    input.insert("\n");
                }
            }
            case BACKSPACE -> {
                if (alt || ctrl) {
                    input.deleteWordBefore();
                } else {
                    input.backspace();
                }
            }
            case DELETE -> input.delete();
            case LEFT -> {
                if (ctrl || alt) {
                    input.wordLeft();
                } else {
                    input.left();
                }
            }
            case RIGHT -> {
                if (ctrl || alt) {
                    input.wordRight();
                } else {
                    input.right();
                }
            }
            case HOME -> input.home();
            case END -> input.end();
            case UP -> {
                if (!input.up(inputWidth) && !input.historyPrevious()) {
                    scroll(1);
                }
            }
            case DOWN -> {
                if (!input.down(inputWidth) && !input.historyNext()) {
                    scroll(-1);
                }
            }
            case PAGE_UP -> scroll(Math.max(3, area.height() / 2));
            case PAGE_DOWN -> scroll(-Math.max(3, area.height() / 2));
            case ESCAPE -> scrollOff = 0;
            case TAB -> {
                if (key.hasShift()) {
                    commands.setMode(session.approvalMode().next());
                }
            }
            case CHAR -> {
                if (ctrl) {
                    switch (ch) {
                        case 'a' -> input.home();
                        case 'e' -> input.end();
                        case 'u' -> input.clear();
                        case 'w' -> input.deleteWordBefore();
                        case 'j' -> input.insert("\n");
                        case 'd' -> {
                            if (input.isEmpty()) {
                                quit.run();
                            } else {
                                input.delete();
                            }
                        }
                        default -> {
                            return EventResult.UNHANDLED;
                        }
                    }
                } else if (key.character() == '\n') {
                    input.insert("\n");
                } else if (!alt && key.character() >= ' ') {
                    input.insert(key.string());
                }
            }
            default -> {
                return EventResult.UNHANDLED;
            }
        }
        if (code == KeyCode.CHAR || code == KeyCode.BACKSPACE) {
            menu.reset();
        }
        return EventResult.HANDLED;
    }

    private void toggleSidebar() {
        sidebar = sideWidth == 0;
        autoSidebar = false;
    }

    /** Alt+Up/Down (or Ctrl+PgUp/PgDn) move through the chat list, Alt+1..9 jump to a chat. */
    private boolean switchChat(KeyEvent key) {
        List<String> threads = chatList.threads();
        int i = threads.indexOf(selected);
        boolean alt = key.hasAlt();
        if ((alt && key.code() == KeyCode.UP) || (key.hasCtrl() && key.code() == KeyCode.PAGE_UP)) {
            select(threads.get((i - 1 + threads.size()) % threads.size()));
            return true;
        }
        if ((alt && key.code() == KeyCode.DOWN) || (key.hasCtrl() && key.code() == KeyCode.PAGE_DOWN)) {
            select(threads.get((i + 1) % threads.size()));
            return true;
        }
        if (alt && key.code() == KeyCode.CHAR && key.character() >= '1' && key.character() <= '9') {
            int n = key.character() - '1';
            if (n < threads.size()) {
                select(threads.get(n));
            }
            return true;
        }
        return false;
    }

    private boolean endsWithBackslash() {
        String t = input.text();
        return input.cursor() == t.length() && t.endsWith("\\") && !t.endsWith("\\\\");
    }

    private void scroll(int lines) {
        scrollOff = Math.max(0, Math.min(scrollMax, scrollOff + lines));
    }

    // ---- sending and commands ----

    /** @return true if the text was a slash command (and was handled); "/home/me/file" is not one */
    boolean runCommand(String text) {
        return commands.run(text);
    }

    void submit() {
        String text = input.text().strip();
        commands.typed(text);
        if (text.isEmpty() && attachments.isEmpty()) {
            return;
        }
        if (attachments.isEmpty() && runCommand(text)) {
            input.remember(text);
            input.clear();
            return;
        }
        session.submit(text, new ArrayList<>(attachments), selected);
        input.remember(text);
        input.clear();
        attachments.clear();
        scrollOff = 0;
    }

    // ---- paste and mouse ----

    @Override
    public EventResult handlePasteEvent(PasteEvent paste) {
        if (connectOpen) {
            connectView.paste(paste.text());
            return EventResult.HANDLED;
        }
        if (infoOpen) {
            infoView.paste(paste.text());
            return EventResult.HANDLED;
        }
        if (settingsOpen) {
            settingsView.paste(paste.text());
            return EventResult.HANDLED;
        }
        List<Attachment> files = filesIn(paste.text());
        if (!files.isEmpty()) {
            attachments.addAll(files);
        } else {
            input.insert(paste.text());
        }
        return EventResult.HANDLED;
    }

    /** Terminals paste the path of a dropped file; if every pasted line is an image or audio file, attach them. */
    private List<Attachment> filesIn(String pasted) {
        List<Attachment> out = new ArrayList<>();
        for (String line : pasted.strip().split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            try {
                Path p = ChatCommands.resolve(cwd, line);
                if (!Files.isRegularFile(p)) {
                    return List.of();
                }
                out.add(Attachment.of(p));
            } catch (IllegalArgumentException e) {
                return List.of();
            }
        }
        return out;
    }

    @Override
    public EventResult handleMouseEvent(MouseEvent m) {
        MouseEventKind kind = m.kind();
        boolean inPane = !(sideWidth > 0 && m.x() < area.x() + sideWidth);
        if (connectOpen && inPane) {
            connectView.mouse(m);
            return EventResult.HANDLED;
        }
        if (settingsOpen && inPane) {
            settingsView.mouse(m);
            return EventResult.HANDLED;
        }
        if (infoOpen && inPane) {
            infoView.mouse(m);
            return EventResult.HANDLED;
        }
        if (kind == MouseEventKind.SCROLL_UP || kind == MouseEventKind.SCROLL_DOWN) {
            int d = kind == MouseEventKind.SCROLL_UP ? 3 : -3;
            if (!inPane) {
                chatList.scrollBy(-d / 3);
            } else if (viewer.isOpen()) {
                viewer.wheel(d);
            } else {
                scroll(d);
            }
            return EventResult.HANDLED;
        }
        if (kind == MouseEventKind.RELEASE) {
            draggingScrollbar = false;
            return EventResult.HANDLED;
        }
        boolean onTrack = !viewer.isOpen() && scrollTrack.width() > 0 && scrollTrack.contains(m.x(), m.y());
        if ((kind == MouseEventKind.PRESS && m.isLeftButton() && onTrack) || (kind == MouseEventKind.DRAG && draggingScrollbar)) {
            draggingScrollbar = true;
            int rel = Math.max(0, Math.min(scrollTrack.height() - 1, m.y() - scrollTrack.y()));
            scrollOff = scrollMax - (int) Math.round((double) rel / Math.max(1, scrollTrack.height() - 1) * scrollMax);
            scrollOff = Math.max(0, Math.min(scrollMax, scrollOff));
            return EventResult.HANDLED;
        }
        if (kind == MouseEventKind.PRESS && m.isRightButton() && !viewer.isOpen() && session.pending() == null) {
            // a right click marks a message, like a long press in a messenger
            int idx = m.y() - convRect.y() - convYOff + convFirst;
            if (convRect.contains(m.x(), m.y()) && idx >= 0 && idx < convRows.size() && convRows.get(idx).message() >= 0) {
                marks.toggle(convRows.get(idx).message());
            }
            return EventResult.HANDLED;
        }
        if (kind == MouseEventKind.PRESS && m.isLeftButton()) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                if (hits.get(i).rect().contains(m.x(), m.y())) {
                    hits.get(i).action().run();
                    return EventResult.HANDLED;
                }
            }
            if (!viewer.isOpen() && session.pending() == null && inputTextArea.contains(m.x(), m.y())) {
                input.moveTo(inputWidth, inputFirstRow + m.y() - inputTextArea.y(), m.x() - inputTextArea.x());
            }
        }
        return EventResult.HANDLED;
    }

    // ---- for tests ----

    InputEditor inputForTest() {
        return input;
    }

    List<Attachment> attachmentsForTest() {
        return attachments;
    }

    String selectedForTest() {
        return selected;
    }

    List<String> threadsForTest() {
        return chatList.threads();
    }

    ChatSession sessionForTest() {
        return session;
    }

    boolean connectOpenForTest() {
        return connectOpen;
    }

    boolean listSearchOpenForTest() {
        return chatList.isSearching();
    }

    void openNewAgentForTest() {
        settingsOpen = true;
        settingsView.startNewAgent();
    }

    boolean settingsOpenForTest() {
        return settingsOpen;
    }

    boolean viewerOpenForTest() {
        return viewer.isOpen();
    }
}
