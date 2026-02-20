package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;

/**
 * Rust language syntax highlighter
 */
public class RustHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "as", "break", "const", "continue", "crate", "else", "enum", "extern", "false",
      "fn", "for", "if", "impl", "in", "let", "loop", "match", "mod", "move", "mut",
      "pub", "ref", "return", "self", "Self", "static", "struct", "super", "trait",
      "true", "type", "unsafe", "use", "where", "while", "async", "await", "dyn",
      "abstract", "become", "box", "do", "final", "macro", "override", "priv",
      "typeof", "unsized", "virtual", "yield"
  };

  private final String[] primitives = {
      "bool", "char", "str", "i8", "i16", "i32", "i64", "i128", "isize",
      "u8", "u16", "u32", "u64", "u128", "usize", "f32", "f64"
  };

  private final String[] attributes = {
      "derive", "feature", "macro_use", "macro_export", "cfg", "allow", "warn",
      "deny", "forbid", "deprecated", "must_use", "test", "ignore", "should_panic"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Highlight generic
    code = SyntaxHighlighter.highlightGeneric(code);

    // Rust literals
    String[] literals = {"true", "false", "Some", "None", "Ok", "Err"};

    // Highlight function declarations
    code = SyntaxHighlighter.highlightPattern(code, "\\bfn\\s+[a-zA-Z_][a-zA-Z0-9_]*",
        s -> BeautifyShell.magentaFg("fn") + " " + blueFg(s.substring(3)));

    // Apply Rust-specific keyword highlighting
    for (String keyword : keywords()) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Highlight primitive types
    for (String primitive : primitives) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + primitive + "\\b", BeautifyShell::cyanFg);
    }

    // Apply literals highlighting
    for (String literal : literals) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + literal + "\\b", BeautifyShell::brightGreenFg);
    }

    // Highlight attributes
    code = SyntaxHighlighter.highlightPattern(code, "#\\[([^\\]]+)\\]", this::attributeStyle);

    // Highlight lifetimes
    code = SyntaxHighlighter.highlightPattern(code, "'[a-zA-Z_][a-zA-Z0-9_]*", BeautifyShell::redFg);

    // Highlight macros
    code = SyntaxHighlighter.highlightPattern(code, "\\b[a-zA-Z_][a-zA-Z0-9_]*!", BeautifyShell::yellowFg);

    // Highlight struct/enum/trait declarations
    for (String type : new String[]{"struct", "enum", "trait", "impl", "mod"}) {
      code = SyntaxHighlighter.highlightPattern(code, "\\b" + type + "\\s+[a-zA-Z_][a-zA-Z0-9_]*",
          s -> BeautifyShell.magentaFg(type) + " " + BeautifyShell.greenFg(s.substring(type.length() + 1)));
    }

    // Highlight generics
    code = SyntaxHighlighter.highlightPattern(code, "<[a-zA-Z_',:+\\s][a-zA-Z0-9_',:+\\s]*>", this::genericsStyle);

    return code;
  }

  private String attributeStyle(String s) {
    // Extract the actual attribute content
    int start = s.indexOf("[") + 1;
    int end = s.lastIndexOf("]");
    if (start < 0 || end < 0 || start >= end) return s;

    String attrContent = s.substring(start, end);

    // Check for known attributes
    for (String attribute : attributes) {
      attrContent = attrContent.replaceAll("\\b" + attribute + "\\b", BeautifyShell.yellowFg(attribute));
    }

    return "#[" + attrContent + "]";
  }

  private String genericsStyle(String s) {
    // Handle lifetime parameters
    s = s.replaceAll("'[a-zA-Z_][a-zA-Z0-9_]*", BeautifyShell.redFg("$0"));

    // Handle type parameters
    s = s.replaceAll("\\b[A-Z][a-zA-Z0-9_]*\\b", BeautifyShell.cyanFg("$0"));

    return s;
  }
}