package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * Bash/Shell script syntax highlighter
 */
public class BashHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "if", "then", "else", "elif", "fi", "case", "esac", "for", "select", "while", "until",
      "do", "done", "in", "function", "time", "coproc", "break", "continue", "return",
      "shift", "eval", "exec", "exit", "export", "readonly", "set", "unset", "declare", "local"
  };

  private final String[] builtins = {
      "echo", "printf", "read", "cd", "pwd", "pushd", "popd", "dirs", "let", "expr",
      "test", "source", "alias", "unalias", "trap", "true", "false", "bg", "fg", "jobs",
      "kill", "wait", "disown", "umask", "getopts"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // Highlight shebang
    code = SyntaxHighlighter.highlightPattern(code, "^#!.*$", BeautifyShell::greenFg, true);

    // Highlight variables
    code = SyntaxHighlighter.highlightPattern(code, "\\$\\w+", BeautifyShell::cyanFg);
    code = SyntaxHighlighter.highlightPattern(code, "\\$\\{[^}]+\\}", BeautifyShell::cyanFg);

    // Highlight command substitution
    code = SyntaxHighlighter.highlightPattern(code, "\\$\\([^)]+\\)", BeautifyShell::yellowFg);
    code = SyntaxHighlighter.highlightPattern(code, "`[^`]+`", BeautifyShell::yellowFg);

    // Apply Bash-specific keyword highlighting
    for (String keyword : keywords()) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Highlight builtin commands
    for (String builtin : builtins) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + builtin + "\\b", BeautifyShell::blueFg);
    }

    // Highlight function declarations
    code = SyntaxHighlighter.highlightPattern(code, "\\b\\w+\\s*\\(\\s*\\)\\s*\\{", this::functionStyle);

    // Highlight redirections
    code = SyntaxHighlighter.highlightPattern(code, "[><](&[0-9])?|[0-9]?>>|[0-9]?<<", BeautifyShell::redFg);

    return code;
  }

  private String functionStyle(String s) {
    int parenIndex = s.indexOf("(");
    String funcName = s.substring(0, parenIndex).trim();
    return blueFg(funcName) + s.substring(parenIndex);
  }
}