package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.TrustGate;
import dev.buildcli.domain.Roster;
import dev.buildcli.infrastructure.FileConfigRepository;
import dev.buildcli.infrastructure.FileTrustStore;
import dev.buildcli.infrastructure.HeadlessUi;
import dev.buildcli.ports.EscalationChoice;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TrustTest {
    @TempDir Path root;
    Path project;
    Path global;
    FileTrustStore store;

    static final String AGENT = "schema: 1\nname: bruno\nrole: developer\ncapabilities: [filesystem.write, command.execute]\n"
            + "permissions:\n  filesystem:\n    write: [\"src/**\"]\n  command:\n    allow: [[\"mvn\", \"test\"]]\n";

    @BeforeEach
    void setUp() throws IOException {
        project = Files.createDirectories(root.resolve("project"));
        global = Files.createDirectories(root.resolve("home"));
        store = new FileTrustStore(global.resolve("trust.json"));
    }

    void write(Path base, String rel, String content) throws IOException {
        Path f = base.resolve(rel);
        Files.createDirectories(f.getParent());
        Files.writeString(f, content);
    }

    FileConfigRepository config() {
        return new FileConfigRepository(project, global);
    }

    HeadlessUi user(boolean approve) {
        return new HeadlessUi(r -> approve, EscalationChoice.ABORT, false);
    }

    boolean gate(FileConfigRepository cfg, HeadlessUi ui) {
        Roster roster = new Roster("t", "bruno", cfg.agents(), dev.buildcli.domain.Limits.defaults());
        return TrustGate.ensureTrusted(roster, cfg, store, project.toString(), ui);
    }

    @Test
    void projectAgentsNeedApprovalAndThePromptShowsWhatTheyMayDo() throws IOException {
        write(project, ".buildcli/agents/bruno.yaml", AGENT);
        var ui = user(true);
        assertTrue(gate(config(), ui));
        assertEquals(1, ui.approvals.size());
        var request = ui.approvals.get(0);
        assertEquals("trust", request.kind());
        assertTrue(request.detail().contains("command.execute"), request.detail());
        assertTrue(request.detail().contains("src/**"), request.detail());
        assertTrue(request.detail().contains("mvn"), request.detail());
    }

    @Test
    void anApprovalIsRememberedForTheSameFiles() throws IOException {
        write(project, ".buildcli/agents/bruno.yaml", AGENT);
        assertTrue(gate(config(), user(true)));
        var second = user(false);
        assertTrue(gate(config(), second), "already trusted");
        assertTrue(second.approvals.isEmpty(), "no second prompt");
    }

    @Test
    void changingAnyDefinitionFileAsksAgain() throws IOException {
        write(project, ".buildcli/agents/bruno.yaml", AGENT);
        assertTrue(gate(config(), user(true)));

        write(project, ".buildcli/agents/bruno.yaml", AGENT.replace("mvn", "curl"));
        var ui = user(false);
        assertFalse(gate(config(), ui), "the widened permissions must be re-approved");
        assertEquals(1, ui.approvals.size());
    }

    @Test
    void aDeclinedProjectIsNotTrustedAndIsAskedAgainNextTime() throws IOException {
        write(project, ".buildcli/agents/bruno.yaml", AGENT);
        assertFalse(gate(config(), user(false)));
        var next = user(true);
        assertTrue(gate(config(), next));
        assertEquals(1, next.approvals.size());
    }

    @Test
    void yourOwnGlobalDefinitionsNeedNoApproval() throws IOException {
        write(global, "agents/bruno.yaml", AGENT);
        var ui = user(false);
        assertTrue(gate(config(), ui));
        assertTrue(ui.approvals.isEmpty());
    }

    @Test
    void theDigestChangesWithTheFilesButNotWithAgentsMd() throws IOException {
        write(project, ".buildcli/agents/bruno.yaml", AGENT);
        String before = config().projectDigest();
        assertFalse(before.isEmpty());
        assertEquals(before, config().projectDigest(), "stable");
        write(project, "AGENTS.md", "context is not configuration");
        assertEquals(before, config().projectDigest());
        write(project, ".buildcli/agents/bruno.yaml", AGENT + "description: changed\n");
        assertNotEquals(before, config().projectDigest());
    }

    @Test
    void aProjectWithoutDefinitionsHasAnEmptyDigest() {
        assertEquals("", config().projectDigest());
    }

    @Test
    void trustSurvivesReopeningTheStoreAndIsPerProject() {
        store.trust("/work/a", "abc");
        var reopened = new FileTrustStore(global.resolve("trust.json"));
        assertTrue(reopened.isTrusted("/work/a", "abc"));
        assertFalse(reopened.isTrusted("/work/a", "different"));
        assertFalse(reopened.isTrusted("/work/b", "abc"));
        assertFalse(reopened.isTrusted("/work/a", ""), "an empty digest is never trusted");
    }

    @Test
    void aCorruptTrustFileMeansNothingIsTrustedAndIsRepairedOnTheNextApproval() throws IOException {
        Files.writeString(global.resolve("trust.json"), "{ this is not json");
        assertFalse(store.isTrusted("/work/a", "abc"));
        store.trust("/work/a", "abc");
        assertTrue(store.isTrusted("/work/a", "abc"));
    }
}
