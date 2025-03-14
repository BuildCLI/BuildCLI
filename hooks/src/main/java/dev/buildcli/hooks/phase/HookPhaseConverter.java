package dev.buildcli.hooks.phase;

import picocli.CommandLine;

public class HookPhaseConverter implements CommandLine.ITypeConverter<HookPhase> {
    @Override
    public HookPhase convert(String value) {
        return switch (value.toLowerCase()) {
            case "after","a" -> HookPhase.AFTER;
            case "b", "before" -> HookPhase.BEFORE;
            default -> throw new IllegalArgumentException("Invalid phase. Use 'before', 'after', 'b', or 'a'");
        };
    }
}