package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.buildcli.domain.Attachment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PreviewsTest {
    @TempDir Path dir;

    Attachment png(String name, int w, int h, int type) throws IOException {
        Path f = dir.resolve(name);
        ImageIO.write(new BufferedImage(w, h, type), "png", f.toFile());
        return new Attachment(Attachment.Kind.IMAGE, f, name, "image/png", Files.size(f));
    }

    @Test
    void anImageWithTooManyPixelsIsNotDecoded() throws Exception {
        // 36 million pixels as a bit each: a few megabytes on disk, hundreds as decoded pixels
        assertNull(Previews.decode(png("huge.png", 6000, 6000, BufferedImage.TYPE_BYTE_BINARY)));
        assertNotNull(Previews.decode(png("small.png", 40, 30, BufferedImage.TYPE_INT_RGB)));
    }

    @Test
    void aFileEditedInPlaceToTheSameSizeIsAnotherPreview() throws Exception {
        Attachment a = png("shot.png", 4, 4, BufferedImage.TYPE_INT_RGB);
        String before = Previews.key(a);
        Files.setLastModifiedTime(a.path(), FileTime.fromMillis(Files.getLastModifiedTime(a.path()).toMillis() + 5000));
        assertNotEquals(before, Previews.key(a));
    }

    @Test
    void aCodeBlockThatStartsWithABlankLineKeepsIt() {
        assertEquals(List.of("\nfoo"), Styled.codeBlocks("```java\n\nfoo\n```"));
        assertEquals(List.of("a\n\nb"), Styled.codeBlocks("```\na\n\nb\n```"));
    }
}
