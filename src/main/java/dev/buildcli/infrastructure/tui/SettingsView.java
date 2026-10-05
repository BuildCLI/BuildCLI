package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;
import static dev.buildcli.infrastructure.tui.Draw.fill;
import static dev.buildcli.infrastructure.tui.Draw.putSafe;
import static dev.buildcli.infrastructure.tui.Draw.st;
import dev.buildcli.application.Settings;
import dev.buildcli.domain.Capability;
import dev.buildcli.infrastructure.ModelCatalog;
import dev.buildcli.ports.SettingsStore.Scope;
import dev.buildcli.infrastructure.TerminalText;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.text.CharWidth;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The settings screen: General, Appearance, Models, Providers and Agents, each saved for this project or for all
 * projects. Everything is reachable by keyboard and mouse. The work (saving, testing a provider, listing models,
 * writing agent files) is done by {@link Services}, so this class only draws and reacts.
 */
final class SettingsView {

    private enum Section { GENERAL("General"), APPEARANCE("Appearance"), MODELS("Models"), PROVIDERS("Providers"), AGENTS("Agents"), PROFILE("About you");

        final String label;

        Section(String label) {
            this.label = label;
        }
    }

    private record Item(String label, String value, Color valueColor, String help, Runnable activate, Runnable left, Runnable right,
                        Runnable delete) {}

    private record Hit(Rect rect, Runnable action) {}

    private final SettingsServices services;
    private final Runnable close;
    private final Consumer<String> open;
    private final Runnable connect;
    private final List<Hit> hits = new ArrayList<>();
    private Section section = Section.GENERAL;
    private Scope scope = Scope.PROJECT;
    private int index;
    private int firstVisible;
    private final SettingsDialogs dialogs = new SettingsDialogs((rect, action) -> hits.add(new Hit(rect, action)), this::fail);
    private String status = "";
    private Color statusColor = Theme.DIM;

    /** Told the key of every setting that was saved, so what applies while running (the approval mode) does not wait for a restart. */
    private final Consumer<String> changed;

    SettingsView(SettingsServices services, Runnable close, Consumer<String> open, Runnable connect, Consumer<String> changed) {
        this.changed = changed;
        this.services = services;
        this.close = close;
        this.open = open;
        this.connect = connect;
    }

    // ---- items of each section ----

    private List<Item> items() {
        Settings s = services.settings();
        List<Item> out = new ArrayList<>();
        switch (section) {
            case GENERAL, APPEARANCE, PROFILE -> {
                for (Settings.Definition d : Settings.section(section.label)) {
                    out.add(settingItem(s, d));
                }
                if (section == Section.GENERAL) {
                    out.add(new Item("Where settings are saved", scope == Scope.PROJECT ? "this project" : "all projects", Theme.DIM,
                            t("Project settings win over the ones for all projects. Press S or click the switch above to change."),
                            this::toggleScope, this::toggleScope, this::toggleScope, null));
                }
            }
            case MODELS -> {
                out.add(settingItem(s, Settings.DEFINITIONS.stream().filter(d -> d.key().equals(Settings.DEFAULT_MODEL)).findFirst().orElseThrow()));
                for (SettingsServices.AgentInfo a : services.agents()) {
                    String key = Settings.AGENT_MODEL + a.name();
                    String own = s.stored(scope, key);
                    String effective = s.modelFor(a.name());
                    String value = effective != null ? effective : s.defaultModel() != null ? s.defaultModel() + "  (" + t("default") + ")" : t("not set");
                    out.add(new Item(a.name(), value, effective != null ? Theme.TEXT : Theme.DIM,
                            (own != null ? t("Set {0}.", scopeLabel()) + " " : "") + t("Enter to choose a model · Del to go back to the default"),
                            () -> pickModel(t("Model for {0}", a.name()), m -> save(key, m)), null, null, () -> save(key, null)));
                }
            }
            case PROVIDERS -> {
                for (SettingsServices.Provider p : services.providers()) {
                    boolean needsKey = p.keyEnv() != null && !p.keySet();
                    String key = p.keyEnv() == null ? t("no key needed") : !p.keySet() ? t("no key yet")
                            : p.keyFrom().equals("saved") ? t("key saved") : t("key from {0}", p.keyEnv());
                    out.add(new Item(p.name(), key, p.keyEnv() == null || p.keySet() ? Theme.GREEN : Theme.AMBER,
                            p.url() + (needsKey ? "   ·  " + t("Enter to add your key") : "   ·  " + t("Enter to test")) + (p.removable() ? " · " + t("Del to remove") : ""),
                            needsKey ? connect : () -> testProvider(p.name()), null, null, p.removable() ? () -> confirm(t("Remove provider {0}?", p.name()), () -> {
                                try {
                                    services.removeProvider(p.name());
                                    ok(t("Removed {0}", p.name()));
                                } catch (Exception e) {
                                    fail(e.getMessage());
                                }
                            }) : null));
                }
                out.add(new Item("+ Add a provider", "", Theme.ACCENT, "Any OpenAI-compatible endpoint: vLLM, a company gateway, another cloud",
                        this::addProvider, null, null, null));
            }
            case AGENTS -> {
                for (SettingsServices.AgentInfo a : services.agents()) {
                    out.add(new Item(a.name(), a.role() + " · " + a.origin(), Theme.agentColor(a.name()),
                            String.join(", ", a.capabilities()) + "   ·  " + t("Enter to view · Del to delete"),
                            () -> open.accept(a.file()), null, null,
                            () -> confirm(t("Delete agent {0}? This deletes {1}", a.name(), a.file()), () -> {
                                try {
                                    services.deleteAgent(a.name());
                                    ok(t("Deleted {0}; it left every group.", a.name()));
                                } catch (Exception e) {
                                    fail(e.getMessage());
                                }
                            })));
                }
                out.add(new Item("+ New agent", "", Theme.ACCENT, "Name, role, what it may do; saved " + scopeLabel(), this::newAgent, null, null, null));
                out.add(new Item("+ Sample agents", "", Theme.ACCENT, "wheslley, breno, matheus and dumildes, in a group, for this project",
                        this::addSamples, null, null, null));
            }
            default -> { }
        }
        return out;
    }

    private Item settingItem(Settings s, Settings.Definition d) {
        // what you tell the agents about yourself is yours, not the project's
        Scope scope = d.section().equals("About you") ? Scope.GLOBAL : this.scope;
        String stored = s.stored(scope, d.key());
        String effective = s.get(d.key());
        String origin = stored != null ? scopeLabel() : s.stored(other(), d.key()) != null ? (other() == Scope.PROJECT ? t("this project") : t("all projects")) : t("default");
        String help = t(d.help()) + "   ·  " + (d.section().equals("About you") ? t("all projects") : origin) + (stored != null ? " · " + t("Del to reset") : "");
        Runnable reset = () -> save(scope, d.key(), null);
        return switch (d.type()) {
            case BOOLEAN -> {
                boolean on = Boolean.parseBoolean(effective);
                Runnable toggle = () -> save(scope, d.key(), Boolean.toString(!on));
                yield new Item(t(d.label()), on ? "● On" : "○ Off", on ? Theme.ACCENT : Theme.DIM, help, toggle, toggle, toggle, reset);
            }
            case CHOICE -> {
                int i = Math.max(0, d.choices().indexOf(effective));
                Runnable next = () -> save(scope, d.key(), d.choices().get((i + 1) % d.choices().size()));
                Runnable prev = () -> save(scope, d.key(), d.choices().get((i - 1 + d.choices().size()) % d.choices().size()));
                yield new Item(t(d.label()), "‹ " + effective + " ›", Theme.TEXT, help, next, prev, next, reset);
            }
            case MODEL -> new Item(t(d.label()), effective == null || effective.isBlank() ? t("not set") : effective,
                    effective == null || effective.isBlank() ? Theme.DIM : Theme.TEXT, help,
                    () -> pickModel(t(d.label()), m -> save(scope, d.key(), m)), null, null, reset);
            default -> new Item(t(d.label()), effective == null ? "" : effective, Theme.TEXT, help,
                    () -> ask(t(d.label()), t(d.help()), effective == null ? "" : effective, v -> {
                        if (d.type() == Settings.Type.NUMBER && !v.isBlank() && !v.strip().matches("\\d{1,4}")) {
                            fail(t("A whole number, please"));
                            return;
                        }
                        save(scope, d.key(), v);
                    }), null, null, reset);
        };
    }

    private Scope other() {
        return scope == Scope.PROJECT ? Scope.GLOBAL : Scope.PROJECT;
    }

    private String scopeLabel() {
        return scope == Scope.PROJECT ? t("for this project") : t("for all projects");
    }

    private void toggleScope() {
        scope = other();
        ok(t("Changes are now saved {0}", scopeLabel()));
    }

    private void save(String key, String value) {
        save(scope, key, value);
    }

    private void save(Scope where, String key, String value) {
        try {
            services.settings().set(where, key, value);
            changed.accept(key);
            ok(value == null ? t("Reset {0}", scopeLabel()) : t("Saved {0}", scopeLabel()));
        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

    private void ok(String text) {
        status = text;
        statusColor = Theme.ACCENT;
    }

    private void fail(String text) {
        status = text == null ? t("Failed") : text;
        statusColor = Theme.RED;
    }

    // ---- flows ----

    private void ask(String title, String hint, String initial, Consumer<String> onSubmit) {
        ask(title, hint, initial, onSubmit, null);
    }

    /** @param back what Esc does: the previous step of a flow; null cancels the whole flow */
    private void ask(String title, String hint, String initial, Consumer<String> onSubmit, Runnable back) {
        dialogs.ask(title, hint, initial, onSubmit, back);
    }

    private void confirm(String text, Runnable yes) {
        dialogs.confirm(text, yes);
    }

    /** What the steps of a flow have collected so far, so that going back shows what was typed. */
    private static final class Draft {
        String a = "";
        String b = "";
        String c = "";
        List<String> caps = List.of(Capability.FILESYSTEM_READ, Capability.SEARCH);
    }

    private void addProvider() {
        providerName(new Draft());
    }

    private void providerName(Draft d) {
        ask(t("New provider: name (1/3)"), t("lowercase, used as name:model"), d.a, name -> {
            d.a = name.strip();
            providerUrl(d);
        });
    }

    private void providerUrl(Draft d) {
        ask(t("URL of {0} (2/3)", d.a), t("e.g. http://gpu-box:8000/v1"), d.b.isEmpty() ? "https://" : d.b, url -> {
            d.b = url.strip();
            providerKey(d);
        }, () -> providerName(d));
    }

    private void providerKey(Draft d) {
        ask(t("Environment variable with the key (3/3)"), t("leave empty if it needs none; you can also type the key in /connect"), d.c, env -> {
            try {
                services.addProvider(d.a, d.b, env.isBlank() ? null : env.strip());
                ok(env.isBlank() ? t("Added {0}", d.a) : t("Added {0}. Set {1} or type the key in /connect.", d.a, env.strip()));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }, () -> providerUrl(d));
    }

    private void newAgent() {
        agentName(new Draft());
    }

    private void agentName(Draft d) {
        ask(t("New agent: name (1/4)"), t("lowercase letters, digits, - and _"), d.a, name -> {
            d.a = name.strip();
            agentRole(d);
        });
    }

    private void agentRole(Draft d) {
        ask(t("Role of {0} (2/4)", d.a), t("e.g. writer, researcher, reviewer"), d.b.isEmpty() ? "assistant" : d.b, role -> {
            d.b = role.strip();
            agentCapabilities(d);
        }, () -> agentName(d));
    }

    private void agentCapabilities(Draft d) {
        List<String> names = dev.buildcli.domain.Capability.KNOWN.stream().sorted().toList();
        List<String> notes = names.stream().map(c -> t(CapabilityInfo.describe(c))).toList();
        dialogs.tick(t("What {0} may do (3/4)", d.a), names, notes, d.caps, picked -> {
            d.caps = picked;
            agentHow(d);
        }, () -> agentRole(d));
    }

    private void agentHow(Draft d) {
        ask(t("How {0} should work (4/4)", d.a), t("one sentence; edit the file later for more"), d.c, how -> {
            d.c = how.strip();
            try {
                String file = services.createAgent(d.a, d.b, d.c, d.caps, scope == Scope.GLOBAL);
                ok(t("Created {0}. You can chat with {1} now.", file, d.a));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }, () -> agentCapabilities(d));
    }

    private void addSamples() {
        try {
            ok(t("Added {0} and a group for them.", String.join(", ", services.createSampleAgents())));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void testProvider(String provider) {
        String suggestion = provider.equals("openrouter") ? "openrouter:openrouter/free" : provider + ":";
        ask(t("Test {0}", provider), t("provider:model to send a one-word request to"), suggestion, model -> {
            status = t("Testing {0}…", model);
            statusColor = Theme.DIM;
            services.test(model.strip()).thenAccept(line -> {
                if (line.startsWith("ok")) {
                    ok(line);
                } else {
                    fail(line);
                }
            });
        });
    }

    private void pickModel(String title, Consumer<String> onPick) {
        List<CompletableFuture<ModelCatalog.Result>> sources = new ArrayList<>();
        for (SettingsServices.Provider pr : services.providers()) {
            if (pr.keyEnv() == null || pr.keySet()) {
                sources.add(services.models(pr.name()));
            }
        }
        dialogs.pick(title, onPick, sources);
    }

    // ---- drawing ----




    void render(Buffer buf, Rect r) {
        hits.clear();
        fill(buf, r, st(Theme.TEXT, Theme.BG));
        Rect bar = new Rect(r.x(), r.y(), r.width(), 2);
        fill(buf, bar, st(Theme.TEXT, Theme.PANEL));
        putSafe(buf, r.x() + 2, r.y(), "Settings", st(Theme.TEXT, Theme.PANEL).bold(), r.right());
        putSafe(buf, r.x() + 2, r.y() + 1, "Saved " + scopeLabel(), st(Theme.DIM, Theme.PANEL), r.right());
        int x = r.x() + 14;
        for (Scope sc : new Scope[] {Scope.PROJECT, Scope.GLOBAL}) {
            String label = sc == Scope.PROJECT ? " " + t("This project") + " " : " " + t("All projects") + " ";
            boolean on = sc == scope;
            int w = putSafe(buf, x, r.y(), label, on ? st(Theme.ON_ACCENT, Theme.ACCENT).bold() : st(Theme.TEXT, Theme.FIELD), r.right());
            hits.add(new Hit(new Rect(x, r.y(), w, 1), () -> {
                if (scope != sc) {
                    toggleScope();
                }
            }));
            x += w + 1;
        }
        String closeLabel = " ✕ Esc ";
        int cx = r.right() - CharWidth.of(closeLabel) - 1;
        putSafe(buf, cx, r.y(), closeLabel, st(Theme.DIM, Theme.PANEL), r.right());
        hits.add(new Hit(new Rect(cx, r.y(), CharWidth.of(closeLabel), 1), close));

        int navW = 18;
        Rect nav = new Rect(r.x(), r.y() + 2, navW, r.height() - 3);
        fill(buf, nav, st(Theme.TEXT, Theme.SIDEBAR));
        int ny = nav.y() + 1;
        for (Section s : Section.values()) {
            boolean on = s == section;
            Rect row = new Rect(nav.x(), ny, navW, 1);
            fill(buf, row, st(Theme.TEXT, on ? Theme.SELECTED : Theme.SIDEBAR));
            putSafe(buf, row.x() + 2, ny, (on ? "▌ " : "  ") + t(s.label), on ? st(Theme.TEXT, Theme.SELECTED).bold() : st(Theme.DIM, Theme.SIDEBAR), row.right());
            hits.add(new Hit(row, () -> select(s)));
            ny += 2;
        }

        Rect content = new Rect(r.x() + navW + 2, r.y() + 3, r.width() - navW - 4, r.height() - 5);
        List<Item> items = items();
        index = Math.max(0, Math.min(index, items.size() - 1));
        int per = 3;
        int visible = Math.max(1, content.height() / per);
        if (index < firstVisible) {
            firstVisible = index;
        } else if (index >= firstVisible + visible) {
            firstVisible = index - visible + 1;
        }
        for (int i = firstVisible; i < items.size() && i < firstVisible + visible; i++) {
            Item it = items.get(i);
            int y = content.y() + (i - firstVisible) * per;
            boolean sel = i == index;
            Color bg = sel ? Theme.SELECTED : Theme.BG;
            Rect row = new Rect(content.x(), y, content.width(), 2);
            fill(buf, row, st(Theme.TEXT, bg));
            putSafe(buf, row.x() + 1, y, it.label(), st(Theme.TEXT, bg).bold(), row.right() - 2);
            int vw = CharWidth.of(TerminalText.sanitize(it.value()));
            putSafe(buf, Math.max(row.x() + 24, row.right() - vw - 2), y, it.value(), st(it.valueColor(), bg), row.right() - 1);
            putSafe(buf, row.x() + 1, y + 1, it.help(), st(Theme.DIM, bg), row.right() - 1);
            int idx = i;
            hits.add(new Hit(row, () -> {
                index = idx;
                it.activate().run();
            }));
        }
        if (items.size() > visible) {
            putSafe(buf, content.right() - 12, content.bottom(), t("{0}–{1} of {2}", firstVisible + 1, Math.min(items.size(), firstVisible + visible), items.size()),
                    st(Theme.FAINT, Theme.BG), content.right());
        }
        Rect foot = new Rect(r.x(), r.bottom() - 1, r.width(), 1);
        fill(buf, foot, st(Theme.DIM, Theme.PANEL));
        String keys = t("↑↓ choose · Enter change · ←→ switch · Del reset · Tab section · S scope · Esc close");
        putSafe(buf, foot.x() + navW + 2, foot.y(), status.isEmpty() ? keys : status, st(status.isEmpty() ? Theme.DIM : statusColor, Theme.PANEL), foot.right());

        if (dialogs.active()) {
            hits.clear(); // a dialog is modal: clicks outside it do nothing
        }
        dialogs.render(buf, r);
    }

    // ---- input ----

    /** Opens the Agents section on the "new agent" questions (from the empty chat screen). */
    void startNewAgent() {
        select(Section.AGENTS);
        newAgent();
    }

    /** Opens the Models section: the default model and the model of each agent. */
    void showModels() {
        select(Section.MODELS);
    }

    private void select(Section s) {
        section = s;
        index = 0;
        firstVisible = 0;
        status = "";
    }

    void key(KeyEvent key) {
        KeyCode code = key.code();
        char ch = code == KeyCode.CHAR ? Character.toLowerCase(key.character()) : 0;
        if (dialogs.key(key)) {
            return;
        }
        List<Item> items = items();
        Item it = items.isEmpty() ? null : items.get(Math.max(0, Math.min(index, items.size() - 1)));
        switch (code) {
            case ESCAPE -> close.run();
            case UP -> index = Math.max(0, index - 1);
            case DOWN -> index = Math.min(items.size() - 1, index + 1);
            case TAB -> select(Section.values()[(section.ordinal() + (key.hasShift() ? Section.values().length - 1 : 1)) % Section.values().length]);
            case ENTER -> run(it == null ? null : it.activate());
            case LEFT -> run(it == null ? null : it.left());
            case RIGHT -> run(it == null ? null : it.right());
            case DELETE, BACKSPACE -> run(it == null ? null : it.delete());
            case CHAR -> {
                if (ch == ' ') {
                    run(it == null ? null : it.activate());
                } else if (ch == 's') {
                    toggleScope();
                } else if (ch == 'q') {
                    close.run();
                } else if (ch >= '1' && ch <= '6') {
                    select(Section.values()[ch - '1']);
                }
            }
            default -> { }
        }
    }

    private static void run(Runnable r) {
        if (r != null) {
            r.run();
        }
    }

    void paste(String text) {
        dialogs.paste(text);
    }

    void mouse(MouseEvent m) {
        if (m.kind() == MouseEventKind.SCROLL_UP || m.kind() == MouseEventKind.SCROLL_DOWN) {
            int d = m.kind() == MouseEventKind.SCROLL_UP ? -1 : 1;
            if (!dialogs.scroll(d)) {
                index = Math.max(0, index + d);
            }
            return;
        }
        if (m.kind() == MouseEventKind.PRESS && m.isLeftButton()) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                if (hits.get(i).rect().contains(m.x(), m.y())) {
                    hits.get(i).action().run();
                    return;
                }
            }
        }
    }
}
