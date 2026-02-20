package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

/**
 * TypeScript syntax highlighter
 */
public class TypeScriptHighlighter implements LanguageHighlighter {
  // Extend JavaScript highlighter and add TypeScript-specific features
  private final JavaScriptHighlighter jsHighlighter = new JavaScriptHighlighter();

  private final String[] tsKeywords = {
      "interface", "type", "namespace", "module", "declare", "implements",
      "readonly", "private", "protected", "public", "abstract", "enum",
      "as", "any", "unknown", "never", "void", "string", "number", "boolean",
      "symbol", "undefined", "null", "object", "keyof", "typeof", "from"
  };

  @Override
  public String[] keywords() {
    // Combine JavaScript keywords with TypeScript keywords
    String[] jsKeywords = jsHighlighter.keywords();
    String[] combinedKeywords = new String[jsKeywords.length + tsKeywords.length];
    System.arraycopy(jsKeywords, 0, combinedKeywords, 0, jsKeywords.length);
    System.arraycopy(tsKeywords, 0, combinedKeywords, jsKeywords.length, tsKeywords.length);
    return combinedKeywords;
  }

  @Override
  public String highlight(String code) {
    // Start with JavaScript highlighting
    code = jsHighlighter.highlight(code);

    // Add TypeScript-specific keyword highlighting
    for (String keyword : tsKeywords) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Highlight type annotations
    code = SyntaxHighlighter.highlightPattern(code, ":\\s*[A-Za-z][A-Za-z0-9_<>\\[\\]]*", this::typeAnnotationStyle);

    // Highlight generic type parameters
    code = SyntaxHighlighter.highlightPattern(code, "<[^>]+>", this::genericTypeStyle);

    // Highlight decorators
    code = SyntaxHighlighter.highlightPattern(code, "@\\w+", BeautifyShell::yellowFg);

    return code;
  }

  private String typeAnnotationStyle(String s) {
    int colonIndex = s.indexOf(":");
    return ":" + BeautifyShell.cyanFg(s.substring(colonIndex + 1));
  }

  private String genericTypeStyle(String s) {
    return BeautifyShell.cyanFg(s);
  }
}