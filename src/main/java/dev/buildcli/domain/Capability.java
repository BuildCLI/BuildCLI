package dev.buildcli.domain;

import java.util.Set;

/**
 * Capabilities an agent may be granted. Agents ask for capabilities, never for tool names: the runtime resolves
 * which tool (built-in, later connector or MCP) provides each one.
 */
public final class Capability {
    public static final String FILESYSTEM_READ = "filesystem.read";
    public static final String FILESYSTEM_WRITE = "filesystem.write";
    public static final String SEARCH = "search";
    public static final String GIT_READ = "git.read";
    public static final String GIT_COMMIT = "git.commit";
    public static final String COMMAND_EXECUTE = "command.execute";
    public static final String AGENT_HANDOFF = "agent.handoff";
    /** Write in a group, or privately to a teammate, when the user asks. */
    public static final String CHAT_POST = "chat.post";

    /** Every capability defined in 1.0. Unknown names in an agent file are rejected. */
    public static final Set<String> KNOWN = Set.of(
            FILESYSTEM_READ, FILESYSTEM_WRITE, SEARCH, GIT_READ, GIT_COMMIT, COMMAND_EXECUTE, AGENT_HANDOFF, CHAT_POST);

    private Capability() {}
}
