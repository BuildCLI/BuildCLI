package dev.buildcli.core.utils.input;

import dev.buildcli.core.utils.BeautifyShell;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static java.util.List.of;

public abstract class InteractiveInputUtils {
  private static final Terminal terminal;
  private static final LineReader reader;

  private InteractiveInputUtils() {}

  static {
    try {
      terminal = TerminalBuilder.builder()
          .system(true)
          .build();

      terminal.enterRawMode();

      reader = LineReaderBuilder.builder()
          .terminal(terminal)
          .option(LineReader.Option.AUTO_FRESH_LINE, true)
          .option(LineReader.Option.AUTO_REMOVE_SLASH, true)
          .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
          .completer(new StringsCompleter(""))
          .build();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean confirm(String message, List<String> yesOptions, List<String> noOptions, Boolean defaultValue) {
    // Set default options if not provided
    var yes = yesOptions != null && !yesOptions.isEmpty() ? yesOptions : of("yes", "y");
    var no = noOptions != null && !noOptions.isEmpty() ? noOptions : of("no", "n");

    //Normalizing options
    yes = yes.stream().map(String::toLowerCase).toList();
    no = no.stream().map(String::toLowerCase).toList();

    // Create prompt text showing default value
    StringBuilder promptBuilder = new StringBuilder(message);
    promptBuilder.append(" (");

    if (defaultValue != null) {
      if (defaultValue) {
        promptBuilder.append("[").append(yes.getFirst()).append("]");
        promptBuilder.append("/").append(no.getFirst());
      } else {
        promptBuilder.append(yes.getFirst());
        promptBuilder.append("/[").append(no.getFirst()).append("]");
      }
    } else {
      promptBuilder.append(yes.getFirst()).append("/").append(no.getFirst());
    }

    promptBuilder.append("): ");

    // Loop until valid input is provided
    while (true) {
      String input;
      try {
        input = reader.readLine(promptBuilder.toString()).trim().toLowerCase();
      } catch (UserInterruptException e) {
        // User pressed Ctrl+C
        return false;
      }

      // Handle empty input with default value
      if (input.isEmpty() && defaultValue != null) {
        return defaultValue;
      }

      // Check for yes match
      if (yes.contains(input)) {
        return true;
      }

      // Check for no match
      if (no.contains(input)) {
        return false;
      }

      // Print error for invalid input
      println("Please enter '" + yes.getFirst() + "' or '" + no.getFirst() + "'");
    }
  }

  public static boolean confirm(String message) {
    return confirm(message, of("yes","y"), of("no", "n"), null);
  }

  /**
   * Displays an interactive terminal menu for selecting an option.
   *
   * @param prompt    The prompt to display to the user.
   * @param options   The list of available options.
   * @param formatter Function to convert each option to a String. If null, uses {@code Object::toString}.
   * @param <T>       The type of the options.
   * @return The selected option or {@code null} if the operation was canceled.
   * @throws IllegalArgumentException if the options list is null or empty.
   */
  public static <T> T options(String prompt, List<T> options, Function<T, String> formatter) {
    if (options == null || options.isEmpty()) {
      throw new IllegalArgumentException("Options list cannot be empty");
    }

    // Define the display function, using the provided formatter or defaulting to Object::toString
    Function<T, String> display = formatter != null ? formatter : Object::toString;
    int selectedIndex = 0;
    int startIndex = 0;
    int maxVisibleOptions = options.size();

    boolean first = true;

    while (true) {
      if (!first) {
        // Clear the previous display area
        clearLines(maxVisibleOptions * 100);
      }

      // Display the prompt and initial instructions
      println(prompt);
      println("(Use arrow keys ↑↓ to navigate, Enter to select, Ctrl+C to cancel)");

      first = false;

      // Render the currently visible options
      renderOptions(options, display, selectedIndex, startIndex, maxVisibleOptions);

      // Display scroll indicators if needed
      if (startIndex > 0) {
        print("↑ more options above\r");
      }
      if (startIndex + maxVisibleOptions < options.size()) {
        println();
        print("↓ more options below\r");
      }

      try {
        var key = KeyDetector.detectKey(terminal.reader());
        switch (key) {
          case UP:
            if (selectedIndex > 0) {
              selectedIndex--;
              if (selectedIndex < startIndex) {
                startIndex = selectedIndex;
              }
            }
            break;
          case DOWN:
            if (selectedIndex < options.size() - 1) {
              selectedIndex++;
              if (selectedIndex >= startIndex + maxVisibleOptions) {
                startIndex = selectedIndex - maxVisibleOptions + 1;
              }
            }
            break;
          case ENTER:
            clearLines(maxVisibleOptions * 100);
            return options.get(selectedIndex);
          case CTRL_C:
            clearLines(maxVisibleOptions * 100);
            println("Operation canceled");
            return null;
          default:
            System.out.println(key);
            // Ignore other keys
            break;
        }
      } catch (UserInterruptException | IOException e) {
        clearLines(maxVisibleOptions * 100);
        println("Operation canceled");
        return null;
      }
    }
  }

  private static void println() {
    terminal.writer().println();
    terminal.flush();
  }

  private static void print(Object message) {
    terminal.writer().print(message);
    terminal.flush();
  }

  private static void println(Object message) {
    terminal.writer().println(message);
    terminal.flush();
  }

  /**
   * Renders the list of options in the terminal.
   *
   * @param options           The complete list of options.
   * @param display           Function to convert each option to a String.
   * @param selectedIndex     The index of the currently selected option.
   * @param startIndex        The starting index for the visible options.
   * @param maxVisibleOptions Maximum number of options visible at once.
   * @param <T>               The type of the options.
   */
  private static <T> void renderOptions(List<T> options, Function<T, String> display, int selectedIndex, int startIndex, int maxVisibleOptions) {
    for (int i = 0; i < maxVisibleOptions; i++) {
      int optionIndex = startIndex + i;
      boolean isSelected = selectedIndex == optionIndex;
      if (optionIndex < options.size()) {
        T option = options.get(optionIndex);
        var item = BeautifyShell.content(display.apply(option));

        if (isSelected) {
          item.blueFg().underline();
        }

        println(item);
      }
    }
  }


  public static <T> T options(String prompt, List<T> options) {
    return options(prompt, options, Object::toString);
  }

  public static String question(String prompt, String defaultValue, boolean required) {
    String effectivePrompt = prompt;

    if (defaultValue != null && !defaultValue.isEmpty()) {
      effectivePrompt += " [" + defaultValue + "]: ";
    } else {
      effectivePrompt += ": ";
    }

    while (true) {
      String input;
      try {
        input = reader.readLine(effectivePrompt);
      } catch (UserInterruptException e) {
        // User pressed Ctrl+C
        println("Operation canceled");
        return null;
      }

      // Use default if input is empty and default exists
      if (input.isEmpty() && defaultValue != null) {
        return defaultValue;
      }

      // Check if input is required but not provided
      if (input.isEmpty() && required) {
        println("Input is required. Please enter a value.");
        continue;
      }

      return input;
    }
  }


  public static String question(String prompt) {
    return question(prompt, null, false);
  }

  private static void clearLines(int count) {
    for (int i = 0; i < count; i++) {
      terminal.puts(InfoCmp.Capability.cursor_up);
      terminal.puts(InfoCmp.Capability.carriage_return);
      terminal.puts(InfoCmp.Capability.clr_eol);
    }
  }
}
