package dev.buildcli.cli;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.Events;
import dev.buildcli.application.Orchestrator;
import dev.buildcli.application.RunAborted;
import dev.buildcli.application.ToolRuntime;
import dev.buildcli.application.TrustGate;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.AgentUsage;
import dev.buildcli.domain.Chat;
import dev.buildcli.domain.ModelRef;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.domain.Roster;
import dev.buildcli.infrastructure.ConsoleUi;
import dev.buildcli.infrastructure.FileTrustStore;
import dev.buildcli.infrastructure.ProviderSettings;
import dev.buildcli.infrastructure.RoutingGateway;
import dev.buildcli.infrastructure.StateStore;
import dev.buildcli.ports.ConfigRepository;
import dev.buildcli.ports.UserInterface;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "run", description = "Open the chat, or with --headless send one request to an agent or a group")
final class RunCommand implements Callable<Integer> {
    private final CliContext ctx;

    @Option(names = "--group", description = "Send the request to a group (default: the only group, if there is one)")
    String groupName;

    @Option(names = "--agent", description = "Send the request to one agent")
    String agentName;

    @Option(names = "--model", description = "Model for agents that have none, as provider:model (e.g. openrouter:openrouter/free)")
    String model;

    @Option(names = "--headless", description = "Plain-text output, no TUI (also used when there is no terminal)")
    boolean headless;

    @Option(names = "--approve", description = "How approvals are decided without the TUI: ask (prompt), none, writes or all. Default: ask on a terminal, none otherwise.")
    ConsoleUi.Policy approve;

    @Option(names = "--no-stream", description = "Wait for each complete reply instead of streaming")
    boolean noStream;

    @Option(names = "--threads", description = "Ollama num_thread (default 4; Ollama's own default of 16 was ~50x slower on a WSL2 box)")
    Integer threads;

    @Option(names = "--temperature", description = "Sampling temperature (default 0)")
    Double temperature;

    @Parameters(arity = "0..*", paramLabel = "REQUEST", description = "What the agents should do (asked for in the TUI if omitted)")
    List<String> request;

    private final AtomicInteger exit = new AtomicInteger();

    RunCommand(CliContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Integer call() throws Exception {
        ConfigRepository config = ctx.loadConfig();
        if (config == null) {
            return 2;
        }
        if (agentName != null && groupName != null) {
            ctx.err.println("error: use either --group or --agent, not both");
            return 2;
        }
        ModelRef fallback = null;
        if (model != null) {
            try {
                fallback = BuildCli.parseModel(model);
            } catch (IllegalArgumentException e) {
                ctx.err.println("error: " + e.getMessage());
                return 2;
            }
        }
        ProviderSettings settings = ctx.providerSettings().with(threads, temperature, !noStream);
        ChatSetup setup = new ChatSetup(ctx, config);
        final ModelRef fallbackModel = fallback;
        // read for every run, so a model chosen on the settings screen applies to the next message
        java.util.function.Supplier<RoutingGateway> gateways = () -> new RoutingGateway(setup.routing(), fallbackModel,
                ref -> ctx.gateways.create(ref, settings));
        String text = request == null ? "" : String.join(" ", request).strip();
        if (!headless && ctx.terminal) {
            return chat(config, setup, gateways, text);
        }
        if (text.isEmpty()) {
            ctx.err.println("error: a request is required without the TUI, for example: buildcli run --headless \"add a health endpoint\"");
            return 2;
        }
        Roster roster = resolveRoster(config, setup);
        if (roster == null) {
            return 2;
        }
        RoutingGateway gateway = gateways.get();
        for (Agent a : roster.agents()) {
            try {
                gateway.modelFor(a);
            } catch (IllegalStateException e) {
                ctx.err.println("error: " + e.getMessage());
                return 2;
            }
        }
        ConsoleUi.Policy policy = approve != null ? approve : ctx.terminal ? ConsoleUi.Policy.ASK : ConsoleUi.Policy.NONE;
        execute(roster, config, gateway, text, new ConsoleUi(ctx.out, ctx.in, policy));
        return exit.get();
    }

    /** The chat UI. It opens with any number of agents, even none: they can be created from its settings screen. */
    private int chat(ConfigRepository config, ChatSetup setup, java.util.function.Supplier<RoutingGateway> gateways, String text)
            throws Exception {
        Map<String, String> models = new LinkedHashMap<>();
        for (Agent a : config.agents()) {
            try {
                ModelRef ref = gateways.get().modelFor(a);
                models.put(a.name(), ref.provider() + ":" + ref.model());
            } catch (IllegalStateException e) {
                models.put(a.name(), "no model: type /connect");
            }
        }
        // one lock for the whole chat: agents working in parallel share it
        var workspaceLock = new dev.buildcli.application.tools.WorkspaceLock();
        ChatServices services = new ChatServices(ctx, config, setup);
        // the conversation history lives in the project's state database, next to runs and events
        try (StateStore history = ctx.openState()) {
            ChatSession session = new ChatSession(config.agents(), setup.groups(), setup.limits(),
                    (chatRoster, req, ui, cancelled, dispatcher) -> runOnce(chatRoster, config, gateways.get(), req, ui, cancelled, dispatcher,
                            workspaceLock, history),
                    setup.store, () -> setup.settings.number(dev.buildcli.application.Settings.AGENT_HOPS, 6), history);
            services.attach(session);
            session.workspace(ctx.cwd, workspaceLock);
            try {
                if (!text.isEmpty()) {
                    Chat target = groupName == null ? null : setup.group(groupName);
                    session.submit(text, List.of(), agentName != null ? agentName : target != null ? target.id() : null);
                }
                ctx.tui.launch(session, models, services);
            } finally {
                session.close();
            }
        }
        return 0;
    }

    /** Headless: --agent talks to one agent; --group to a group; otherwise the only group, or the only agent. */
    private Roster resolveRoster(ConfigRepository config, ChatSetup setup) {
        if (agentName != null) {
            Agent a = config.agent(agentName).orElse(null);
            if (a == null) {
                ctx.err.println("error: no agent named '" + agentName + "'. Available: " + config.agents().stream().map(Agent::name).toList());
                return null;
            }
            return new Roster(a.name(), a.name(), List.of(a), setup.limits(), setup.routing());
        }
        List<Chat> groups = setup.groups();
        if (groupName != null) {
            Chat g = setup.group(groupName);
            if (g == null || g.members().isEmpty()) {
                ctx.err.println("error: no group named '" + groupName + "'. Available: " + groups.stream().map(Chat::name).toList());
                return null;
            }
            return setup.rosterOf(g);
        }
        if (config.agents().isEmpty()) {
            ctx.err.println("error: there are no agents yet. Run 'buildcli init' for sample agents, or 'buildcli agent create <name>'.");
            return null;
        }
        List<Chat> usable = groups.stream().filter(g -> !g.members().isEmpty()).toList();
        if (usable.size() == 1) {
            return setup.rosterOf(usable.get(0));
        }
        if (usable.isEmpty() && config.agents().size() == 1) {
            Agent a = config.agents().get(0);
            return new Roster(a.name(), a.name(), List.of(a), setup.limits(), setup.routing());
        }
        ctx.err.println("error: choose who to ask with --agent " + config.agents().stream().map(Agent::name).toList()
                + (usable.isEmpty() ? "" : " or --group " + usable.stream().map(Chat::name).toList()));
        return null;
    }

    /** One request, start to finish: trust check, a fresh run id and store, the orchestrator. Throws if it cannot run. */
    private Task runOnce(Roster roster, ConfigRepository config, RoutingGateway gateway, Orchestrator.Request request, UserInterface ui,
            java.util.function.BooleanSupplier cancelled, Orchestrator.Dispatcher dispatcher,
            dev.buildcli.application.tools.WorkspaceLock workspaceLock, StateStore shared) throws Exception {
        String runId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-"
                + HexFormat.of().formatHex(randomBytes());
        StateStore store = shared != null ? shared : ctx.openState();
        try {
            if (!TrustGate.ensureTrusted(roster, config, new FileTrustStore(ctx.trustFile()), ctx.projectKey(), ui)) {
                throw new IllegalStateException("the project's agent definitions were not trusted, so nothing was run");
            }
            Events events = new Events(store, runId, ui);
            Orchestrator orchestrator = new Orchestrator(roster, gateway, new ToolRuntime(ctx.cwd, ui, events, workspaceLock).protect(List.of(ctx.globalDir())), ui, events,
                    config.projectContext());
            orchestrator.cancelWhen(cancelled);
            orchestrator.dispatchWith(dispatcher);
            Task root = orchestrator.run(request);
            lastRunId = runId;
            lastUsage = store.usage(runId);
            return root;
        } finally {
            if (shared == null) {
                store.close();
            }
        }
    }

    private String lastRunId;
    private List<AgentUsage> lastUsage = List.of();

    /** Headless: runs on the calling thread and records the exit code in {@link #exit}. */
    private void execute(Roster roster, ConfigRepository config, RoutingGateway gateway, String requestText, UserInterface ui) {
        try {
            Task root = runOnce(roster, config, gateway, new Orchestrator.Request(requestText), ui, () -> false,
                    Orchestrator.Dispatcher.INLINE, new dev.buildcli.application.tools.WorkspaceLock(), null);
            exit.set(root.status == TaskStatus.DONE ? 0 : 1);
            summarize(root, lastRunId, lastUsage);
        } catch (RunAborted e) {
            exit.set(1);
            ctx.out.println();
            ctx.out.println("Run aborted: " + e.getMessage());
        } catch (Exception e) {
            exit.set(1);
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            ctx.err.println(msg.startsWith("the project's agent definitions were not trusted")
                    ? "The project's agent definitions were not trusted, so nothing was run." : "error: " + msg);
        }
    }

    private void summarize(Task root, String runId, List<AgentUsage> usage) {
        ctx.out.println();
        ctx.out.println("Run " + runId + ": " + (root.status == TaskStatus.DONE ? "done" : "failed"));
        ctx.out.println();
        ctx.out.println(root.result == null ? "(no result)" : root.result);
        long in = usage.stream().mapToLong(AgentUsage::inputTokens).sum();
        long out = usage.stream().mapToLong(AgentUsage::outputTokens).sum();
        ctx.out.println();
        ctx.out.println("Tokens: " + in + " in / " + out + " out.   See: buildcli task list, buildcli usage");
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private static byte[] randomBytes() {
        byte[] b = new byte[2];
        RANDOM.nextBytes(b);
        return b;
    }
}
