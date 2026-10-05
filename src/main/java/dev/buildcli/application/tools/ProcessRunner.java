package dev.buildcli.application.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs a command with a scrubbed environment, a timeout and a bounded output buffer. Output is drained while the process
 * runs (a process that prints more than the pipe buffer must not deadlock) and on timeout the whole process tree dies.
 */
final class ProcessRunner {
    private static final int MAX_CAPTURE_BYTES = 1_000_000;

    record Result(int exitCode, String output, boolean timedOut) {}

    private ProcessRunner() {}

    static Result run(List<String> argv, Path dir, Duration timeout, Map<String, String> extraEnv)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(argv).directory(dir.toFile()).redirectErrorStream(true);
        pb.environment().clear();
        pb.environment().putAll(SafeEnvironment.filter(System.getenv()));
        pb.environment().putAll(extraEnv);
        Process process = pb.start();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        // a platform thread: reading a process's pipe blocks in native code, which would pin a virtual thread's carrier
        Thread drain = Thread.ofPlatform().daemon().start(() -> {
            try (InputStream in = process.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    int room = MAX_CAPTURE_BYTES - captured.size();
                    if (room > 0) {
                        captured.write(buf, 0, Math.min(n, room));
                    }
                }
            } catch (IOException ignored) {
                // the process was killed; whatever was captured so far is kept
            }
        });
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
        drain.join(TimeUnit.SECONDS.toMillis(5));
        String out = captured.toString(StandardCharsets.UTF_8);
        return new Result(finished ? process.exitValue() : -1, out, !finished);
    }
}
