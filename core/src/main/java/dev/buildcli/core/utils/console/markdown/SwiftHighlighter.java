package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import java.util.Arrays;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * Swift syntax highlighter
 */
public class SwiftHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "associatedtype", "class", "deinit", "enum", "extension", "fileprivate",
      "func", "import", "init", "inout", "internal", "let", "open", "operator",
      "private", "protocol", "public", "rethrows", "static", "struct", "subscript",
      "typealias", "var", "break", "case", "catch", "continue", "default", "defer",
      "do", "else", "fallthrough", "for", "guard", "if", "in", "repeat", "return",
      "throw", "throws", "try", "where", "while", "as", "Any", "catch", "false",
      "is", "nil", "super", "self", "Self", "true", "throws", "throw", "rethrows"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // Swift literals
    String[] literals = {"true", "false", "nil"};

    // Highlight function declarations
    code = SyntaxHighlighter.highlightPattern(code, "\\bfunc\\s+\\w+", this::functionStyle);

    // Highlight method calls
    code = SyntaxHighlighter.highlightPattern(code, "\\b\\w+\\s*\\(", this::methodCallStyle);

    // Apply Swift-specific keyword highlighting
    for (String keyword : keywords()) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Apply literals highlighting
    for (String literal : literals) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + literal + "\\b", BeautifyShell::brightGreenFg);
    }

    // Highlight type annotations
    code = SyntaxHighlighter.highlightPattern(code, ":\\s*[A-Z]\\w*", BeautifyShell::yellowFg);

    // Highlight Swift attributes
    code = SyntaxHighlighter.highlightPattern(code, "@\\w+", BeautifyShell::blueFg);

    // Highlight Swift optional types
    code = SyntaxHighlighter.highlightPattern(code, "\\w+\\?", BeautifyShell::yellowFg);

    // Highlight Swift forced unwrapping
    code = SyntaxHighlighter.highlightPattern(code, "\\w+!", BeautifyShell::redFg);

    code = SyntaxHighlighter.highlightPattern(code, "[()\\\\[\\\\]{}]", BeautifyShell::greenFg);

    return code;
  }

  private String functionStyle(String s) {
    String functionName = s.replace("func", "").trim();
    return "func " + blueFg(functionName);
  }

  private String methodCallStyle(String s) {
    if (Arrays.asList(keywords).contains(s.replace("(", "").trim())) return s;

    return blueFg(s.substring(0, s.length() - 1)) + s.substring(s.length() - 1);
  }
}