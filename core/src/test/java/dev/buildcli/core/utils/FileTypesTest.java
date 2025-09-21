package dev.buildcli.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the FileTypes enum.
 */
class FileTypesTest {

    @Test
    @DisplayName("Should return correct type for MARKDOWN")
    void getType_whenMarkdown_shouldReturnType() {
        assertEquals("markdown", FileTypes.MARKDOWN.getType());
    }

    @Test
    @DisplayName("Should return correct extension for MARKDOWN")
    void getExtension_whenMarkdown_shouldReturnExtension() {
        assertEquals(".md", FileTypes.MARKDOWN.getExtension());
    }

    @Test
    @DisplayName("Should return correct type for HTML")
    void getType_whenHtml_shouldReturnType() {
        assertEquals("html", FileTypes.HTML.getType());
    }

    @Test
    @DisplayName("Should return correct extension for HTML")
    void getExtension_whenHtml_shouldReturnExtension() {
        assertEquals(".html", FileTypes.HTML.getExtension());
    }

    @Test
    @DisplayName("Should return correct type for JSON")
    void getType_whenJson_shouldReturnType() {
        assertEquals("json", FileTypes.JSON.getType());
    }

    @Test
    @DisplayName("Should return correct extension for JSON")
    void getExtension_whenJson_shouldReturnExtension() {
        assertEquals(".json", FileTypes.JSON.getExtension());
    }

    @Test
    @DisplayName("fromExtension should return correct extension for 'markdown'")
    void fromExtension_withMarkdownType_shouldReturnMdExtension() {
        assertEquals(".md", FileTypes.fromExtension("markdown"));
    }

    @Test
    @DisplayName("fromExtension should return correct extension for 'html'")
    void fromExtension_withHtmlType_shouldReturnHtmlExtension() {
        assertEquals(".html", FileTypes.fromExtension("html"));
    }

    @Test
    @DisplayName("fromExtension should return correct extension for 'json'")
    void fromExtension_withJsonType_shouldReturnJsonExtension() {
        assertEquals(".json", FileTypes.fromExtension("json"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("fromExtension should return default markdown extension for null, empty, or blank input")
    void fromExtension_withNullOrBlankInput_shouldReturnDefault(String input) {
        assertEquals(FileTypes.MARKDOWN.getExtension(), FileTypes.fromExtension(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"xml", "java", "TEXT", "Markdown"})
    @DisplayName("fromExtension should return default markdown extension for unknown types")
    void fromExtension_withUnknownType_shouldReturnDefault(String unknownType) {
        assertEquals(FileTypes.MARKDOWN.getExtension(), FileTypes.fromExtension(unknownType));
    }
}
