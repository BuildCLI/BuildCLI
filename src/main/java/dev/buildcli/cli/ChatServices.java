package dev.buildcli.cli;

import dev.buildcli.application.Settings;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.domain.ModelRef;
import dev.buildcli.domain.Origin;
import dev.buildcli.infrastructure.FileCredentialStore;
import dev.buildcli.infrastructure.ModelCatalog;
import dev.buildcli.infrastructure.ProviderProbe;
import dev.buildcli.infrastructure.ProviderRegistry;
import dev.buildcli.infrastructure.ProviderSettings;
import dev.buildcli.infrastructure.ProviderSpec;
import dev.buildcli.infrastructure.tui.SettingsServices;
import dev.buildcli.ports.ConfigRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/** The settings screen's view of the machine: settings files, providers, the model catalog and agent files. */
final class ChatServices implements SettingsServices {
    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9_-]{0,39}");

    private final CliContext ctx;
    private final ConfigRepository config;
    private final ChatSetup setup;
    private final Settings settings;
    private final java.util.Map<String, AgentInfo> created = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<String> deleted = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile ModelCatalog catalog;
    private volatile dev.buildcli.application.ChatSession session;

    ChatServices(CliContext ctx, ConfigRepository config, ChatSetup setup) {
        this.ctx = ctx;
        this.config = config;
        this.setup = setup;
        this.settings = setup.settings;
    }

    /** Agents created or deleted on the settings screen join or leave the open chat at once. */
    void attach(dev.buildcli.application.ChatSession session) {
        this.session = session;
    }

    @Override
    public Settings settings() {
        return settings;
    }

    private ProviderSettings providerSettings() {
        return ctx.providerSettings();
    }

    @Override
    public List<Provider> providers() {
        List<String> mine = ProviderRegistry.userDefined(ctx.globalDir());
        List<Provider> out = new ArrayList<>();
        for (ProviderSpec s : providerSettings().registry().all()) {
            boolean keySet = s.needsKey() && !ctx.env.getOrDefault(s.apiKeyEnv(), "").isBlank();
            // a set variable wins over a saved key, so that is what the key is "from" when both exist
            boolean fromEnvironment = s.needsKey() && !ctx.processEnv.getOrDefault(s.apiKeyEnv(), "").isBlank();
            String from = !keySet ? "" : fromEnvironment ? "environment" : "saved";
            out.add(new Provider(s.name(), s.baseUrl(), s.apiKeyEnv(), keySet, mine.contains(s.name()) && !ProviderRegistry.isBuiltIn(s.name()),
                    s.description() == null ? "" : s.description(), from));
        }
        return out;
    }

    @Override
    public void addProvider(String name, String url, String keyEnv) throws Exception {
        ProviderRegistry.save(ctx.globalDir(), new ProviderSpec(name, ProviderSpec.Kind.OPENAI_COMPATIBLE, url, keyEnv, ""));
        catalog = null;
    }

    @Override
    public void removeProvider(String name) throws Exception {
        if (!ProviderRegistry.remove(ctx.globalDir(), name)) {
            throw new IllegalArgumentException(name + " is built in or not in your providers file");
        }
        catalog = null;
    }

    @Override
    public CompletableFuture<String> test(String model) {
        return CompletableFuture.supplyAsync(() -> {
            ModelRef ref;
            try {
                ref = BuildCli.parseModel(model);
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
            var o = ProviderProbe.test(ref, r -> ctx.gateways.create(r, providerSettings().with(null, null, false)));
            return o.ok() ? "ok " + o.millis() + " ms: " + o.text() : "failed after " + o.millis() + " ms: " + o.text();
        });
    }

    @Override
    public CompletableFuture<ModelCatalog.Result> models(String provider) {
        ModelCatalog c = catalog;
        if (c == null) {
            c = new ModelCatalog(providerSettings());
            catalog = c;
        }
        return c.models(provider);
    }

    @Override
    public void saveKey(String provider, String key) throws Exception {
        ProviderSpec spec = providerSettings().registry().find(provider).orElseThrow(() -> new IllegalArgumentException("unknown provider " + provider));
        if (!spec.needsKey()) {
            throw new IllegalArgumentException(provider + " needs no key");
        }
        ctx.credentials.put(spec.apiKeyEnv(), key);
        catalog = null;
    }

    @Override
    public CompletableFuture<ModelCatalog.Result> checkKey(String provider, String key) {
        ProviderSpec spec = providerSettings().registry().find(provider).orElse(null);
        if (spec == null || !spec.needsKey()) {
            return CompletableFuture.completedFuture(new ModelCatalog.Result(List.of(), "unknown provider or no key needed"));
        }
        // a catalog of its own, with only this key in its environment: nothing is saved and nothing is cached
        var settings = ProviderSettings.fromEnvironment(Map.of(spec.apiKeyEnv(), FileCredentialStore.clean(key)), ctx.globalDir());
        return new ModelCatalog(settings).models(provider);
    }

    @Override
    public boolean forgetKey(String provider) throws Exception {
        ProviderSpec spec = providerSettings().registry().find(provider).orElseThrow(() -> new IllegalArgumentException("unknown provider " + provider));
        boolean removed = spec.needsKey() && ctx.credentials.remove(spec.apiKeyEnv());
        catalog = null;
        return removed;
    }

    @Override
    public String keyFile() {
        return ctx.credentials.file().toString();
    }

    @Override
    public void refreshModels() {
        catalog = null;
    }

    @Override
    public List<AgentInfo> agents() {
        List<AgentInfo> out = new ArrayList<>();
        for (Agent a : config.agents()) {
            if (!deleted.contains(a.name()) && !created.containsKey(a.name())) {
                out.add(new AgentInfo(a.name(), a.role(), switch (a.origin()) {
                    case PROJECT -> "this project";
                    case GLOBAL -> "all projects";
                    default -> "built in";
                }, a.source(), a.capabilities().stream().sorted().toList()));
            }
        }
        created.values().stream().filter(a -> !deleted.contains(a.name())).forEach(out::add);
        return out;
    }

    @Override
    public List<String> createSampleAgents() throws Exception {
        return createSampleAgents(SampleKits.DEV.key());
    }

    @Override
    public List<String> sampleTeams() {
        return SampleKits.ALL.stream().map(k -> k.key() + ": " + k.title() + " (" + k.about() + ")").toList();
    }

    @Override
    public List<String> createSampleAgents(String kitKey) throws Exception {
        SampleKits.Kit kit = SampleKits.find(kitKey);
        if (kit == null) {
            throw new IllegalArgumentException("there is no sample team '" + kitKey + "'; the ones I have are "
                    + String.join(", ", SampleKits.ALL.stream().map(SampleKits.Kit::key).toList()));
        }
        SampleKits.writeFiles(ctx.cwd, kit, msg -> { });
        retrust();
        var fresh = new dev.buildcli.infrastructure.FileConfigRepository(ctx.cwd, ctx.globalDir()); // reads exactly what was written
        List<String> added = new ArrayList<>();
        var s = session;
        for (String name : kit.names()) {
            Agent agent = fresh.agent(name).orElse(null);
            boolean known = (config.agent(name).isPresent() || created.containsKey(name)) && !deleted.contains(name);
            if (agent == null || known) {
                continue;
            }
            deleted.remove(name);
            created.put(name, new AgentInfo(name, agent.role(), "this project", agent.source(), agent.capabilities().stream().sorted().toList()));
            added.add(name);
            if (s != null) {
                s.addContact(agent);
            }
        }
        if (added.isEmpty()) {
            throw new IllegalArgumentException("those sample agents are already here");
        }
        if (s != null && s.group("#" + kit.group()) == null) {
            s.createGroup(kit.group(), kit.names()); // the first member becomes its admin
        }
        return added;
    }

    @Override
    public String createAgent(String name, String role, String instructions, List<String> capabilities, boolean global) throws Exception {
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("the name must be lowercase letters, digits, - and _");
        }
        List<String> unknown = capabilities.stream().filter(c -> !Capability.KNOWN.contains(c)).toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("unknown capabilities " + unknown);
        }
        if ((config.agent(name).isPresent() || created.containsKey(name)) && !deleted.contains(name)) {
            throw new IllegalArgumentException("there is already an agent named " + name);
        }
        Path file = (global ? ctx.globalDir() : ctx.cwd.resolve(".buildcli")).resolve("agents").resolve(name + ".md");
        Files.createDirectories(file.getParent());
        List<String> caps = capabilities.isEmpty() ? List.of(Capability.FILESYSTEM_READ, Capability.SEARCH) : capabilities;
        String r = role.isBlank() ? "assistant" : role;
        Files.writeString(file, AgentCommand.CreateCmd.template(name, r, caps, instructions), StandardCharsets.UTF_8);
        deleted.remove(name);
        retrust();
        created.put(name, new AgentInfo(name, r, global ? "all projects" : "this project", file.toString(), caps.stream().sorted().toList()));
        var s = session;
        if (s != null) {
            // the same agent the file describes: read everything, write nothing, run nothing without asking
            s.addContact(new Agent(name, r, instructions == null || instructions.isBlank() ? "You help the user with this project." : instructions,
                    java.util.Set.copyOf(caps), new dev.buildcli.domain.Permissions(dev.buildcli.domain.Permissions.READ_EVERYTHING, List.of(),
                            List.of(), java.time.Duration.ofMinutes(2)), global ? Origin.GLOBAL : Origin.PROJECT, file.toString()));
        }
        return file.toString();
    }

    /** The file of an agent the user may change: inside the agents folders of this project or of the user, never anywhere else. */
    private Path agentFile(String name, String doing) {
        String source;
        if (created.containsKey(name)) {
            source = created.get(name).file();
        } else {
            Agent a = config.agent(name).orElseThrow(() -> new IllegalArgumentException("no agent named " + name));
            if (a.origin() == Origin.BUILTIN || a.source() == null || a.source().isBlank()) {
                throw new IllegalArgumentException(name + " is built in and cannot be " + doing);
            }
            source = a.source();
        }
        Path file = Path.of(source).toAbsolutePath().normalize();
        Path projectAgents = ctx.cwd.resolve(".buildcli").resolve("agents").toAbsolutePath().normalize();
        Path globalAgents = ctx.globalDir().resolve("agents").toAbsolutePath().normalize();
        if (!file.startsWith(projectAgents) && !file.startsWith(globalAgents)) {
            throw new IllegalArgumentException("refusing to touch a file outside the agents folders: " + file);
        }
        return file;
    }

    @Override
    public void updateAgent(String name, dev.buildcli.infrastructure.AgentFileEditor.Edit edit) throws Exception {
        Path file = agentFile(name, "edited");
        String updated = dev.buildcli.infrastructure.AgentFileEditor.apply(Files.readString(file, StandardCharsets.UTF_8), edit);
        Files.writeString(file, updated, StandardCharsets.UTF_8);
        retrust();
        var fresh = new dev.buildcli.infrastructure.FileConfigRepository(ctx.cwd, ctx.globalDir()); // reads exactly what was written
        Agent agent = fresh.agent(name).orElseThrow(() -> new IllegalStateException("the edited file no longer loads; see " + file));
        created.put(name, new AgentInfo(name, agent.role(), agent.origin() == Origin.GLOBAL ? "all projects" : "this project", file.toString(),
                agent.capabilities().stream().sorted().toList()));
        var s = session;
        if (s != null) {
            s.addContact(agent);
        }
    }

    @Override
    public void deleteAgent(String name) throws Exception {
        Path file = agentFile(name, "deleted");
        Files.deleteIfExists(file);
        retrust();
        deleted.add(name);
        created.remove(name);
        var s = session;
        if (s != null) {
            s.removeContact(name);
        }
    }

    /**
     * The user just made this change on the settings screen, so it is trusted, like the files 'init' writes. Only if
     * the project was trusted before the change: an untrusted project stays untrusted.
     */
    private void retrust() {
        var trust = new dev.buildcli.infrastructure.FileTrustStore(ctx.trustFile());
        if (!trust.isTrusted(ctx.projectKey(), config.projectDigest()) && !config.projectDigest().isEmpty()) {
            return;
        }
        try {
            var fresh = new dev.buildcli.infrastructure.FileConfigRepository(ctx.cwd, ctx.globalDir());
            if (!fresh.projectDigest().isEmpty()) {
                trust.trust(ctx.projectKey(), fresh.projectDigest());
            }
        } catch (RuntimeException e) {
            // an invalid project file: nothing is trusted, and the next run reports the problem
        }
    }
}
