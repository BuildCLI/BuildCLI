package dev.buildcli.core.model;

import java.util.List;

public record DockerComposeConfig(
        List<String> ports,
        List<String> volumes,
        String cpu,
        String memory,
        String dockerFilePath
) {
    public DockerComposeConfig{
        ports          = (ports == null || ports.isEmpty()) ? List.of("8080:8080") : List.copyOf(ports);
        volumes        = (volumes == null || volumes.isEmpty()) ? List.of() : List.copyOf(volumes);
        cpu            = (cpu == null || cpu.isBlank()) ? "1" : cpu;
        memory         = (memory == null || memory.isBlank()) ? "512m" : memory;
        dockerFilePath = (dockerFilePath == null || dockerFilePath.isBlank()) ? "./Dockerfile" : dockerFilePath;
    }
}
