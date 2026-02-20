package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import java.util.Arrays;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * SQL syntax highlighter
 */
public class SqlHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "SELECT", "FROM", "WHERE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "FULL",
      "ON", "GROUP", "BY", "HAVING", "ORDER", "LIMIT", "OFFSET", "INSERT", "INTO",
      "VALUES", "UPDATE", "SET", "DELETE", "CREATE", "TABLE", "ALTER", "DROP", "INDEX",
      "VIEW", "PROCEDURE", "FUNCTION", "TRIGGER", "DATABASE", "SCHEMA", "GRANT", "REVOKE",
      "COMMIT", "ROLLBACK", "SAVEPOINT", "TRANSACTION", "BEGIN", "END", "DECLARE", "CASE",
      "WHEN", "THEN", "ELSE", "UNION", "ALL", "DISTINCT", "AS", "AND", "OR", "NOT", "NULL",
      "IS", "IN", "BETWEEN", "LIKE", "EXISTS"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // Case-insensitive keyword highlighting for SQL
    for (String keyword : keywords()) {
      code = SyntaxHighlighter.highlightPattern(code, "(?i)\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Highlight column aliases
    code = SyntaxHighlighter.highlightPattern(code, "(?i)\\bAS\\s+\\w+",
        s -> BeautifyShell.magentaFg("AS") + " " + BeautifyShell.yellowFg(s.substring(3)));

    // Highlight function calls
    code = SyntaxHighlighter.highlightPattern(code, "\\b\\w+\\s*\\(", this::functionStyle);

    // Highlight table names (simplistic approach)
    code = SyntaxHighlighter.highlightPattern(code, "(?i)FROM\\s+[\\w\\.]+",
        s -> BeautifyShell.magentaFg("FROM") + " " + BeautifyShell.greenFg(s.substring(5)));

    code = SyntaxHighlighter.highlightPattern(code, "(?i)JOIN\\s+[\\w\\.]+",
        s -> BeautifyShell.magentaFg(s.substring(0, 4)) + " " + BeautifyShell.greenFg(s.substring(5)));

    return code;
  }

  private String functionStyle(String s) {
    if (Arrays.asList(keywords).contains(s.replace("(", "").trim().toUpperCase())) return s;

    return blueFg(s.substring(0, s.length() - 1)) + s.substring(s.length() - 1);
  }
}