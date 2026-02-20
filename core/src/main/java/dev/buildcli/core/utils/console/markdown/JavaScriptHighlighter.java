package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * JavaScript syntax highlighter
 */
public class JavaScriptHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "await", "break", "case", "catch", "class", "const", "continue", "debugger",
      "default", "delete", "do", "else", "export", "extends", "finally", "for",
      "function", "if", "import", "in", "instanceof", "new", "return", "super",
      "switch", "this", "throw", "try", "typeof", "var", "void", "while", "with",
      "yield", "let", "static", "async", "of"
  };

  private final String[] literals = {
      "true", "false", "null", "undefined", "NaN", "Infinity"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // Highlight function declarations
    code = SyntaxHighlighter.highlightPattern(code, "function\\s+\\w+\\s*\\(", this::methodStyle);
    code = SyntaxHighlighter.highlightPattern(code, "\\b\\w+\\s*:\\s*function\\s*\\(", this::objectMethodStyle);
    code = SyntaxHighlighter.highlightPattern(code, "\\b\\w+\\s*=\\s*function\\s*\\(", this::assignedMethodStyle);
    code = SyntaxHighlighter.highlightPattern(code, "\\b\\w+\\s*=\\s*\\([^)]*\\)\\s*=>", this::arrowFunctionStyle);

    // Apply JavaScript-specific keyword highlighting
    for (String keyword : keywords()) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Apply literals highlighting
    for (String literal : literals) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + literal + "\\b", BeautifyShell::brightGreenFg);
    }

    // Highlight built-in objects
    String[] builtIns = {"Object", "Array", "String", "Number", "Boolean", "Date", "Math", "RegExp", "Promise"};
    for (String builtIn : builtIns) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + builtIn + "\\b", BeautifyShell::cyanFg);
    }

    // Highlight template literals
    code = SyntaxHighlighter.highlightPattern(code, "`[^`]*`", BeautifyShell::greenFg);

    // Highlight brackets
    code = SyntaxHighlighter.highlightPattern(code, "[()\\\\[\\\\]{}]", BeautifyShell::greenFg);

    return code;
  }

  private String methodStyle(String s) {
    return BeautifyShell.magentaFg("function") + " " + blueFg(s.substring(9, s.length() - 1).trim()) + s.substring(s.length() - 1);
  }

  private String objectMethodStyle(String s) {
    int colonIndex = s.indexOf(":");
    String methodName = s.substring(0, colonIndex).trim();
    return blueFg(methodName) + ":" + BeautifyShell.magentaFg(" function") + s.substring(colonIndex + 9);
  }

  private String assignedMethodStyle(String s) {
    int equalsIndex = s.indexOf("=");
    String varName = s.substring(0, equalsIndex).trim();
    return blueFg(varName) + " =" + BeautifyShell.magentaFg(" function") + s.substring(equalsIndex + 9);
  }

  private String arrowFunctionStyle(String s) {
    int equalsIndex = s.indexOf("=");
    String varName = s.substring(0, equalsIndex).trim();
    int arrowIndex = s.lastIndexOf("=>");
    return blueFg(varName) + " = " + s.substring(equalsIndex + 1, arrowIndex) + BeautifyShell.magentaFg("=>");
  }
}