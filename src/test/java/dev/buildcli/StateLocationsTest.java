package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.infrastructure.StateLocations;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StateLocationsTest {
    @TempDir Path dir;

    @Test
    void theGlobalDirDefaultsToTheHomeAndCanBeOverridden() {
        Path home = Path.of("/home/someone");
        assertEquals(home.resolve(".buildcli"), StateLocations.globalDir(Map.of(), home));
        assertEquals(Path.of("/custom"), StateLocations.globalDir(Map.of("BUILDCLI_HOME", "/custom"), home));
        assertEquals(home.resolve(".buildcli"), StateLocations.globalDir(Map.of("BUILDCLI_HOME", " "), home));
    }

    @Test
    void stateIsKeyedByProjectAndNeverInsideTheProject() throws Exception {
        Path global = dir.resolve("global");
        Path a = Files.createDirectories(dir.resolve("work/api"));
        Path b = Files.createDirectories(dir.resolve("other/api"));
        Path stateA = StateLocations.stateDb(global, a);
        Path stateB = StateLocations.stateDb(global, b);

        assertNotEquals(stateA, stateB, "two projects with the same folder name must not share state");
        assertEquals(stateA, StateLocations.stateDb(global, a), "the location is stable");
        assertTrue(stateA.startsWith(global.resolve("projects")));
        assertFalse(stateA.startsWith(a));
        assertTrue(stateA.getParent().getFileName().toString().startsWith("api-"));
    }
}
