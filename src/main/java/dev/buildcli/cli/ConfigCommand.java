package dev.buildcli.cli;

import dev.buildcli.infrastructure.ProviderSettings;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(name = "config", description = "Show where BuildCLI reads its configuration and keeps its state")
final class ConfigCommand implements Callable<Integer> {
    private final CliContext ctx;

    ConfigCommand(CliContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Integer call() {
        ProviderSettings s = ctx.providerSettings();
        String v = BuildCli.class.getPackage().getImplementationVersion();
        ctx.out.println("buildcli " + (v == null ? "dev" : v));
        ctx.out.println();
        ctx.out.println("project directory : " + ctx.cwd);
        ctx.out.println("project config    : " + ctx.cwd.resolve(".buildcli") + "  (agents/), and AGENTS.md");
        ctx.out.println("global directory  : " + ctx.globalDir() + "  (override with BUILDCLI_HOME)");
        ctx.out.println("state database    : " + ctx.storageBackend().name().toLowerCase(java.util.Locale.ROOT) + " ("
                + (ctx.storageBackend() == dev.buildcli.infrastructure.StateStore.Backend.MEMORY ? "nothing kept after BuildCLI closes" : ctx.stateDb()) + ")");
        ctx.out.println("trust file        : " + ctx.trustFile());
        ctx.out.println("saved API keys    : " + ctx.credentials.file() + "  (only if you saved any: 'buildcli provider login <provider>')");
        ctx.out.println();
        ctx.out.println("providers         : " + ctx.globalDir().resolve(dev.buildcli.infrastructure.ProviderRegistry.FILE_NAME)
                + "  (your own; see 'buildcli provider list')");
        ctx.out.println("ollama            : " + s.ollamaUrl() + "  (OLLAMA_HOST)");
        for (var spec : s.registry().all()) {
            if (spec.needsKey() && !spec.name().equals("moonshot")) {
                String key = ctx.env.get(spec.apiKeyEnv());
                boolean fromEnvironment = !ctx.processEnv.getOrDefault(spec.apiKeyEnv(), "").isBlank();
                ctx.out.println(String.format("%-18s: %s", spec.apiKeyEnv(), key == null || key.isBlank() ? "not set"
                        : fromEnvironment ? "set in the environment (not shown)" : "saved by you (not shown)"));
            }
        }
        return 0;
    }
}
