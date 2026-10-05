package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.domain.Attachment;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** In a GraalVM native executable the desktop module is missing and calling it aborts the process, so previews are skipped there. */
class PreviewsNativeTest {
    static final String FLAG = "org.graalvm.nativeimage.imagecode";
    @TempDir Path dir;

    @AfterEach
    void restore() {
        System.clearProperty(FLAG);
    }

    Attachment png() throws Exception {
        Path file = dir.resolve("p.png");
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", file.toFile());
        return Attachment.of(file);
    }

    @Test
    void onAJvmTheThumbnailIsDecoded() throws Exception {
        assertFalse(Previews.nativeExecutable());
        var image = Previews.decode(png());
        assertNotNull(image);
        assertEquals(8, image.width());
    }

    @Test
    void inANativeExecutableNeitherImagesNorAudioTouchTheDesktopModule() throws Exception {
        Attachment image = png();
        System.setProperty(FLAG, "runtime");
        assertTrue(Previews.nativeExecutable());
        assertNull(Previews.decode(image), "no thumbnail, no crash");
        assertNull(Previews.analyse(image), "no waveform either");
    }
}
