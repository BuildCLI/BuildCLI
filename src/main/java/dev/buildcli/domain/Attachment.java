package dev.buildcli.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * A file the user attached to a message: an image or an audio clip that goes to the model together with the text.
 * Only the user attaches files (a model cannot), so any file the user names is allowed; size is capped.
 */
public record Attachment(Kind kind, Path path, String name, String mime, long size) {
    public enum Kind { IMAGE, AUDIO }

    public static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;
    public static final long MAX_AUDIO_BYTES = 20L * 1024 * 1024;

    private static final Map<String, String> IMAGES = Map.of("png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg",
            "gif", "image/gif", "webp", "image/webp");
    private static final Map<String, String> AUDIO = Map.of("wav", "audio/wav", "mp3", "audio/mpeg", "m4a", "audio/mp4",
            "ogg", "audio/ogg", "flac", "audio/flac", "opus", "audio/opus", "aac", "audio/aac");

    /** @throws IllegalArgumentException with a message fit for the user when the file cannot be attached */
    public static Attachment of(Path path) {
        Path file = path.toAbsolutePath().normalize();
        String name = file.getFileName() == null ? file.toString() : file.getFileName().toString();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        Kind kind;
        String mime;
        if (IMAGES.containsKey(ext)) {
            kind = Kind.IMAGE;
            mime = IMAGES.get(ext);
        } else if (AUDIO.containsKey(ext)) {
            kind = Kind.AUDIO;
            mime = AUDIO.get(ext);
        } else {
            throw new IllegalArgumentException("can attach images (" + String.join(", ", IMAGES.keySet().stream().sorted().toList())
                    + ") and audio (" + String.join(", ", AUDIO.keySet().stream().sorted().toList()) + "), not '" + name
                    + "'. For text files, ask the agent to read them.");
        }
        long size;
        try {
            if (!Files.isRegularFile(file)) {
                throw new IllegalArgumentException("no such file: " + file);
            }
            size = Files.size(file);
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read " + file + ": " + e.getMessage());
        }
        long max = kind == Kind.IMAGE ? MAX_IMAGE_BYTES : MAX_AUDIO_BYTES;
        if (size > max) {
            throw new IllegalArgumentException(name + " is " + size / 1024 / 1024 + " MB; the limit for " + kind.name().toLowerCase(Locale.ROOT)
                    + " is " + max / 1024 / 1024 + " MB");
        }
        return new Attachment(kind, file, name, mime, size);
    }
}
