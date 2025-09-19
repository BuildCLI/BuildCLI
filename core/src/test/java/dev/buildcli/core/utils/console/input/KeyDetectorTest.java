package dev.buildcli.core.utils.console.input;

import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class KeyDetectorTest {

    private NonBlockingReader mockSimpleReader(int... inputs) throws IOException {
        NonBlockingReader reader = mock(NonBlockingReader.class);
        when(reader.read()).thenReturn(inputs[0], toObjectArray(inputs, 1));
        when(reader.peek(anyLong())).thenReturn(-2);
        return reader;
    }

    private NonBlockingReader mockEscapeReader(int... inputs) throws IOException {
        NonBlockingReader reader = mock(NonBlockingReader.class);
        when(reader.read()).thenReturn(inputs[0], toObjectArray(inputs, 1));
        when(reader.peek(anyLong())).thenReturn(1, 1, -2);
        return reader;
    }

    private Integer[] toObjectArray(int[] arr, int start) {
        Integer[] boxed = new Integer[arr.length - start];
        for (int i = start; i < arr.length; i++) {
            boxed[i - start] = arr[i];
        }
        return boxed;
    }

    @Test
    @DisplayName("Detect ENTER from CR (13)")
    void testEnterFromCR() throws IOException {
        NonBlockingReader reader = mockSimpleReader(13);
        assertEquals(KeyDetector.KeyType.ENTER, KeyDetector.detectKey(reader));
    }

    @Test
    @DisplayName("Detect ENTER from LF (10)")
    void testEnterFromLF() throws IOException {
        NonBlockingReader reader = mockSimpleReader(10);
        assertEquals(KeyDetector.KeyType.ENTER, KeyDetector.detectKey(reader));
    }

    @Test
    @DisplayName("Detect CTRL+C")
    void testCtrlC() throws IOException {
        NonBlockingReader reader = mockSimpleReader(3);
        assertEquals(KeyDetector.KeyType.CTRL_C, KeyDetector.detectKey(reader));
    }

    @Test
    @DisplayName("Detect SPACE")
    void testSpace() throws IOException {
        NonBlockingReader reader = mockSimpleReader(32);
        assertEquals(KeyDetector.KeyType.SPACE, KeyDetector.detectKey(reader));
    }

    @Test
    @DisplayName("Detect UP from ESC [ A")
    void testUpFromAnsi() throws IOException {
        NonBlockingReader reader = mockEscapeReader(27, '[', 'A');
        assertEquals(KeyDetector.KeyType.UP, KeyDetector.detectKey(reader));
    }

    @Test
    @DisplayName("Detect UP from VT100 ESC O A")
    void testUpFromVT100() throws IOException {
        NonBlockingReader reader = mockEscapeReader(27, 'O', 'A');
        assertEquals(KeyDetector.KeyType.UP, KeyDetector.detectKey(reader));
    }

    @Test
    @DisplayName("Detect UP from Ctrl+P (16)")
    void testUpFromCtrlP() throws IOException {
        NonBlockingReader reader = mockSimpleReader(16);
        assertEquals(KeyDetector.KeyType.UP, KeyDetector.detectKey(reader));
    }

    @Test
    @DisplayName("Detect DOWN from ESC [ B")
    void testDownFromAnsi() throws IOException {
        NonBlockingReader reader = mockEscapeReader(27, '[', 'B');
        assertEquals(KeyDetector.KeyType.DOWN, KeyDetector.detectKey(reader));
    }

    @Test
    @DisplayName("Detect DOWN from ESC O B")
    void testDownFromVT100() throws IOException {
        NonBlockingReader reader = mockEscapeReader(27, 'O', 'B');
        assertEquals(KeyDetector.KeyType.DOWN, KeyDetector.detectKey(reader));
    }

    @Test
    @DisplayName("Detect DOWN from Ctrl+N (14)")
    void testDownFromCtrlN() throws IOException {
        NonBlockingReader reader = mockSimpleReader(14);
        assertEquals(KeyDetector.KeyType.DOWN, KeyDetector.detectKey(reader));
    }

    @Test
    @DisplayName("Detect OTHER key")
    void testOtherKey() throws IOException {
        NonBlockingReader reader = mockSimpleReader(99);
        assertEquals(KeyDetector.KeyType.OTHER, KeyDetector.detectKey(reader));
    }
}
