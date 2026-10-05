package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;
import static dev.buildcli.infrastructure.tui.Draw.fill;
import static dev.buildcli.infrastructure.tui.Draw.putSafe;
import static dev.buildcli.infrastructure.tui.Draw.st;

import dev.buildcli.infrastructure.FileCredentialStore;
import dev.buildcli.infrastructure.ModelCatalog;
import dev.buildcli.infrastructure.ProviderRegistry;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.text.CharWidth;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Typing an API key on the connect screen: the masked field, checking the key with the provider before it is kept, and
 * saving it. A key that the provider rejects is never saved; one that could not be checked is saved on a second Enter.
 */
final class KeyEntry {
    private final SettingsServices services;
    private final Consumer<String> saved;
    private boolean active;
    private SettingsServices.Provider provider;
    private final InputEditor input = new InputEditor();
    private CompletableFuture<ModelCatalog.Result> check;
    private String checking = "";
    private String message = "";
    private Color color = Theme.DIM;
    /** The key a second Enter saves without a successful check (the provider could not be reached). */
    private String saveAnyway;

    /** @param saved told the provider's name once its key is saved */
    KeyEntry(SettingsServices services, Consumer<String> saved) {
        this.services = services;
        this.saved = saved;
    }

    boolean active() {
        return active;
    }

    /** True while the provider is being asked about the key. */
    boolean busy() {
        return check != null && !check.isDone();
    }

    void start(SettingsServices.Provider p) {
        active = true;
        provider = p;
        input.clear();
        check = null;
        message = "";
        color = Theme.DIM;
        saveAnyway = null;
    }

    void stop() {
        active = false;
        check = null;
    }

    private void submit() {
        if (check != null && !check.isDone()) {
            return;
        }
        String key = FileCredentialStore.clean(input.text());
        if (!FileCredentialStore.valid(key)) {
            message = key.isEmpty() ? t("Paste the key first.") : t("That does not look like a key: it has spaces or line breaks.");
            color = Theme.RED;
            return;
        }
        if (key.equals(saveAnyway)) {
            save(key);
            return;
        }
        saveAnyway = null;
        checking = key;
        message = t("Checking the key with {0}…", provider.name());
        color = Theme.DIM;
        check = services.checkKey(provider.name(), key);
    }

    /** Called every frame: acts on a finished check. */
    void poll() {
        if (check != null && check.isDone() && active) {
            ModelCatalog.Result r = check.getNow(null);
            check = null;
            String problem = r == null ? t("no answer") : r.problem();
            if (problem == null) {
                save(checking);
            } else if (problem.startsWith("HTTP 401") || problem.startsWith("HTTP 403")) {
                message = t("{0} did not accept that key ({1}). Check it and paste it again.", provider.name(), problem);
                color = Theme.RED;
                input.clear();
            } else {
                message = t("Could not check it: {0}. Press Enter again to save it anyway, or paste a different one.", problem);
                color = Theme.AMBER;
                saveAnyway = checking;
            }
        }
    }

    private void save(String key) {
        try {
            services.saveKey(provider.name(), key);
        } catch (Exception e) {
            message = t("Could not save it: {0}", e.getMessage());
            color = Theme.RED;
            return;
        }
        String name = provider.name();
        active = false;
        input.clear();
        saveAnyway = null;
        saved.accept(name);
    }

    void key(KeyEvent key) {
        KeyCode code = key.code();
        switch (code) {
            case ESCAPE -> {
                stop();
            }
            case ENTER -> submit();
            case BACKSPACE -> {
                input.backspace();
                saveAnyway = null;
            }
            case LEFT -> input.left();
            case RIGHT -> input.right();
            case HOME -> input.home();
            case END -> input.end();
            case CHAR -> {
                if (key.hasCtrl() && Character.toLowerCase(key.character()) == 'u') {
                    input.clear();
                } else if (!key.hasCtrl() && !key.hasAlt() && key.character() > ' ') {
                    input.insert(key.string());
                }
                saveAnyway = null;
            }
            default -> { }
        }
    }

    void paste(String text) {
        input.insert(text.replaceAll("\\s+", "")); // a key never has spaces: drop the line break that came with the paste
        saveAnyway = null;
    }

    static String masked(String key) {
        if (key.length() <= 8) {
            return "•".repeat(key.length());
        }
        return "•".repeat(key.length() - 4) + key.substring(key.length() - 4);
    }

    void draw(Buffer buf, Rect b) {
        putSafe(buf, b.x(), b.y(), t("Your {0} key", provider.name()), st(Theme.TEXT, Theme.BG).bold(), b.right());
        String page = ProviderRegistry.keyPage(provider.name());
        int y = b.y() + 1;
        putSafe(buf, b.x(), y, page != null ? t("Create one at {0}", page) : t("Get one from {0}", provider.url()), st(Theme.DIM, Theme.BG), b.right());
        Rect field = new Rect(b.x(), b.y() + 3, b.width(), 1);
        fill(buf, field, st(Theme.TEXT, Theme.FIELD));
        String shown = masked(input.text());
        int avail = field.width() - 3;
        String clipped = CharWidth.of(shown) > avail ? CharWidth.substringByWidthFromEnd(shown, avail) : shown;
        putSafe(buf, field.x() + 1, field.y(), shown.isEmpty() ? t("Paste the key here") + "▏" : clipped + "▏",
                st(shown.isEmpty() ? Theme.DIM : Theme.TEXT, Theme.FIELD), field.right());
        int row = b.y() + 5;
        if (!message.isEmpty()) {
            for (String line : Wrap.lines(message, b.width())) {
                putSafe(buf, b.x(), row++, line, st(color, Theme.BG), b.right());
            }
            row++;
        }
        String where = services.keyFile().isEmpty() ? t("on this computer") : t("in {0}", services.keyFile());
        for (String line : Wrap.lines(t("It is saved only on this computer, {0}, readable by your account only. It is never put in a project, and it is sent nowhere except to {1} ({2}).", where, provider.name(), provider.url()), b.width())) {
            putSafe(buf, b.x(), row++, line, st(Theme.FAINT, Theme.BG), b.right());
        }
        row++;
        putSafe(buf, b.x(), row, t("You can also set {0} in your environment instead; that always wins over a saved key.", provider.keyEnv()),
                st(Theme.FAINT, Theme.BG), b.right());
    }
}
