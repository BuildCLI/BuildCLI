package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

/**
 * HTML syntax highlighter
 */
public class HtmlHighlighter implements LanguageHighlighter {
  private final String[] keywords = {
      "html", "head", "body", "div", "span", "p", "h1", "h2", "h3", "h4", "h5", "h6",
      "a", "img", "ul", "ol", "li", "table", "tr", "td", "th", "form", "input", "button",
      "script", "style", "link", "meta", "title", "section", "article", "nav", "header",
      "footer", "aside", "main", "canvas", "video", "audio", "source", "iframe"
  };

  @Override
  public String[] keywords() {
    return keywords;
  }

  @Override
  public String highlight(String code) {
    // Leverage XML highlighter for basic syntax
    XmlHighlighter xmlHighlighter = new XmlHighlighter();
    code = xmlHighlighter.highlight(code);

    // Highlight HTML-specific elements
    for (String tag : keywords()) {
      // Highlight opening tags
      code = SyntaxHighlighter.highlightPattern(code, "<" + tag + "(\\s+|>)",
          s -> "<" + BeautifyShell.blueFg(tag) + s.substring(tag.length() + 1));

      // Highlight closing tags
      code = SyntaxHighlighter.highlightPattern(code, "</" + tag + ">",
          s -> "</" + BeautifyShell.blueFg(tag) + ">");
    }

    // Highlight embedded CSS and JavaScript
    code = SyntaxHighlighter.highlightPattern(code,
        "<style[^>]*>[\\s\\S]*?</style>",
        this::styleTagContent);

    code = SyntaxHighlighter.highlightPattern(code,
        "<script[^>]*>[\\s\\S]*?</script>",
        this::scriptTagContent);

    return code;
  }

  private String styleTagContent(String content) {
    // Find the actual style content
    int start = content.indexOf(">") + 1;
    int end = content.lastIndexOf("</style>");

    if (start >= 0 && end > start) {
      String styleContent = content.substring(start, end);
      CssHighlighter cssHighlighter = new CssHighlighter();
      String highlightedCss = cssHighlighter.highlight(styleContent);

      return content.substring(0, start) + highlightedCss + content.substring(end);
    }

    return content;
  }

  private String scriptTagContent(String content) {
    // Find the actual script content
    int start = content.indexOf(">") + 1;
    int end = content.lastIndexOf("</script>");

    if (start >= 0 && end > start) {
      String scriptContent = content.substring(start, end);
      JavaScriptHighlighter jsHighlighter = new JavaScriptHighlighter();
      String highlightedJs = jsHighlighter.highlight(scriptContent);

      return content.substring(0, start) + highlightedJs + content.substring(end);
    }

    return content;
  }
}