package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import java.util.Arrays;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * C# syntax highlighter
 */
public class CSharpHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "abstract", "as", "base", "bool", "break", "byte", "case", "catch", "char", "checked",
      "class", "const", "continue", "decimal", "default", "delegate", "do", "double", "else",
      "enum", "event", "explicit", "extern", "false", "finally", "fixed", "float", "for",
      "foreach", "goto", "if", "implicit", "in", "int", "interface", "internal", "is", "lock",
      "long", "namespace", "new", "null", "object", "operator", "out", "override", "params",
      "private", "protected", "public", "readonly", "ref", "return", "sbyte", "sealed",
      "short", "sizeof", "stackalloc", "static", "string", "struct", "switch", "this", "throw",
      "true", "try", "typeof", "uint", "ulong", "unchecked", "unsafe", "ushort", "using",
      "virtual", "void", "volatile", "while", "add", "alias", "ascending", "async", "await",
      "by", "descending", "dynamic", "equals", "from", "get", "global", "group", "into", "join",
      "let", "nameof", "on", "orderby", "partial", "remove", "select", "set", "value", "var",
      "when", "where", "yield"
  };

  private final String[] preprocessor = {
      "#if", "#else", "#elif", "#endif", "#define", "#undef", "#warning", "#error", "#line",
      "#region", "#endregion", "#pragma"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // C# literals
    String[] literals = {"null", "true", "false"};

    // Highlight method declarations
    code = SyntaxHighlighter.highlightPattern(code,
        "(public|private|protected|internal|static|virtual|override|abstract|async)?(\\s+[a-zA-Z_<>][a-zA-Z0-9_<>\\[\\]]*)+\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\([^\\)]*\\)",
        this::methodStyle);

    // Apply C#-specific keyword highlighting
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

    // Highlight attributes
    code = SyntaxHighlighter.highlightPattern(code, "\\[\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*\\]", BeautifyShell::yellowFg);

    // Highlight string interpolation
    code = SyntaxHighlighter.highlightPattern(code, "\\$\"[^\"]*\"", BeautifyShell::greenFg);

    // Highlight namespaces
    code = SyntaxHighlighter.highlightPattern(code, "\\bnamespace\\s+[a-zA-Z_.][a-zA-Z0-9_.]*",
        s -> BeautifyShell.magentaFg("namespace") + " " + BeautifyShell.yellowFg(s.substring(10)));

    // Highlight class/interface/struct/enum declarations
    code = SyntaxHighlighter.highlightPattern(code, "\\b(class|interface|struct|enum)\\s+[a-zA-Z_][a-zA-Z0-9_]*",
        s -> BeautifyShell.magentaFg(s.substring(0, s.indexOf(" "))) +
            " " + BeautifyShell.greenFg(s.substring(s.indexOf(" ") + 1)));

    // Highlight generics
    code = SyntaxHighlighter.highlightPattern(code, "<[a-zA-Z_][a-zA-Z0-9_,\\s]*>", BeautifyShell::cyanFg);

    // Highlight property accessors
    code = SyntaxHighlighter.highlightPattern(code, "\\b(get|set)\\s*[{]",
        s -> BeautifyShell.magentaFg(s.substring(0, s.indexOf("{"))) + "{");

    return code;
  }

  private String methodStyle(String s) {
    int startParam = s.indexOf("(");
    String beforeParams = s.substring(0, startParam);
    String[] parts = beforeParams.trim().split("\\s+");

    if (parts.length < 2) return s;

    StringBuilder result = new StringBuilder();

    // Check for access modifiers and other keywords
    for (int i = 0; i < parts.length - 2; i++) {
      if (Arrays.asList(keywords).contains(parts[i])) {
        result.append(BeautifyShell.magentaFg(parts[i])).append(" ");
      } else {
        result.append(parts[i]).append(" ");
      }
    }

    // Highlight return type
    result.append(BeautifyShell.cyanFg(parts[parts.length - 2])).append(" ");

    // Highlight method name
    result.append(blueFg(parts[parts.length - 1]));

    // Add parameters
    result.append(s.substring(startParam));

    return result.toString();
  }
}