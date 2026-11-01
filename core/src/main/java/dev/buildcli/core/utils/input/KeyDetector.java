package dev.buildcli.core.utils.input;

import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.util.Arrays;

final class KeyDetector {
  public enum KeyType {
    UP, DOWN, ENTER, CTRL_C, SPACE, OTHER
  }

  // Read a potential escape sequence from the reader
  private static int[] readEscapeSequence(NonBlockingReader reader) throws IOException {
    int[] sequence = new int[8]; // Should be enough for any escape sequence
    int length = 0;

    // Read the first character
    sequence[length++] = reader.read();

    // Check if it's the start of an escape sequence
    if (sequence[0] == 27) { // ESC
      while (length < sequence.length && reader.peek(100) != -2) {
        sequence[length++] = reader.read();
      }
    }

    return Arrays.copyOf(sequence, length);
  }

  // Detect which key was pressed based on the sequence
  public static KeyType detectKey(NonBlockingReader reader) throws IOException {
    var sequence = readEscapeSequence(reader);
    // Check simple keys first
    if (sequence.length == 1) {
      if (sequence[0] == 13 || sequence[0] == 10) { // CR or LF
        return KeyType.ENTER;
      }
      if (sequence[0] == 3) { // Ctrl+C
        return KeyType.CTRL_C;
      }
      if (sequence[0] == 32) { // Space
        return KeyType.SPACE;
      }
    }

    // Check for escape sequences
    if (sequence.length >= 3 && sequence[0] == 27 && sequence[1] == '[') {
      // ANSI escape sequence
      if (sequence[2] == 'A' || sequence[2] == 65) { // 'A' in ASCII is 65
        return KeyType.UP;
      }
      if (sequence[2] == 'B' || sequence[2] == 66) { // 'B' in ASCII is 66
        return KeyType.DOWN;
      }
    }

    // Check for other UP key sequences
    if (isUpArrowSequence(sequence)) {
      return KeyType.UP;
    }

    // Check for other DOWN key sequences
    if (isDownArrowSequence(sequence)) {
      return KeyType.DOWN;
    }

    return KeyType.OTHER;
  }

  // Helper to identify UP arrow in different terminal environments
  private static boolean isUpArrowSequence(int[] sequence) {
    // Common UP arrow sequences:
    // - ESC [ A (ANSI)
    // - ESC O A (VT100)
    // - 16 (CTRL-P in some terminals)
    // - 224 72 (Windows console)

    if (sequence.length == 3 && sequence[0] == 27 &&
        (sequence[1] == '[' || sequence[1] == 'O') &&
        (sequence[2] == 'A' || sequence[2] == 65)) {
      return true;
    }

    if (sequence.length == 1 && sequence[0] == 16) {
      return true;
    }

    if (sequence.length == 2 && sequence[0] == 224 && sequence[1] == 72) {
      return true;
    }

    return false;
  }

  // Helper to identify DOWN arrow in different terminal environments
  private static boolean isDownArrowSequence(int[] sequence) {
    // Common DOWN arrow sequences:
    // - ESC [ B (ANSI)
    // - ESC O B (VT100)
    // - 14 (CTRL-N in some terminals)
    // - 224 80 (Windows console)

    if (sequence.length == 3 && sequence[0] == 27 &&
        (sequence[1] == '[' || sequence[1] == 'O') &&
        (sequence[2] == 'B' || sequence[2] == 66)) {
      return true;
    }

    if (sequence.length == 1 && sequence[0] == 14) {
      return true;
    }

    if (sequence.length == 2 && sequence[0] == 224 && sequence[1] == 80) {
      return true;
    }

    return false;
  }
}