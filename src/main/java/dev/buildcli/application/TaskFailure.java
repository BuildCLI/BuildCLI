package dev.buildcli.application;

/** A retryable failure of one task attempt. */
class TaskFailure extends RuntimeException {
    private final boolean tellModel;

    /** @param tellModel whether the model should be told about this failure on the next attempt */
    TaskFailure(String message, boolean tellModel) {
        super(message);
        this.tellModel = tellModel;
    }

    TaskFailure(String message) {
        this(message, true);
    }

    boolean tellModel() {
        return tellModel;
    }
}

/** Token budget exhausted: escalates immediately, no retries. */
final class TokenBudgetExceeded extends TaskFailure {
    TokenBudgetExceeded(String message) {
        super(message, false);
    }
}
