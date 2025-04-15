package dev.buildcli.core.utils;

import java.util.List;
import java.util.function.Function;

/**
 * BeautifyShell - A utility class for styling terminal content with colors and formatting
 * using a fluent interface pattern.
 */
public class BeautifyShell {
  // ANSI escape codes for colors (foreground)
  private static final String RESET = "\u001B[0m";
  private static final String BLACK_FG = "\u001B[30m";
  private static final String RED_FG = "\u001B[31m";
  private static final String GREEN_FG = "\u001B[32m";
  private static final String YELLOW_FG = "\u001B[33m";
  private static final String BLUE_FG = "\u001B[34m";
  private static final String MAGENTA_FG = "\u001B[35m";
  private static final String CYAN_FG = "\u001B[36m";
  private static final String WHITE_FG = "\u001B[37m";
  // Bright foreground colors
  private static final String BRIGHT_BLACK_FG = "\u001B[90m";
  private static final String BRIGHT_RED_FG = "\u001B[91m";
  private static final String BRIGHT_GREEN_FG = "\u001B[92m";
  private static final String BRIGHT_YELLOW_FG = "\u001B[93m";
  private static final String BRIGHT_BLUE_FG = "\u001B[94m";
  private static final String BRIGHT_MAGENTA_FG = "\u001B[95m";
  private static final String BRIGHT_CYAN_FG = "\u001B[96m";
  private static final String BRIGHT_WHITE_FG = "\u001B[97m";
  // ANSI escape codes for colors (background)
  private static final String BLACK_BG = "\u001B[40m";
  private static final String RED_BG = "\u001B[41m";
  private static final String GREEN_BG = "\u001B[42m";
  private static final String YELLOW_BG = "\u001B[43m";
  private static final String BLUE_BG = "\u001B[44m";
  private static final String MAGENTA_BG = "\u001B[45m";
  private static final String CYAN_BG = "\u001B[46m";
  private static final String WHITE_BG = "\u001B[47m";
  // Bright background colors
  private static final String BRIGHT_BLACK_BG = "\u001B[100m";
  private static final String BRIGHT_RED_BG = "\u001B[101m";
  private static final String BRIGHT_GREEN_BG = "\u001B[102m";
  private static final String BRIGHT_YELLOW_BG = "\u001B[103m";
  private static final String BRIGHT_BLUE_BG = "\u001B[104m";
  private static final String BRIGHT_MAGENTA_BG = "\u001B[105m";
  private static final String BRIGHT_CYAN_BG = "\u001B[106m";
  private static final String BRIGHT_WHITE_BG = "\u001B[107m";
  // ANSI escape codes for content style
  private static final String BOLD = "\u001B[1m";
  private static final String ITALIC = "\u001B[3m";
  private static final String UNDERLINE = "\u001B[4m";
  private static final String BLINK = "\u001B[5m";
  private static final String REVERSE = "\u001B[7m";
  private static final String STRIKETHROUGH = "\u001B[9m";
  private static final String DOUBLE_UNDERLINE = "\u001B[21m";
  private static final String FRAMED = "\u001B[51m";
  private static final String ENCIRCLED = "\u001B[52m";
  private static final String OVERLINED = "\u001B[53m";
  // Content to be styled
  private final StringBuilder builder;

  private BeautifyShell(Object content) {
    this.builder = new StringBuilder(content.toString());
  }

  public static BeautifyShell content(Object content) {
    return new BeautifyShell(content.toString());
  }

  public static String blackFg(Object content) {
    return BLACK_FG + content + RESET;
  }

  public static String redFg(Object content) {
    return RED_FG + content + RESET;
  }

  public static String greenFg(Object content) {
    return GREEN_FG + content + RESET;
  }

  // ---------- FOREGROUND COLORS ----------

  public static String yellowFg(Object content) {
    return YELLOW_FG + content + RESET;
  }

  public static String blueFg(Object content) {
    return BLUE_FG + content + RESET;
  }

  public static String magentaFg(Object content) {
    return MAGENTA_FG + content + RESET;
  }

  public static String cyanFg(Object content) {
    return CYAN_FG + content + RESET;
  }

  public static String whiteFg(Object content) {
    return WHITE_FG + content + RESET;
  }

  public static String brightBlackFg(Object content) {
    return BRIGHT_BLACK_FG + content + RESET;
  }

  public static String brightRedFg(Object content) {
    return BRIGHT_RED_FG + content + RESET;
  }

  public static String brightGreenFg(Object content) {
    return BRIGHT_GREEN_FG + content + RESET;
  }

  public static String brightYellowFg(Object content) {
    return BRIGHT_YELLOW_FG + content + RESET;
  }

  public static String brightBlueFg(Object content) {
    return BRIGHT_BLUE_FG + content + RESET;
  }

  public static String brightMagentaFg(Object content) {
    return BRIGHT_MAGENTA_FG + content + RESET;
  }

  public static String brightCyanFg(Object content) {
    return BRIGHT_CYAN_FG + content + RESET;
  }

  public static String brightWhiteFg(Object content) {
    return BRIGHT_WHITE_FG + content + RESET;
  }

  public static String blackBg(Object content) {
    return BLACK_BG + content + RESET;
  }

  public static String redBg(Object content) {
    return RED_BG + content + RESET;
  }

  public static String greenBg(Object content) {
    return GREEN_BG + content + RESET;
  }

  // ---------- BRIGHT FOREGROUND COLORS ----------

  public static String yellowBg(Object content) {
    return YELLOW_BG + content + RESET;
  }

  public static String blueBg(Object content) {
    return BLUE_BG + content + RESET;
  }

  public static String magentaBg(Object content) {
    return MAGENTA_BG + content + RESET;
  }

  public static String cyanBg(Object content) {
    return CYAN_BG + content + RESET;
  }

  public static String whiteBg(Object content) {
    return WHITE_BG + content + RESET;
  }

  public static String brightBlackBg(Object content) {
    return BRIGHT_BLACK_BG + content + RESET;
  }

  public static String brightRedBg(Object content) {
    return BRIGHT_RED_BG + content + RESET;
  }

  public static String brightGreenBg(Object content) {
    return BRIGHT_GREEN_BG + content + RESET;
  }

  public static String brightYellowBg(Object content) {
    return BRIGHT_YELLOW_BG + content + RESET;
  }

  public static String brightBlueBg(Object content) {
    return BRIGHT_BLUE_BG + content + RESET;
  }

  public static String brightMagentaBg(Object content) {
    return BRIGHT_MAGENTA_BG + content + RESET;
  }

  public static String brightCyanBg(Object content) {
    return BRIGHT_CYAN_BG + content + RESET;
  }

  public static String brightWhiteBg(Object content) {
    return BRIGHT_WHITE_BG + content + RESET;
  }

  public static String bold(Object content) {
    return BOLD + content + RESET;
  }

  public static String italic(Object content) {
    return ITALIC + content + RESET;
  }

  public static String underline(Object content) {
    return UNDERLINE + content + RESET;
  }

  // ---------- BACKGROUND COLORS ----------

  public static String blink(Object content) {
    return BLINK + content + RESET;
  }

  public static String reverse(Object content) {
    return REVERSE + content + RESET;
  }

  public static String strikethrough(Object content) {
    return STRIKETHROUGH + content + RESET;
  }

  public static String doubleUnderline(Object content) {
    return DOUBLE_UNDERLINE + content + RESET;
  }

  public static String framed(Object content) {
    return FRAMED + content + RESET;
  }

  public static String encircled(Object content) {
    return ENCIRCLED + content + RESET;
  }

  public static String overlined(Object content) {
    return OVERLINED + content + RESET;
  }

  public static String rainbow(Object content) {
    StringBuilder result = new StringBuilder();
    String[] colors = {RED_FG, YELLOW_FG, GREEN_FG, CYAN_FG, BLUE_FG, MAGENTA_FG};
    for (int i = 0; i < content.toString().length(); i++) {
      result.append(colors[i % colors.length]).append(content.toString().charAt(i));
    }
    return result + RESET;
  }

  public static String gradient(Object content, String startColor, String endColor) {
    StringBuilder result = new StringBuilder();
    int mid = content.toString().length() / 2;
    for (int i = 0; i < content.toString().length(); i++) {
      if (i < mid) {
        result.append(startColor).append(content.toString().charAt(i));
      } else {
        result.append(endColor).append(content.toString().charAt(i));
      }
    }
    return result + RESET;
  }

  public static String blinking(Object content) {
    return BLINK + content + RESET;
  }

  public static String table(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return "";
    }

    var size = lines.stream().map(s -> s.replaceAll("\u001B\\[[;\\d]*m", "")).mapToInt(String::length).max().orElse(6);
    var builder = new StringBuilder();
    builder.append("┌").append("─".repeat(size + 2)).append("┐").append("\n");
    for (String line : lines) {
      builder.append(brightBlackFg("│ "))
          .append(line)
          .append(" ".repeat(size - line.replaceAll("\u001B\\[[;\\d]*m", "").length()))
          .append(brightBlackFg(" │"))
          .append('\n');
    }
    builder.append("└").append("─".repeat(size + 2)).append("┘").append("\n");

    return builder.toString();
  }

  @Override
  public String toString() {
    return builder + RESET;
  }

  public BeautifyShell append(Object content) {
    builder.append(content.toString());
    return this;
  }

  public BeautifyShell append(Object content, Function<String, String> styling) {
    builder.append(styling.apply(content.toString()));
    return this;
  }

  public BeautifyShell blackFg() {
    builder.insert(0, BLACK_FG);
    return this;
  }

  public BeautifyShell redFg() {
    builder.insert(0, RED_FG);
    return this;
  }

  public BeautifyShell greenFg() {
    builder.insert(0, GREEN_FG);
    return this;
  }

  // ---------- BRIGHT BACKGROUND COLORS ----------

  public BeautifyShell yellowFg() {
    builder.insert(0, YELLOW_FG);
    return this;
  }

  public BeautifyShell blueFg() {
    builder.insert(0, BLUE_FG);
    return this;
  }

  public BeautifyShell magentaFg() {
    builder.insert(0, MAGENTA_FG);
    return this;
  }

  public BeautifyShell cyanFg() {
    builder.insert(0, CYAN_FG);
    return this;
  }

  public BeautifyShell whiteFg() {
    builder.insert(0, WHITE_FG);
    return this;
  }

  public BeautifyShell brightBlackFg() {
    builder.insert(0, BRIGHT_BLACK_FG);
    return this;
  }

  public BeautifyShell brightRedFg() {
    builder.insert(0, BRIGHT_RED_FG);
    return this;
  }

  public BeautifyShell brightGreenFg() {
    builder.insert(0, BRIGHT_GREEN_FG);
    return this;
  }

  public BeautifyShell brightYellowFg() {
    builder.insert(0, BRIGHT_YELLOW_FG);
    return this;
  }

  public BeautifyShell brightBlueFg() {
    builder.insert(0, BRIGHT_BLUE_FG);
    return this;
  }

  public BeautifyShell brightMagentaFg() {
    builder.insert(0, BRIGHT_MAGENTA_FG);
    return this;
  }

  public BeautifyShell brightCyanFg() {
    builder.insert(0, BRIGHT_CYAN_FG);
    return this;
  }

  public BeautifyShell brightWhiteFg() {
    builder.insert(0, BRIGHT_WHITE_FG);
    return this;
  }

  public BeautifyShell blackBg() {
    builder.insert(0, BLACK_BG);
    return this;
  }

  public BeautifyShell redBg() {
    builder.insert(0, RED_BG);
    return this;
  }

  public BeautifyShell greenBg() {
    builder.insert(0, GREEN_BG);
    return this;
  }

  // ---------- content STYLES ----------

  public BeautifyShell yellowBg() {
    builder.insert(0, YELLOW_BG);
    return this;
  }

  public BeautifyShell blueBg() {
    builder.insert(0, BLUE_BG);
    return this;
  }

  public BeautifyShell magentaBg() {
    builder.insert(0, MAGENTA_BG);
    return this;
  }

  public BeautifyShell cyanBg() {
    builder.insert(0, CYAN_BG);
    return this;
  }

  public BeautifyShell whiteBg() {
    builder.insert(0, WHITE_BG);
    return this;
  }

  public BeautifyShell brightBlackBg() {
    builder.insert(0, BRIGHT_BLACK_BG);
    return this;
  }

  public BeautifyShell brightRedBg() {
    builder.insert(0, BRIGHT_RED_BG);
    return this;
  }

  public BeautifyShell brightGreenBg() {
    builder.insert(0, BRIGHT_GREEN_BG);
    return this;
  }

  public BeautifyShell brightYellowBg() {
    builder.insert(0, BRIGHT_YELLOW_BG);
    return this;
  }

  public BeautifyShell brightBlueBg() {
    builder.insert(0, BRIGHT_BLUE_BG);
    return this;
  }

  public BeautifyShell brightMagentaBg() {
    builder.insert(0, BRIGHT_MAGENTA_BG);
    return this;
  }

  public BeautifyShell brightCyanBg() {
    builder.insert(0, BRIGHT_CYAN_BG);
    return this;
  }

  public BeautifyShell brightWhiteBg() {
    builder.insert(0, BRIGHT_WHITE_BG);
    return this;
  }

  public BeautifyShell bold() {
    builder.insert(0, BOLD);
    return this;
  }

  public BeautifyShell italic() {
    builder.insert(0, ITALIC);
    return this;
  }

  public BeautifyShell underline() {
    builder.insert(0, UNDERLINE);
    return this;
  }

  public BeautifyShell blink() {
    builder.insert(0, BLINK);
    return this;
  }

  public BeautifyShell reverse() {
    builder.insert(0, REVERSE);
    return this;
  }

  public BeautifyShell strikethrough() {
    builder.insert(0, STRIKETHROUGH);
    return this;
  }

  public BeautifyShell doubleUnderline() {
    builder.insert(0, DOUBLE_UNDERLINE);
    return this;
  }

  // ---------- UTILITY METHODS ----------

  public BeautifyShell framed() {
    builder.insert(0, FRAMED);
    return this;
  }

  public BeautifyShell encircled() {
    builder.insert(0, ENCIRCLED);
    return this;
  }

  public BeautifyShell overlined() {
    builder.insert(0, OVERLINED);
    return this;
  }

  public BeautifyShell reset() {
    builder.insert(0, RESET);
    builder.append(RESET);
    return this;
  }

  public BeautifyShell rainbow() {
    String originalcontent = builder.toString();
    builder.setLength(0);
    builder.append(rainbow(originalcontent));
    return this;
  }
}
