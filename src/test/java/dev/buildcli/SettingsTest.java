package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.Settings;
import dev.buildcli.infrastructure.FileSettingsStore;
import dev.buildcli.ports.SettingsStore.Scope;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsTest {
    @TempDir Path dir;

    @Test
    void projectWinsOverGlobalWhichWinsOverTheDefault() {
        var s = new Settings(new FileSettingsStore(dir.resolve("global"), dir.resolve("project")));
        assertEquals("dark", s.get(Settings.THEME));
        s.set(Scope.GLOBAL, Settings.THEME, "light");
        assertEquals("light", s.get(Settings.THEME));
        s.set(Scope.PROJECT, Settings.THEME, "contrast");
        assertEquals("contrast", s.get(Settings.THEME));
        s.set(Scope.PROJECT, Settings.THEME, null);
        assertEquals("light", s.get(Settings.THEME), "resetting the project value falls back to the global one");
        assertTrue(s.flag(Settings.ENTER_SENDS));
        assertEquals(6, s.number(Settings.AGENT_HOPS, 0));
    }

    @Test
    void valuesSurviveARestartAndLiveOutsideTheProject() {
        var s = new Settings(new FileSettingsStore(dir.resolve("global"), dir.resolve("state")));
        s.set(Scope.PROJECT, Settings.AGENT_MODEL + "ana", "openrouter:openrouter/free");
        s.set(Scope.GLOBAL, Settings.DEFAULT_MODEL, "ollama:qwen2.5:7b");
        var again = new Settings(new FileSettingsStore(dir.resolve("global"), dir.resolve("state")));
        assertEquals("openrouter:openrouter/free", again.modelFor("ana"));
        assertNull(again.modelFor("bruno"));
        assertEquals("ollama:qwen2.5:7b", again.defaultModel());
        assertTrue(Files.isRegularFile(dir.resolve("state").resolve(FileSettingsStore.FILE_NAME)));
    }

    @Test
    void aBrokenFileGivesTheDefaultsInsteadOfStoppingTheApp() throws Exception {
        Files.createDirectories(dir.resolve("g"));
        Files.writeString(dir.resolve("g").resolve(FileSettingsStore.FILE_NAME), "{{{ not yaml");
        var s = new Settings(new FileSettingsStore(dir.resolve("g"), dir.resolve("p")));
        assertEquals("dark", s.get(Settings.THEME));
        assertFalse(s.flag(Settings.COMPACT));
    }
}
