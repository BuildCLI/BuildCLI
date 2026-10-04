package dev.buildcli.cli;

import dev.buildcli.application.Settings;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Chat;
import dev.buildcli.domain.Limits;
import dev.buildcli.domain.ModelRef;
import dev.buildcli.domain.ModelRouting;
import dev.buildcli.domain.Roster;
import dev.buildcli.infrastructure.FileChatStore;
import dev.buildcli.infrastructure.FileSettingsStore;
import dev.buildcli.ports.ConfigRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * BuildCLI is about agents: they are the people you talk to, and groups are chats you put them in. This gathers what a chat
 * needs from the project: the agents, the groups, the model for each agent and the limits.
 */
final class ChatSetup {
    final CliContext ctx;
    final ConfigRepository config;
    final Settings settings;
    final FileChatStore store;

    ChatSetup(CliContext ctx, ConfigRepository config) {
        this.ctx = ctx;
        this.config = config;
        this.settings = new Settings(new FileSettingsStore(ctx.globalDir(), ctx.projectStateDir()));
        this.store = new FileChatStore(ctx.projectStateDir());
    }

    static String groupId(String name) {
        return "#" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(?:^-)|(?:-$)", "");
    }

    /** The groups saved for this project. */
    List<Chat> groups() {
        List<Chat> all = new ArrayList<>();
        for (Chat g : store.load()) {
            if (!g.id().isEmpty()) {
                all.add(g);
            }
        }
        return all;
    }

    Chat group(String nameOrId) {
        for (Chat g : groups()) {
            if (g.id().equals(nameOrId) || g.name().equalsIgnoreCase(nameOrId) || g.id().equals(groupId(nameOrId))) {
                return g;
            }
        }
        return null;
    }

    /** The limits of a run: steps, depth, handoffs, retries and tokens. */
    Limits limits() {
        return Limits.defaults();
    }

    /** The model of each agent: the settings screen's choice, else the default model from the settings (--model fills any gap). */
    ModelRouting routing() {
        Map<String, ModelRef> overrides = new HashMap<>();
        for (Agent a : config.agents()) {
            String m = settings.modelFor(a.name());
            if (m != null) {
                try {
                    overrides.put(a.name(), BuildCli.parseModel(m));
                } catch (IllegalArgumentException ignored) {
                    // an unparsable value is shown on the settings screen; the other sources still apply
                }
            }
        }
        ModelRef def = null;
        if (settings.defaultModel() != null) {
            try {
                def = BuildCli.parseModel(settings.defaultModel());
            } catch (IllegalArgumentException ignored) {
                // as above
            }
        }
        return new ModelRouting(def, overrides);
    }

    /** The group as the roster of one run: its members, led by its first admin. */
    Roster rosterOf(Chat g) {
        List<Agent> members = new ArrayList<>();
        for (String m : g.members()) {
            config.agent(m).ifPresent(members::add);
        }
        String lead = g.admins().isEmpty() ? g.members().get(0) : g.admins().get(0);
        return new Roster(g.name(), lead, members, limits(), routing());
    }
}
