package dev.buildcli.core.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Utility class for managing the display of ASCII art in the BuildCLI.
 * This class encapsulates the logic for determining when ASCII art should be shown.
 */
public class AsciiArtManager {

    /**
     * Determines whether ASCII art should be displayed based on the provided command line arguments.
     *
     * @param args Command line arguments
     * @return true if ASCII art should be displayed, false otherwise
     */
    public static boolean shouldShowAsciiArt(String[] args) {
        if (args.length == 0) {
            return false;
        }

        if (Arrays.asList(args).contains("--help")) {
            return true;
        }

        Map<String, List<String>> commandAliases = Map.of(
                "p", List.of("p", "project"),
                "about", List.of("a", "about"),
                "help", List.of("help", "h")
        );

        String mainCommand = args[0];
        if (matchesCommand(mainCommand, commandAliases.get("p"))) {
            return args.length > 1 && (args[1].equals("run") || (args.length > 2 && args[1].equals("i") && args[2].equals("-n")));
        }

        if (matchesCommand(mainCommand, commandAliases.get("help"))) {
            return true;
        }

        return matchesCommand(mainCommand, commandAliases.get("about"));
    }

    /**
     * Checks if the input command matches any of the valid commands in the provided list.
     *
     * @param input Command to check
     * @param validCommands List of valid commands
     * @return true if the input matches any valid command, false otherwise
     */
    private static boolean matchesCommand(String input, List<String> validCommands) {
        return validCommands != null && validCommands.contains(input);
    }
}