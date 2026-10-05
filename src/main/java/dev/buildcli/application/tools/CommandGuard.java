package dev.buildcli.application.tools;

import java.util.List;

/**
 * Extra checks on a command's argv before it is started.
 *
 * <p>On Windows, tools such as {@code mvn} are {@code .cmd} batch files, and Java starts those through {@code cmd.exe},
 * which interprets {@code & | < > ^ %} and quotes inside the arguments. An allow-listed {@code ["mvn", "test"]} could then
 * be turned into a different command by a crafted argument. Arguments containing those characters are refused there.
 * This is defence in depth: some JDK releases also reject such arguments, and nothing here relies on that.
 */
public final class CommandGuard {
    private static final String CMD_SPECIAL = "&|<>^%\"\r\n";

    private CommandGuard() {}

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /** @return why the argv must be refused, or null if it is acceptable on the given platform */
    public static String refusal(List<String> argv, boolean windows) {
        for (String arg : argv) {
            if (arg.indexOf('\0') >= 0) {
                return "an argument contains a NUL character";
            }
            if (windows) {
                for (char c : CMD_SPECIAL.toCharArray()) {
                    if (arg.indexOf(c) >= 0) {
                        return "an argument contains a character that cmd.exe treats specially (& | < > ^ % \" or a line break),"
                                + " which is refused on Windows because tools like mvn run through cmd.exe";
                    }
                }
            }
        }
        return null;
    }
}
