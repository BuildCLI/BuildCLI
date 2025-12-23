package dev.buildcli.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FileTypesTest {

    @Test
    void testEnumValues() {
        assertEquals("markdown", FileTypes.MARKDOWN.getType());
        assertEquals(".md", FileTypes.MARKDOWN.getExtension());

        assertEquals("html", FileTypes.HTML.getType());
        assertEquals(".html", FileTypes.HTML.getExtension());

        assertEquals("json", FileTypes.JSON.getType());
        assertEquals(".json", FileTypes.JSON.getExtension());
    }

    @Test
    void testFromExtension_withKnownTypes() {
        assertEquals(".html", FileTypes.fromExtension("html"));
        assertEquals(".json", FileTypes.fromExtension("json"));
        assertEquals(".md", FileTypes.fromExtension("markdown"));
    }

    @Test
    void testFromExtension_withNullOrBlank() {
        assertEquals(".md", FileTypes.fromExtension(null));
        assertEquals(".md", FileTypes.fromExtension(""));
        assertEquals(".md", FileTypes.fromExtension("   "));
    }

    @Test
    void testFromExtension_withUnknownType() {
        assertEquals(".md", FileTypes.fromExtension("xml"));
        assertEquals(".md", FileTypes.fromExtension("yaml"));
    }
}
