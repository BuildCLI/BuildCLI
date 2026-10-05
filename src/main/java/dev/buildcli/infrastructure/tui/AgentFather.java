package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;

import dev.buildcli.application.ChatSession;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.infrastructure.AgentFileEditor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * AgentFather: the built-in contact you talk to in order to create and manage agents, like Telegram's BotFather. It is not
 * an agent and uses no model, so it works before any model is connected. It follows a short script of questions, shows what
 * it is about to do and does it only after you say yes; permissions come from the capabilities you choose, never from text.
 */
final class AgentFather {
    /** The commands it understands, for the menu of its chat. */
    static final List<ChatCommands.Command> COMMANDS = List.of(
            new ChatCommands.Command("newagent", "[name]", "Create an agent, step by step", ""),
            new ChatCommands.Command("agents", "", "List your agents with their model and what they may do", ""),
            new ChatCommands.Command("editagent", "<name>", "Change an agent: role, what it may do, how it works, where it writes", ""),
            new ChatCommands.Command("deleteagent", "<name>", "Delete an agent (asks to confirm)", ""),
            new ChatCommands.Command("samples", "[team]", "Add a ready-made team: software, writing desk or office assistants", ""),
            new ChatCommands.Command("cancel", "", "Stop what we were doing", ""),
            new ChatCommands.Command("back", "", "Go back one question", ""));

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_-]*");

    private enum Step { IDLE, NAME, ROLE, CAPS, HOW, CONFIRM, DELETE, SAMPLES, EDIT_PICK, EDIT_VALUE, EDIT_CONFIRM }

    private final ChatSession session;
    private final SettingsServices services;
    private Step step = Step.IDLE;
    private String name = "";
    private String role = "";
    private List<String> caps = List.of(Capability.FILESYSTEM_READ, Capability.SEARCH);
    private String how = "";
    /** While editing: which setting is being asked (1 to 5), and the change waiting for your yes. */
    private int editing;
    private AgentFileEditor.Edit change;

    AgentFather(ChatSession session, SettingsServices services) {
        this.session = session;
        this.services = services;
    }

    private void say(String text) {
        session.post(ChatSession.FATHER, ChatSession.FATHER_NAME, text);
    }

    /** Says hello the first time the chat is opened. */
    void greet() {
        if (session.messages().stream().anyMatch(m -> m.thread().equals(ChatSession.FATHER))) {
            return;
        }
        say(t("Hi, I'm AgentFather. I create and manage your agents. I am not a model, so I work even before you have connected one.\n"
                + "/newagent creates an agent, step by step\n/agents lists them\n/editagent <name> changes one\n/deleteagent <name> deletes one\n/samples adds a ready-made team (software, writing or office)\n"
                + "/connect picks a provider and a model\n/help shows everything."));
    }

    /** What you wrote in the chat with AgentFather. */
    void handle(String text) {
        String t = text.strip();
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.equals("/cancel") || lower.equals("cancel") || lower.equals("cancelar")) {
            step = Step.IDLE;
            say(t("Okay, nothing was changed."));
            return;
        }
        if (lower.equals("/back") || lower.equals("back") || lower.equals("voltar")) {
            back();
            return;
        }
        if (t.startsWith("/")) {
            command(t);
            return;
        }
        if (step == Step.IDLE) {
            say(t("I only follow commands: /newagent, /agents, /editagent, /deleteagent, /samples, /help. Type / to see them."));
            return;
        }
        answer(t);
    }

    // ---- commands ----

    private void command(String text) {
        String[] parts = text.substring(1).split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].strip() : "";
        switch (cmd) {
            case "newagent" -> {
                name = "";
                role = "";
                caps = List.of(Capability.FILESYSTEM_READ, Capability.SEARCH);
                how = "";
                String wanted = arg.startsWith("@") ? arg.substring(1) : arg;
                if (wanted.isBlank()) {
                    askName();
                } else {
                    acceptName(wanted);
                }
            }
            case "agents" -> listAgents();
            case "deleteagent" -> {
                String who = arg.startsWith("@") ? arg.substring(1) : arg;
                if (session.contact(who) == null) {
                    say(who.isBlank() ? t("Which agent? For example /deleteagent rita") : t("There is no agent called {0}. /agents lists them.", who));
                    return;
                }
                name = who;
                step = Step.DELETE;
                say(t("Delete {0}? This removes its file, and it leaves its groups; what it already said stays in the chats.\nReply yes or no.", who));
            }
            case "samples" -> samples(arg);
            case "editagent" -> startEdit(arg.startsWith("@") ? arg.substring(1) : arg);
            case "help" -> say(t("/newagent [name]: create an agent, step by step\n/agents: list your agents\n/editagent <name>: change an agent\n"
                    + "/deleteagent <name>: delete one\n"
                    + "/samples [team]: add a ready-made team (software, writing or office)\n/cancel: stop what we were doing · /back: go back one question\n"
                    + "Other commands work here too: /connect, /model, /newgroup, /add, /settings."));
            default -> say(t("I don't know /{0}. Type / to see what I understand.", cmd));
        }
    }

    private void listAgents() {
        List<Agent> all = session.contacts();
        if (all.isEmpty()) {
            say(t("You have no agents yet. /newagent creates one, /samples adds a ready-made team to start with."));
            return;
        }
        StringBuilder sb = new StringBuilder((all.size() == 1 ? t("1 agent:") : t("{0} agents:", all.size())) + "\n");
        for (Agent a : all) {
            String model = services.settings().modelFor(a.name());
            if (model == null) {
                model = services.settings().defaultModel();
            }
            sb.append("• ").append(a.name()).append(" · ").append(a.role()).append(" · ").append(model == null ? t("no model yet (/connect)") : model)
                    .append("\n    ").append(t("may: {0}", String.join(", ", a.capabilities().stream().sorted().toList()))).append('\n');
        }
        say(sb.toString().stripTrailing());
    }

    private void samples(String team) {
        List<String> teams = services.sampleTeams();
        if (team.isBlank() && !teams.isEmpty()) {
            step = Step.SAMPLES;
            say(t("Which team do you want? Reply with its name:") + "\n" + teams.stream().map(x -> "• " + x).collect(java.util.stream.Collectors.joining("\n"))
                    + "\n" + t("They are only starting points: you can change them with /editagent, or make your own with /newagent."));
            return;
        }
        step = Step.IDLE;
        try {
            List<String> added = team.isBlank() ? services.createSampleAgents() : services.createSampleAgents(team.strip());
            say(t("Added {0} and a group for them. Open the group and say something to start.", String.join(", ", added)));
        } catch (Exception e) {
            say(t("I could not add them: {0}", e.getMessage()));
        }
    }

    // ---- the questions of /newagent ----

    private void askName() {
        step = Step.NAME;
        say(t("What should the new agent be called? Lowercase letters, digits, - and _ (for example rita)."));
    }

    private void acceptName(String candidate) {
        String n = candidate.strip().toLowerCase(Locale.ROOT);
        if (!NAME.matcher(n).matches()) {
            say(t("A name is lowercase letters, digits, - and _, starting with a letter. Try another."));
            step = Step.NAME;
            return;
        }
        if (session.contact(n) != null) {
            say(t("There is already an agent called {0}. Pick another name.", n));
            step = Step.NAME;
            return;
        }
        name = n;
        askRole();
    }

    private void askRole() {
        step = Step.ROLE;
        say(t("What is {0}'s role? For example writer, researcher, reviewer, analyst. Say skip for assistant.", name));
    }

    private void askCaps() {
        step = Step.CAPS;
        StringBuilder sb = new StringBuilder(t("What may {0} do? Reply with numbers, like 1,3, or default (read files and search).", name) + "\n");
        int i = 1;
        for (String c : Capability.KNOWN.stream().sorted().toList()) {
            sb.append(' ').append(i++).append(". ").append(c).append(": ").append(t(CapabilityInfo.describe(c))).append('\n');
        }
        say(sb.toString().stripTrailing());
    }

    private void askHow() {
        step = Step.HOW;
        say(t("In one sentence, how should {0} work? Or say skip.", name));
    }

    private void askConfirm() {
        step = Step.CONFIRM;
        say(t("I will create {0} ({1}) for this project. It may: {2}.", name, role, String.join(", ", caps)) + "\n"
                + (how.isBlank() ? "" : t("It will work like this: {0}", how) + "\n")
                + t("It starts able to read the whole project, and with no folder to write in and no command allowed; to give it more, use /editagent afterwards. Every write still shows you a diff and asks first.")
                + "\n" + t("Reply yes to create it, back to change something, or cancel."));
    }

    private void answer(String t) {
        switch (step) {
            case NAME -> acceptName(t);
            case ROLE -> {
                role = skip(t) || t.isBlank() ? "assistant" : t.strip();
                askCaps();
            }
            case CAPS -> {
                List<String> chosen = parseCaps(t);
                if (chosen == null) {
                    say(t("I did not understand that. Reply with numbers from the list, like 1,3, or default."));
                    return;
                }
                caps = chosen;
                askHow();
            }
            case HOW -> {
                how = skip(t) ? "" : t.strip();
                askConfirm();
            }
            case CONFIRM -> {
                if (yes(t)) {
                    create();
                } else if (no(t)) {
                    step = Step.IDLE;
                    say(t("Okay, nothing was changed."));
                } else {
                    say(t("Reply yes to create it, back to change something, or cancel."));
                }
            }
            case SAMPLES -> samples(t.strip());
            case EDIT_PICK -> pickEdit(t);
            case EDIT_VALUE -> acceptEdit(t);
            case EDIT_CONFIRM -> {
                if (yes(t)) {
                    applyEdit();
                } else if (no(t)) {
                    change = null;
                    editMenu(t("Okay, {0} is unchanged.", name));
                } else {
                    say(t("Reply yes to make the change, or no."));
                }
            }
            case DELETE -> {
                if (yes(t)) {
                    delete();
                } else {
                    step = Step.IDLE;
                    say(t("Okay, {0} stays.", name));
                }
            }
            default -> { }
        }
    }

    private void back() {
        switch (step) {
            case ROLE -> askName();
            case CAPS -> askRole();
            case HOW -> askCaps();
            case CONFIRM -> askHow();
            case NAME -> {
                step = Step.IDLE;
                say(t("Okay, nothing was changed."));
            }
            case EDIT_VALUE, EDIT_CONFIRM -> {
                change = null;
                editMenu("");
            }
            default -> say(t("There is nothing to go back to."));
        }
    }

    /** "default", or numbers and capability names separated by commas or spaces; null if something is not understood. */
    private static List<String> parseCaps(String text) {
        String t = text.strip().toLowerCase(Locale.ROOT);
        if (t.equals("default") || t.equals("predefinido") || t.equals("padrão") || t.isBlank()) {
            return List.of(Capability.FILESYSTEM_READ, Capability.SEARCH);
        }
        List<String> known = Capability.KNOWN.stream().sorted().toList();
        List<String> out = new ArrayList<>();
        for (String token : t.split("[,\\s]+")) {
            String pick = null;
            if (token.matches("\\d+")) {
                int n = Integer.parseInt(token);
                pick = n >= 1 && n <= known.size() ? known.get(n - 1) : null;
            } else if (known.contains(token)) {
                pick = token;
            }
            if (pick == null) {
                return null;
            }
            if (!out.contains(pick)) {
                out.add(pick);
            }
        }
        return out.isEmpty() ? null : out;
    }

    private static boolean skip(String t) {
        String l = t.strip().toLowerCase(Locale.ROOT);
        return l.equals("skip") || l.equals("saltar");
    }

    private static boolean none(String t) {
        String l = t.strip().toLowerCase(Locale.ROOT);
        return l.equals("none") || l.equals("nenhum") || l.equals("nenhuma");
    }

    private static boolean yes(String t) {
        String l = t.strip().toLowerCase(Locale.ROOT);
        return l.equals("yes") || l.equals("y") || l.equals("sim") || l.equals("ok");
    }

    private static boolean no(String t) {
        String l = t.strip().toLowerCase(Locale.ROOT);
        return l.equals("no") || l.equals("n") || l.equals("não") || l.equals("nao");
    }

    // ---- /editagent ----

    private void startEdit(String who) {
        Agent a = session.contact(who);
        if (a == null) {
            say(who.isBlank() ? t("Which agent? For example /editagent rita") : t("There is no agent called {0}. /agents lists them.", who));
            return;
        }
        if (a.source() == null || a.source().isBlank() || !a.source().endsWith(".md")) {
            say(t("{0} is not defined by a Markdown file in your project or folder, so I cannot edit it. Change its file by hand.", who));
            return;
        }
        name = who;
        editMenu("");
    }

    private void editMenu(String lead) {
        step = Step.EDIT_PICK;
        Agent a = session.contact(name);
        if (a == null) {
            step = Step.IDLE;
            say(t("That agent is gone."));
            return;
        }
        var p = a.permissions();
        say((lead.isEmpty() ? "" : lead + "\n") + t("{0} now:", name) + "\n"
                + "  " + t("role: {0}", a.role()) + "\n  " + t("may: {0}", String.join(", ", a.capabilities().stream().sorted().toList())) + "\n"
                + "  " + t("writes in: {0}", p.writeGlobs().isEmpty() ? t("nowhere") : code(p.writeGlobs())) + "\n"
                + "  " + t("runs without asking: {0}", p.commandAllow().isEmpty() ? t("nothing") : p.commandAllow().stream().map(c -> String.join(" ", c))
                        .collect(java.util.stream.Collectors.joining("; "))) + "\n"
                + t("What do you want to change?\n 1. role\n 2. what it may do\n 3. how it works\n 4. folders it may write in\n 5. commands it may run without asking\nReply with a number, or done."));
    }

    private void pickEdit(String t) {
        String l = t.strip().toLowerCase(Locale.ROOT);
        if (l.equals("done") || l.equals("feito") || l.equals("0")) {
            step = Step.IDLE;
            say(t("Okay, {0} is as you left it.", name));
            return;
        }
        editing = l.matches("[1-5]") ? Integer.parseInt(l) : 0;
        if (editing == 0) {
            say(t("Reply with a number from 1 to 5, or done."));
            return;
        }
        step = Step.EDIT_VALUE;
        switch (editing) {
            case 1 -> say(t("The new role for {0}? For example writer, researcher, reviewer.", name));
            case 2 -> {
                StringBuilder sb = new StringBuilder(t("What may {0} do? Reply with numbers, like 1,3, or default (read files and search).", name) + "\n");
                int i = 1;
                for (String c : Capability.KNOWN.stream().sorted().toList()) {
                    sb.append(' ').append(i++).append(". ").append(c).append(": ").append(t(CapabilityInfo.describe(c))).append('\n');
                }
                say(sb.toString().stripTrailing());
            }
            case 3 -> say(t("In one sentence or a short paragraph, how should {0} work? This replaces what its file says now.", name));
            case 4 -> say(t("Which folders may {0} write in? Patterns inside the project, separated by commas, like `src/**`, `docs/**`. "
                    + "Say none to take writing away. Every write is still shown to you as a diff and needs your yes, and it needs the capability "
                    + "filesystem.write (option 2).", name));
            default -> say(t("Which commands may {0} run WITHOUT asking you first? One per command, separated by ;, like mvn -q test; mvn -q verify. "
                    + "Anything else still asks. Say none to take this away. It needs the capability command.execute (option 2).", name));
        }
    }

    private void acceptEdit(String t) {
        try {
            switch (editing) {
                case 1 -> change = AgentFileEditor.Edit.role(t.strip());
                case 2 -> {
                    List<String> chosen = parseCaps(t);
                    if (chosen == null) {
                        say(t("I did not understand that. Reply with numbers from the list, like 1,3, or default."));
                        return;
                    }
                    change = AgentFileEditor.Edit.capabilities(chosen);
                }
                case 3 -> change = AgentFileEditor.Edit.instructions(t.strip());
                case 4 -> {
                    List<String> globs = new ArrayList<>();
                    if (!none(t)) {
                        for (String g : t.split("[,\\s]+")) {
                            if (!g.isBlank()) {
                                globs.add(AgentFileEditor.checkGlob(g));
                            }
                        }
                    }
                    change = AgentFileEditor.Edit.writeGlobs(globs);
                }
                default -> {
                    List<List<String>> commands = new ArrayList<>();
                    if (!none(t)) {
                        for (String one : t.split(";")) {
                            if (!one.isBlank()) {
                                commands.add(List.of(one.strip().split("\\s+")));
                            }
                        }
                    }
                    change = AgentFileEditor.Edit.commands(commands);
                }
            }
        } catch (IllegalArgumentException e) {
            say(e.getMessage() + ". " + t("Try again, or /back."));
            return;
        }
        step = Step.EDIT_CONFIRM;
        say(t("I will change {0}: {1}", name, describe(change)) + "\n" + t("Reply yes to do it, or no."));
    }

    /** Patterns like src/** would be read as Markdown bold, so they are shown as code. */
    private static String code(List<String> globs) {
        return globs.stream().map(g -> "`" + g + "`").collect(java.util.stream.Collectors.joining(", "));
    }

    private static String describe(AgentFileEditor.Edit e) {
        if (e.role() != null) {
            return t("role -> {0}.", e.role());
        }
        if (e.capabilities() != null) {
            return t("it may {0}.", String.join(", ", e.capabilities()));
        }
        if (e.instructions() != null) {
            return t("how it works -> {0}", e.instructions()) + "\n" + t("(the text in its file is replaced; comments in its header are lost.)");
        }
        if (e.writeGlobs() != null) {
            return e.writeGlobs().isEmpty() ? t("it may write nowhere.") : t("it may write in {0}. Each write still needs your yes.", code(e.writeGlobs()));
        }
        return e.commands().isEmpty() ? t("it runs nothing without asking.")
                : t("it runs these WITHOUT asking you: {0}.", e.commands().stream().map(c -> String.join(" ", c)).collect(java.util.stream.Collectors.joining("; ")));
    }

    private void applyEdit() {
        try {
            services.updateAgent(name, change);
            change = null;
            editMenu(t("Done."));
        } catch (Exception e) {
            say(t("I could not change it: {0}", e.getMessage()));
            editMenu("");
        }
    }

    private void create() {
        try {
            String file = services.createAgent(name, role, how, caps, false);
            step = Step.IDLE;
            session.openDirect(name);
            say(t("Created {0}. Its file is {1}.\nSay hello in its chat, or put it in a group with /newgroup <name> @{0}.", name, file));
        } catch (Exception e) {
            say(t("I could not create it: {0}", e.getMessage()));
            askName();
        }
    }

    private void delete() {
        try {
            services.deleteAgent(name);
            say(t("Deleted {0}.", name));
        } catch (Exception e) {
            say(t("I could not delete it: {0}", e.getMessage()));
        }
        step = Step.IDLE;
    }
}
