package dev.buildcli.infrastructure.tui;

import dev.buildcli.application.ChatSession;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.BindingSets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/** Runs the chat screen until the user quits. Mouse capture can be turned off with BUILDCLI_MOUSE=0 (e.g. to select text freely). */
public final class ChatApp extends ToolkitApp {
    private final ChatSession session;
    private final ChatScreen screen;
    private final boolean mouse;
    private final SettingsServices services;
    private final Attention attention = new Attention();
    private volatile Redraw redraw;
    private final java.util.List<String> themeProblems;

    public ChatApp(ChatSession session, Map<String, String> models, Path cwd, boolean mouse) {
        this(session, models, cwd, mouse, ChatScreen.basicServices(session, models));
    }

    public ChatApp(ChatSession session, Map<String, String> models, Path cwd, boolean mouse, SettingsServices services) {
        this.session = session;
        this.mouse = mouse;
        this.services = services;
        // your own colours, if there is a theme.css; a broken one is reported on the first screen and ignored
        ThemeFile.Result theme = ThemeFile.load(dev.buildcli.infrastructure.StateLocations
                .globalDir(System.getenv(), Path.of(System.getProperty("user.home"))).resolve("theme.css"));
        Theme.customise(theme.colours());
        this.themeProblems = theme.problems();
        this.screen = new ChatScreen(session, models, cwd, this::leave, services);
    }

    @Override
    protected TuiConfig configure() {
        // no fixed tick: the screen is drawn when you type or click, and when the chat changes (see onStart)
        // Tab and Shift+Tab move focus in the toolkit and never reach the screen; there is one focusable here, and Tab completes / and @
        TuiConfig.Builder config = TuiConfig.builder().noTick().mouseCapture(mouse).bracketedPaste(true)
                .bindings(BindingSets.defaults().toBuilder().unbind(Actions.FOCUS_NEXT).unbind(Actions.FOCUS_PREVIOUS).build());
        if (WindowsBackend.isWindows()) {
            WindowsBackend backend = WindowsBackend.open(mouse);
            if (backend != null) {
                config.backend(backend);
            }
        }
        return config.build();
    }

    @Override
    protected void onStart() {
        setWindowTitle("BuildCLI");
        runner().focusManager().setFocus("chat");
        // OSC 52 copy: written on the thread that draws, like the title and the bell
        screen.rawOutput(text -> runner().runOnRenderThread(() -> {
            try {
                runner().tuiRunner().backend().writeRaw(text);
                runner().tuiRunner().backend().flush();
            } catch (java.io.IOException e) {
                // the copy then simply does not happen; the chat says so
            }
        }));
        // Nothing polls: the session calls us when something changes and the screen when it needs a frame (a toast, a
        // spinner starting). Idle, the program does no work at all; see Redraw.
        long start = System.nanoTime();
        redraw = new Redraw(() -> {
            long ms = (System.nanoTime() - start) / 1_000_000;
            runner().tuiRunner().dispatch(dev.tamboui.tui.event.TickEvent.of(ms, Duration.ofMillis(ms)));
            watchAttention(start);
        }, runner().tuiRunner().scheduler(), screen::animating);
        screen.redraw(redraw::request);
        session.onChange(redraw::request);
        if (!themeProblems.isEmpty()) {
            screen.notice("theme.css: " + themeProblems.get(0) + (themeProblems.size() > 1 ? " (+" + (themeProblems.size() - 1) + " more)" : ""));
        }
        redraw.request();
    }

    /** The window title and the bell, written on the thread that draws so they cannot cut a frame in two. */
    private void watchAttention(long start) {
        var pending = session.pending();
        Attention.Signal signal = attention.update((System.nanoTime() - start) / 1_000_000, session.pendingCount(),
                pending == null ? null : pending.agent(), session.busy(), services.settings().flag(dev.buildcli.application.Settings.BELL));
        if (signal.title() == null && !signal.bell()) {
            return;
        }
        runner().runOnRenderThread(() -> {
            if (signal.title() != null) {
                setWindowTitle(signal.title());
            }
            if (signal.bell()) {
                var backend = runner().tuiRunner().backend();
                try {
                    backend.writeRaw("\u0007");
                    backend.flush();
                } catch (java.io.IOException e) {
                    // a failed bell is not worth interrupting the chat for
                }
            }
        });
    }

    @Override
    protected Element render() {
        Redraw r = redraw;
        if (r != null) {
            r.frameDrawn();
        }
        return screen;
    }

    private void leave() {
        session.close();
        quit();
    }
}
