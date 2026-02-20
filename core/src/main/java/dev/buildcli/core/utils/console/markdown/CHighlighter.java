package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import java.util.Arrays;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * C language syntax highlighter
 */
public class CHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
      "else", "enum", "extern", "float", "for", "goto", "if", "int", "long", "register",
      "return", "short", "signed", "sizeof", "static", "struct", "switch", "typedef",
      "union", "unsigned", "void", "volatile", "while", "_Bool", "_Complex", "_Imaginary",
      "inline", "restrict"
  };

  private final String[] preprocessor = {
      "#include", "#define", "#undef", "#ifdef", "#ifndef", "#if", "#else", "#elif",
      "#endif", "#error", "#pragma", "#line"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // C literals
    String[] literals = {"NULL", "true", "false"};

    // Highlight function declarations
    code = SyntaxHighlighter.highlightPattern(code, "[a-zA-Z_][a-zA-Z0-9_]*\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\([^\\)]*\\)\\s*\\{",
        this::functionStyle);

    // Apply C-specific keyword highlighting
    for (String keyword : keywords()) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Apply literals highlighting
    for (String literal : literals) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + literal + "\\b", BeautifyShell::brightGreenFg);
    }

    // Highlight preprocessor directives
    for (String directive : preprocessor) {
      code = SyntaxHighlighter.highlightPattern(code, "^\\s*" + directive + ".*$", BeautifyShell::cyanFg, true);
    }

    // Highlight struct/enum definitions
    code = SyntaxHighlighter.highlightPattern(code, "\\b(struct|enum|union)\\s+[a-zA-Z_][a-zA-Z0-9_]*",
        s -> BeautifyShell.magentaFg(s.substring(0, s.indexOf(" "))) +
            " " + BeautifyShell.greenFg(s.substring(s.indexOf(" ") + 1)));

    // Highlight type definitions
    code = SyntaxHighlighter.highlightPattern(code, "\\btypedef\\s+.*?\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*;",
        this::typedefStyle);

    // Highlight brackets and braces
    code = SyntaxHighlighter.highlightPattern(code, "[()\\[\\]{}]", BeautifyShell::greenFg);

    return code;
  }

  private String functionStyle(String s) {
    int startParam = s.indexOf("(");
    String beforeParams = s.substring(0, startParam);
    String[] parts = beforeParams.trim().split("\\s+");

    if (parts.length < 2) return s;

    StringBuilder result = new StringBuilder();

    // Highlight the return type
    for (int i = 0; i < parts.length - 1; i++) {
      if (Arrays.asList(keywords).contains(parts[i])) {
        result.append(BeautifyShell.magentaFg(parts[i])).append(" ");
      } else {
        result.append(parts[i]).append(" ");
      }
    }

    // Highlight the function name
    result.append(blueFg(parts[parts.length - 1]));

    // Add the rest of the function
    result.append(s.substring(startParam));

    return result.toString();
  }

  private String typedefStyle(String s) {
    String keyword = "typedef";
    String withoutTypedef = s.substring(keyword.length()).trim();
    int lastSpace = withoutTypedef.lastIndexOf(' ');
    int semicolon = withoutTypedef.lastIndexOf(';');

    if (lastSpace < 0 || semicolon < 0) return s;

    String type = withoutTypedef.substring(0, lastSpace).trim();
    String newType = withoutTypedef.substring(lastSpace + 1, semicolon).trim();

    return BeautifyShell.magentaFg(keyword) + " " + type + " " + BeautifyShell.greenFg(newType) + ";";
  }
}