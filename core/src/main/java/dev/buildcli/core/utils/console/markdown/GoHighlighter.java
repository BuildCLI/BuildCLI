package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * Go language syntax highlighter
 */
public class GoHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "break", "default", "func", "interface", "select", "case", "defer", "go", "map",
      "struct", "chan", "else", "goto", "package", "switch", "const", "fallthrough",
      "if", "range", "type", "continue", "for", "import", "return", "var"
  };

  private final String[] builtins = {
      "append", "cap", "close", "complex", "copy", "delete", "imag", "len", "make", "new",
      "panic", "print", "println", "real", "recover"
  };

  private final String[] types = {
      "bool", "byte", "complex64", "complex128", "error", "float32", "float64",
      "int", "int8", "int16", "int32", "int64", "rune", "string", "uint", "uint8",
      "uint16", "uint32", "uint64", "uintptr"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // Go literals
    String[] literals = {"nil", "true", "false", "iota"};

    // Highlight function declarations
    code = SyntaxHighlighter.highlightPattern(code, "\\bfunc\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(",
        s -> BeautifyShell.magentaFg("func") + " " +
            blueFg(s.substring(5, s.lastIndexOf("("))) + "(");

    // Highlight method declarations (with receiver)
    code = SyntaxHighlighter.highlightPattern(code, "\\bfunc\\s+\\([^)]+\\)\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(",
        this::methodWithReceiverStyle);

    // Apply Go-specific keyword highlighting
    for (String keyword : keywords()) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Highlight built-in functions
    for (String builtin : builtins) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + builtin + "\\b", BeautifyShell::blueFg);
    }

    // Highlight types
    for (String type : types) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + type + "\\b", BeautifyShell::cyanFg);
    }

    // Apply literals highlighting
    for (String literal : literals) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + literal + "\\b", BeautifyShell::brightGreenFg);
    }

    // Highlight struct field definitions
    code = SyntaxHighlighter.highlightPattern(code, "\\b[a-zA-Z_][a-zA-Z0-9_]*\\s+[a-zA-Z_][a-zA-Z0-9_\\[\\]]*\\s+`[^`]*`",
        this::structTagStyle);

    // Highlight struct literals
    code = SyntaxHighlighter.highlightPattern(code, "\\b[a-zA-Z_][a-zA-Z0-9_]*\\s*:",
        s -> BeautifyShell.yellowFg(s.substring(0, s.length() - 1)) + ":");

    // Highlight package imports
    code = SyntaxHighlighter.highlightPattern(code, "\\bimport\\s+\\([\\s\\S]*?\\)", this::importStyle);

    // Highlight package declaration
    code = SyntaxHighlighter.highlightPattern(code, "\\bpackage\\s+[a-zA-Z_][a-zA-Z0-9_]*",
        s -> BeautifyShell.magentaFg("package") + " " + BeautifyShell.yellowFg(s.substring(8)));

    return code;
  }

  private String methodWithReceiverStyle(String s) {
    int receiverStart = s.indexOf("(");
    int receiverEnd = s.indexOf(")");
    int methodStart = s.indexOf(" ", receiverEnd + 1) + 1;
    int paramStart = s.lastIndexOf("(");

    if (receiverStart < 0 || receiverEnd < 0 || methodStart < 0 || paramStart < 0) {
      return s;
    }

    return BeautifyShell.magentaFg("func") + " " +
        s.substring(receiverStart, receiverEnd + 1) + " " +
        blueFg(s.substring(methodStart, paramStart).trim()) + "(";
  }

  private String structTagStyle(String s) {
    int firstSpace = s.indexOf(" ");
    int lastSpace = s.lastIndexOf(" `");

    if (firstSpace < 0 || lastSpace < 0) return s;

    String fieldName = s.substring(0, firstSpace);
    String fieldType = s.substring(firstSpace + 1, lastSpace);
    String tag = s.substring(lastSpace + 1);

    return BeautifyShell.yellowFg(fieldName) + " " +
        BeautifyShell.cyanFg(fieldType) + " " +
        BeautifyShell.greenFg(tag);
  }

  private String importStyle(String s) {
    return s.replaceAll("\"([^\"]+)\"", BeautifyShell.greenFg("\"$1\""));
  }
}