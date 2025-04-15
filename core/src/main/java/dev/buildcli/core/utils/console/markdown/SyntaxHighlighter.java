package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SyntaxHighlighter - A utility class for syntax highlighting of various programming languages
 * to be used with the MarkdownInterpreter and BeautifyShell.
 */
public class SyntaxHighlighter {

  // Singleton instance map for language highlighters
  private static final Map<String, LanguageHighlighter> HIGHLIGHTERS = new HashMap<>();

  static {
    // Register default language highlighters
    registerHighlighter("java", new JavaHighlighter());
    registerHighlighter("kotlin", new KotlinHighlighter());
    /*
    registerHighlighter("python", new PythonHighlighter());
    registerHighlighter("json", new JsonHighlighter());
    registerHighlighter("xml", new XmlHighlighter());
    registerHighlighter("markdown", new MarkdownHighlighter());
    registerHighlighter("md", new MarkdownHighlighter());
    registerHighlighter("sql", new SqlHighlighter());
    registerHighlighter("bash", new BashHighlighter());
    registerHighlighter("sh", new BashHighlighter());
    registerHighlighter("c", new CHighlighter());
    registerHighlighter("cpp", new CppHighlighter());
    registerHighlighter("csharp", new CSharpHighlighter());
    registerHighlighter("cs", new CSharpHighlighter());
    registerHighlighter("go", new GoHighlighter());
    registerHighlighter("rust", new RustHighlighter());
    registerHighlighter("php", new PhpHighlighter());
    registerHighlighter("ruby", new RubyHighlighter());
    registerHighlighter("javascript", new JavaScriptHighlighter());
    registerHighlighter("js", new JavaScriptHighlighter());
    registerHighlighter("typescript", new TypeScriptHighlighter());
    registerHighlighter("ts", new TypeScriptHighlighter());
    registerHighlighter("html", new HtmlHighlighter());
    registerHighlighter("css", new CssHighlighter());
    registerHighlighter("swift", new SwiftHighlighter());
    */
  }

  /**
   * Register a custom language highlighter
   *
   * @param language    Language identifier (case-insensitive)
   * @param highlighter The highlighter implementation
   */
  public static void registerHighlighter(String language, LanguageHighlighter highlighter) {
    HIGHLIGHTERS.put(language.toLowerCase(), highlighter);
  }

  /**
   * Highlight code based on specified language
   *
   * @param code     The code to highlight
   * @param language The programming language (case-insensitive)
   * @return Highlighted code with ANSI color codes
   */
  public static String highlight(String code, String language) {
    if (language == null || language.trim().isEmpty()) {
      return highlightGeneric(code);
    }

    LanguageHighlighter highlighter = HIGHLIGHTERS.get(language.toLowerCase());
    if (highlighter != null) {
      return highlighter.highlight(code);
    }

    // Fallback to generic highlighting for unsupported languages
    return highlightGeneric(code);
  }

  /**
   * Generic syntax highlighting for unknown languages
   *
   * @param code The code to highlight
   * @return Highlighted code with basic syntax highlighting
   */
  public static String highlightGeneric(String code) {
    // Highlight strings
    code = highlightPattern(code, "\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"", BeautifyShell::greenFg);
    code = highlightPattern(code, "'[^'\\\\]*(\\\\.[^'\\\\]*)*'", BeautifyShell::greenFg);

    // Highlight numbers
    code = highlightPattern(code, "\\b[0-9]+\\b", BeautifyShell::cyanFg);

    // Highlight common keywords across many languages
    String[] commonKeywords = {
        "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
        "return", "true", "false", "null", "void", "this", "new", "try", "catch",
        "finally", "throw", "throws", "class", "interface", "extends", "implements",
        "import", "package", "public", "private", "protected", "static", "final",
        "abstract", "default", "const", "let", "var", "function", "def", "async", "await",
        "fun", "data"
    };

    for (String keyword : commonKeywords) {
      code = highlightPattern(code, "\\b" + keyword + "\\b", BeautifyShell::magentaFg);
    }

    // Highlight comments
    code = highlightPattern(code, "//.*$", BeautifyShell::brightBlackFg, true);
    code = highlightPattern(code, "/\\*[\\s\\S]*?\\*/", BeautifyShell::brightBlackFg);
    code = highlightPattern(code, "#.*$", BeautifyShell::brightBlackFg, true);

    return code;
  }

  /**
   * Highlight parts of text using a regex pattern and styling function
   *
   * @param text   Text to process
   * @param regex  Regular expression to match
   * @param styler Function to apply styling
   * @return Text with highlighted matches
   */
  static String highlightPattern(String text, String regex, Function<String, String> styler) {
    return highlightPattern(text, regex, styler, false);
  }

  /**
   * Highlight parts of text using a regex pattern and styling function
   *
   * @param text      Text to process
   * @param regex     Regular expression to match
   * @param styler    Function to apply styling
   * @param multiline Whether to apply multiline matching
   * @return Text with highlighted matches
   */
  static String highlightPattern(String text, String regex, Function<String, String> styler, boolean multiline) {
    Pattern pattern = multiline
        ? Pattern.compile(regex, Pattern.MULTILINE)
        : Pattern.compile(regex);
    Matcher matcher = pattern.matcher(text);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String match = matcher.group();
      matcher.appendReplacement(result, Matcher.quoteReplacement(styler.apply(match)));
    }

    matcher.appendTail(result);
    return result.toString();
  }
}
