package dev.buildcli.core.actions.changelog;

import dev.buildcli.core.utils.FileTypes;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ChangelogFileUtils {

    public static String formatOutputFile(String fileName, String format) {
        if (fileName == null || fileName.isBlank()) {
            return "CHANGELOG" + FileTypes.fromExtension(format);
        }
        String outputFileName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String extension = FileTypes.fromExtension(format);
        return Path.of(outputFileName + extension).toString();
    }

    public static void writeToFile(String content, String outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)){
            writer.write(content);
        }
    }
}
