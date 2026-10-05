package dev.buildcli.application;

import dev.buildcli.ports.SettingsStore;
import dev.buildcli.ports.SettingsStore.Scope;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User settings: what they are, their defaults, and the merged view the app reads (project over global over default).
 * Settings are stored by the user's own tools only, outside the project tree, so a cloned repository cannot change them.
 */
public final class Settings {

    public enum Type { BOOLEAN, CHOICE, NUMBER, MODEL, TEXT }

    /** One setting as the settings screen shows it. Keys ending in '.' are prefixes (one entry per agent). */
    public record Definition(String key, String section, String label, String help, Type type, List<String> choices, String defaultValue) {}

    public static final String THEME = "appearance.theme";
    public static final String SHOW_ACTIVITY = "appearance.activity";
    public static final String COMPACT = "appearance.compact";
    public static final String ENTER_SENDS = "input.enterSends";
    public static final String MOUSE = "input.mouse";
    public static final String DEFAULT_MODEL = "models.default";
    public static final String AGENT_MODEL = "models.agent.";
    public static final String AGENT_HOPS = "chat.agentMessages";
    public static final String BELL = "notify.bell";
    public static final String STORAGE = "storage.backend";
    public static final String APPROVAL_MODE = "approvals.mode";
    public static final String LANGUAGE = "ui.language";
    public static final String PROFILE_NAME = "profile.name";
    public static final String PROFILE_ABOUT = "profile.about";
    public static final String PROFILE_STYLE = "profile.style";

    public static final List<Definition> DEFINITIONS = List.of(
            new Definition(THEME, "Appearance", "Theme", "Colours of the chat", Type.CHOICE, List.of("dark", "light", "contrast"), "dark"),
            new Definition(SHOW_ACTIVITY, "Appearance", "Show agent activity", "Small lines like 'bruno wrote out/a.txt'", Type.BOOLEAN,
                    List.of(), "true"),
            new Definition(COMPACT, "Appearance", "Compact messages", "Less space between messages", Type.BOOLEAN, List.of(), "false"),
            new Definition(ENTER_SENDS, "General", "Enter sends", "Off: Enter adds a new line and Ctrl+Enter or Alt+Enter sends",
                    Type.BOOLEAN, List.of(), "true"),
            new Definition(MOUSE, "General", "Mouse", "Click and scroll with the mouse (takes effect on the next start)", Type.BOOLEAN,
                    List.of(), "true"),
            new Definition(BELL, "General", "Sound when an agent needs you", "Rings the terminal bell (many terminals flash the taskbar) "
                    + "when an agent asks for approval, and when a job of 15 seconds or more ends", Type.BOOLEAN, List.of(), "true"),
            new Definition(AGENT_HOPS, "General", "Agent-to-agent messages", "How many messages agents may send each other before "
                    + "they pause and wait for you", Type.NUMBER, List.of(), "6"),
            new Definition(LANGUAGE, "General", "Language", "What BuildCLI's screens are written in: auto (your computer's language), en or pt. The agents answer in the language you write in", Type.CHOICE, List.of("auto", "en", "pt"), "auto"),
            new Definition(APPROVAL_MODE, "General", "Approval mode", "manual: ask before every write, command and commit. edits: "
                    + "write files without asking (you can undo), still ask for commands. auto: ask for nothing; use it where nothing can be "
                    + "lost. Applies at once; you can also change it while chatting with Shift+Tab or /mode", Type.CHOICE, List.of("manual", "edits", "auto"), "manual"),
            new Definition(STORAGE, "General", "State database", "Where runs, events and chat history are kept. sqlite: a file, readable "
                    + "from several terminals. h2: a file, several times faster, one BuildCLI at a time. memory: fastest, forgotten when "
                    + "BuildCLI closes. Each keeps its own history (takes effect on the next start; BUILDCLI_STORAGE overrides it)",
                    Type.CHOICE, List.of("sqlite", "h2", "memory"), "sqlite"),
            new Definition(PROFILE_NAME, "About you", "Your name", "What the agents call you. Empty: they just say 'you'", Type.TEXT, List.of(), ""),
            new Definition(PROFILE_ABOUT, "About you", "About you", "What the agents should know about you and your work: who you are, what "
                    + "you do, what you care about. Kept on this computer; agents see it in every conversation", Type.TEXT, List.of(), ""),
            new Definition(PROFILE_STYLE, "About you", "How to deal with you", "How you like to be talked to and helped: short or detailed, "
                    + "formal or casual, your language, whether to ask before acting", Type.TEXT, List.of(), ""),
            new Definition(DEFAULT_MODEL, "Models", "Default model", "Used by agents that have no model of their own, e.g. "
                    + "openrouter:openrouter/free", Type.MODEL, List.of(), ""));

    private final SettingsStore store;
    private final Map<Scope, Map<String, String>> values = new LinkedHashMap<>();

    public Settings(SettingsStore store) {
        this.store = store;
        reload();
    }

    /** Settings with nothing stored, for tests and headless runs. */
    public static Settings defaults() {
        return new Settings(new SettingsStore() {
            private final Map<Scope, Map<String, String>> mem = new LinkedHashMap<>();

            @Override
            public Map<String, String> load(Scope scope) {
                return mem.getOrDefault(scope, Map.of());
            }

            @Override
            public void save(Scope scope, Map<String, String> v) {
                mem.put(scope, new LinkedHashMap<>(v));
            }
        });
    }

    public synchronized void reload() {
        for (Scope s : Scope.values()) {
            values.put(s, new LinkedHashMap<>(store.load(s)));
        }
    }

    /** The effective value: project, then global, then the default. */
    public synchronized String get(String key) {
        String p = values.get(Scope.PROJECT).get(key);
        if (p != null) {
            return p;
        }
        String g = values.get(Scope.GLOBAL).get(key);
        return g != null ? g : defaultOf(key);
    }

    /** The value stored in one scope, or null. */
    public synchronized String stored(Scope scope, String key) {
        return values.get(scope).get(key);
    }

    public boolean flag(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public int number(String key, int fallback) {
        try {
            return Integer.parseInt(get(key).strip());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** Stores a value in a scope; null or empty removes it there (the other scope or the default then applies). */
    public synchronized void set(Scope scope, String key, String value) {
        Map<String, String> m = values.get(scope);
        if (value == null || value.isBlank()) {
            m.remove(key);
        } else {
            m.put(key, value.strip());
        }
        store.save(scope, m);
    }

    /** The model an agent should use, from settings, or null to fall back to the default model. */
    public String modelFor(String agent) {
        String own = get(AGENT_MODEL + agent);
        return own == null || own.isBlank() ? null : own;
    }

    public String defaultModel() {
        String d = get(DEFAULT_MODEL);
        return d == null || d.isBlank() ? null : d;
    }

    private static String defaultOf(String key) {
        for (Definition d : DEFINITIONS) {
            if (d.key().equals(key)) {
                return d.defaultValue();
            }
        }
        return key.startsWith(AGENT_MODEL) ? "" : null;
    }

    public static List<Definition> section(String name) {
        List<Definition> out = new ArrayList<>();
        for (Definition d : DEFINITIONS) {
            if (d.section().equals(name)) {
                out.add(d);
            }
        }
        return out;
    }
}
