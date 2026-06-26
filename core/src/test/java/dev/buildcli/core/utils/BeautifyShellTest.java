package dev.buildcli.core.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BeautifyShellTest {

  private static final String RESET = "\u001B[0m";
  private static final String BLACK_FG = "\u001B[30m";
  private static final String RED_FG = "\u001B[31m";
  private static final String GREEN_FG = "\u001B[32m";
  private static final String YELLOW_FG = "\u001B[33m";
  private static final String BLUE_FG = "\u001B[34m";
  private static final String MAGENTA_FG = "\u001B[35m";
  private static final String CYAN_FG = "\u001B[36m";
  private static final String WHITE_FG = "\u001B[37m";
  private static final String BRIGHT_BLACK_FG = "\u001B[90m";
  private static final String BRIGHT_RED_FG = "\u001B[91m";
  private static final String BRIGHT_GREEN_FG = "\u001B[92m";
  private static final String BRIGHT_YELLOW_FG = "\u001B[93m";
  private static final String BRIGHT_BLUE_FG = "\u001B[94m";
  private static final String BRIGHT_MAGENTA_FG = "\u001B[95m";
  private static final String BRIGHT_CYAN_FG = "\u001B[96m";
  private static final String BRIGHT_WHITE_FG = "\u001B[97m";
  private static final String BLACK_BG = "\u001B[40m";
  private static final String RED_BG = "\u001B[41m";
  private static final String GREEN_BG = "\u001B[42m";
  private static final String YELLOW_BG = "\u001B[43m";
  private static final String BLUE_BG = "\u001B[44m";
  private static final String MAGENTA_BG = "\u001B[45m";
  private static final String CYAN_BG = "\u001B[46m";
  private static final String WHITE_BG = "\u001B[47m";
  private static final String BRIGHT_BLACK_BG = "\u001B[100m";
  private static final String BRIGHT_RED_BG = "\u001B[101m";
  private static final String BRIGHT_GREEN_BG = "\u001B[102m";
  private static final String BRIGHT_YELLOW_BG = "\u001B[103m";
  private static final String BRIGHT_BLUE_BG = "\u001B[104m";
  private static final String BRIGHT_MAGENTA_BG = "\u001B[105m";
  private static final String BRIGHT_CYAN_BG = "\u001B[106m";
  private static final String BRIGHT_WHITE_BG = "\u001B[107m";
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

  @ParameterizedTest
  @MethodSource("staticStyles")
  void staticStyleMethodsWrapContentWithAnsiCode(Function<Object, String> style, String ansiCode) {
    assertEquals(ansiCode + "BuildCLI" + RESET, style.apply("BuildCLI"));
  }

  @ParameterizedTest
  @MethodSource("staticStyles")
  void staticStyleMethodsRenderNullContentAsString(Function<Object, String> style, String ansiCode) {
    assertEquals(ansiCode + "null" + RESET, style.apply(null));
  }

  @ParameterizedTest
  @MethodSource("fluentStyles")
  void fluentStyleMethodsPrefixContentWithAnsiCode(Function<BeautifyShell, BeautifyShell> style, String ansiCode) {
    assertEquals(ansiCode + "BuildCLI" + RESET, style.apply(BeautifyShell.content("BuildCLI")).toString());
  }

  @Test
  void contentCreatesFluentShellFromNonStringObject() {
    assertEquals("123" + RESET, BeautifyShell.content(123).toString());
  }

  @Test
  void contentThrowsNullPointerExceptionForNullContent() {
    assertThrows(NullPointerException.class, () -> BeautifyShell.content(null));
  }

  @Test
  void appendAddsPlainAndStyledContentInOrder() {
    String result = BeautifyShell.content("Build")
        .append("CLI")
        .append("!", BeautifyShell::greenFg)
        .toString();

    assertEquals("BuildCLI" + GREEN_FG + "!" + RESET + RESET, result);
  }

  @Test
  void appendThrowsNullPointerExceptionForNullContent() {
    BeautifyShell shell = BeautifyShell.content("BuildCLI");

    assertThrows(NullPointerException.class, () -> shell.append(null));
  }

  @Test
  void chainedFluentStylesAreAppliedInReverseCallOrder() {
    String result = BeautifyShell.content("BuildCLI")
        .redFg()
        .bold()
        .underline()
        .toString();

    assertEquals(UNDERLINE + BOLD + RED_FG + "BuildCLI" + RESET, result);
  }

  @Test
  void resetAddsResetCodesAroundExistingContentAndToStringAppendsAnotherReset() {
    assertEquals(RESET + "BuildCLI" + RESET + RESET, BeautifyShell.content("BuildCLI").reset().toString());
  }

  @Test
  void rainbowAppliesRepeatingForegroundColorsToEachCharacter() {
    String expected = RED_FG + "B"
        + YELLOW_FG + "u"
        + GREEN_FG + "i"
        + CYAN_FG + "l"
        + BLUE_FG + "d"
        + MAGENTA_FG + "C"
        + RED_FG + "L"
        + YELLOW_FG + "I"
        + RESET;

    assertEquals(expected, BeautifyShell.rainbow("BuildCLI"));
  }

  @Test
  void fluentRainbowReplacesContentWithRainbowOutput() {
    String expected = RED_FG + "C" + YELLOW_FG + "L" + GREEN_FG + "I" + RESET + RESET;

    assertEquals(expected, BeautifyShell.content("CLI").rainbow().toString());
  }

  @Test
  void gradientUsesStartColorForFirstHalfAndEndColorForSecondHalf() {
    assertEquals(RED_FG + "B" + RED_FG + "u" + BLUE_FG + "i" + BLUE_FG + "l" + BLUE_FG + "d" + RESET,
        BeautifyShell.gradient("Build", RED_FG, BLUE_FG));
  }

  @Test
  void tableReturnsEmptyStringForNullOrEmptyInput() {
    assertEquals("", BeautifyShell.table(null));
    assertEquals("", BeautifyShell.table(List.of()));
  }

  @Test
  void tableBuildsBorderedOutputUsingVisibleTextWidthAndPreservesStyledContent() {
    String styledLine = BeautifyShell.redFg("warn");
    String expected = "┌──────┐\n"
        + BRIGHT_BLACK_FG + "│ " + RESET + "ok  " + BRIGHT_BLACK_FG + " │" + RESET + "\n"
        + BRIGHT_BLACK_FG + "│ " + RESET + styledLine + BRIGHT_BLACK_FG + " │" + RESET + "\n"
        + "└──────┘\n";

    assertEquals(expected, BeautifyShell.table(List.of("ok", styledLine)));
  }

  private static Stream<Arguments> staticStyles() {
    return Stream.of(
        Arguments.of((Function<Object, String>) BeautifyShell::blackFg, BLACK_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::redFg, RED_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::greenFg, GREEN_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::yellowFg, YELLOW_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::blueFg, BLUE_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::magentaFg, MAGENTA_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::cyanFg, CYAN_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::whiteFg, WHITE_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightBlackFg, BRIGHT_BLACK_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightRedFg, BRIGHT_RED_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightGreenFg, BRIGHT_GREEN_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightYellowFg, BRIGHT_YELLOW_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightBlueFg, BRIGHT_BLUE_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightMagentaFg, BRIGHT_MAGENTA_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightCyanFg, BRIGHT_CYAN_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightWhiteFg, BRIGHT_WHITE_FG),
        Arguments.of((Function<Object, String>) BeautifyShell::blackBg, BLACK_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::redBg, RED_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::greenBg, GREEN_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::yellowBg, YELLOW_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::blueBg, BLUE_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::magentaBg, MAGENTA_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::cyanBg, CYAN_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::whiteBg, WHITE_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightBlackBg, BRIGHT_BLACK_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightRedBg, BRIGHT_RED_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightGreenBg, BRIGHT_GREEN_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightYellowBg, BRIGHT_YELLOW_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightBlueBg, BRIGHT_BLUE_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightMagentaBg, BRIGHT_MAGENTA_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightCyanBg, BRIGHT_CYAN_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::brightWhiteBg, BRIGHT_WHITE_BG),
        Arguments.of((Function<Object, String>) BeautifyShell::bold, BOLD),
        Arguments.of((Function<Object, String>) BeautifyShell::italic, ITALIC),
        Arguments.of((Function<Object, String>) BeautifyShell::underline, UNDERLINE),
        Arguments.of((Function<Object, String>) BeautifyShell::blink, BLINK),
        Arguments.of((Function<Object, String>) BeautifyShell::blinking, BLINK),
        Arguments.of((Function<Object, String>) BeautifyShell::reverse, REVERSE),
        Arguments.of((Function<Object, String>) BeautifyShell::strikethrough, STRIKETHROUGH),
        Arguments.of((Function<Object, String>) BeautifyShell::doubleUnderline, DOUBLE_UNDERLINE),
        Arguments.of((Function<Object, String>) BeautifyShell::framed, FRAMED),
        Arguments.of((Function<Object, String>) BeautifyShell::encircled, ENCIRCLED),
        Arguments.of((Function<Object, String>) BeautifyShell::overlined, OVERLINED)
    );
  }

  private static Stream<Arguments> fluentStyles() {
    return Stream.of(
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.blackFg(), BLACK_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.redFg(), RED_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.greenFg(), GREEN_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.yellowFg(), YELLOW_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.blueFg(), BLUE_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.magentaFg(), MAGENTA_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.cyanFg(), CYAN_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.whiteFg(), WHITE_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightBlackFg(), BRIGHT_BLACK_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightRedFg(), BRIGHT_RED_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightGreenFg(), BRIGHT_GREEN_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightYellowFg(), BRIGHT_YELLOW_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightBlueFg(), BRIGHT_BLUE_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightMagentaFg(), BRIGHT_MAGENTA_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightCyanFg(), BRIGHT_CYAN_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightWhiteFg(), BRIGHT_WHITE_FG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.blackBg(), BLACK_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.redBg(), RED_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.greenBg(), GREEN_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.yellowBg(), YELLOW_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.blueBg(), BLUE_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.magentaBg(), MAGENTA_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.cyanBg(), CYAN_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.whiteBg(), WHITE_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightBlackBg(), BRIGHT_BLACK_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightRedBg(), BRIGHT_RED_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightGreenBg(), BRIGHT_GREEN_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightYellowBg(), BRIGHT_YELLOW_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightBlueBg(), BRIGHT_BLUE_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightMagentaBg(), BRIGHT_MAGENTA_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightCyanBg(), BRIGHT_CYAN_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.brightWhiteBg(), BRIGHT_WHITE_BG),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.bold(), BOLD),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.italic(), ITALIC),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.underline(), UNDERLINE),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.blink(), BLINK),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.reverse(), REVERSE),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.strikethrough(), STRIKETHROUGH),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.doubleUnderline(), DOUBLE_UNDERLINE),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.framed(), FRAMED),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.encircled(), ENCIRCLED),
        Arguments.of((Function<BeautifyShell, BeautifyShell>) shell -> shell.overlined(), OVERLINED)
    );
  }
}
