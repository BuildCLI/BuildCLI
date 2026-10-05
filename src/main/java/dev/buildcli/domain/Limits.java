package dev.buildcli.domain;

/** Run limits enforced by the runtime. */
public record Limits(int maxRetries, int maxSteps, int maxDepth, int maxTokensPerTask, int maxHandoffsPerAttempt) {

    public static Limits defaults() {
        return new Limits(3, 12, 3, 30_000, 3);
    }
}
