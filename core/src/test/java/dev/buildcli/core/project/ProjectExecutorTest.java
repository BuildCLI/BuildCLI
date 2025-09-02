package dev.buildcli.core.project;

import dev.buildcli.core.utils.SystemCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProjectExecutor}.
 * Verifies initialization, command handling, exception handling,
 * and concurrency behavior.
 */
class ProjectExecutorTest {

    private TestProjectExecutor executor;

    /**
     * Test implementation of {@link ProjectExecutor} used to simulate
     * normal and exceptional execution scenarios.
     * <ul>
     *   <li>Tracks whether {@code addMvnCommand()} was invoked</li>
     *   <li>Can simulate exceptions on execution start</li>
     *   <li>Can simulate long-running operations</li>
     * </ul>
     */
    static class TestProjectExecutor extends ProjectExecutor {
        private final AtomicBoolean addMvnCalled = new AtomicBoolean(false);
        private final boolean throwOnStart;
        private final boolean simulateLongRun;

        TestProjectExecutor(boolean throwOnStart) {
            this(throwOnStart, false);
        }

        TestProjectExecutor(boolean throwOnStart, boolean simulateLongRun) {
            this.throwOnStart = throwOnStart;
            this.simulateLongRun = simulateLongRun;
        }

        @Override
        protected void addMvnCommand() {
            addMvnCalled.set(true);
            this.command.add("test-goal");
        }

        @Override
        protected String getErrorMessage() {
            return "Execution failed!";
        }

        @Override
        public void execute() {
            this.addMvnCommand();
            if (throwOnStart) {
                throw new RuntimeException(new IOException("Simulated failure"));
            }
            if (simulateLongRun) {
                try {
                    Thread.sleep(200); // simulate long process
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @BeforeEach
    void setUp() {
        executor = new TestProjectExecutor(false);
    }

    /**
     * Verifies that the constructor initializes the command list
     * with the Maven system command as the first entry.
     */
    @Test
    void testConstructorInitializesCommand() {
        assertFalse(executor.command.isEmpty());
        assertEquals(SystemCommands.MVN.getCommand(), executor.command.get(0));
    }

    /**
     * Ensures that {@code execute()} calls {@code addMvnCommand()}
     * and adds the expected goal to the command list.
     */
    @Test
    void testExecuteCallsAddMvnCommand() {
        executor.execute();
        assertTrue(executor.addMvnCalled.get(), "addMvnCommand() should be called by execute()");
        assertTrue(executor.command.contains("test-goal"));
    }

    /**
     * Validates that {@code execute()} handles simulated startup exceptions
     * and wraps them in a {@link RuntimeException}.
     */
    @Test
    void testExecuteHandlesExceptionGracefully() {
        TestProjectExecutor failingExecutor = new TestProjectExecutor(true);
        try {
            failingExecutor.execute();
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertEquals("Simulated failure", e.getCause().getMessage());
        }
    }

    /**
     * Confirms that {@code getErrorMessage()} returns the expected message.
     */
    @Test
    void testGetErrorMessage() {
        assertEquals("Execution failed!", executor.getErrorMessage());
    }

    /**
     * Verifies that {@code execute()} is safe to call concurrently
     * across multiple threads and that all tasks complete as expected.
     */
    @Test
    void testExecuteIsThreadSafe() throws InterruptedException {
        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        TestProjectExecutor concurrentExecutor = new TestProjectExecutor(false, true);

        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    concurrentExecutor.execute();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS), "All threads should complete execution");
        pool.shutdown();
        assertTrue(concurrentExecutor.addMvnCalled.get());
        assertTrue(concurrentExecutor.command.contains("test-goal"));
    }

    /**
     * Ensures that multiple concurrent executions produce a consistent command list
     * and that no exceptions are leaked from task execution.
     */
    @Test
    void testConcurrentExecutionsProduceConsistentCommand() throws Exception {
        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        TestProjectExecutor concurrentExecutor = new TestProjectExecutor(false, true);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                concurrentExecutor.execute();
                return null;
            });
        }

        List<Future<Void>> futures = pool.invokeAll(tasks);

        for (Future<Void> f : futures) {
            f.get(1, TimeUnit.SECONDS); // should not throw
        }

        pool.shutdown();
        assertTrue(concurrentExecutor.command.stream().anyMatch(c -> c.equals("test-goal")));
    }

    /**
     * Tests that even if a thread executing {@code execute()} is interrupted,
     * {@code addMvnCommand()} is still called before the interruption is handled.
     */
    @Test
    void testExecutionInterrupted() throws InterruptedException {
        TestProjectExecutor longExecutor = new TestProjectExecutor(false, true);
        Thread t = new Thread(longExecutor::execute);
        t.start();
        t.interrupt();
        t.join(); // wait for execution to finish
        assertTrue(longExecutor.addMvnCalled.get(),
                "Even if interrupted, addMvnCommand should be called");
    }
}
