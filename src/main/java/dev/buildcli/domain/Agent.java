package dev.buildcli.domain;

import java.util.Set;

public record Agent(String name, String role, String instructions, Set<String> capabilities, Permissions permissions,
                    Origin origin, String source) {

    public Agent(String name, String role, String instructions, Set<String> capabilities, Permissions permissions) {
        this(name, role, instructions, capabilities, permissions, Origin.BUILTIN, "");
    }

    public boolean can(String capability) {
        return capabilities.contains(capability);
    }
}
