package dev.buildcli.domain;

/** A provider and a model, e.g. ollama / qwen3-coder. Model choice is runtime configuration, not part of an agent. */
public record ModelRef(String provider, String model) {}
