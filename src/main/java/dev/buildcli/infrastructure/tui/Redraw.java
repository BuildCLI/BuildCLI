package dev.buildcli.infrastructure.tui;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Draws the screen when something changes, instead of looking for changes on a timer. Whoever changes something calls
 * {@link #request()} from any thread; requests that arrive together become one frame, and frames are at least
 * {@link #MIN_GAP_MS} apart so a model streaming hundreds of tokens a second cannot make the terminal work harder than
 * the eye can follow. The only clock left is for things that move by themselves (a spinner, a toast about to expire):
 * it runs only while something moves, and draws one frame more when it stops.
 */
final class Redraw {
    static final long MIN_GAP_MS = 16;
    static final long ANIMATION_MS = 80;

    private final Runnable frame;
    private final ScheduledExecutorService scheduler;
    private final BooleanSupplier moving;
    private final AtomicBoolean queued = new AtomicBoolean();
    private final AtomicBoolean clock = new AtomicBoolean();
    private volatile long lastFrame;
    private volatile boolean wasMoving;

    /**
     * @param frame posts one frame to the thread that draws; must be cheap and safe to call from any thread
     * @param moving whether something on screen changes by itself
     */
    Redraw(Runnable frame, ScheduledExecutorService scheduler, BooleanSupplier moving) {
        this.frame = frame;
        this.scheduler = scheduler;
        this.moving = moving;
    }

    /** Something visible changed: draw soon. Safe from any thread, and cheap when a frame is already on its way. */
    void request() {
        if (queued.compareAndSet(false, true)) {
            long wait = Math.max(0, MIN_GAP_MS - (System.currentTimeMillis() - lastFrame));
            scheduler.schedule(this::fire, wait, TimeUnit.MILLISECONDS);
        }
    }

    /** Called by the thread that draws, after every frame: starts the clock if something has begun to move. */
    void frameDrawn() {
        if (moving.getAsBoolean() && clock.compareAndSet(false, true)) {
            scheduler.schedule(this::tick, ANIMATION_MS, TimeUnit.MILLISECONDS);
        }
    }

    /** True while the clock runs; for tests and for the idle-cost check. */
    boolean clockRunning() {
        return clock.get();
    }

    private void fire() {
        queued.set(false);
        lastFrame = System.currentTimeMillis();
        frame.run();
    }

    private void tick() {
        request();
        boolean now = moving.getAsBoolean();
        if (now || wasMoving) {
            scheduler.schedule(this::tick, ANIMATION_MS, TimeUnit.MILLISECONDS);
        } else {
            clock.set(false);
        }
        wasMoving = now;
    }
}
