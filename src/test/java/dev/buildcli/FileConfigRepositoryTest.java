package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Origin;
import dev.buildcli.infrastructure.FileConfigRepository;
import dev.buildcli.ports.ConfigException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileConfigRepositoryTest {
    @TempDir Path root;
    Path project;
    Path global;

    static final String ANA = """
            ---
            schema: 1
            name: ana
            role: architect
            instructions: |
              You are the software architect.
            capabilities: [filesystem.read, agent.handoff]
            permissions:
              filesystem:
                read: ["**"]
            ---
            Challenge unnecessary complexity.
            """;

    static final String BRUNO = """
            schema: 1
            name: bruno
            role: developer
            instructions: You implement.
            capabilities: [filesystem.read, filesystem.write, command.execute]
            permissions:
              filesystem:
                write: ["src/**"]
              command:
                allow: [["mvn", "test"], ["./mvnw", "-q", "verify"]]
                timeout: 10m
            """;

    @BeforeEach
    void setUp() throws IOException {
        project = Files.createDirectories(root.resolve("project"));
        global = Files.createDirectories(root.resolve("home/.buildcli"));
    }

    void write(Path base, String rel, String content) throws IOException {
        Path f = base.resolve(rel);
        Files.createDirectories(f.getParent());
        Files.writeString(f, content);
    }

    void writeProject(String rel, String content) throws IOException {
        write(project, ".buildcli/" + rel, content);
    }

    FileConfigRepository load() {
        return new FileConfigRepository(project, global);
    }

    List<String> problems(Runnable load) {
        return assertThrows(ConfigException.class, load::run).problems();
    }

    static boolean anyContains(List<String> problems, String text) {
        return problems.stream().anyMatch(p -> p.contains(text));
    }

    @Test
    void loadsAgentsFromTheProject() throws IOException {
        writeProject("agents/ana.md", ANA);
        writeProject("agents/bruno.yaml", BRUNO);
        var repo = load();

        Agent ana = repo.agent("ana").orElseThrow();
        assertEquals("architect", ana.role());
        assertEquals(Set.of("filesystem.read", "agent.handoff"), ana.capabilities());
        assertTrue(ana.instructions().contains("software architect"));
        assertTrue(ana.instructions().contains("Challenge unnecessary complexity."), "the Markdown body is appended");
        assertEquals(Origin.PROJECT, ana.origin());

        Agent bruno = repo.agent("bruno").orElseThrow();
        assertEquals(List.of("src/**"), bruno.permissions().writeGlobs());
        assertEquals(List.of("**"), bruno.permissions().readGlobs(), "reads default to the whole workspace");
        assertEquals(List.of(List.of("mvn", "test"), List.of("./mvnw", "-q", "verify")), bruno.permissions().commandAllow());
        assertEquals(Duration.ofMinutes(10), bruno.permissions().commandTimeout());

    }

    @Test
    void projectDefinitionsOverrideGlobalOnesOfTheSameName() throws IOException {
        write(global, "agents/ana.md", ANA.replace("architect", "global-architect"));
        writeProject("agents/ana.md", ANA);
        Agent ana = load().agent("ana").orElseThrow();
        assertEquals("architect", ana.role());
        assertEquals(Origin.PROJECT, ana.origin());
    }

    @Test
    void globalDefinitionsAreUsedWhenTheProjectHasNone() throws IOException {
        write(global, "agents/ana.md", ANA);
        assertEquals(Origin.GLOBAL, load().agent("ana").orElseThrow().origin());
    }

    @Test
    void noConfigurationAtAllIsValidAndEmpty() {
        var repo = load();
        assertTrue(repo.agents().isEmpty());
        assertEquals("", repo.projectContext());
    }

    @Test
    void agentsMdIsReadAsContextAndTruncated() throws IOException {
        write(project, "AGENTS.md", "Build with mvn verify.");
        assertEquals("Build with mvn verify.", load().projectContext());
        write(project, "AGENTS.md", "x".repeat(FileConfigRepository.MAX_CONTEXT_CHARS + 500));
        String ctx = load().projectContext();
        assertTrue(ctx.length() < FileConfigRepository.MAX_CONTEXT_CHARS + 100);
        assertTrue(ctx.contains("truncated"));
    }

    @Test
    void aMissingSchemaIsRejected() throws IOException {
        writeProject("agents/a.yaml", "name: a\nrole: r\n");
        assertTrue(anyContains(problems(this::load), "missing 'schema'"));
    }

    @Test
    void anUnsupportedSchemaIsRejectedWithAClearMessage() throws IOException {
        writeProject("agents/a.yaml", "schema: 2\nname: a\nrole: r\n");
        assertTrue(anyContains(problems(this::load), "unsupported schema 2"));
    }

    @Test
    void aTypoInAKeyIsAnErrorNotSilentlyIgnored() throws IOException {
        writeProject("agents/a.yaml", "schema: 1\nname: a\nrole: r\ncapabilites: [filesystem.read]\n");
        assertTrue(anyContains(problems(this::load), "unknown key 'capabilites'"));
    }

    @Test
    void unknownCapabilitiesListTheKnownOnes() throws IOException {
        writeProject("agents/a.yaml", "schema: 1\nname: a\nrole: r\ncapabilities: [jira.issue.read]\n");
        var p = problems(this::load);
        assertTrue(anyContains(p, "unknown capability"));
        assertTrue(anyContains(p, "filesystem.read"));
    }

    @Test
    void networkPermissionsAreRejectedBecauseTheyCannotBeEnforced() throws IOException {
        writeProject("agents/a.yaml", "schema: 1\nname: a\nrole: r\npermissions:\n  network: false\n");
        assertTrue(anyContains(problems(this::load), "network is not supported"));
    }

    @Test
    void shellStringsInTheCommandAllowListAreRejected() throws IOException {
        writeProject("agents/a.yaml", "schema: 1\nname: a\nrole: r\npermissions:\n  command:\n    allow: [\"mvn test\"]\n");
        assertTrue(anyContains(problems(this::load), "not a shell string"));
    }

    @Test
    void invalidTimeoutsAreRejected() throws IOException {
        writeProject("agents/a.yaml", "schema: 1\nname: a\nrole: r\npermissions:\n  command:\n    timeout: soon\n");
        assertTrue(anyContains(problems(this::load), "invalid timeout"));
    }

    @Test
    void aMarkdownAgentWithoutFrontMatterIsRejected() throws IOException {
        writeProject("agents/a.md", "Just some text");
        assertTrue(anyContains(problems(this::load), "front matter"));
    }

    @Test
    void everyProblemIsReportedAtOnce() throws IOException {
        writeProject("agents/one.yaml", "name: one\nrole: r\n");
        writeProject("agents/two.yaml", "schema: 1\nname: Two\nrole: r\n");
        writeProject("agents/three.yaml", "schema: 1: nope");
        var p = problems(this::load);
        assertTrue(p.size() >= 3, p.toString());
        assertFalse(p.isEmpty());
    }

    @Test
    void twoAgentsWithTheSameNameInOneDirectoryClash() throws IOException {
        writeProject("agents/a.yaml", "schema: 1\nname: dup\nrole: r\n");
        writeProject("agents/b.yaml", "schema: 1\nname: dup\nrole: r\n");
        assertTrue(anyContains(problems(this::load), "is also defined in"));
    }

    @Test
    void anInvalidGlobIsAConfigurationErrorNotARuntimeSurprise() throws IOException {
        writeProject("agents/a.yaml", "schema: 1\nname: a\nrole: r\npermissions:\n  filesystem:\n    write: [\"src/[unclosed\"]\n");
        assertTrue(anyContains(problems(this::load), "invalid glob 'src/[unclosed'"));
    }

    @Test
    void aHugeDefinitionFileIsRefusedWithoutBeingRead() throws IOException {
        writeProject("agents/big.yaml", "schema: 1\nname: big\nrole: r\ninstructions: \"" + "x".repeat((int) FileConfigRepository.MAX_FILE_BYTES) + "\"\n");
        assertTrue(anyContains(problems(this::load), "larger than 256 KB"));
    }

    @Test
    void aYamlAliasBombInsideAnIgnoredValueIsNotExpanded() throws IOException {
        // Each level references the previous one nine times: 9^12 (~2.8e11) nodes if aliases were expanded.
        StringBuilder bomb = new StringBuilder("schema: 1\nname: bomb\nrole: r\ndescription:\n  a0: &a0 [\"lol\", \"lol\", \"lol\"]\n");
        for (int i = 1; i <= 12; i++) {
            bomb.append("  a").append(i).append(": &a").append(i).append(" [");
            for (int k = 0; k < 9; k++) {
                bomb.append(k == 0 ? "" : ", ").append("*a").append(i - 1);
            }
            bomb.append("]\n");
        }
        writeProject("agents/bomb.yaml", bomb.toString());
        long start = System.nanoTime();
        boolean loadedOrRejected;
        try {
            loadedOrRejected = load().agent("bomb").isPresent();
        } catch (ConfigException e) {
            loadedOrRejected = true; // a clean rejection is fine too
        }
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertTrue(loadedOrRejected);
        assertTrue(millis < 5_000, "the bomb must not be expanded: took " + millis + " ms");
    }
}
