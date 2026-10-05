package dev.buildcli.ports;

import java.util.List;

public record ToolSpec(String name, String description, List<Param> params) {
    public record Param(String name, String description, boolean array, boolean required) {}
}
