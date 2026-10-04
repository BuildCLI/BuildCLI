package dev.buildcli.infrastructure.tui;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.syntax.RegexSyntaxHighlighter;
import dev.tamboui.widgets.syntax.SyntaxTheme;
import dev.tamboui.widgets.syntax.TokenType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Colours code blocks with TamboUI's highlighter. It only changes the foreground of what the block's own style already
 * says, so the code background of every theme stays; a language it does not know is left as plain code.
 */
final class CodeHighlight {
    private static final RegexSyntaxHighlighter HIGHLIGHTER = RegexSyntaxHighlighter.defaults();
    /** Names people put after the fence that the highlighter knows under another name. */
    private static final Map<String, String> ALIASES = Map.of("yml", "yaml", "shell", "bash", "zsh", "bash", "console", "bash",
            "py", "python", "kt", "java", "javascript", "js", "typescript", "ts", "jsonc", "json", "htm", "html");

    private CodeHighlight() {}

    /** The palette of a theme: dark text colours on dark code, darker ones on the light theme, bright ones for contrast. */
    static SyntaxTheme themeFor(String name) {
        return switch (name == null ? "dark" : name) {
            case "light" -> palette(c(0x8a3fb0), c(0x3c7a2e), c(0xb35a00), c(0x6a737d), c(0x0b5fa5), c(0x8a5a00), c(0x6a4bb0), c(0x4a5560));
            case "contrast" -> palette(c(0xff8cf2), c(0x9dff9d), c(0xffd24a), c(0xb0b0b0), c(0x8fd3ff), c(0xffb36b), c(0xc6a8ff), c(0xe0e0e0));
            default -> SyntaxTheme.DEFAULTS;
        };
    }

    private static Color c(int rgb) {
        return Color.rgb(rgb >> 16 & 0xff, rgb >> 8 & 0xff, rgb & 0xff);
    }

    private static SyntaxTheme palette(Color keyword, Color string, Color number, Color comment, Color type, Color constant, Color function, Color punctuation) {
        return SyntaxTheme.builder()
                .token(TokenType.KEYWORD, Style.create().fg(keyword))
                .token(TokenType.STRING, Style.create().fg(string))
                .token(TokenType.NUMBER, Style.create().fg(number))
                .token(TokenType.COMMENT, Style.create().fg(comment).italic())
                .token(TokenType.TYPE, Style.create().fg(type))
                .token(TokenType.CONSTANT, Style.create().fg(constant))
                .token(TokenType.FUNCTION, Style.create().fg(function))
                .token(TokenType.ANNOTATION, Style.create().fg(constant))
                .token(TokenType.OPERATOR, Style.create().fg(punctuation))
                .token(TokenType.PUNCTUATION, Style.create().fg(punctuation))
                .token(TokenType.TAG, Style.create().fg(keyword))
                .token(TokenType.ATTRIBUTE, Style.create().fg(type))
                .build();
    }

    /** The extension of a file name or path ("src/A.java" gives "java"), or "" when it has none. */
    static String extension(String path) {
        String name = path == null ? "" : path.strip().replaceAll("^.*[/\\\\]", "");
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }

    /** True when the highlighter has a grammar for this fence label ("java", "yml", "json title=x"...). */
    static boolean knows(String label) {
        return HIGHLIGHTER.grammar(language(label)) != null;
    }

    private static String language(String label) {
        String first = label == null ? "" : label.strip().split("[\\s{]", 2)[0].toLowerCase(Locale.ROOT);
        return ALIASES.getOrDefault(first, first);
    }

    /**
     * One array of styles per line of {@code block}, one style per character, or null when the language is unknown or
     * the highlighter changed the text (then the caller draws the block plain rather than risk a wrong picture).
     */
    static List<Style[]> styles(String block, String label, Style base) {
        String lang = language(label);
        if (lang.isEmpty() || HIGHLIGHTER.grammar(lang) == null) {
            return null;
        }
        List<Line> lines = HIGHLIGHTER.highlight(block, lang, base, themeFor(Theme.current()));
        String[] source = block.split("\n", -1);
        if (lines.size() != source.length) {
            return null;
        }
        List<Style[]> out = new ArrayList<>();
        for (int i = 0; i < source.length; i++) {
            Style[] styles = new Style[source[i].length()];
            int at = 0;
            for (Span span : lines.get(i).spans()) {
                Style merged = base.patch(span.style());
                for (int k = 0; k < span.content().length() && at < styles.length; k++) {
                    styles[at++] = merged;
                }
            }
            if (at != styles.length) {
                return null;
            }
            out.add(styles);
        }
        return out;
    }
}
