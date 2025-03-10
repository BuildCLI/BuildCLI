package dev.buildcli.core.utils;

import java.util.function.Function;

/**
 * BeautifyShell - A utility class for styling terminal text with colors and formatting
 * using a fluent interface pattern.
 */
public class BeautifyShell {
  // Content to be styled
  private final StringBuilder content;

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

  // ANSI escape codes for text style
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

  private BeautifyShell(String text) {
    this.content = new StringBuilder(text);
  }

  public static BeautifyShell content(String text) {
    return new BeautifyShell(text);
  }

  @Override
  public String toString() {
    return content + RESET;
  }

  public BeautifyShell append(String text) {
    content.append(text);
    return this;
  }

  public BeautifyShell append(String text, Function<String, String> styling) {
    content.append(styling.apply(text));
    return this;
  }

  // ---------- FOREGROUND COLORS ----------

  public BeautifyShell blackFg() {
    content.insert(0, BLACK_FG);
    return this;
  }
  public static String blackFg(String text) {
    return BLACK_FG + text + RESET;
  }

  public BeautifyShell redFg() {
    content.insert(0, RED_FG);
    return this;
  }
  public static String redFg(String text) {
    return RED_FG + text + RESET;
  }

  public BeautifyShell greenFg() {
    content.insert(0, GREEN_FG);
    return this;
  }
  public static String greenFg(String text) {
    return GREEN_FG + text + RESET;
  }

  public BeautifyShell yellowFg() {
    content.insert(0, YELLOW_FG);
    return this;
  }
  public static String yellowFg(String text) {
    return YELLOW_FG + text + RESET;
  }

  public BeautifyShell blueFg() {
    content.insert(0, BLUE_FG);
    return this;
  }
  public static String blueFg(String text) {
    return BLUE_FG + text + RESET;
  }

  public BeautifyShell magentaFg() {
    content.insert(0, MAGENTA_FG);
    return this;
  }
  public static String magentaFg(String text) {
    return MAGENTA_FG + text + RESET;
  }

  public BeautifyShell cyanFg() {
    content.insert(0, CYAN_FG);
    return this;
  }
  public static String cyanFg(String text) {
    return CYAN_FG + text + RESET;
  }

  public BeautifyShell whiteFg() {
    content.insert(0, WHITE_FG);
    return this;
  }
  public static String whiteFg(String text) {
    return WHITE_FG + text + RESET;
  }

  // ---------- BRIGHT FOREGROUND COLORS ----------

  public BeautifyShell brightBlackFg() {
    content.insert(0, BRIGHT_BLACK_FG);
    return this;
  }
  public static String brightBlackFg(String text) {
    return BRIGHT_BLACK_FG + text + RESET;
  }

  public BeautifyShell brightRedFg() {
    content.insert(0, BRIGHT_RED_FG);
    return this;
  }
  public static String brightRedFg(String text) {
    return BRIGHT_RED_FG + text + RESET;
  }

  public BeautifyShell brightGreenFg() {
    content.insert(0, BRIGHT_GREEN_FG);
    return this;
  }
  public static String brightGreenFg(String text) {
    return BRIGHT_GREEN_FG + text + RESET;
  }

  public BeautifyShell brightYellowFg() {
    content.insert(0, BRIGHT_YELLOW_FG);
    return this;
  }
  public static String brightYellowFg(String text) {
    return BRIGHT_YELLOW_FG + text + RESET;
  }

  public BeautifyShell brightBlueFg() {
    content.insert(0, BRIGHT_BLUE_FG);
    return this;
  }
  public static String brightBlueFg(String text) {
    return BRIGHT_BLUE_FG + text + RESET;
  }

  public BeautifyShell brightMagentaFg() {
    content.insert(0, BRIGHT_MAGENTA_FG);
    return this;
  }
  public static String brightMagentaFg(String text) {
    return BRIGHT_MAGENTA_FG + text + RESET;
  }

  public BeautifyShell brightCyanFg() {
    content.insert(0, BRIGHT_CYAN_FG);
    return this;
  }
  public static String brightCyanFg(String text) {
    return BRIGHT_CYAN_FG + text + RESET;
  }

  public BeautifyShell brightWhiteFg() {
    content.insert(0, BRIGHT_WHITE_FG);
    return this;
  }
  public static String brightWhiteFg(String text) {
    return BRIGHT_WHITE_FG + text + RESET;
  }

  // ---------- BACKGROUND COLORS ----------

  public BeautifyShell blackBg() {
    content.insert(0, BLACK_BG);
    return this;
  }
  public static String blackBg(String text) {
    return BLACK_BG + text + RESET;
  }

  public BeautifyShell redBg() {
    content.insert(0, RED_BG);
    return this;
  }
  public static String redBg(String text) {
    return RED_BG + text + RESET;
  }

  public BeautifyShell greenBg() {
    content.insert(0, GREEN_BG);
    return this;
  }
  public static String greenBg(String text) {
    return GREEN_BG + text + RESET;
  }

  public BeautifyShell yellowBg() {
    content.insert(0, YELLOW_BG);
    return this;
  }
  public static String yellowBg(String text) {
    return YELLOW_BG + text + RESET;
  }

  public BeautifyShell blueBg() {
    content.insert(0, BLUE_BG);
    return this;
  }
  public static String blueBg(String text) {
    return BLUE_BG + text + RESET;
  }

  public BeautifyShell magentaBg() {
    content.insert(0, MAGENTA_BG);
    return this;
  }
  public static String magentaBg(String text) {
    return MAGENTA_BG + text + RESET;
  }

  public BeautifyShell cyanBg() {
    content.insert(0, CYAN_BG);
    return this;
  }
  public static String cyanBg(String text) {
    return CYAN_BG + text + RESET;
  }

  public BeautifyShell whiteBg() {
    content.insert(0, WHITE_BG);
    return this;
  }
  public static String whiteBg(String text) {
    return WHITE_BG + text + RESET;
  }

  // ---------- BRIGHT BACKGROUND COLORS ----------

  public BeautifyShell brightBlackBg() {
    content.insert(0, BRIGHT_BLACK_BG);
    return this;
  }
  public static String brightBlackBg(String text) {
    return BRIGHT_BLACK_BG + text + RESET;
  }

  public BeautifyShell brightRedBg() {
    content.insert(0, BRIGHT_RED_BG);
    return this;
  }
  public static String brightRedBg(String text) {
    return BRIGHT_RED_BG + text + RESET;
  }

  public BeautifyShell brightGreenBg() {
    content.insert(0, BRIGHT_GREEN_BG);
    return this;
  }
  public static String brightGreenBg(String text) {
    return BRIGHT_GREEN_BG + text + RESET;
  }

  public BeautifyShell brightYellowBg() {
    content.insert(0, BRIGHT_YELLOW_BG);
    return this;
  }
  public static String brightYellowBg(String text) {
    return BRIGHT_YELLOW_BG + text + RESET;
  }

  public BeautifyShell brightBlueBg() {
    content.insert(0, BRIGHT_BLUE_BG);
    return this;
  }
  public static String brightBlueBg(String text) {
    return BRIGHT_BLUE_BG + text + RESET;
  }

  public BeautifyShell brightMagentaBg() {
    content.insert(0, BRIGHT_MAGENTA_BG);
    return this;
  }
  public static String brightMagentaBg(String text) {
    return BRIGHT_MAGENTA_BG + text + RESET;
  }

  public BeautifyShell brightCyanBg() {
    content.insert(0, BRIGHT_CYAN_BG);
    return this;
  }
  public static String brightCyanBg(String text) {
    return BRIGHT_CYAN_BG + text + RESET;
  }

  public BeautifyShell brightWhiteBg() {
    content.insert(0, BRIGHT_WHITE_BG);
    return this;
  }
  public static String brightWhiteBg(String text) {
    return BRIGHT_WHITE_BG + text + RESET;
  }

  // ---------- TEXT STYLES ----------

  public BeautifyShell bold() {
    content.insert(0, BOLD);
    return this;
  }
  public static String bold(String text) {
    return BOLD + text + RESET;
  }

  public BeautifyShell italic() {
    content.insert(0, ITALIC);
    return this;
  }
  public static String italic(String text) {
    return ITALIC + text + RESET;
  }

  public BeautifyShell underline() {
    content.insert(0, UNDERLINE);
    return this;
  }
  public static String underline(String text) {
    return UNDERLINE + text + RESET;
  }

  public BeautifyShell blink() {
    content.insert(0, BLINK);
    return this;
  }
  public static String blink(String text) {
    return BLINK + text + RESET;
  }

  public BeautifyShell reverse() {
    content.insert(0, REVERSE);
    return this;
  }
  public static String reverse(String text) {
    return REVERSE + text + RESET;
  }

  public BeautifyShell strikethrough() {
    content.insert(0, STRIKETHROUGH);
    return this;
  }
  public static String strikethrough(String text) {
    return STRIKETHROUGH + text + RESET;
  }

  public BeautifyShell doubleUnderline() {
    content.insert(0, DOUBLE_UNDERLINE);
    return this;
  }
  public static String doubleUnderline(String text) {
    return DOUBLE_UNDERLINE + text + RESET;
  }

  public BeautifyShell framed() {
    content.insert(0, FRAMED);
    return this;
  }
  public static String framed(String text) {
    return FRAMED + text + RESET;
  }

  public BeautifyShell encircled() {
    content.insert(0, ENCIRCLED);
    return this;
  }
  public static String encircled(String text) {
    return ENCIRCLED + text + RESET;
  }

  public BeautifyShell overlined() {
    content.insert(0, OVERLINED);
    return this;
  }
  public static String overlined(String text) {
    return OVERLINED + text + RESET;
  }

  // ---------- UTILITY METHODS ----------

  public BeautifyShell reset() {
    content.insert(0, RESET);
    content.append(RESET);
    return this;
  }

  public static String rainbow(String text) {
    StringBuilder result = new StringBuilder();
    String[] colors = {RED_FG, YELLOW_FG, GREEN_FG, CYAN_FG, BLUE_FG, MAGENTA_FG};
    for (int i = 0; i < text.length(); i++) {
      result.append(colors[i % colors.length]).append(text.charAt(i));
    }
    return result + RESET;
  }

  public BeautifyShell rainbow() {
    String originalText = content.toString();
    content.setLength(0);
    content.append(rainbow(originalText));
    return this;
  }

  public static String gradient(String text, String startColor, String endColor) {
    StringBuilder result = new StringBuilder();
    int mid = text.length() / 2;
    for (int i = 0; i < text.length(); i++) {
      if (i < mid) {
        result.append(startColor).append(text.charAt(i));
      } else {
        result.append(endColor).append(text.charAt(i));
      }
    }
    return result + RESET;
  }

  public static String blinking(String text) {
    return BLINK + text + RESET;
  }
}
