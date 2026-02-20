package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import java.util.Arrays;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * PHP syntax highlighter
 */
public class PhpHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "abstract", "and", "array", "as", "break", "callable", "case", "catch",
      "class", "clone", "const", "continue", "declare", "default", "die", "do",
      "echo", "else", "elseif", "empty", "enddeclare", "endfor", "endforeach",
      "endif", "endswitch", "endwhile", "eval", "exit", "extends", "final",
      "finally", "fn", "for", "foreach", "function", "global", "goto", "if",
      "implements", "include", "include_once", "instanceof", "insteadof",
      "interface", "isset", "list", "match", "namespace", "new", "or", "print",
      "private", "protected", "public", "require", "require_once", "return",
      "static", "switch", "throw", "trait", "try", "unset", "use", "var",
      "while", "xor", "yield", "yield from"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // PHP literals
    String[] literals = {"true", "false", "null", "NULL", "TRUE", "FALSE"};

    // Highlight method declarations
    code = SyntaxHighlighter.highlightPattern(code, "\\b\\w+\\s*\\(", this::methodStyle);

    // Apply PHP-specific keyword highlighting
    for (String keyword : keywords()) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Apply literals highlighting
    for (String literal : literals) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + literal + "\\b", BeautifyShell::brightGreenFg);
    }

    // Highlight PHP variable names
    code = SyntaxHighlighter.highlightPattern(code, "\\$\\w+", BeautifyShell::yellowFg);

    // Highlight PHP tag delimiters
    code = SyntaxHighlighter.highlightPattern(code, "<\\?php|\\?>", BeautifyShell::redFg);

    code = SyntaxHighlighter.highlightPattern(code, "[()\\\\[\\\\]{}]", BeautifyShell::greenFg);

    return code;
  }

  private String methodStyle(String s) {
    if (Arrays.asList(keywords).contains(s.replace("(", "").trim())) return s;

    return blueFg(s.substring(0, s.length() - 1)) + s.substring(s.length() - 1);
  }
}