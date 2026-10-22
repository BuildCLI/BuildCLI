package dev.buildcli.cli;

import dev.buildcli.domain.ModelRef;
import dev.buildcli.application.ChatSession;
import dev.buildcli.infrastructure.FileConfigRepository;
import dev.buildcli.infrastructure.ProviderSettings;
import dev.buildcli.infrastructure.StateLocations;
import dev.buildcli.ports.ConfigException;
import dev.buildcli.ports.ConfigRepository;
import dev.buildcli.ports.LlmGateway;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Everything the commands need from the outside world, injected so they can be tested without a terminal, a model or
 * the user's real home directory.
 */
public final class CliContext {

    /** Builds the gateway for one provider/model pair. */
    public interface GatewayFactory {
        LlmGateway create(ModelRef ref, ProviderSettings settings);
    }

    /** Opens the chat UI on a session and blocks until the user quits. */
    public interface TuiLauncher {
        void launch(ChatSession session, Map<String, String> models, dev.buildcli.infrastructure.tui.SettingsServices services) throws Exception;
    }

    public final Path cwd;
    public final Path home;
    /** The environment plus the keys saved from inside BuildCLI; a variable that is set wins over a saved key. */
    public final Map<String, String> env;
    /** The environment as the process has it, without saved keys. */
    public final Map<String, String> processEnv;
    /** Keys typed into BuildCLI, in the user's own folder. */
    public final dev.buildcli.infrastructure.FileCredentialStore credentials;
    public final PrintStream out;
    public final PrintStream err;
    public final BufferedReader in;
    public final boolean terminal;
    public final GatewayFactory gateways;
    public final TuiLauncher tui;

    public CliContext(Path cwd, Path home, Map<String, String> env, PrintStream out, PrintStream err, BufferedReader in,
                      boolean terminal, GatewayFactory gateways, TuiLauncher tui) {
        this.cwd = cwd.toAbsolutePath().normalize();
        this.home = home;
        this.processEnv = env;
        this.credentials = new dev.buildcli.infrastructure.FileCredentialStore(
                StateLocations.globalDir(env, home).resolve(dev.buildcli.infrastructure.FileCredentialStore.FILE_NAME));
        this.env = new KeyedEnvironment(env, credentials);
        // everything printed may contain untrusted text (model replies, tool output): never let it drive the terminal
        this.out = dev.buildcli.infrastructure.SafePrintStream.wrap(out);
        this.err = dev.buildcli.infrastructure.SafePrintStream.wrap(err);
        this.in = in;
        this.terminal = terminal;
        this.gateways = gateways;
        this.tui = tui;
    }

    /** The real process: real streams, real environment, real terminal detection, real providers. */
    public static CliContext system() {
        return new CliContext(Path.of("").toAbsolutePath(), Path.of(System.getProperty("user.home")), System.getenv(),
                System.out, System.err, new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                isInteractiveTerminal(), (ref, settings) -> settings.gatewayFor(ref),
                (session, models, services) -> new dev.buildcli.infrastructure.tui.ChatApp(session, models, Path.of("").toAbsolutePath(),
                        !"0".equals(System.getenv("BUILDCLI_MOUSE")) && services.settings().flag(dev.buildcli.application.Settings.MOUSE),
                        services).run());
    }

    /** Providers (built in plus the user's providers.yaml) and the environment their keys are read from. */
    public ProviderSettings providerSettings() {
        return ProviderSettings.fromEnvironment(env, globalDir());
    }

    public Path globalDir() {
        return StateLocations.globalDir(processEnv, home);
    }

    public Path projectStateDir() {
        return StateLocations.projectStateDir(globalDir(), cwd);
    }

    /** Which engine keeps the state: BUILDCLI_STORAGE, else the setting (project over global), else SQLite. */
    public dev.buildcli.infrastructure.StateStore.Backend storageBackend() {
        String fromEnv = processEnv.get("BUILDCLI_STORAGE");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return dev.buildcli.infrastructure.StateStore.Backend.parse(fromEnv);
        }
        try {
            var settings = new dev.buildcli.application.Settings(new dev.buildcli.infrastructure.FileSettingsStore(globalDir(), projectStateDir()));
            return dev.buildcli.infrastructure.StateStore.Backend.parse(settings.get(dev.buildcli.application.Settings.STORAGE));
        } catch (RuntimeException e) {
            return dev.buildcli.infrastructure.StateStore.Backend.SQLITE;
        }
    }

    /** Opens the project's state database with the chosen engine; writes are batched, and closing waits for them. */
    public dev.buildcli.infrastructure.StateStore openState() {
        return dev.buildcli.infrastructure.StateStore.open(stateDb(), storageBackend(), true);
    }

    public Path stateDb() {
        return StateLocations.stateDb(globalDir(), cwd);
    }

    /** Identifies this project in the trust store. */
    public String projectKey() {
        try {
            return cwd.toRealPath().toString();
        } catch (java.io.IOException e) {
            return cwd.toString();
        }
    }

    public Path trustFile() {
        return globalDir().resolve("trust.json");
    }

    /** Loads the configuration, printing every problem and returning null if it is invalid. */
    public ConfigRepository loadConfig() {
        try {
            return new FileConfigRepository(cwd, globalDir());
        } catch (ConfigException e) {
            err.println("The configuration has " + e.problems().size() + " problem(s):");
            e.problems().forEach(p -> err.println("  - " + p));
            return null;
        }
    }

    public boolean hasState() {
        return switch (storageBackend()) {
            case SQLITE -> Files.isRegularFile(stateDb());
            case H2 -> Files.isRegularFile(stateDb().resolveSibling("state.mv.db"));
            case MEMORY -> false; // another process cannot see it, and this one has just started
        };
    }

    /**
     * Whether stdin and stdout are a real terminal. On JDK 22+ {@code System.console()} is non-null even when redirected,
     * so {@code Console.isTerminal()} is consulted by reflection (the project compiles for 21).
     */
    static boolean isInteractiveTerminal() {
        var console = System.console();
        if (console == null || "dumb".equals(System.getenv("TERM"))) {
            return false;
        }
        try {
            Method m = console.getClass().getMethod("isTerminal");
            m.setAccessible(true);
            return (Boolean) m.invoke(console);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return true; // JDK 21: a non-null console means a terminal
        }
    }
}
