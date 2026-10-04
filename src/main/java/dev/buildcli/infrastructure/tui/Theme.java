package dev.buildcli.infrastructure.tui;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * Colours of the chat, taken from the BuildCLI logo: deep navy and cream (dark, light) and a high-contrast one. Every style sets both
 * foreground and background, so the look does not depend on the terminal's own theme. Switched live from Settings.
 */
final class Theme {
    private Theme() {}

    static Color BG;
    static Color SIDEBAR;
    static Color PANEL;
    static Color FIELD;
    static Color SELECTED;
    static Color ME;
    static Color THEM;
    static Color PILL;
    static Color CODE;
    static Color ERROR_BG;
    static Color DIALOG;
    static Color TEXT;
    static Color DIM;
    static Color FAINT;
    static Color ACCENT;
    static Color GREEN;
    static Color BLUE;
    static Color RED;
    static Color AMBER;
    static Color ON_ME;
    static Color ON_ME_DIM;
    /** The double tick of a delivered message, readable on the colour of your own bubbles. */
    static Color TICK;
    static Color CODE_TEXT;
    static Color LINE;
    static Color ON_ACCENT;
    static Color ADD_BG;
    static Color ADD_FG;
    static Color DEL_BG;
    static Color DEL_FG;
    static Color DANGER;
    private static String current = "";
    private static java.util.Map<String, Color> custom = java.util.Map.of();

    private static Color[] agentColors;

    static {
        use("dark");
    }

    /** Switches the palette: "dark", "light" or "contrast". Unknown names fall back to dark. */
    static synchronized void use(String name) {
        String n = name == null ? "dark" : name;
        if (n.equals(current)) {
            return;
        }
        current = n;
        switch (n) {
            case "light" -> {
                BG = Color.rgb(234, 237, 233);
                SIDEBAR = Color.rgb(250, 252, 249);
                PANEL = Color.rgb(241, 244, 240);
                FIELD = Color.rgb(255, 255, 255);
                SELECTED = Color.rgb(226, 231, 226);
                ME = Color.rgb(30, 37, 48);
                THEM = Color.rgb(255, 255, 255);
                PILL = Color.rgb(255, 255, 255);
                CODE = Color.rgb(238, 240, 237);
                ERROR_BG = Color.rgb(253, 226, 226);
                DIALOG = Color.rgb(255, 255, 255);
                TEXT = Color.rgb(28, 34, 44);
                DIM = Color.rgb(96, 108, 122);
                FAINT = Color.rgb(158, 168, 176);
                ACCENT = Color.rgb(30, 37, 48);
                GREEN = Color.rgb(30, 140, 80);
                BLUE = Color.rgb(0, 112, 200);
                RED = Color.rgb(200, 40, 60);
                AMBER = Color.rgb(176, 108, 0);
                ON_ME = Color.rgb(246, 250, 244);
                TICK = Color.rgb(120, 180, 255);
                ON_ME_DIM = Color.rgb(170, 182, 196);
                CODE_TEXT = Color.rgb(150, 60, 0);
                LINE = Color.rgb(220, 225, 220);
                ON_ACCENT = Color.rgb(246, 250, 244);
                ADD_BG = Color.rgb(220, 245, 225);
                ADD_FG = Color.rgb(20, 110, 40);
                DEL_BG = Color.rgb(253, 225, 225);
                DEL_FG = Color.rgb(170, 30, 40);
                DANGER = Color.rgb(220, 60, 70);
                agentColors = new Color[] {Color.rgb(0, 110, 190), Color.rgb(170, 100, 0), Color.rgb(150, 50, 190), Color.rgb(30, 140, 60),
                    Color.rgb(200, 80, 40), Color.rgb(190, 40, 110), Color.rgb(0, 140, 150), Color.rgb(100, 130, 20)};
            }
            case "contrast" -> {
                BG = Color.rgb(0, 0, 0);
                SIDEBAR = Color.rgb(0, 0, 0);
                PANEL = Color.rgb(20, 20, 20);
                FIELD = Color.rgb(30, 30, 30);
                SELECTED = Color.rgb(50, 50, 50);
                ME = Color.rgb(255, 255, 255);
                THEM = Color.rgb(30, 30, 30);
                PILL = Color.rgb(25, 25, 25);
                CODE = Color.rgb(15, 15, 15);
                ERROR_BG = Color.rgb(90, 0, 0);
                DIALOG = Color.rgb(20, 20, 20);
                TEXT = Color.rgb(255, 255, 255);
                DIM = Color.rgb(200, 200, 200);
                FAINT = Color.rgb(150, 150, 150);
                ACCENT = Color.rgb(255, 255, 255);
                GREEN = Color.rgb(110, 255, 150);
                BLUE = Color.rgb(120, 200, 255);
                RED = Color.rgb(255, 90, 100);
                AMBER = Color.rgb(255, 215, 0);
                ON_ME = Color.rgb(0, 0, 0);
                TICK = Color.rgb(0, 80, 200);
                ON_ME_DIM = Color.rgb(60, 60, 60);
                CODE_TEXT = Color.rgb(255, 220, 150);
                LINE = Color.rgb(120, 120, 120);
                ON_ACCENT = Color.rgb(0, 0, 0);
                ADD_BG = Color.rgb(0, 60, 0);
                ADD_FG = Color.rgb(120, 255, 140);
                DEL_BG = Color.rgb(80, 0, 0);
                DEL_FG = Color.rgb(255, 150, 150);
                DANGER = Color.rgb(180, 0, 0);
                agentColors = new Color[] {Color.rgb(120, 200, 255), Color.rgb(255, 215, 0), Color.rgb(230, 150, 255), Color.rgb(120, 255, 140),
                    Color.rgb(255, 160, 110), Color.rgb(255, 130, 180), Color.rgb(100, 240, 250), Color.rgb(200, 240, 140)};
            }
            default -> {
                BG = Color.rgb(20, 26, 35);
                SIDEBAR = Color.rgb(30, 37, 48);
                PANEL = Color.rgb(38, 47, 60);
                FIELD = Color.rgb(50, 61, 77);
                SELECTED = Color.rgb(50, 61, 77);
                ME = Color.rgb(246, 250, 244);
                THEM = Color.rgb(38, 47, 60);
                PILL = Color.rgb(27, 34, 44);
                CODE = Color.rgb(14, 18, 25);
                ERROR_BG = Color.rgb(74, 30, 36);
                DIALOG = Color.rgb(38, 47, 60);
                TEXT = Color.rgb(240, 243, 240);
                DIM = Color.rgb(142, 154, 170);
                FAINT = Color.rgb(92, 104, 120);
                ACCENT = Color.rgb(246, 250, 244);
                GREEN = Color.rgb(125, 205, 155);
                BLUE = Color.rgb(112, 172, 240);
                RED = Color.rgb(240, 104, 114);
                AMBER = Color.rgb(240, 192, 92);
                ON_ME = Color.rgb(30, 37, 48);
                TICK = Color.rgb(38, 118, 210);
                ON_ME_DIM = Color.rgb(104, 116, 132);
                CODE_TEXT = Color.rgb(255, 214, 165);
                LINE = Color.rgb(46, 56, 71);
                ON_ACCENT = Color.rgb(30, 37, 48);
                ADD_BG = Color.rgb(24, 56, 42);
                ADD_FG = Color.rgb(140, 220, 160);
                DEL_BG = Color.rgb(70, 30, 36);
                DEL_FG = Color.rgb(240, 150, 158);
                DANGER = Color.rgb(178, 48, 60);
                agentColors = new Color[] {Color.rgb(83, 189, 235), Color.rgb(255, 202, 40), Color.rgb(224, 120, 255), Color.rgb(102, 187, 106),
                    Color.rgb(255, 138, 101), Color.rgb(240, 98, 146), Color.rgb(77, 208, 225), Color.rgb(174, 213, 129)};
            }
        }
        custom.forEach((key, color) -> {
            int dash = key.indexOf('-');
            boolean scoped = dash > 0 && java.util.Set.of("dark", "light", "contrast").contains(key.substring(0, dash));
            if (!scoped) {
                set(key, color);
            }
        });
        custom.forEach((key, color) -> {
            if (key.startsWith(n + "-")) {
                set(key.substring(n.length() + 1), color); // the palette's own colour wins over the general one
            }
        });
    }

    /** Puts your own colours (see {@link ThemeFile}) over the palette, now and whenever the palette is switched. */
    static synchronized void customise(java.util.Map<String, Color> colours) {
        custom = java.util.Map.copyOf(colours);
        String n = current;
        current = "";
        use(n);
    }

    private static void set(String name, Color c) {
        switch (name) {
            case "bg" -> BG = c;
            case "sidebar" -> SIDEBAR = c;
            case "panel" -> PANEL = c;
            case "field" -> FIELD = c;
            case "selected" -> SELECTED = c;
            case "me" -> ME = c;
            case "them" -> THEM = c;
            case "pill" -> PILL = c;
            case "code" -> CODE = c;
            case "error-bg" -> ERROR_BG = c;
            case "dialog" -> DIALOG = c;
            case "text" -> TEXT = c;
            case "dim" -> DIM = c;
            case "faint" -> FAINT = c;
            case "accent" -> ACCENT = c;
            case "green" -> GREEN = c;
            case "blue" -> BLUE = c;
            case "red" -> RED = c;
            case "amber" -> AMBER = c;
            case "on-me" -> ON_ME = c;
            case "on-me-dim" -> ON_ME_DIM = c;
            case "tick" -> TICK = c;
            case "code-text" -> CODE_TEXT = c;
            case "line" -> LINE = c;
            case "on-accent" -> ON_ACCENT = c;
            case "add-bg" -> ADD_BG = c;
            case "add-fg" -> ADD_FG = c;
            case "del-bg" -> DEL_BG = c;
            case "del-fg" -> DEL_FG = c;
            case "danger" -> DANGER = c;
            default -> {
                if (name.startsWith("agent-")) {
                    int i = Integer.parseInt(name.substring(6)) - 1;
                    agentColors = agentColors.clone();
                    agentColors[i] = c;
                }
            }
        }
    }

    static String current() {
        return current;
    }

    static Style on(Color fg, Color bg) {
        return Style.create().fg(fg).bg(bg);
    }

    static Color agentColor(String name) {
        return agentColors[Math.floorMod(name.hashCode(), agentColors.length)];
    }
}
