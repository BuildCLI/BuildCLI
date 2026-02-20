package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

/**
 * Markdown syntax highlighter
 */
public class MarkdownHighlighter implements LanguageHighlighter {
  @Override
  public String[] keywords() {
    return new String[0]; // Markdown doesn't have traditional keywords
  }

  @Override
  public String highlight(String code) {
    // Highlight headers
    code = SyntaxHighlighter.highlightPattern(code, "^#{1,6}\\s.*$", BeautifyShell::blueFg, true);

    // Highlight bold
    code = SyntaxHighlighter.highlightPattern(code, "\\*\\*[^*]+\\*\\*", BeautifyShell::brightWhiteFg);
    code = SyntaxHighlighter.highlightPattern(code, "__[^_]+__", BeautifyShell::brightWhiteFg);

    // Highlight italic
    code = SyntaxHighlighter.highlightPattern(code, "\\*[^*]+\\*", BeautifyShell::yellowFg);
    code = SyntaxHighlighter.highlightPattern(code, "_[^_]+_", BeautifyShell::yellowFg);

    // Highlight links
    code = SyntaxHighlighter.highlightPattern(code, "\\[([^\\]]+)\\]\\(([^)]+)\\)",
        s -> BeautifyShell.greenFg(s));

    // Highlight code blocks
    code = SyntaxHighlighter.highlightPattern(code, "```[\\s\\S]*?```", BeautifyShell::cyanFg);

    // Highlight inline code
    code = SyntaxHighlighter.highlightPattern(code, "`[^`]+`", BeautifyShell::cyanFg);

    // Highlight blockquotes
    code = SyntaxHighlighter.highlightPattern(code, "^>.*$", BeautifyShell::magentaFg, true);

    // Highlight lists
    code = SyntaxHighlighter.highlightPattern(code, "^[\\s]*[\\*\\-\\+]\\s.*$", BeautifyShell::yellowFg, true);
    code = SyntaxHighlighter.highlightPattern(code, "^[\\s]*\\d+\\.\\s.*$", BeautifyShell::yellowFg, true);

    return code;
  }
}