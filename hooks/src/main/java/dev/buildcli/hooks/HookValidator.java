package dev.buildcli.hooks;

import dev.buildcli.hooks.phase.HookPhase;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.Set;

public class HookValidator {
    private final Set<String[]> availableCommands;

    public HookValidator(CommandLine commandLine) {
        this.availableCommands = loadAvailableCommands(commandLine);
    }

    private Set<String[]> loadAvailableCommands(CommandLine commandLine) {
        Set<String[]> commands = new HashSet<>();
        collectCommands(commandLine, new String[]{}, commands);
        return commands;
    }

    private void collectCommands(CommandLine commandLine, String[] parent, Set<String[]> commands) {
        commandLine.getSubcommands().forEach((name, subcommand) -> {
            String[] fullCommand = new String[parent.length + 1];
            System.arraycopy(parent, 0, fullCommand, 0, parent.length);
            fullCommand[parent.length] = name;
            commands.add(fullCommand);
            collectCommands(subcommand, fullCommand, commands);
        });
    }

    public boolean isValidCommand(String commandName) {
        String[] commandParts = commandName.split(" ");
        for (String[] command : availableCommands) {
            if (java.util.Arrays.equals(command, commandParts)) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidPhase(String phase) {
        try {
            HookPhase.valueOf(phase.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isHookAlreadyRegistered(Set<Hook> hooks, Hook hook) {
        return hooks.stream().anyMatch(h -> h.command().equals(hook.command()) && h.phase() == hook.phase());
    }
}