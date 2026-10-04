package dev.buildcli.ports;

import dev.buildcli.domain.Agent;
import java.util.List;
import java.util.Optional;

/** Read-only access to the agent definitions and to the project context (AGENTS.md). */
public interface ConfigRepository {
    List<Agent> agents();

    Optional<Agent> agent(String name);

    /**
     * A digest of the project's own agent files ({@code .buildcli/}), or an empty string if it has none.
     * It changes whenever any of them changes; it is what the user approves in the trust prompt.
     */
    String projectDigest();

    /** The project's AGENTS.md (possibly truncated), or an empty string. It is context, never configuration. */
    String projectContext();
}
