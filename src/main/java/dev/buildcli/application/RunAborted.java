package dev.buildcli.application;

public final class RunAborted extends RuntimeException {
    public RunAborted(String message) {
        super(message);
    }
}
