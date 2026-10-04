package dev.buildcli.cli;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.ModelRef;
import dev.buildcli.infrastructure.OllamaProbe;
import dev.buildcli.infrastructure.ProviderSettings;
import dev.buildcli.ports.ConfigException;
import dev.buildcli.ports.ConfigRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(name = "doctor", description = "Check Java, git, the configuration, the state directory and the model providers")
final class DoctorCommand implements Callable<Integer> {
    private final CliContext ctx;
    private boolean failed;

    DoctorCommand(CliContext ctx) {
        this.ctx = ctx;
    }

    private void ok(String message) {
        ctx.out.println("  ok    " + message);
    }

    private void warn(String message) {
        ctx.out.println("  warn  " + message);
    }

    private void fail(String message) {
        failed = true;
        ctx.out.println("  FAIL  " + message);
    }

    @Override
    public Integer call() {
        int java = Runtime.version().feature();
        if (java >= 21) {
            ok("Java " + java);
        } else {
            fail("Java " + java + " found, BuildCLI needs 21 or newer");
        }
        if (ctx.terminal) {
            ok("interactive terminal: the TUI is available");
        } else {
            warn("no interactive terminal: use 'buildcli run --headless'");
        }
        checkGit();
        checkState();
        ConfigRepository config = checkConfig();
        ProviderSettings settings = ctx.providerSettings();
        boolean usesOllama = true;
        java.util.TreeSet<String> cloud = new java.util.TreeSet<>();
        java.util.TreeSet<String> wantedOllama = new java.util.TreeSet<>();
        if (config != null && !config.agents().isEmpty()) {
            usesOllama = false;
            var routing = new ChatSetup(ctx, config).routing();
            for (Agent a : config.agents()) {
                ModelRef ref = routing.forAgent(a.name());
                if (ref == null) {
                    warn("agent '" + a.name() + "' has no model: choose one in the chat's settings (F2 > Models) or pass --model");
                } else if (ref.provider().equals("ollama")) {
                    usesOllama = true;
                    wantedOllama.add(ref.model());
                } else {
                    cloud.add(ref.provider());
                }
            }
        }
        if (usesOllama) {
            checkOllama(settings, wantedOllama);
        }
        for (String name : cloud) {
            var spec = settings.registry().find(name).orElse(null);
            if (spec == null) {
                fail("an agent uses provider '" + name + "', which is not known (see 'buildcli provider list')");
            } else if (spec.needsKey() && ctx.env.getOrDefault(spec.apiKeyEnv(), "").isBlank()) {
                warn("an agent uses '" + name + "' but it has no key: run 'buildcli provider login " + name + "' (or set " + spec.apiKeyEnv() + ")");
            } else {
                ok("provider '" + name + "' is configured (" + spec.baseUrl() + ")");
            }
        }
        ctx.out.println();
        ctx.out.println(failed ? "Some checks failed." : "Everything needed is in place.");
        return failed ? 1 : 0;
    }

    private void checkGit() {
        // git is built in (JGit): the tools never start a git program, so there is nothing to find on the PATH
        ok("git support is built in; no git program is needed");
    }

    private void checkState() {
        try {
            Files.createDirectories(ctx.stateDb().getParent());
            if (Files.isWritable(ctx.stateDb().getParent())) {
                ok("state directory is writable: " + ctx.stateDb().getParent());
            } else {
                fail("state directory is not writable: " + ctx.stateDb().getParent());
            }
        } catch (IOException e) {
            fail("cannot create the state directory " + ctx.stateDb().getParent() + ": " + e.getMessage());
        }
    }

    private ConfigRepository checkConfig() {
        try {
            var config = new dev.buildcli.infrastructure.FileConfigRepository(ctx.cwd, ctx.globalDir());
            if (config.agents().isEmpty()) {
                warn("no agents yet: run 'buildcli init' for three sample agents, or create one in the chat (F2 > Agents)");
            } else {
                ok(config.agents().size() + " agent(s) loaded, configuration is valid");
            }
            return config;
        } catch (ConfigException e) {
            fail("the configuration has " + e.problems().size() + " problem(s):");
            e.problems().forEach(p -> ctx.out.println("          - " + p));
            return null;
        }
    }

    private void checkOllama(ProviderSettings settings, java.util.Set<String> wanted) {
        Optional<List<String>> models = OllamaProbe.listModels(settings.ollamaUrl(), Duration.ofSeconds(2));
        if (models.isEmpty()) {
            warn("Ollama is not reachable at " + settings.ollamaUrl() + " (only needed for the ollama provider; set OLLAMA_HOST if it runs elsewhere)");
            return;
        }
        ok("Ollama at " + settings.ollamaUrl() + " has " + models.get().size() + " model(s): " + String.join(", ", models.get()));
        for (String m : wanted) {
            boolean present = models.get().stream().anyMatch(n -> n.equals(m) || n.equals(m + ":latest"));
            if (present) {
                ok("model '" + m + "' is available");
            } else {
                warn("model '" + m + "' is not pulled: run 'ollama pull " + m + "'");
            }
        }
    }
}
