package dev.buildcli.core.utils.console.markdown;

public class KotlinHighlighter extends JavaHighlighter {
  private final String[] keywords = {
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
      "class", "const", "continue", "data", "default", "do", "double", "else", "enum",
      "final", "finally", "float", "for", "fun", "goto", "if", "inline",
      "import", "is", "int", "interface", "long", "native", "new", "package",
      "private", "protected", "public", "return", "short", "static", "strictfp",
      "super", "switch", "synchronized", "this", "throw", "throws", "transient",
      "try", "void", "volatile", "while", "var", "record", "sealed", "permits",
      "yield"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

}
