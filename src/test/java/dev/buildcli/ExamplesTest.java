package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.infrastructure.FileConfigRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The shipped examples must always load with the current schema, and stay within the permissions they advertise. */
class ExamplesTest {
    @TempDir Path home;
    static final Path EXAMPLES = Path.of("examples");

    FileConfigRepository load(String example) {
        return new FileConfigRepository(EXAMPLES.resolve(example), home);
    }

    @Test
    void everyExampleLoadsAndDefinesAtLeastOneAgent() throws IOException {
        try (Stream<Path> dirs = Files.list(EXAMPLES)) {
            List<Path> examples = dirs.filter(Files::isDirectory).toList();
            assertFalse(examples.isEmpty());
            for (Path dir : examples) {
                assertFalse(new FileConfigRepository(dir, home).agents().isEmpty(), dir + " defines no agent");
            }
        }
    }

    @Test
    void theJavaExampleIsAThreeAgentBackendTeam() {
        var repo = load("java-maven-backend");
        assertEquals(List.of("ana", "bruno", "carla"), repo.agents().stream().map(Agent::name).sorted().toList());
        assertEquals(List.of("src/**"), repo.agent("bruno").orElseThrow().permissions().writeGlobs());
        assertFalse(repo.projectContext().isEmpty());
    }

    @Test
    void theDocsExampleCannotRunAnyCommandAndOnlyWritesDocumentation() {
        var repo = load("docs-team");
        for (Agent a : repo.agents()) {
            assertFalse(a.can(Capability.COMMAND_EXECUTE), a.name() + " must not execute commands");
        }
        assertEquals(List.of("docs/**", "README.md"), repo.agent("writer").orElseThrow().permissions().writeGlobs());
        assertTrue(repo.agent("editor").orElseThrow().permissions().writeGlobs().isEmpty(), "the editor is read-only");
    }

    @Test
    void theSoloReviewerIsReadOnly() {
        Agent reviewer = load("solo-reviewer").agent("reviewer").orElseThrow();
        assertFalse(reviewer.can(Capability.FILESYSTEM_WRITE) || reviewer.can(Capability.COMMAND_EXECUTE) || reviewer.can(Capability.GIT_COMMIT));
    }
}
