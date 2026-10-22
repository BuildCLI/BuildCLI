package dev.buildcli.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.infrastructure.FileTrustStore;
import dev.buildcli.ports.ChatLog;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** "Add the sample team" from an empty chat: the same agents as `init`, alive in the open chat, in a group, already trusted. */
class SampleAgentsTest {
    @TempDir Path root;

    @Test
    void anEmptyChatGetsTheSampleAgentsWithTheirRealPermissionsAndAGroupLedByWheslley() throws Exception {
        Path project = Files.createDirectories(root.resolve("project"));
        Path home = Files.createDirectories(root.resolve("home"));
        var out = new PrintStream(new ByteArrayOutputStream());
        var ctx = new CliContext(project, home, Map.of(), out, out, new BufferedReader(new StringReader("")), false, (r, s) -> null,
                (session, models, services) -> { });
        var config = ctx.loadConfig();
        assertTrue(config.agents().isEmpty(), "it starts with no agents at all");
        var setup = new ChatSetup(ctx, config);
        var services = new ChatServices(ctx, config, setup);
        var session = new ChatSession(config.agents(), setup.groups(), setup.limits(), (t, req, ui, c, d) -> {
            throw new AssertionError("no model is called");
        }, setup.store, () -> 6, ChatLog.NONE);
        services.attach(session);

        List<String> added = services.createSampleAgents();
        assertEquals(List.of("wheslley", "breno", "matheus", "dumildes"), added);
        for (String f : List.of(".buildcli/agents/wheslley.md", ".buildcli/agents/breno.md", ".buildcli/agents/matheus.md", ".buildcli/agents/dumildes.md", "AGENTS.md")) {
            assertTrue(Files.exists(project.resolve(f)), f);
        }
        assertEquals(List.of("breno", "dumildes", "matheus", "wheslley"), session.contacts().stream().map(a -> a.name()).sorted().toList());
        assertEquals(List.of("src/**", "docs/**"), session.contact("matheus").permissions().writeGlobs(), "matheus is live with what his file says, not a read-only copy");
        assertTrue(session.contact("matheus").can("command.execute"));
        assertTrue(session.contact("matheus").can("chat.post"));
        assertTrue(session.contact("breno").permissions().writeGlobs().contains(".github/**"), "the devops writes the pipeline");
        assertTrue(session.contact("wheslley").permissions().writeGlobs().isEmpty(), "the architect only reads");

        var group = session.group("#maintainers");
        assertEquals(List.of("wheslley", "breno", "matheus", "dumildes"), group.members());
        assertEquals(List.of("wheslley"), group.admins());
        assertEquals(List.of("maintainers"), new dev.buildcli.infrastructure.FileChatStore(ctx.projectStateDir()).load().stream().map(g -> g.name()).toList(),
                "the group is saved, outside the project");

        var fresh = ctx.loadConfig();
        assertTrue(new FileTrustStore(ctx.trustFile()).isTrusted(ctx.projectKey(), fresh.projectDigest()), "the user asked for them, so no approval prompt follows");
        assertEquals(4, services.agents().size());

        assertThrows(IllegalArgumentException.class, services::createSampleAgents, "a second click says they are already here");
        assertEquals(4, session.contacts().size());
        session.close();
    }

    @Test
    void agentsTheUserAlreadyHasAreKeptAndOnlyTheMissingOnesAreAdded() throws Exception {
        Path project = Files.createDirectories(root.resolve("project"));
        Path home = Files.createDirectories(root.resolve("home"));
        var out = new PrintStream(new ByteArrayOutputStream());
        var ctx = new CliContext(project, home, Map.of(), out, out, new BufferedReader(new StringReader("")), false, (r, s) -> null,
                (session, models, services) -> { });
        Files.createDirectories(project.resolve(".buildcli/agents"));
        String mine = "---\nschema: 1\nname: matheus\nrole: my own matheus\ncapabilities: [filesystem.read]\npermissions:\n  filesystem:\n    read: [\"**\"]\n---\nMine.\n";
        Files.writeString(project.resolve(".buildcli/agents/matheus.md"), mine);
        var config = ctx.loadConfig();
        var setup = new ChatSetup(ctx, config);
        var services = new ChatServices(ctx, config, setup);
        var session = new ChatSession(config.agents(), setup.groups(), setup.limits(), (t, req, ui, c, d) -> {
            throw new AssertionError();
        }, setup.store, () -> 6, ChatLog.NONE);
        services.attach(session);

        assertEquals(List.of("wheslley", "breno", "dumildes"), services.createSampleAgents());
        assertEquals(mine, Files.readString(project.resolve(".buildcli/agents/matheus.md")), "their own file is untouched");
        assertEquals("my own matheus", session.contact("matheus").role());
        session.close();
    }

    @Test
    void theNonTechnicalTeamsLoadAsValidAgentsThatCannotRunCommandsOrCommit() throws Exception {
        Path project = Files.createDirectories(root.resolve("project"));
        Path home = Files.createDirectories(root.resolve("home"));
        var out = new PrintStream(new ByteArrayOutputStream());
        var ctx = new CliContext(project, home, Map.of(), out, out, new BufferedReader(new StringReader("")), false, (r, s) -> null,
                (session, models, services) -> { });
        var config = ctx.loadConfig();
        var setup = new ChatSetup(ctx, config);
        var services = new ChatServices(ctx, config, setup);
        var session = new ChatSession(config.agents(), setup.groups(), setup.limits(), (t, req, ui, c, d) -> {
            throw new AssertionError("no model is called");
        }, setup.store, () -> 6, ChatLog.NONE);
        services.attach(session);

        assertEquals(3, services.sampleTeams().size());
        assertEquals(List.of("writer", "editor", "researcher"), services.createSampleAgents("writing"));
        assertEquals(List.of("assistant", "analyst", "planner"), services.createSampleAgents("office"));
        assertThrows(IllegalArgumentException.class, () -> services.createSampleAgents("pirates"));
        assertEquals(List.of("writer"), session.group("#writing-desk").admins());
        for (String name : List.of("writer", "editor", "researcher", "assistant", "analyst", "planner")) {
            var agent = session.contact(name);
            assertTrue(agent != null, name);
            assertTrue(!agent.can("command.execute") && !agent.can("git.commit") && !agent.can("git.read"), name + " has no commands and no git");
            assertTrue(agent.permissions().commandAllow().isEmpty(), name);
        }
        assertEquals(List.of("drafts/**"), session.contact("editor").permissions().writeGlobs());
        assertEquals(6, ctx.loadConfig().agents().size(), "every file loads without a problem");
        session.close();
    }
}
