package dev.buildcli.cli;

import dev.buildcli.domain.ModelRef;
import dev.buildcli.infrastructure.FileTrustStore;
import dev.buildcli.ports.ConfigRepository;
import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "init", description = "Create three sample agents (architect, developer, reviewer), a group with them, and an AGENTS.md")
final class InitCommand implements Callable<Integer> {
    private final CliContext ctx;

    @Option(names = "--model", description = "Default model for the agents as provider:model (default: ${DEFAULT-VALUE})",
            defaultValue = "ollama:qwen2.5:7b")
    String model;

    InitCommand(CliContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Integer call() throws IOException {
        ModelRef ref;
        try {
            ref = BuildCli.parseModel(model);
        } catch (IllegalArgumentException e) {
            ctx.err.println("error: " + e.getMessage());
            return 2;
        }
        int created = SampleAgents.writeFiles(ctx.cwd, msg -> ctx.out.println(msg));

        // the group and the model are this machine's choices for this project: kept in its state directory, not the repo
        var groups = new dev.buildcli.infrastructure.FileChatStore(ctx.projectStateDir());
        if (groups.load().isEmpty()) {
            groups.save(java.util.List.of(new dev.buildcli.domain.Chat("#" + SampleAgents.GROUP, SampleAgents.GROUP, true, SampleAgents.NAMES,
                    java.util.List.of(SampleAgents.NAMES.get(0)))));
            ctx.out.println("created  group '" + SampleAgents.GROUP + "' with " + String.join(", ", SampleAgents.NAMES) + " (" + SampleAgents.NAMES.get(0) + " is admin)");
        }
        var settings = new dev.buildcli.application.Settings(new dev.buildcli.infrastructure.FileSettingsStore(ctx.globalDir(), ctx.projectStateDir()));
        if (settings.stored(dev.buildcli.ports.SettingsStore.Scope.PROJECT, dev.buildcli.application.Settings.DEFAULT_MODEL) == null) {
            settings.set(dev.buildcli.ports.SettingsStore.Scope.PROJECT, dev.buildcli.application.Settings.DEFAULT_MODEL, ref.provider() + ":" + ref.model());
            ctx.out.println("set      default model " + ref.provider() + ":" + ref.model() + " for this project");
        }

        ConfigRepository config = ctx.loadConfig();
        if (config == null) {
            return 2;
        }
        // The user just asked for these files, so approving them is implied; anything added later asks again.
        if (created > 0 && !config.projectDigest().isEmpty()) {
            new FileTrustStore(ctx.trustFile()).trust(ctx.projectKey(), config.projectDigest());
        }
        ctx.out.println();
        ctx.out.println("Next steps:");
        ctx.out.println("  1. buildcli doctor                            check Java, git, the model provider and the config");
        ctx.out.println("  2. edit AGENTS.md and .buildcli/agents/*.md    describe your project and tune each agent's permissions");
        ctx.out.println("  3. buildcli                                    open the chat (F2 for settings: models, providers, agents)");
        ctx.out.println();
        ctx.out.println("The agents use " + ref.provider() + ":" + ref.model() + " unless you choose another model per agent in the settings"
                + " (F2 > Models); small models (3B) are unreliable for tool use.");
        return 0;
    }

}
