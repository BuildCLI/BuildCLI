package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

/**
 * XML syntax highlighter
 */
public class XmlHighlighter implements LanguageHighlighter {
  @Override
  public String[] keywords() {
    return new String[0]; // XML doesn't have traditional keywords
  }

  @Override
  public String highlight(String code) {
    // Highlight generic first for basic elements
    code = SyntaxHighlighter.highlightGeneric(code);

    // Highlight tags
    code = SyntaxHighlighter.highlightPattern(code, "</?[^>]+>", this::tagStyle);

    // Highlight attributes
    code = SyntaxHighlighter.highlightPattern(code, "\\s+\\w+=\"[^\"]*\"", this::attributeStyle);

    // Highlight XML declarations
    code = SyntaxHighlighter.highlightPattern(code, "<\\?xml[^>]+\\?>", BeautifyShell::cyanFg);

    // Highlight CDATA sections
    code = SyntaxHighlighter.highlightPattern(code, "<!\\[CDATA\\[.*?\\]\\]>", BeautifyShell::brightBlackFg);

    return code;
  }

  private String tagStyle(String tag) {
    // Highlight tag names in blue, keep brackets in green
    String result = tag;
    result = SyntaxHighlighter.highlightPattern(result, "[<>/]", BeautifyShell::greenFg);
    result = SyntaxHighlighter.highlightPattern(result, "\\b\\w+\\b(?!=)", BeautifyShell::blueFg);
    return result;
  }

  private String attributeStyle(String attr) {
    // Highlight attribute names in yellow and values in green
    String result = attr;
    result = SyntaxHighlighter.highlightPattern(result, "\\b\\w+\\b(?==)", BeautifyShell::yellowFg);
    result = SyntaxHighlighter.highlightPattern(result, "\"[^\"]*\"", BeautifyShell::greenFg);
    return result;
  }
}