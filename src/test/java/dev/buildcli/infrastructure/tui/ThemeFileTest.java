package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.tamboui.style.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Your own colours from a CSS file: they win over the palette, a palette can have its own, and mistakes are reported. */
class ThemeFileTest {
    @TempDir Path dir;

    @AfterEach
    void restore() {
        Theme.customise(Map.of());
        Theme.use("dark");
    }

    @Test
    void aVariableRepaintsThatPartOfEveryPaletteAndSurvivesSwitchingThemes() throws Exception {
        Color original = Theme.BG;
        Files.writeString(dir.resolve("theme.css"), "/* mine */\n$bg: #102030;\n$accent: #ff8800;\n");
        var result = ThemeFile.load(dir.resolve("theme.css"));
        assertTrue(result.problems().isEmpty(), result.problems().toString());
        Theme.customise(result.colours());
        assertEquals(Color.hex("#102030"), Theme.BG);
        assertEquals(Color.hex("#ff8800"), Theme.ACCENT);
        assertNotEquals(original, Theme.BG);
        Theme.use("light");
        assertEquals(Color.hex("#102030"), Theme.BG, "also in the light theme");
        Theme.customise(Map.of());
        assertNotEquals(Color.hex("#102030"), Theme.BG, "no custom colours: the light palette is back");
    }

    @Test
    void aPaletteCanHaveItsOwnColourAndItBeatsTheGeneralOne() {
        var result = ThemeFile.parse("$bg: #111111;\n$light-bg: #fafafa;\n$agent-2: #00ff00;\n");
        assertTrue(result.problems().isEmpty(), result.problems().toString());
        Theme.customise(result.colours());
        Theme.use("light");
        assertEquals(Color.hex("#fafafa"), Theme.BG);
        Theme.use("dark");
        assertEquals(Color.hex("#111111"), Theme.BG);
        assertEquals(Color.hex("#00ff00"), Theme.agentColor("\u0001"), "agent colour 2 is picked by name");
    }

    @Test
    void mistakesAreReportedAndSkippedNeverFatal() {
        var result = ThemeFile.parse("$bg: not-a-colour;\n$nonsense: #123456;\n$text: #eeeeee;\n");
        assertEquals(2, result.problems().size(), result.problems().toString());
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("$bg") && p.contains("not a colour")));
        assertTrue(result.problems().stream().anyMatch(p -> p.contains("$nonsense")));
        assertEquals(Map.of("text", Color.hex("#eeeeee")), result.colours(), "what was fine still applies");
    }

    @Test
    void noFileMeansNothingToDoAndAnUnreadableOneIsNotAnException() throws Exception {
        assertEquals(ThemeFile.Result.NONE, ThemeFile.load(dir.resolve("theme.css")));
        Files.createDirectory(dir.resolve("dir.css"));
        assertEquals(ThemeFile.Result.NONE, ThemeFile.load(dir.resolve("dir.css")));
    }
}
