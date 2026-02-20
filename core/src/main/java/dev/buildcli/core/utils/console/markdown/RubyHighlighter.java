package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import java.util.Arrays;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * Ruby syntax highlighter
 */
public class RubyHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "BEGIN", "END", "alias", "and", "begin", "break", "case", "class",
      "def", "defined?", "do", "else", "elsif", "end", "ensure", "false",
      "for", "if", "in", "module", "next", "nil", "not", "or", "redo",
      "rescue", "retry", "return", "self", "super", "then", "true",
      "undef", "unless", "until", "when", "while", "yield"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // Ruby literals
    String[] literals = {"true", "false", "nil"};

    // Highlight method declarations
    code = SyntaxHighlighter.highlightPattern(code, "\\bdef\\s+\\w+", this::methodStyle);

    // Highlight method calls
    code = SyntaxHighlighter.highlightPattern(code, "\\b\\w+\\s*\\(", this::methodCallStyle);

    // Apply Ruby-specific keyword highlighting
    for (String keyword : keywords()) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Apply literals highlighting
    for (String literal : literals) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + literal + "\\b", BeautifyShell::brightGreenFg);
    }

    // Highlight Ruby symbols
    code = SyntaxHighlighter.highlightPattern(code, ":\\w+", BeautifyShell::yellowFg);

    // Highlight instance variables
    code = SyntaxHighlighter.highlightPattern(code, "@\\w+", BeautifyShell::yellowFg);

    // Highlight class variables
    code = SyntaxHighlighter.highlightPattern(code, "@@\\w+", BeautifyShell::yellowFg);

    // Highlight global variables
    code = SyntaxHighlighter.highlightPattern(code, "\\$\\w+", BeautifyShell::redFg);

    code = SyntaxHighlighter.highlightPattern(code, "[()\\\\[\\\\]{}]", BeautifyShell::greenFg);

    return code;
  }

  private String methodStyle(String s) {
    String methodName = s.replace("def", "").trim();
    return "def " + blueFg(methodName);
  }

  private String methodCallStyle(String s) {
    if (Arrays.asList(keywords).contains(s.replace("(", "").trim())) return s;

    return blueFg(s.substring(0, s.length() - 1)) + s.substring(s.length() - 1);
  }
}