package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;
import dev.buildcli.application.I18n;
import dev.buildcli.domain.Attachment;
import dev.buildcli.domain.Task;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The slash commands of the chat: the table the menu and the help show, and what each one does. It reaches the screen only
 * through {@link Host}, so it can be tried without a terminal.
 */
final class ChatCommands {

    /** What a command may ask of the screen it runs in. */
    /** /me: what the agents are told about you. Kept in your own settings, for all projects. */
    private void aboutMe(String arg) {
        var settings = services.settings();
        String[] parts = arg.strip().split("\\s+", 2);
        String word = parts[0].toLowerCase(Locale.ROOT);
        String text = parts.length > 1 ? parts[1].strip() : "";
        String key = switch (word) {
            case "name" -> dev.buildcli.application.Settings.PROFILE_NAME;
            case "about" -> dev.buildcli.application.Settings.PROFILE_ABOUT;
            case "style" -> dev.buildcli.application.Settings.PROFILE_STYLE;
            default -> null;
        };
        if (key == null) {
            String name = settings.get(dev.buildcli.application.Settings.PROFILE_NAME);
            String about = settings.get(dev.buildcli.application.Settings.PROFILE_ABOUT);
            String style = settings.get(dev.buildcli.application.Settings.PROFILE_STYLE);
            session.system(t("What your agents know about you (on this computer only):") + "\n"
                    + t("name: {0}", name.isBlank() ? t("(not set)") : name) + "\n" + t("about you: {0}", about.isBlank() ? t("(not set)") : about)
                    + "\n" + t("how to deal with you: {0}", style.isBlank() ? t("(not set)") : style)
                    + "\n" + t("/me name <your name> · /me about <text> · /me style <text>; leave the text empty to clear one."));
            return;
        }
        dev.buildcli.application.UserProfile.set(settings, key, text);
        session.system(text.isEmpty() ? t("Cleared.") : t("Saved. Your agents will use it from their next message."));
    }

    /** /context: what the agents of the open group are told about the work, besides what you say in the chat. */
    private void groupContext(String arg) {
        String id = host.selected();
        var g = session.group(id);
        if (g == null) {
            session.error(t("Open a group first: context belongs to a group."));
            return;
        }
        String[] parts = arg.strip().split("\\s+", 2);
        String word = parts[0].toLowerCase(Locale.ROOT);
        String rest = parts.length > 1 ? parts[1].strip() : "";
        java.util.List<String> files = new java.util.ArrayList<>(g.files());
        if (arg.isBlank()) {
            if (!g.hasContext()) {
                session.system(t("No context for {0} yet. /context <text> sets it; /context attach <file> adds a file. The agents of the group read it before they answer.", g.name()));
            } else {
                session.system(t("Context of {0}: {1}", g.name(), g.context().isEmpty() ? t("(no text)") : g.context())
                        + (files.isEmpty() ? "" : "\n" + t("Files: {0}", String.join(", ", files.stream().map(f -> Path.of(f).getFileName().toString()).toList()))));
            }
            return;
        }
        switch (word) {
            case "clear" -> {
                session.setGroupContext(id, "", java.util.List.of());
                session.system(t("Context of {0} cleared.", g.name()));
            }
            case "attach" -> {
                try {
                    Path file = resolve(cwd, rest);
                    if (!Files.isRegularFile(file)) {
                        session.error(t("There is no file {0}.", rest));
                        return;
                    }
                    String abs = file.toAbsolutePath().normalize().toString();
                    if (!files.contains(abs)) {
                        files.add(abs);
                    }
                    session.setGroupContext(id, g.context(), files);
                    session.system(t("{0} added to the context of {1}. Only text files are read, up to {2} KB of text and 20 KB per file.", file.getFileName(), g.name(), dev.buildcli.domain.Chat.MAX_CONTEXT / 1000));
                } catch (IllegalArgumentException e) {
                    session.error(e.getMessage());
                }
            }
            case "detach" -> {
                boolean removed = files.removeIf(f -> Path.of(f).getFileName().toString().equalsIgnoreCase(rest));
                if (removed) {
                    session.setGroupContext(id, g.context(), files);
                    session.system(t("{0} removed from the context of {1}.", rest, g.name()));
                } else {
                    session.error(t("No attached file called {0}. /context lists them.", rest));
                }
            }
            default -> {
                session.setGroupContext(id, arg.strip(), files);
                session.system(t("Context of {0} set. Its agents read it before they answer.", g.name()));
            }
        }
    }

    /** Changes how much the agents may do before asking, and says so in the chat. */
    void setMode(dev.buildcli.application.ApprovalMode mode) {
        session.approvalMode(mode);
        session.system(t("Mode: {0}, {1}.", t(mode.label()), t(mode.description())));
    }

    interface Host {
        String selected();

        void select(String thread);

        /** Shows a full-screen view (a diff, a file, the tasks, the help). */
        void openView(ViewSpec view);

        void setInput(String text);

        void attach(Attachment attachment);

        void openSettings();

        /** Opens the Models section of the settings. */
        void showModels();

        void openConnect();

        void openInfo(ChatInfoView.Mode mode);

        void toggleSidebar();

        void openFind(String text);

        void openChats(String text);

        void copyLast(boolean wholeMessage);

        void review(long changesId);

        void undoLast();

        void quit();

        void say(String text);

        String titleOf(String thread);
    }

    /** What the full-screen viewer is asked to show. */
    record ViewSpec(String title, List<String> lines, boolean diff, boolean numbered) {}

    private final dev.buildcli.application.ChatSession session;
    private final SettingsServices services;
    private final Path cwd;
    private final Host host;
    /** The chat a first /clear was typed in; a second /clear there deletes it. */
    private String clearArmedFor;

    ChatCommands(dev.buildcli.application.ChatSession session, SettingsServices services, Path cwd, Host host) {
        this.session = session;
        this.services = services;
        this.cwd = cwd;
        this.host = host;
    }

    /** Anything typed that is not the command being confirmed cancels a pending /clear. */
    void typed(String text) {
        if (!text.equals("/clear")) {
            clearArmedFor = null;
        }
    }

    /** A slash command: what the menu shows and what it does. {@code arg} is a hint when it takes an argument. */
    record Command(String name, String arg, String description, String shortcut) {}

    static final List<Command> LIST = List.of(
            new Command("diff", "[--staged] [path]", "Show uncommitted changes (git diff)", "Ctrl+G"),
            new Command("status", "", "Show git status", ""),
            new Command("log", "", "Show recent commits", ""),
            new Command("open", "<file>", "Open a file in the viewer", "Ctrl+O"),
            new Command("attach", "<file>", "Attach an image or audio file (or paste its path)", ""),
            new Command("tasks", "", "Show what the agents are doing: tasks and handoffs", "Ctrl+T"),
            new Command("stop", "", "Stop the work in this chat", "Ctrl+X"),
            new Command("retry", "", "Send the last failed message again", ""),
            new Command("copy", "[message]", "Copy the last code block (or the whole last answer)", ""),
            new Command("find", "[text]", "Search this chat", "Ctrl+F"),
            new Command("chats", "[text]", "Search your chats and agents by name, role or what was said", "Ctrl+K"),
            new Command("review", "", "See the files agents changed in this chat", ""),
            new Command("undo", "", "Put back the files an agent changed last (shows them first)", ""),
            new Command("context", "[text | attach <file> | detach <file> | clear]", "Background for this group's agents: a text and files (optional)", ""),
            new Command("me", "[name|about|style <text>]", "Tell the agents who you are and how to deal with you (stays on this computer)", ""),
            new Command("mode", "[manual|edits|auto]", "How much agents may do before asking you (Shift+Tab cycles it)", "Shift+Tab"),
            new Command("revoke", "", "Stop approving automatically in this chat (shows what was allowed)", ""),
            new Command("queue", "[clear]", "Show or drop messages waiting their turn", ""),
            new Command("settings", "", "Providers, models, agents, theme and more", "F2"),
            new Command("connect", "", "Connect a provider and choose the default model", ""),
            new Command("model", "[@agent] [provider:model]", "Show the models, or change the default (or one agent's) model", ""),
            new Command("reach", "[@agent @other on|off]", "Show or set which agents may contact each other", ""),
            new Command("dm", "@agent", "Open a direct chat with an agent", ""),
            new Command("newgroup", "<name> [@agents]", "Create a group with some agents", ""),
            new Command("add", "@agent", "Add an agent to this group", ""),
            new Command("remove", "@agent", "Remove an agent from this group", ""),
            new Command("admin", "@agent", "Make an agent admin of this group", ""),
            new Command("dismiss", "@agent", "Dismiss an admin of this group", ""),
            new Command("rename", "<name>", "Rename this group", ""),
            new Command("info", "", "Group or contact info", ""),
            new Command("sidebar", "", "Show or hide the chat list", "Ctrl+B"),
            new Command("clear", "", "Delete this chat's messages (asks you to confirm)", ""),
            new Command("help", "", "Keys, commands and tips", ""),
            new Command("quit", "", "Leave BuildCLI", "Ctrl+C"));


    /** @return true if the text was a slash command (and was handled); "/home/me/file" is not one */
    boolean run(String text) {
        if (!text.startsWith("/")) {
            return false;
        }
        String[] parts = text.substring(1).split("\\s+", 2);
        String name = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].strip() : "";
        if (LIST.stream().noneMatch(c -> c.name().equals(name))) {
            return false;
        }
        try {
            switch (name) {
                case "diff" -> {
                    List<String> lines = LocalViews.diff(cwd, arg);
                    if (lines.isEmpty()) {
                        session.system(arg.contains("--staged") ? t("No staged changes.") : t("No changes."));
                    } else {
                        host.openView(new ViewSpec(t("Changes") + (arg.isEmpty() ? "" : "  " + arg), lines, true, false));
                    }
                }
                case "copy" -> host.copyLast(arg.equalsIgnoreCase("message"));
                case "find" -> host.openFind(arg);
                case "review" -> {
                    long id = session.lastChanges(host.selected());
                    if (id < 0) {
                        session.system(t("No files changed by agents in this chat yet."));
                    } else {
                        host.review(id);
                    }
                }
                case "undo" -> host.undoLast();
                case "mode" -> {
                    if (arg.isEmpty()) {
                        var m = session.approvalMode();
                        session.system(t("Mode: {0}, {1}. /mode manual, /mode edits or /mode auto changes it.", t(m.label()), t(m.description())));
                    } else if (java.util.Arrays.stream(dev.buildcli.application.ApprovalMode.values()).noneMatch(m -> m.label().equalsIgnoreCase(arg))) {
                        session.system(t("There is no mode '{0}'. Use manual, edits or auto.", arg));
                    } else {
                        setMode(dev.buildcli.application.ApprovalMode.parse(arg));
                    }
                }
                case "revoke" -> {
                    List<String> was = session.grants(host.selected());
                    if (was.isEmpty()) {
                        session.system(t("Nothing is approved automatically in this chat."));
                    } else {
                        session.revokeGrants(host.selected());
                        session.system(t("Asking again from now on. Was allowed: {0}.", String.join("; ", was)));
                    }
                }
                case "status" -> host.openView(new ViewSpec("git status", LocalViews.status(cwd), false, false));
                case "log" -> host.openView(new ViewSpec("Recent commits", LocalViews.log(cwd), false, false));
                case "open" -> {
                    if (arg.isEmpty()) {
                        host.setInput("/open ");
                        return true;
                    }
                    Path file = resolve(cwd, arg);
                    host.openView(new ViewSpec(cwd.relativize(file.toAbsolutePath().normalize()).toString(), LocalViews.file(file), false, true));
                }
                case "attach" -> {
                    if (arg.isEmpty()) {
                        host.setInput("/attach ");
                        return true;
                    }
                    host.attach(Attachment.of(resolve(cwd, arg)));
                }
                case "tasks" -> host.openView(new ViewSpec("Tasks", taskLines(), false, false));
                case "stop" -> session.stop(host.selected());
                case "retry" -> {
                    long id = session.lastFailedMessage();
                    if (id < 0 || !session.retry(id)) {
                        session.system(t("Nothing to retry."));
                    }
                }
                case "queue" -> {
                    if (arg.equals("clear")) {
                        session.clearQueue();
                    } else {
                        session.system(session.queued() == 0 ? t("No messages are waiting.") : t("{0} message(s) waiting their turn.", session.queued()));
                    }
                }
                case "sidebar" -> host.toggleSidebar();
                case "settings" -> host.openSettings();
                case "connect" -> host.openConnect();
                case "chats" -> {
                    host.openChats(arg);
                }
                case "model" -> changeModel(arg);
                case "reach" -> changeReach(arg);
                case "info" -> host.openInfo(ChatInfoView.Mode.INFO);
                case "dm" -> {
                    String who = firstMention(arg);
                    if (who == null) {
                        host.openInfo(ChatInfoView.Mode.NEW_CHAT);
                    } else {
                        session.openDirect(who);
                        host.select(who);
                    }
                }
                case "context" -> groupContext(arg);
                case "me" -> aboutMe(arg);
                case "newgroup" -> {
                    List<String> members = session.mentioned(arg);
                    String groupName = arg.replaceAll("@[A-Za-z][A-Za-z0-9_-]*", "").strip();
                    if (groupName.isEmpty()) {
                        host.openInfo(ChatInfoView.Mode.NEW_CHAT);
                    } else {
                        host.select(session.createGroup(groupName, members));
                    }
                }
                case "add", "remove", "admin", "dismiss" -> {
                    if (session.group(host.selected()) == null) {
                        session.error(t("Open a group first: this is a direct chat."));
                        return true;
                    }
                    List<String> who = session.mentioned(arg);
                    if (who.isEmpty()) {
                        host.openInfo(name.equals("add") ? ChatInfoView.Mode.ADD_MEMBER : ChatInfoView.Mode.INFO);
                        return true;
                    }
                    for (String w : who) {
                        switch (name) {
                            case "add" -> session.addMember(host.selected(), w);
                            case "remove" -> session.removeMember(host.selected(), w);
                            case "admin" -> session.setAdmin(host.selected(), w, true);
                            default -> session.setAdmin(host.selected(), w, false);
                        }
                    }
                }
                case "rename" -> {
                    if (session.group(host.selected()) != null && !arg.isBlank()) {
                        session.renameGroup(host.selected(), arg);
                    }
                }
                case "clear" -> {
                    if (host.selected().equals(clearArmedFor)) {
                        clearArmedFor = null;
                        session.clearChat(host.selected());
                    } else {
                        clearArmedFor = host.selected();
                        long n = session.messages().stream().filter(m -> m.thread().equals(host.selected())).count();
                        session.system(t("This deletes the {0} message(s) of {1}, also from the saved history. Type /clear again to confirm.", n, host.titleOf(host.selected())));
                    }
                }
                case "help" -> host.openView(new ViewSpec(t("Help"), help(), false, false));
                case "quit" -> host.quit();
                default -> { }
            }
        } catch (IOException | IllegalArgumentException e) {
            session.error(e.getMessage());
        }
        return true;
    }

    private List<String> taskLines() {
        List<String> out = new ArrayList<>();
        List<Task> tasks = session.tasks(host.selected());
        if (tasks.isEmpty()) {
            out.add(t("No tasks yet. Each message becomes a task; a handoff becomes a child task."));
        }
        for (Task t : tasks) {
            int depth = 0;
            Integer parent = t.parentId;
            while (parent != null && depth < 10) {
                int id = parent;
                Task up = tasks.stream().filter(x -> x.id == id).findFirst().orElse(null);
                parent = up == null ? null : up.parentId;
                depth++;
            }
            out.add("  ".repeat(depth) + "#" + t.id + "  " + t.status + "  " + t.from + " → " + t.to + "   " + I18n.t("{0} tokens", t.tokens())
                    + (t.attempts() > 1 ? ", " + I18n.t("attempt {0}", t.attempts()) : ""));
            out.add("  ".repeat(depth) + "    " + t.objective.replace('\n', ' '));
        }
        return out;
    }

    private static List<String> help() {
        List<String> out = new ArrayList<>();
        out.add(t("Talk to your agents like in a chat. Press Enter to send; you can keep typing while they work,"));
        out.add(t("messages wait their turn. @name sends a message straight to one agent."));
        out.add("");
        out.add(t("Commands (type / to see the menu)"));
        for (Command c : LIST) {
            out.add("  /" + c.name() + (c.arg().isEmpty() ? "" : " " + c.arg()) + (c.shortcut().isEmpty() ? "" : "   (" + c.shortcut() + ")"));
            out.add("      " + t(c.description()));
        }
        out.add("");
        out.add(t("Keys"));
        out.add("  " + t("Enter send · Shift+Enter, Alt+Enter, Ctrl+J or a trailing \\ new line · ↑ previous message"));
        out.add("  " + t("PgUp/PgDn or the mouse wheel scroll · Esc jump to the latest · Ctrl+W delete word · Ctrl+U clear"));
        out.add("  " + t("Ctrl+F find in this chat (↑ older, ↓ newer, Esc close) · Ctrl+K search your chats and agents · /copy copies the last code block"));
        out.add("  " + t("Shift+Tab changes the mode (manual, edits, auto) · Alt+M marks the newest message, then C copy, F forward, D delete"));
        out.add("");
        out.add(t("Mouse"));
        out.add("  " + t("Click agents to mention them, menu items, buttons, ⧉ copy on a code block and the ✕ on attachments."));
        out.add("  " + t("Right-click a message to mark it. Drag the scrollbar. To select text for copying, hold Shift (Option on macOS) while dragging."));
        out.add("");
        out.add(t("Attachments"));
        out.add("  " + t("Drop or paste the path of an image (png, jpg, gif, webp) or audio file (wav, mp3, m4a, ogg, flac),"));
        out.add("  " + t("or use /attach <file>. The model must support images or audio for it to be understood."));
        return out;
    }

    /** A path as a person types or drops it: quoted, as a file:// URL, with ~ or escaped spaces; relative to {@code cwd}. */
    static Path resolve(Path cwd, String pathText) {
        String s = pathText.strip();
        if (s.length() > 1 && (s.startsWith("\"") && s.endsWith("\"") || s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1);
        }
        if (s.startsWith("file://")) {
            try {
                return Path.of(new URI(s));
            } catch (Exception e) {
                s = s.substring(7);
            }
        }
        if (s.startsWith("~")) {
            s = System.getProperty("user.home") + s.substring(1);
        }
        if (!s.matches("^[A-Za-z]:\\\\.*")) {
            s = s.replace("\\ ", " ");
        }
        return cwd.resolve(s);
    }

    private String firstMention(String text) {
        List<String> m = session.mentioned(text);
        if (!m.isEmpty()) {
            return m.get(0);
        }
        String bare = text.strip();
        return session.contact(bare) != null ? bare : null;
    }

    /**
     * /model: with no argument, the Models settings (the default and each agent's model). With {@code provider:model} it
     * becomes the default model of this project; with {@code @agent provider:model}, that agent's model. The model is not
     * tested here (use /connect for that); a typo shows up as an error on the next message, with Retry.
     */
    private void changeModel(String arg) {
        if (arg.isBlank()) {
            host.showModels();
            return;
        }
        String[] words = arg.strip().split("\\s+");
        String agent = null;
        String model = words[words.length - 1];
        if (words.length == 2 && words[0].startsWith("@")) {
            agent = words[0].substring(1);
        } else if (words.length != 1) {
            host.say("Use /model provider:model, or /model @agent provider:model");
            return;
        }
        int colon = model.indexOf(':');
        if (colon <= 0 || colon == model.length() - 1) {
            host.say("A model is written provider:model, for example openrouter:openrouter/free");
            return;
        }
        String provider = model.substring(0, colon);
        if (services.providers().stream().noneMatch(p -> p.name().equals(provider))) {
            host.say("Unknown provider '" + provider + "'. Try /connect to see them.");
            return;
        }
        if (agent != null && session.contact(agent) == null) {
            host.say("There is no agent called " + agent + ".");
            return;
        }
        try {
            services.settings().set(dev.buildcli.ports.SettingsStore.Scope.PROJECT,
                    agent == null ? dev.buildcli.application.Settings.DEFAULT_MODEL : dev.buildcli.application.Settings.AGENT_MODEL + agent, model);
            host.say((agent == null ? "Default model: " : agent + " now uses ") + model + " (this project)");
        } catch (RuntimeException e) {
            host.say("Could not save it: " + e.getMessage());
        }
    }

    /** {@code /reach} lists who cannot contact whom; {@code /reach @bruno @ana off} stops bruno writing to ana, {@code on} lifts it. */
    private void changeReach(String arg) {
        List<String> names = session.mentioned(arg);
        String lower = arg.toLowerCase(java.util.Locale.ROOT).strip();
        boolean off = lower.endsWith(" off");
        boolean on = lower.endsWith(" on");
        if (names.size() == 2 && (on || off)) {
            session.setReach(names.get(0), names.get(1), on);
            session.system(on ? t("{0} can contact {1}.", names.get(0), names.get(1)) : t("{0} can no longer contact {1}.", names.get(0), names.get(1)));
            return;
        }
        List<String> pairs = session.blockedPairs();
        session.system((pairs.isEmpty() ? t("Every agent can contact every other.") : t("Cannot contact: {0}.", String.join(", ", pairs)))
                + " " + t("Change it with /reach @bruno @ana off (or on)."));
    }
}
