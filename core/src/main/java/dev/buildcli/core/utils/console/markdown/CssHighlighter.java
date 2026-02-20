package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

/**
 * CSS syntax highlighter
 */
public class CssHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "@import", "@media", "@font-face", "@keyframes", "@supports", "@charset",
      "!important", "inherit", "initial", "unset"
  };

  private final String[] properties = {
      "color", "background", "margin", "padding", "font", "border", "display",
      "position", "width", "height", "top", "left", "right", "bottom", "flex",
      "grid", "animation", "transition", "transform", "opacity", "z-index"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // Highlight selectors
    code = SyntaxHighlighter.highlightPattern(code, "[^}\\s][^{]*(?=\\s*\\{)", BeautifyShell::yellowFg);

    // Highlight properties
    for (String property : properties) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + property + "\\s*:",
          s -> BeautifyShell.cyanFg(s.substring(0, s.length() - 1)) + ":");
    }

    // Highlight values
    code = SyntaxHighlighter.highlightPattern(code, ":\\s*[^;\\s]+",
        s -> ":" + BeautifyShell.greenFg(s.substring(1)));

    // Highlight keywords
    for (String keyword : keywords) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Highlight colors
    code = SyntaxHighlighter.highlightPattern(code, "#[0-9a-fA-F]{3,8}\\b", BeautifyShell::greenFg);

    // Highlight brackets
    code = SyntaxHighlighter.highlightPattern(code, "[{}]", BeautifyShell::redFg);

    return code;
  }
}