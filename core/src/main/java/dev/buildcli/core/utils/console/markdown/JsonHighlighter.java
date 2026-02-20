package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

/**
 * JSON syntax highlighter
 */
public class JsonHighlighter implements LanguageHighlighter {
  @Override
  public String[] keywords() {
    return new String[0]; // JSON doesn't have traditional keywords
  }

  @Override
  public String highlight(String code) {
    // Start with generic highlighting for strings and numbers
    code = SyntaxHighlighter.highlightGeneric(code);

    // Highlight property names
    code = SyntaxHighlighter.highlightPattern(code, "\"[^\"]+\"\\s*:",
        s -> BeautifyShell.yellowFg(s.substring(0, s.length() - 1)) + ":");

    // Highlight literals
    code = SyntaxHighlighter.highlightPattern(code, "\\b(true|false|null)\\b", BeautifyShell::brightGreenFg);

    // Highlight brackets and braces
    code = SyntaxHighlighter.highlightPattern(code, "[\\[\\]{}]", BeautifyShell::cyanFg);

    return code;
  }
}