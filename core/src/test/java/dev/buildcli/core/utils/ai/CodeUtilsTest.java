package dev.buildcli.core.utils.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CodeUtilsTest {
    private static final String JAVA_MARKER = "```java";
    private static final String KOTLIN_MARKER = "```kotlin";
    private static final String SCALA_MARKER = "```scala";
    private static final String GROOVY_MARKER = "```groovy";

    private static Stream<Arguments> codeBlockLanguages() {
        return Stream.of(
                Arguments.of(JAVA_MARKER, 7),
                Arguments.of(KOTLIN_MARKER, 9),
                Arguments.of(SCALA_MARKER, 8),
                Arguments.of(GROOVY_MARKER, 9));
    }

    @Test
    void startCode_InvalidInput_returnsNegativeOne() {
        // Tests null and invalid inputs
        assertEquals(-1, CodeUtils.startCode(null));
        assertEquals(-1, CodeUtils.startCode("some_text"));
        assertEquals(-1, CodeUtils.startCode("```JAVA\ncode\n```")); // Case sensitivity
    }

    @ParameterizedTest
    @MethodSource("codeBlockLanguages")
    void startCode_VariousLanguages_returnsCorrectIndex(String marker, int offset) {

        String content = String.format("Some text\n%s\ncode\n```", marker);

        int result = CodeUtils.startCode(content);

        assertEquals(content.indexOf(marker) + offset, result);
    }

    @ParameterizedTest
    @MethodSource("codeBlockLanguages")
    void endCode_VariousLanguages_returnsCorrectIndex(String marker, int offset) {

        String content = String.format("Some text\n%s\ncode\n```", marker);

        int result = CodeUtils.endCode(content);

        int expectedIndex = content.lastIndexOf("```");
        assertEquals(expectedIndex, result);
    }

    @Test
    void endCode_MultipleCodeBlocks_returnsFirstEndMarker() {

        String content = String.format("%s\ncode1\n```\n%s\ncode2\n```", JAVA_MARKER, KOTLIN_MARKER);

        int result = CodeUtils.endCode(content);

        assertEquals(content.indexOf("```", content.indexOf(JAVA_MARKER) + 3), result);
    }

    @Test
    void endCode_NoEndMarker_returnsNegativeOne() {

        String content = JAVA_MARKER + "\nSystem.out.println();";

        int result = CodeUtils.endCode(content);

        assertEquals(-1, result);
    }

    @Test
    void extractCode_NullContent_returnsEmptyString() {

        String content = null;

        String result = CodeUtils.extractCode(content);

        assertEquals("", result);
    }

    @Test
    void extractCode_NoCodeBlock_returnsTrimedContent() {

        String content = "  Some text no code block  ";

        String result = CodeUtils.extractCode(content);

        assertEquals("Some text no code block", result);
    }

    @ParameterizedTest
    @MethodSource("codeBlockLanguages")
    void extractCode_VariousLanguages_returnsOnlyCode(String marker, int unused) {

        String expectedCode = "println(\"Hello\")";
        String content = String.format("Some text before\n%s\n%s\n```\nText after", marker, expectedCode);

        String result = CodeUtils.extractCode(content);

        assertEquals(expectedCode, result);
    }

    @Test
    void extractCode_MultipleCodeBlocks_returnsFirstBlock() {

        String firstBlock = "first block";
        String content = String.format("%s\n%s\n```\n%s\nsecond block\n```",
                JAVA_MARKER, firstBlock, KOTLIN_MARKER);

        String result = CodeUtils.extractCode(content);

        assertEquals(firstBlock, result);
    }

    @Test
    void extractCode_EmptyCodeBlock_returnsEmptyString() {

        String content = "";

        String result = CodeUtils.extractCode(content);

        assertEquals("", result);
    }
}
