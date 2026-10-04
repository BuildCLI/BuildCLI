package dev.buildcli.infrastructure.tui;

import dev.buildcli.domain.Attachment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Small terminal previews of attachments, computed off the render thread and cached: an image becomes colour pixels
 * (drawn two per cell with half blocks), a WAV clip becomes a duration and a waveform. Formats the JDK cannot decode
 * simply have no preview; the attachment is still sent.
 */
final class Previews {
    private Previews() {}

    /** Pixels are packed 0xRRGGBB, row-major, {@code width x height}. */
    record Image(int width, int height, int[] pixels) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Image i && width == i.width && height == i.height && java.util.Arrays.equals(pixels, i.pixels);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * width + height) + java.util.Arrays.hashCode(pixels);
        }

        @Override
        public String toString() {
            return "Image[" + width + "x" + height + "]";
        }
    }

    record Audio(double seconds, String waveform) {}

    private static final int MAX_W = 48;
    private static final int MAX_H = 28;
    private static final Map<String, CompletableFuture<Image>> IMAGES = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Audio>> AUDIO = new ConcurrentHashMap<>();
    private static final String BARS = "▁▂▃▄▅▆▇█";

    static {
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * True in a GraalVM native executable. Decoding images and audio needs the JDK's desktop module (AWT, ImageIO, Java
     * Sound), which a native executable does not have: calling it there aborts the whole process, not just the preview.
     * The attachment is still sent to the model; only its thumbnail or waveform is left out.
     */
    static boolean nativeExecutable() {
        return System.getProperty("org.graalvm.nativeimage.imagecode") != null;
    }

    /** The image once decoded, or null while it is loading or if it cannot be decoded. */
    static Image image(Attachment a) {
        var f = IMAGES.computeIfAbsent(key(a), k -> CompletableFuture.supplyAsync(() -> decode(a)));
        return f.isDone() ? f.getNow(null) : null;
    }

    /** True while a preview is still being made. */
    static boolean loading(Attachment a) {
        var i = IMAGES.get(key(a));
        var s = AUDIO.get(key(a));
        return i != null && !i.isDone() || s != null && !s.isDone();
    }

    static Audio audio(Attachment a) {
        var f = AUDIO.computeIfAbsent(key(a), k -> CompletableFuture.supplyAsync(() -> analyse(a)));
        return f.isDone() ? f.getNow(null) : null;
    }

    private static String key(Attachment a) {
        return a.path() + "|" + a.size();
    }

    static Image decode(Attachment a) {
        if (nativeExecutable()) {
            return null;
        }
        try (var in = Files.newInputStream(a.path())) {
            BufferedImage src = ImageIO.read(in);
            if (src == null || src.getWidth() <= 0 || src.getHeight() <= 0) {
                return null;
            }
            double scale = Math.min(1.0, Math.min((double) MAX_W / src.getWidth(), (double) MAX_H / src.getHeight()));
            int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
            int[] px = new int[w * h];
            // area average per target pixel: better than nearest neighbour for a thumbnail
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int x0 = x * src.getWidth() / w;
                    int x1 = Math.max(x0 + 1, (x + 1) * src.getWidth() / w);
                    int y0 = y * src.getHeight() / h;
                    int y1 = Math.max(y0 + 1, (y + 1) * src.getHeight() / h);
                    long r = 0;
                    long g = 0;
                    long b = 0;
                    int n = 0;
                    for (int yy = y0; yy < y1; yy++) {
                        for (int xx = x0; xx < x1; xx++) {
                            int argb = src.getRGB(xx, yy);
                            int alpha = argb >>> 24;
                            // transparent pixels show the chat background
                            r += ((argb >> 16 & 0xFF) * alpha + 17 * (255 - alpha)) / 255;
                            g += ((argb >> 8 & 0xFF) * alpha + 27 * (255 - alpha)) / 255;
                            b += ((argb & 0xFF) * alpha + 33 * (255 - alpha)) / 255;
                            n++;
                        }
                    }
                    px[y * w + x] = (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
                }
            }
            return new Image(w, h, px);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    static Audio analyse(Attachment a) {
        if (nativeExecutable()) {
            return null;
        }
        try (AudioInputStream in = AudioSystem.getAudioInputStream(a.path().toFile())) {
            AudioFormat fmt = in.getFormat();
            long frames = in.getFrameLength();
            double seconds = frames > 0 && fmt.getFrameRate() > 0 ? frames / (double) fmt.getFrameRate() : -1;
            if (fmt.getSampleSizeInBits() != 16 || frames <= 0) {
                return new Audio(seconds, "");
            }
            int bars = 28;
            double[] peak = new double[bars];
            byte[] buf = new byte[fmt.getFrameSize() * 1024];
            long seen = 0;
            int read;
            boolean big = fmt.isBigEndian();
            while ((read = in.read(buf)) > 0) {
                for (int i = 0; i + 1 < read; i += fmt.getFrameSize()) {
                    int lo = big ? buf[i + 1] & 0xFF : buf[i] & 0xFF;
                    int hi = big ? buf[i] : buf[i + 1];
                    double v = Math.abs((hi << 8 | lo)) / 32768.0;
                    int bar = (int) Math.min((long) bars - 1, (long) seen * bars / frames);
                    peak[bar] = Math.max(peak[bar], v);
                    seen++;
                }
            }
            StringBuilder sb = new StringBuilder();
            for (double p : peak) {
                sb.append(BARS.charAt((int) Math.min((long) BARS.length() - 1, Math.round(Math.sqrt(p) * (BARS.length() - 1)))));
            }
            return new Audio(seconds, sb.toString());
        } catch (Exception e) {
            return null;
        }
    }

    static String duration(double seconds) {
        if (seconds < 0) {
            return "";
        }
        int s = (int) Math.round(seconds);
        return s / 60 + ":" + String.format("%02d", s % 60);
    }
}
