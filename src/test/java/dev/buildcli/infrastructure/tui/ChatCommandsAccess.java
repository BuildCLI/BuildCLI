package dev.buildcli.infrastructure.tui;

import java.util.ArrayList;
import java.util.List;

/** Lets a test in another package read the descriptions of the slash commands. */
public final class ChatCommandsAccess {
    private ChatCommandsAccess() { }

    public static List<String> commandDescriptions() {
        List<String> out = new ArrayList<>();
        ChatCommands.LIST.forEach(c -> out.add(c.description()));
        AgentFather.COMMANDS.forEach(c -> out.add(c.description()));
        return out;
    }
}
