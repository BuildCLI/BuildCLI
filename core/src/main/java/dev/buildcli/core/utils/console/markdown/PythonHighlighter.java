package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * Python syntax highlighter
 */
public class PythonHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class",
      "continue", "def", "del", "elif", "else", "except", "finally", "for", "from", "global",
      "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise",
      "return", "try", "while", "with", "yield"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // Python literals
    String[] literals = {"True", "False", "None"};

    // Highlight function declarations
    code = SyntaxHighlighter.highlightPattern(code, "def\\s+\\w+\\s*\\(", this::methodStyle);

    // Apply Python-specific keyword highlighting
    for (String keyword : keywords()) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Apply literals highlighting
    for (String literal : literals) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + literal + "\\b", BeautifyShell::brightGreenFg);
    }

    // Highlight decorators
    code = SyntaxHighlighter.highlightPattern(code, "@\\w+", BeautifyShell::yellowFg);

    // Highlight f-strings
    code = SyntaxHighlighter.highlightPattern(code, "f['\"].*?['\"]", BeautifyShell::greenFg);

    code = SyntaxHighlighter.highlightPattern(code, "[()\\\\[\\\\]{}]", BeautifyShell::greenFg);

    return code;
  }

  private String methodStyle(String s) {
    return BeautifyShell.magentaFg("def") + " " + blueFg(s.substring(4, s.length() - 1).trim()) + s.substring(s.length() - 1);
  }
}