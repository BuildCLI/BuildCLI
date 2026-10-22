package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.infrastructure.AgentFileEditor;
import dev.buildcli.infrastructure.AgentFileEditor.Edit;
import dev.buildcli.infrastructure.FileConfigRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Editing an existing agent's file: what changes, what stays, and what is refused. */
class AgentFileEditorTest {
    @TempDir Path dir;

    static final String FILE = """
            ---
            schema: 1
            name: rita
            role: reviewer
            description: Reviews code.
            capabilities: [filesystem.read, search]
            permissions:
              filesystem:
                read: ["**"]
            ---
            You review the changes.
            """;

    Agent reload(String text) throws Exception {
        Path agents = Files.createDirectories(dir.resolve("p/.buildcli/agents"));
        Files.writeString(agents.resolve("rita.md"), text);
        return new FileConfigRepository(dir.resolve("p"), dir.resolve("g")).agent("rita").orElseThrow();
    }

    @Test
    void changesOnlyWhatWasAskedAndTheResultStillLoads() throws Exception {
        Agent a = reload(AgentFileEditor.apply(FILE, Edit.role("tester")));
        assertEquals("tester", a.role());
        assertEquals("You review the changes.", a.instructions());
        assertTrue(a.can(Capability.SEARCH));

        a = reload(AgentFileEditor.apply(FILE, Edit.capabilities(List.of("filesystem.read", "filesystem.write"))));
        assertTrue(a.can("filesystem.write") && !a.can("search"));

        a = reload(AgentFileEditor.apply(FILE, Edit.instructions("Be brief.")));
        assertEquals("Be brief.", a.instructions());
        assertEquals("reviewer", a.role());
    }

    @Test
    void writeFoldersAndAllowedCommandsAreWrittenWhereTheLoaderReadsThem() throws Exception {
        String once = AgentFileEditor.apply(FILE, Edit.writeGlobs(List.of("src/**", "docs/**")));
        String twice = AgentFileEditor.apply(once, Edit.commands(List.of(List.of("mvn", "-q", "test"))));
        Agent a = reload(twice);
        assertEquals(List.of("src/**", "docs/**"), a.permissions().writeGlobs());
        assertEquals(List.of(List.of("mvn", "-q", "test")), a.permissions().commandAllow());
        assertEquals(List.of("**"), a.permissions().readGlobs(), "reading was not touched");
    }

    @Test
    void refusesWhatWouldReachOutsideTheProjectOrBuildCLIsOwnFiles() {
        for (String bad : new String[] {"../x/**", "/etc/**", "~/**", ".buildcli/**", ".git/**", "C:/x", "a\\b", ""}) {
            assertThrows(IllegalArgumentException.class, () -> AgentFileEditor.apply(FILE, Edit.writeGlobs(List.of(bad))), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> AgentFileEditor.apply(FILE, Edit.capabilities(List.of("root.everything"))));
        assertThrows(IllegalArgumentException.class, () -> AgentFileEditor.apply(FILE, Edit.commands(List.of(List.of()))));
        assertThrows(IllegalArgumentException.class, () -> AgentFileEditor.apply("name: x\nrole: y\n", Edit.role("z")));
    }
}
