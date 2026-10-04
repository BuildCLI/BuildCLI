package dev.buildcli.infrastructure.tui;

import dev.tamboui.css.parser.CssParser;
import dev.tamboui.style.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Your own colours, in a CSS file ({@code ~/.buildcli/theme.css}) read with TamboUI's CSS parser. Each colour is a
 * variable named like the part of the screen it paints:
 *
 * <pre>
 * $bg: #101820;            every theme
 * $light-bg: #fffdf5;      only the light theme (also $dark-… and $contrast-…)
 * $agent-1: #ff8800;       the colours agents get, 1 to 8
 * </pre>
 *
 * Anything it does not understand is reported and skipped; a broken file never stops the chat from opening.
 */
final class ThemeFile {
    /** The colours of {@link Theme}, as they are written in the file. */
    static final Set<String> NAMES = Set.of("bg", "sidebar", "panel", "field", "selected", "me", "them", "pill", "code", "error-bg", "dialog",
            "text", "dim", "faint", "accent", "green", "blue", "red", "amber", "on-me", "on-me-dim", "tick", "code-text", "line", "on-accent",
            "add-bg", "add-fg", "del-bg", "del-fg", "danger", "agent-1", "agent-2", "agent-3", "agent-4", "agent-5", "agent-6", "agent-7", "agent-8");
    private static final Set<String> PALETTES = Set.of("dark", "light", "contrast");

    /** @param colours name (optionally prefixed with a palette) to colour; @param problems what was skipped, one line each */
    record Result(Map<String, Color> colours, List<String> problems) {
        static final Result NONE = new Result(Map.of(), List.of());
    }

    private ThemeFile() {}

    static Result load(Path file) {
        if (!Files.isRegularFile(file)) {
            return Result.NONE;
        }
        try {
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            return new Result(Map.of(), List.of(file.getFileName() + ": " + e.getMessage()));
        }
    }

    static Result parse(String css) {
        Map<String, String> vars;
        try {
            vars = CssParser.parse(css).variables();
        } catch (RuntimeException e) {
            return new Result(Map.of(), List.of("not valid CSS: " + e.getMessage()));
        }
        Map<String, Color> colours = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        vars.forEach((name, value) -> {
            if (!known(name)) {
                problems.add("$" + name + ": no such colour");
                return;
            }
            try {
                colours.put(name, Color.hex(value.strip()));
            } catch (RuntimeException e) {
                problems.add("$" + name + ": " + value + " is not a colour like #1a2b3c");
            }
        });
        return new Result(colours, problems);
    }

    /** "bg", "light-bg", "agent-3"... */
    private static boolean known(String name) {
        int dash = name.indexOf('-');
        if (dash > 0 && PALETTES.contains(name.substring(0, dash)) && NAMES.contains(name.substring(dash + 1))) {
            return true;
        }
        return NAMES.contains(name);
    }
}
