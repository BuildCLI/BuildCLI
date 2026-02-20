package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

/**
 * C++ syntax highlighter
 */
public class CppHighlighter implements LanguageHighlighter {
  // Extend C highlighter and add C++-specific features
  private final CHighlighter cHighlighter = new CHighlighter();

  private final String[] cppKeywords = {
      "class", "namespace", "template", "try", "catch", "throw", "using", "delete", "new",
      "private", "protected", "public", "virtual", "friend", "operator", "explicit",
      "export", "inline", "typename", "this", "nullptr", "constexpr", "decltype", "mutable",
      "noexcept", "thread_local", "alignas", "alignof", "override", "final", "auto",
      "bool", "true", "false", "asm", "dynamic_cast", "static_cast", "reinterpret_cast",
      "const_cast", "typeid"
  };

  @Override
  public String[] keywords() {
    // Combine C keywords with C++ keywords
    String[] cKeywords = cHighlighter.keywords();
    String[] combinedKeywords = new String[cKeywords.length + cppKeywords.length];
    System.arraycopy(cKeywords, 0, combinedKeywords, 0, cKeywords.length);
    System.arraycopy(cppKeywords, 0, combinedKeywords, cKeywords.length, cppKeywords.length);
    return combinedKeywords;
  }

  @Override
  public String highlight(String code) {
    // Start with C highlighting
    code = cHighlighter.highlight(code);

    // Add C++-specific keyword highlighting
    for (String keyword : cppKeywords) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Highlight class definitions
    code = SyntaxHighlighter.highlightPattern(code, "\\bclass\\s+[a-zA-Z_][a-zA-Z0-9_]*",
        s -> BeautifyShell.magentaFg("class") + " " + BeautifyShell.greenFg(s.substring(6)));

    // Highlight namespaces
    code = SyntaxHighlighter.highlightPattern(code, "\\bnamespace\\s+[a-zA-Z_][a-zA-Z0-9_]*",
        s -> BeautifyShell.magentaFg("namespace") + " " + BeautifyShell.yellowFg(s.substring(10)));

    // Highlight C++ style comments
    code = SyntaxHighlighter.highlightPattern(code, "//.*$", BeautifyShell::brightBlackFg, true);

    // Highlight Templates (generic angle brackets)
    code = SyntaxHighlighter.highlightPattern(code, "template\\s*<[^>]*>", this::templateStyle);

    // Highlight STL containers and algorithms
    String[] stlClasses = {
        "vector", "map", "set", "queue", "stack", "list", "deque", "array",
        "string", "bitset", "unique_ptr", "shared_ptr", "weak_ptr"
    };

    for (String stlClass : stlClasses) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + stlClass + "\\s*<",
          s -> BeautifyShell.cyanFg(s.substring(0, s.length() - 1)) + "<");
    }

    // Highlight scope resolution operator
    code = SyntaxHighlighter.highlightPattern(code, "\\w+::\\w+", this::scopeResolutionStyle);

    return code;
  }

  private String templateStyle(String s) {
    int openBracket = s.indexOf("<");
    if (openBracket < 0) return s;

    return BeautifyShell.magentaFg("template") + s.substring(8, openBracket) +
        BeautifyShell.greenFg(s.substring(openBracket));
  }

  private String scopeResolutionStyle(String s) {
    int separator = s.indexOf("::");
    if (separator < 0) return s;

    return BeautifyShell.yellowFg(s.substring(0, separator)) + "::" +
        BeautifyShell.blueFg(s.substring(separator + 2));
  }
}