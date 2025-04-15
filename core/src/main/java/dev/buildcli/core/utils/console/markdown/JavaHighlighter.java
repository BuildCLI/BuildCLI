package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import static dev.buildcli.core.utils.BeautifyShell.yellowFg;
import static dev.buildcli.core.utils.console.markdown.SyntaxHighlighter.highlightGeneric;
import static dev.buildcli.core.utils.console.markdown.SyntaxHighlighter.highlightPattern;

/**
 * Java syntax highlighter
 */
public class JavaHighlighter implements LanguageHighlighter {
  @Override
  public String highlight(String code) {
    // Java keywords
    String[] keywords = {
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "final", "finally", "float", "for", "goto", "if", "implements",
        "import", "instanceof", "int", "interface", "long", "native", "new", "package",
        "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "void", "volatile", "while", "var", "record", "sealed", "permits",
        "yield"
    };

    // Java literals
    String[] literals = {"true", "false", "null"};

    // Apply general highlighting
    code = highlightGeneric(code);

    // Apply Java-specific keyword highlighting
    for (String keyword : keywords) {
      code = highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::brightMagentaFg);
    }

    // Apply literals highlighting
    for (String literal : literals) {
      code = highlightPattern(code, "\\b" + literal + "\\b", BeautifyShell::cyanFg);
    }

    // Highlight annotations
    code = highlightPattern(code, "@\\w+", BeautifyShell::yellowFg);

    // Highlight method declarations (approximate)
    code = highlightPattern(code, "\\b\\w+\\s*\\(", s ->
        yellowFg(s.substring(0, s.length() - 1)) + s.substring(s.length() - 1));

    return code;
  }
}
