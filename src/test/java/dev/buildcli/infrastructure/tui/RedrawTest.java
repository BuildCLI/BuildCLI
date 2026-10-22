package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The screen is drawn when something changes, and not otherwise: no timer runs while nothing moves. */
class RedrawTest {
    final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    final AtomicInteger frames = new AtomicInteger();
    final AtomicBoolean moving = new AtomicBoolean();
    final Redraw redraw = new Redraw(frames::incrementAndGet, scheduler, moving::get);

    @AfterEach
    void stop() {
        scheduler.shutdownNow();
    }

    static void pause(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    /** Waits for a condition instead of guessing how long a loaded CI machine takes. */
    static void awaitUntil(java.util.function.BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000;
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }

    @Test
    void idleMeansNoFramesAndNoClock() throws Exception {
        redraw.frameDrawn();
        pause(300);
        assertEquals(0, frames.get());
        assertFalse(redraw.clockRunning());
    }

    @Test
    void aChangeDrawsOnceAndABurstOfChangesDrawsFarFewerFramesThanChanges() throws Exception {
        redraw.request();
        pause(100);
        assertEquals(1, frames.get());

        for (int i = 0; i < 1000; i++) {
            redraw.request();
        }
        pause(150);
        assertTrue(frames.get() >= 2 && frames.get() <= 4, "a burst is one or two frames, not a thousand: " + frames.get());
    }

    @Test
    void somethingThatMovesGetsFramesUntilItStopsAndThenOneMore() throws Exception {
        moving.set(true);
        redraw.frameDrawn();
        awaitUntil(() -> frames.get() >= 3, 5000);
        int whileMoving = frames.get();
        assertTrue(whileMoving >= 3, "the spinner turns: " + whileMoving);
        assertTrue(redraw.clockRunning());

        moving.set(false);
        pause(300);
        int after = frames.get();
        assertFalse(redraw.clockRunning(), "the clock stops by itself");
        assertTrue(after >= whileMoving + 1, "one frame after it stopped, so the last state is shown");
        pause(300);
        assertEquals(after, frames.get(), "and then nothing, until something changes");
    }
}
