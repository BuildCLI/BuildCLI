package dev.buildcli.core.utils.formatter;

public class FormatterFactory {
    public static Formatter getFormatter(String type) {
        return switch (type) {
            case "html" -> new HtmlFormatter();
            case "json" -> new JsonFormatter();
            default -> new MarkdownFormatter();
        };
    }
}
