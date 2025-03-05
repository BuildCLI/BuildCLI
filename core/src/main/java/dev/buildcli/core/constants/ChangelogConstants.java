package dev.buildcli.core.constants;

import java.util.List;

public abstract class ChangelogConstants {
    private ChangelogConstants() {}
    public static final List<String> ORDERED_SECTIONS = List.of("Added", "Changed", "Fixed", "Removed");
}
