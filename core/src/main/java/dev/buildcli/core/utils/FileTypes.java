package org.buildcli.utils;

public enum FileTypes {
    MARKDOWN("markdown", ".md"),
    HTML("html", ".html"),
    JSON("json", ".json");

    private final String type;
    private final String extension;

    FileTypes(String type, String extension) {
        this.type = type;
        this.extension = extension;
    }

    public String getType() {
        return type;
    }

    public String getExtension() {
        return extension;
    }

    public static String fromExtension(String format) {
        if (format == null || format.isBlank()) {
            return MARKDOWN.getExtension();
        }

        for (FileTypes fileTypes : FileTypes.values()) {
            if (fileTypes.getType().equals(format)) {
                return fileTypes.getExtension();
            }
        }
        return MARKDOWN.getExtension();
    }
}
