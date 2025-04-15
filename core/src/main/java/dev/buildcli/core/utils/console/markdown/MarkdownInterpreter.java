package dev.buildcli.core.utils.console.markdown;

import dev.buildcli.core.utils.BeautifyShell;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.buildcli.core.utils.BeautifyShell.*;

public class MarkdownInterpreter {
  /**
   * Parses a Markdown formatted string and converts it to styled console output.
   * <p>
   * Supported Markdown features:
   * - Headers (# to ######)
   * - Bold text (**text** or __text__)
   * - Italic text (*text* or _text_)
   * - Bold and italic (***text*** or ___text___)
   * - Strikethrough (~~text~~)
   * - Code blocks (```code```)
   * - Inline code (`code`)
   * - Blockquotes (> text)
   * - Lists (ordered and unordered)
   * - Horizontal rules (---, ***, ___)
   * - Links ([text](url))
   *
   * @param markdown The Markdown formatted string to parse
   * @return A styled string for console output
   */
  public String interpret(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return "";
    }

    StringBuilder result = new StringBuilder();
    String[] lines = markdown.split("\n");
    String lang = "";

    boolean inCodeBlock = false;
    boolean inBlockQuote = false;
    StringBuilder codeBlockContent = new StringBuilder();

    for (String line : lines) {
      // Check for code blocks
      if (line.startsWith("```") || (inCodeBlock && line.contains("```"))) {
        if (inCodeBlock) {
          // End of code block
          inCodeBlock = false;
          result.append(processCodeBlock(codeBlockContent.toString())).append("\n");
          codeBlockContent.setLength(0);
        } else {
          // Start of code block
          inCodeBlock = true;
          // Extract language if specified
          lang = line.length() > 3 ? line.substring(3).trim() : "";
          codeBlockContent.append("LANGUAGE:").append(lang).append("\n");
        }
        continue;
      }

      if (inCodeBlock) {
        codeBlockContent.append(line).append("\n");
        continue;
      }

      // Check for blockquotes
      if (line.startsWith(">")) {
        String quoteContent = line.substring(1).trim();
        result.append(processBlockQuote(quoteContent)).append("\n");
        inBlockQuote = true;
        continue;
      } else if (inBlockQuote && line.trim().isEmpty()) {
        inBlockQuote = false;
        result.append("\n");
        continue;
      } else if (inBlockQuote) {
        // Continuation of a blockquote without > prefix
        result.append(processBlockQuote(line)).append("\n");
        continue;
      }

      // Check for headers
      if (line.startsWith("#")) {
        result.append(processHeader(line)).append("\n");
        continue;
      }

      // Check for horizontal rules
      if (line.matches("^([-*_])\\1{2,}$")) {
        result.append(processHorizontalRule()).append("\n");
        continue;
      }

      // Check for unordered lists
      if (line.matches("^\\s*[*+-]\\s.*$")) {
        result.append(processUnorderedListItem(line)).append("\n");
        continue;
      }

      // Check for ordered lists
      if (line.matches("^\\s*\\d+\\.\\s.*$")) {
        result.append(processOrderedListItem(line)).append("\n");
        continue;
      }

      // Process inline formatting for normal text
      result.append(processInlineFormatting(line)).append("\n");
    }

    return result.toString();
  }

  private String processHeader(String line) {
    int level = 0;
    while (level < line.length() && level < 6 && line.charAt(level) == '#') {
      level++;
    }

    String headerText = line.substring(level).trim();
    headerText = processInlineFormatting(headerText);

    return switch (level) {
      case 1 -> content(headerText).bold().brightYellowFg().toString();
      case 2 -> content(headerText).bold().yellowFg().toString();
      case 3 -> content(headerText).bold().cyanFg().toString();
      case 4 -> content(headerText).bold().brightCyanFg().toString();
      case 5 -> content(headerText).bold().brightMagentaFg().toString();
      case 6 -> content(headerText).bold().magentaFg().toString();
      default -> headerText;
    };
  }

  private String processBlockQuote(String line) {
    String processedLine = processInlineFormatting(line);
    return content("│ ").blueFg().append(processedLine, BeautifyShell::italic).toString();
  }

  private String processHorizontalRule() {
    return brightBlackFg("─".repeat(50));
  }

  private String processUnorderedListItem(String line) {
    // Count leading spaces to determine indentation level
    int indentLevel = 0;
    int i = 0;
    while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
      indentLevel++;
      i++;
    }

    // Extract the list marker and content
    Pattern pattern = Pattern.compile("^\\s*([*+-])\\s(.*)$");
    Matcher matcher = pattern.matcher(line);

    if (matcher.find()) {
      String marker = matcher.group(1);
      String content = matcher.group(2);

      // Process any inline formatting in the content
      String processedContent = processInlineFormatting(content);

      // Indent based on level and add styled bullet
      String indent = " ".repeat(Math.max(0, indentLevel));
      String bullet = brightCyanFg("• ");

      return indent + bullet + processedContent;
    }

    return line; // Fallback if regex doesn't match
  }

  private String processOrderedListItem(String line) {
    // Count leading spaces to determine indentation level
    int indentLevel = 0;
    int i = 0;
    while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
      indentLevel++;
      i++;
    }

    // Extract the number and content
    Pattern pattern = Pattern.compile("^\\s*(\\d+)\\.\\s(.*)$");
    Matcher matcher = pattern.matcher(line);

    if (matcher.find()) {
      String number = matcher.group(1);
      String content = matcher.group(2);

      // Process any inline formatting in the content
      String processedContent = processInlineFormatting(content);

      // Indent based on level and add styled number
      String indent = " ".repeat(Math.max(0, indentLevel));
      String formattedNumber = brightMagentaFg(number + ". ");

      return indent + formattedNumber + processedContent;
    }

    return line; // Fallback if regex doesn't match
  }

  private String processCodeBlock(String content) {
    // Extract language if specified
    String language = "";
    if (content.startsWith("LANGUAGE:")) {
      int newlineIndex = content.indexOf('\n');
      if (newlineIndex > 0) {
        language = content.substring(9, newlineIndex).trim();
        content = content.substring(newlineIndex + 1);
      }
    }

    // Add a language identifier if one was specified
    StringBuilder result = new StringBuilder();
    if (!language.isEmpty()) {
      result.append(content(language).brightWhiteFg().blackBg().toString()).append("\n");
    }

    // Apply syntax highlighting based on language
    String highlightedContent = SyntaxHighlighter.highlight(content, language);

    // Style the code block with a background and padding
    String[] lines = highlightedContent.split("\n");
    int maxLength = 0;
    for (String line : lines) {
      // Remove ANSI codes when calculating length
      String plainLine = line.replaceAll("\u001B\\[[;\\d]*m", "");
      maxLength = Math.max(maxLength, plainLine.length());
    }

    String horizontalBorder = "┌" + "─".repeat(maxLength + 2) + "┐";
    result.append(brightBlackFg(horizontalBorder)).append("\n");

    for (String line : lines) {
      result.append(brightBlackFg("│ "))
          .append(line)  // Line is already highlighted
          .append(" ".repeat(maxLength - line.replaceAll("\u001B\\[[;\\d]*m", "").length()))
          .append(brightBlackFg(" │"))
          .append("\n");
    }

    String horizontalBottomBorder = "└" + "─".repeat(maxLength + 2) + "┘";
    result.append(brightBlackFg(horizontalBottomBorder));

    return result.toString();
  }

  private String processInlineFormatting(String text) {
    // Process code spans
    text = processCodeSpans(text);

    // Process bold and italic (must be done before processing bold or italic separately)
    text = processBoldItalic(text);

    // Process bold
    text = processBold(text);

    // Process italic
    text = processItalic(text);

    // Process strikethrough
    text = processStrikethrough(text);

    // Process links
    text = processLinks(text);

    return text;
  }

  private String processCodeSpans(String text) {
    Pattern pattern = Pattern.compile("`([^`]+)`");
    Matcher matcher = pattern.matcher(text);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String code = matcher.group(1);
      matcher.appendReplacement(result, Matcher.quoteReplacement(
          content(code).blackBg().brightGreenFg().toString()
      ));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  private String processBoldItalic(String text) {
    // Process ***text*** or ___text___
    Pattern pattern = Pattern.compile("(\\*{3}|_{3})([^*_]+)(\\*{3}|_{3})");
    Matcher matcher = pattern.matcher(text);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String content = matcher.group(2);
      matcher.appendReplacement(result, Matcher.quoteReplacement(
          content(content).bold().italic().toString()
      ));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  private String processBold(String text) {
    // Process **text** or __text__
    Pattern pattern = Pattern.compile("(\\*{2}|_{2})([^*_]+)(\\*{2}|_{2})");
    Matcher matcher = pattern.matcher(text);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String content = matcher.group(2);
      matcher.appendReplacement(result, Matcher.quoteReplacement(
          bold(content)
      ));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  private String processItalic(String text) {
    // Process *text* or _text_
    Pattern pattern = Pattern.compile("([*_])([^*_]+)([*_])");
    Matcher matcher = pattern.matcher(text);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String content = matcher.group(2);
      matcher.appendReplacement(result, Matcher.quoteReplacement(
          italic(content)
      ));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  private String processStrikethrough(String text) {
    // Process ~~text~~
    Pattern pattern = Pattern.compile("~~([^~]+)~~");
    Matcher matcher = pattern.matcher(text);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String content = matcher.group(1);
      matcher.appendReplacement(result, Matcher.quoteReplacement(
          strikethrough(content)
      ));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  private String processLinks(String text) {
    // Process [text](url)
    Pattern pattern = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)");
    Matcher matcher = pattern.matcher(text);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String linkText = matcher.group(1);
      String url = matcher.group(2);
      matcher.appendReplacement(result, Matcher.quoteReplacement(
          content(linkText).underline().cyanFg()
              .append(" (").append(url, BeautifyShell::brightBlackFg).append(")")
              .toString()
      ));
    }
    matcher.appendTail(result);

    return result.toString();
  }
}
