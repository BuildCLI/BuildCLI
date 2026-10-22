package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.buildcli.infrastructure.AtomicFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicFileTest {
    @TempDir Path dir;

    @Test
    void aFailedWriteLeavesTheOldFileAndNoLeftovers() throws Exception {
        Path file = dir.resolve("chats.yaml");
        AtomicFile.write(file, tmp -> Files.writeString(tmp, "good\n"));
        assertThrows(IOException.class, () -> AtomicFile.write(file, tmp -> {
            Files.writeString(tmp, "half of a new");
            throw new IOException("disk full");
        }));

        assertEquals("good\n", Files.readString(file));
        try (var files = Files.list(dir)) {
            assertEquals(1, files.count(), "the temporary file is removed");
        }
    }

    @Test
    void aSuccessfulWriteReplacesTheFile() throws Exception {
        Path file = dir.resolve("nested/settings.yaml");
        AtomicFile.write(file, tmp -> Files.writeString(tmp, "one\n"));
        AtomicFile.write(file, tmp -> Files.writeString(tmp, "two\n"));
        assertEquals("two\n", Files.readString(file));
    }
}
