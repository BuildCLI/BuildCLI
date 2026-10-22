package dev.buildcli.infrastructure;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Replaces a file in one step: the content is written to a temporary file in the same folder and then moved over the
 * destination, so an interrupted or failed write leaves the old file exactly as it was.
 */
public final class AtomicFile {
    /** Fills the temporary file. */
    public interface Writer {
        void write(Path tmp) throws IOException;
    }

    private AtomicFile() {
    }

    public static void write(Path file, Writer writer) throws IOException {
        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, file.getFileName() + "-", ".tmp");
        try {
            writer.write(tmp);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
