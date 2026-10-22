package dev.buildcli.infrastructure.tui;

import dev.buildcli.domain.Capability;

/** What each capability lets an agent do, in the words of someone who has not read the docs. */
final class CapabilityInfo {
    private CapabilityInfo() {}

    static String describe(String capability) {
        return switch (capability) {
            case Capability.FILESYSTEM_READ -> "read the files in this folder";
            case Capability.FILESYSTEM_WRITE -> "create and change files (you approve each write)";
            case Capability.SEARCH -> "search the files";
            case Capability.GIT_READ -> "read git status, log and diffs";
            case Capability.GIT_COMMIT -> "commit to git (you approve)";
            case Capability.COMMAND_EXECUTE -> "run commands, such as tests (you approve)";
            case Capability.AGENT_HANDOFF -> "ask other agents for help";
            case Capability.CHAT_POST -> "write in a group or to an agent when you ask";
            default -> "";
        };
    }
}
