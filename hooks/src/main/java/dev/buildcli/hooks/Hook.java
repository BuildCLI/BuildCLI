package dev.buildcli.hooks;

import dev.buildcli.hooks.phase.HookPhase;

public record Hook(String command, HookPhase phase, String hookCommand) {
}
