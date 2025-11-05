package dev.buildcli.core.utils.formatter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatterFactoryTest {

    @Test
    void testGetFormatter_withHtmlType() {
        // Test for HTML formatter
        Formatter formatter = FormatterFactory.getFormatter("html");

        assertNotNull(formatter, "Formatter should not be null.");
        assertTrue(formatter instanceof HtmlFormatter, "Formatter should be an instance of HtmlFormatter.");
    }

    @Test
    void testGetFormatter_withJsonType() {
        // Test for JSON formatter
        Formatter formatter = FormatterFactory.getFormatter("json");

        assertNotNull(formatter, "Formatter should not be null.");
        assertTrue(formatter instanceof JsonFormatter, "Formatter should be an instance of JsonFormatter.");
    }

    @Test
    void testGetFormatter_withMarkdownType() {
        // Test for Markdown formatter (default case)
        Formatter formatter = FormatterFactory.getFormatter("markdown");

        assertNotNull(formatter, "Formatter should not be null.");
        assertTrue(formatter instanceof MarkdownFormatter, "Formatter should be an instance of MarkdownFormatter.");
    }

    @Test
    void testGetFormatter_withUnknownType() {
        // Test for an unknown type (default case should be MarkdownFormatter)
        Formatter formatter = FormatterFactory.getFormatter("unknown");

        assertNotNull(formatter, "Formatter should not be null.");
        assertTrue(formatter instanceof MarkdownFormatter, "Formatter should be an instance of MarkdownFormatter.");
    }

}
