package dev.buildcli.hooks;

public record Hook(String command, HookPhase phase, String hookCommand) {
}
