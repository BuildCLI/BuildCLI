package dev.buildcli.application.tools;

import dev.buildcli.domain.Agent;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;

/**
 * A capability an agent can use. The agent only ever sees the {@link ToolSpec}; where the implementation comes from
 * (built-in today, connectors and MCP later) is the registry's business.
 */
public interface Tool {
    String name();

    /** The capability an agent must hold to use this tool. */
    String capability();

    ToolSpec spec();

    /**
     * Whether the output carries content from outside the runtime (file text, command output, repository data).
     * Such output is delimited as untrusted data before it reaches the model.
     */
    boolean returnsExternalContent();

    /**
     * Runs the tool. Expected refusals are returned as text starting with {@code DENIED} or {@code ERROR} so the model
     * can adapt; unexpected problems may throw and are reported as errors by the runtime.
     */
    String execute(ToolContext context, Agent agent, ToolCall call) throws Exception;
}
