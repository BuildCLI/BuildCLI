package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import java.util.Arrays;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * Java syntax highlighter
 */
public class JavaHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
      "class", "const", "continue", "default", "do", "double", "else", "enum",
      "extends", "final", "finally", "float", "for", "goto", "if", "implements",
      "import", "instanceof", "int", "interface", "long", "native", "new", "package",
      "private", "protected", "public", "return", "short", "static", "strictfp",
      "super", "switch", "synchronized", "this", "throw", "throws", "transient",
      "try", "void", "volatile", "while", "var", "record", "sealed", "permits",
      "yield"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // Java literals
    String[] literals = {"true", "false", "null"};

    // Highlight method declarations (approximate)
    code = SyntaxHighlighter.highlightPattern(code, "\\b\\w+\\s*\\(", this::methodStyle);

    // Apply Java-specific keyword highlighting
    for (String keyword : keywords()) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Apply literals highlighting
    for (String literal : literals) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + literal + "\\b", BeautifyShell::brightGreenFg);
    }

    // Highlight annotations
    code = SyntaxHighlighter.highlightPattern(code, "@\\w+", BeautifyShell::yellowFg);

    code = SyntaxHighlighter.highlightPattern(code, "[()\\\\[\\\\]{}]", BeautifyShell::greenFg);

    return code;
  }

  private String methodStyle(String s) {
    if (Arrays.asList(keywords).contains(s.replace("(", "").trim())) return s;

    return blueFg(s.substring(0, s.length() - 1)) + s.substring(s.length() - 1);
  }
}
