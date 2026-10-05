package dev.buildcli;

import dev.buildcli.application.ChatSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import dev.buildcli.cli.BuildCli;
import dev.buildcli.cli.CliContext;
import dev.buildcli.domain.ModelRef;
import dev.buildcli.infrastructure.ScriptedGateway;
import dev.buildcli.ports.LlmGateway;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Drives the real commands through picocli with an injected context: no terminal, no model, no real home directory. */
class CliTest {
    @TempDir Path root;
    Path project;
    Path home;
    ByteArrayOutputStream outBytes;
    ByteArrayOutputStream errBytes;
    final List<ModelRef> modelsRequested = new java.util.ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        project = Files.createDirectories(root.resolve("project"));
        home = Files.createDirectories(root.resolve("home"));
    }

    record Result(int code, String out, String err) {}

    /** Where BuildCLI keeps this project's state (settings, groups, history): never inside the project. */
    Path stateDir() {
        return dev.buildcli.infrastructure.StateLocations.projectStateDir(home.resolve(".buildcli"), project);
    }

    Result cli(Supplier<LlmGateway> gateway, String stdin, Map<String, String> env, String... args) {
        outBytes = new ByteArrayOutputStream();
        errBytes = new ByteArrayOutputStream();
        var ctx = new CliContext(project, home, env, new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8), new BufferedReader(new StringReader(stdin)), false,
                (ref, settings) -> {
                    modelsRequested.add(ref);
                    return gateway.get();
                }, (session, models, services) -> {
                    throw new AssertionError("the TUI must not open in these tests");
                });
        int code = BuildCli.run(args, ctx);
        return new Result(code, outBytes.toString(StandardCharsets.UTF_8), errBytes.toString(StandardCharsets.UTF_8));
    }

    Result cli(String... args) {
        return cli(() -> {
            throw new AssertionError("no model expected");
        }, "", Map.of(), args);
    }

    /** wheslley hands off to matheus, who writes src/Hello.java and finishes. */
    static ScriptedGateway happyScript() {
        return new ScriptedGateway()
                .call("wheslley", "handoff", Map.of("to", "matheus", "objective", "add Hello", "brief", "keep it small"))
                .call("matheus", "write_file", Map.of("path", "src/Hello.java", "content", "class Hello {}\n"))
                .say("matheus", "Added src/Hello.java.")
                .say("wheslley", "Hello was added.");
    }

    // ---- init, agents, teams ----

    @Test
    void initCreatesSampleAgentsAndAGroupThatLoadAndAreAlreadyTrusted() throws Exception {
        Result r = cli("init");
        assertEquals(0, r.code, r.err);
        for (String f : List.of(".buildcli/agents/wheslley.md", ".buildcli/agents/matheus.md", ".buildcli/agents/breno.md", "AGENTS.md")) {
            assertTrue(Files.exists(project.resolve(f)), f);
        }
        assertFalse(Files.exists(project.resolve(".buildcli/teams")), "no team: agents, and a group kept outside the project");
        assertTrue(r.out.contains("Next steps") && r.out.contains("group 'maintainers'"), r.out);
        Path state = stateDir();
        assertTrue(Files.readString(state.resolve("chats.yaml")).contains("maintainers"));
        assertTrue(Files.readString(state.resolve("settings.yaml")).contains("ollama:qwen2.5:7b"));
        assertEquals(0, cli("agent", "list").code, "the generated files pass validation");
        assertTrue(Files.exists(home.resolve(".buildcli/trust.json")), "init approves what it just generated");
    }

    @Test
    void initNeverOverwritesExistingFiles() throws Exception {
        Files.writeString(project.resolve("AGENTS.md"), "my notes");
        Result r = cli("init", "--model", "openai:gpt-x");
        assertEquals(0, r.code);
        assertEquals("my notes", Files.readString(project.resolve("AGENTS.md")));
        assertTrue(r.out.contains("skipped  AGENTS.md"));
        assertTrue(Files.readString(stateDir().resolve("settings.yaml")).contains("openai:gpt-x"));
        assertEquals("skipped", cli("init").out.lines().filter(l -> l.contains("wheslley.md")).findFirst().orElseThrow().substring(0, 7));
    }

    @Test
    void initRejectsAModelWithoutAProvider() {
        Result r = cli("init", "--model", "justamodel");
        assertEquals(2, r.code);
        assertTrue(r.err.contains("model must be provider:model"), r.err);
        assertFalse(Files.exists(project.resolve(".buildcli")), "nothing is written when the arguments are invalid");
    }

    @Test
    void agentListShowAndCreate() throws Exception {
        cli("init");
        Result list = cli("agent", "list");
        assertTrue(list.out.contains("wheslley") && list.out.contains("architect") && list.out.contains("project"), list.out);
        Result show = cli("agent", "show", "matheus");
        assertTrue(show.out.contains("may write:    [src/**, docs/**]"), show.out);
        assertTrue(show.out.contains("[mvn, -q, verify]"), show.out);
        assertEquals(2, cli("agent", "show", "nobody").code);

        Result create = cli("agent", "create", "dora", "--role", "tester", "--capabilities", "filesystem.read,command.execute");
        assertEquals(0, create.code, create.err);
        assertTrue(create.out.contains("asks you to approve"));
        assertTrue(cli("agent", "list").out.contains("dora"));
        assertEquals(2, cli("agent", "create", "dora").code, "no overwrite");
        assertEquals(2, cli("agent", "create", "Bad Name").code);
        assertEquals(2, cli("agent", "create", "erin", "--capabilities", "telepathy").code);
    }

    @Test
    void anInvalidConfigurationIsReportedWithEveryProblemAndExitCode2() throws Exception {
        Files.createDirectories(project.resolve(".buildcli/agents"));
        Files.writeString(project.resolve(".buildcli/agents/a.yaml"), "name: a\nrole: r\n");
        Files.writeString(project.resolve(".buildcli/agents/b.yaml"), "schema: 1\nname: b\nrole: r\ncapabilites: []\n");
        Result r = cli("agent", "list");
        assertEquals(2, r.code);
        assertTrue(r.err.contains("2 problem(s)") && r.err.contains("missing 'schema'") && r.err.contains("capabilites"), r.err);
    }

    // ---- run ----

    /** Opens the chat on a pretend terminal and returns the session it was given. */
    ChatSession openChat() {
        var launched = new java.util.concurrent.atomic.AtomicReference<ChatSession>();
        var ctx = new CliContext(project, home, Map.of(), new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8), new BufferedReader(new StringReader("")), true,
                (ref, settings) -> new ScriptedGateway(), (session, models, services) -> launched.set(session));
        assertEquals(0, BuildCli.run(new String[] {"run"}, ctx));
        return launched.get();
    }

    @Test
    void everySubcommandHasHelp() {
        Result r = cli("agent", "create", "--help");
        assertEquals(0, r.code, r.err);
        assertTrue(r.out.contains("Usage: buildcli agent create"), r.out);
        assertFalse(r.err.contains("Missing required parameter"), r.err);
        Result created = cli("agent", "create", "lia");
        assertTrue(created.out.contains("run 'buildcli' and chat with lia"), created.out);
        assertFalse(created.out.contains("team"), "agents are not about teams any more: " + created.out);
    }

    @Test
    void theChatOpensWithNoAgentsAndNoTeams() {
        ChatSession session = openChat();
        assertTrue(session.contacts().isEmpty());
        assertEquals(null, session.defaultChat(), "nothing to talk to yet: the screen offers to create an agent");
    }

    @Test
    void anAgentWithoutAnyGroupIsEnoughToChat() {
        assertEquals(0, cli("agent", "create", "lia", "--role", "helper").code);
        ChatSession session = openChat();
        assertEquals(List.of("lia"), session.contacts().stream().map(dev.buildcli.domain.Agent::name).toList());
        assertEquals("lia", session.defaultChat(), "a direct chat with the only agent");
        assertTrue(session.groups().isEmpty());
    }

    @Test
    void runHeadlessExecutesTheGroupPersistsTheRunAndReportsTheResult() throws Exception {
        cli("init");
        Result r = cli(CliTest::happyScript, "", Map.of(), "run", "--group", "maintainers", "--headless", "--approve", "writes", "add a Hello class");
        assertEquals(0, r.code, r.err + r.out);
        assertTrue(r.out.contains("Hello was added."), r.out);
        assertTrue(r.out.contains("HandoffCreated"), r.out);
        assertTrue(r.out.contains("[approved by --approve writes]"), r.out);
        assertEquals("class Hello {}\n", Files.readString(project.resolve("src/Hello.java")));
        assertEquals("ollama", modelsRequested.get(0).provider());
        assertEquals("qwen2.5:7b", modelsRequested.get(0).model());

        assertTrue(cli("runs").out.contains("DONE") && cli("runs").out.contains("add a Hello class"));
        Result tasks = cli("task", "list");
        assertTrue(tasks.out.contains("#1") && tasks.out.contains("wheslley -> matheus") && tasks.out.contains("DONE"), tasks.out + tasks.err);
        assertTrue(cli("task", "show", "2").out.contains("Added src/Hello.java."));
        Result usage = cli("usage");
        assertTrue(usage.out.contains("wheslley") && usage.out.contains("matheus") && usage.out.contains("total"), usage.out);
        assertTrue(cli("usage", "--json").out.contains("\"totals\":{\"calls\":4,\"inputTokens\":400,\"outputTokens\":80}"));
    }

    @Test
    void approvalsAreDeniedByDefaultWithoutATerminal() throws Exception {
        cli("init");
        Result r = cli(CliTest::happyScript, "", Map.of(), "run", "--headless", "add a Hello class");
        assertFalse(Files.exists(project.resolve("src/Hello.java")), "nothing is approved unless asked for");
        assertTrue(r.out.contains("[denied by --approve none]"), r.out);
    }

    @Test
    void approvalsCanBeAnsweredOnTheConsole() throws Exception {
        cli("init");
        Result r = cli(CliTest::happyScript, "y\n", Map.of(), "run", "--headless", "--approve", "ask", "add a Hello class");
        assertEquals(0, r.code, r.out + r.err);
        assertTrue(Files.exists(project.resolve("src/Hello.java")));
        assertTrue(r.out.contains("Approve? [y/N]") && r.out.contains("+class Hello {}"), r.out);
    }

    @Test
    void theOnlyTeamIsUsedAndADirectAgentRunWorks() throws Exception {
        cli("init");
        Result direct = cli(() -> new ScriptedGateway().say("breno", "Looks good."), "", Map.of(), "run", "--headless", "--agent", "breno", "review it");
        assertEquals(0, direct.code, direct.err);
        assertTrue(direct.out.contains("Looks good."));
    }

    @Test
    void runRefusesClearlyWhenThereIsNothingToRun() throws Exception {
        assertTrue(cli("run", "--headless", "x").err.contains("there are no agents yet"));
        cli("init");
        assertTrue(cli("run", "--group", "nope", "--headless", "x").err.contains("no group named 'nope'"));
        assertTrue(cli("run", "--agent", "nope", "--headless", "x").err.contains("no agent named 'nope'"));
        assertTrue(cli("run", "--group", "maintainers", "--agent", "wheslley", "--headless", "x").err.contains("either --group or --agent"));
        assertTrue(cli("run", "--headless").err.contains("a request is required"));
        assertEquals(2, cli("run", "--model", "justamodel", "--headless", "x").code);
    }

    @Test
    void anAgentWithoutAModelIsRefusedUpFrontUnlessOneIsGiven() throws Exception {
        cli("init");
        Files.delete(stateDir().resolve("settings.yaml"));
        Result refused = cli("run", "--headless", "x");
        assertEquals(2, refused.code);
        assertTrue(refused.err.contains("no model configured for agent 'wheslley'"), refused.err);
        Result given = cli(CliTest::happyScript, "", Map.of(), "run", "--headless", "--approve", "all", "--model", "ollama:m", "add Hello");
        assertEquals(0, given.code, given.err + given.out);
        assertEquals("m", modelsRequested.get(0).model());
    }

    @Test
    void changedProjectDefinitionsMustBeTrustedAgainBeforeAnythingRuns() throws Exception {
        cli("init");
        Files.writeString(project.resolve(".buildcli/agents/matheus.md"),
                Files.readString(project.resolve(".buildcli/agents/matheus.md")).replace("src/**", "**"));
        Result r = cli(CliTest::happyScript, "", Map.of(), "run", "--headless", "--approve", "writes", "add Hello");
        assertEquals(1, r.code);
        assertTrue(r.err.contains("were not trusted, so nothing was run"), r.err);
        assertFalse(Files.exists(project.resolve("src/Hello.java")));
        assertTrue(r.out.contains("from the project?"), r.out);
        Result ok = cli(CliTest::happyScript, "", Map.of(), "run", "--headless", "--approve", "all", "add Hello");
        assertEquals(0, ok.code, ok.out + ok.err);
    }

    @Test
    void aFailingTaskEscalatesAndWithoutATerminalTheRunAborts() throws Exception {
        cli("init");
        Result r = cli(() -> {
            var g = new ScriptedGateway();
            for (int i = 0; i < 4; i++) {
                g.then("wheslley", new RuntimeException("provider down"));
            }
            return g;
        }, "", Map.of(), "run", "--headless", "--approve", "all", "do it");
        assertEquals(1, r.code);
        assertTrue(r.out.contains("[task #1 needs you]") && r.out.contains("TaskRetried"), r.out);
        assertTrue(r.out.contains("aborted"), r.out);
        assertTrue(cli("runs").out.contains("ABORTED"));
    }

    @Test
    void stateCommandsSayWhatToDoWhenThereAreNoRuns() {
        assertTrue(cli("runs").out.contains("No runs yet"));
        assertTrue(cli("task", "list").out.contains("No runs yet"));
        assertTrue(cli("usage").out.contains("No runs yet"));
        assertEquals("{\"runs\":[]}", cli("usage", "--json").out.strip());
    }

    @Test
    void anUnknownRunOrTaskIsAnError() throws Exception {
        cli("init");
        cli(CliTest::happyScript, "", Map.of(), "run", "--headless", "--approve", "writes", "add Hello");
        assertEquals(2, cli("task", "list", "--run", "nope").code);
        assertEquals(2, cli("task", "show", "99").code);
    }

    // ---- doctor, config, help ----

    @Test
    void doctorReportsAnUnreachableOllamaAsAWarningNotAFailure() throws Exception {
        cli("init");
        Result r = cli(() -> null, "", Map.of("OLLAMA_HOST", "127.0.0.1:1"), "doctor");
        assertEquals(0, r.code, r.out);
        assertTrue(r.out.contains("warn  Ollama is not reachable at http://127.0.0.1:1"), r.out);
        assertTrue(r.out.contains("4 agent(s) loaded, configuration is valid"), r.out);
    }

    @Test
    void doctorFindsOllamaAndChecksThatTheAgentsModelIsPulled() throws Exception {
        cli("init");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/tags", ex -> {
            byte[] body = "{\"models\":[{\"name\":\"qwen2.5:3b\"}]}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        try {
            String host = "127.0.0.1:" + server.getAddress().getPort();
            Result r = cli(() -> null, "", Map.of("OLLAMA_HOST", host), "doctor");
            assertTrue(r.out.contains("has 1 model(s): qwen2.5:3b"), r.out);
            assertTrue(r.out.contains("model 'qwen2.5:7b' is not pulled: run 'ollama pull qwen2.5:7b'"), r.out);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void doctorFailsOnAnInvalidConfiguration() throws Exception {
        Files.createDirectories(project.resolve(".buildcli/agents"));
        Files.writeString(project.resolve(".buildcli/agents/a.yaml"), "name: a\n");
        Result r = cli(() -> null, "", Map.of("OLLAMA_HOST", "127.0.0.1:1"), "doctor");
        assertEquals(1, r.code);
        assertTrue(r.out.contains("FAIL  the configuration has"), r.out);
    }

    @Test
    void configShowsLocationsAndNeverTheApiKey() {
        Result r = cli(() -> null, "", Map.of("OPENAI_API_KEY", "sk-very-secret-value-123456", "BUILDCLI_HOME", home.resolve("custom").toString()), "config");
        assertTrue(r.out.contains("global directory  : " + home.resolve("custom")), r.out);
        assertTrue(r.out.contains("OPENAI_API_KEY    : set in the environment (not shown)"), r.out);
        assertFalse(r.out.contains("very-secret"));
    }

    @Test
    void helpListsTheCommandsAndHidesTheDevelopmentOnes() {
        Result r = cli("--help");
        for (String c : List.of("init", "agent", "run", "runs", "task", "usage", "doctor", "config")) {
            assertTrue(r.out.contains("  " + c), c + "\n" + r.out);
        }
        assertFalse(r.out.contains("bench") || r.out.contains("  team"), "the development commands and the old team command are gone");
        assertEquals(0, cli().code, "no arguments without a terminal prints the help");
        assertTrue(cli().out.contains("Usage: buildcli"));
        assertEquals(2, cli("nonsense").code);
    }

    @Test
    void untrustedTextCannotDriveTheTerminalInHeadlessOutput() throws Exception {
        cli("init");
        Files.writeString(project.resolve("notes.txt"), "\u001b[2Jcleared \u001b]0;pwned\u0007 \u202Edisguised");
        Result r = cli(() -> new ScriptedGateway()
                .call("wheslley", "handoff", Map.of("to", "breno", "objective", "read \u001b[31mnotes"))
                .call("breno", "read_file", Map.of("path", "notes.txt"))
                .say("breno", "read it \u001b[1mbold")
                .say("wheslley", "final \u001b]0;title\u0007 report"), "", Map.of(), "run", "--headless", "--approve", "all", "look at the notes");
        assertEquals(0, r.code, r.out + r.err);
        assertFalse(r.out.contains("\u001b"), "an ESC reached the terminal");
        assertFalse(r.out.contains("\u0007"), "a BEL reached the terminal");
        assertFalse(r.out.contains("\u202E"), "a bidi override reached the terminal");
        assertTrue(r.out.contains("final \u241B]0;title\u2407 report"), r.out);
    }
}
