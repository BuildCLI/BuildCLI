package dev.buildcli.cli.commands.bug;

import picocli.CommandLine;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "bug",
        description = "Opens a GitHub issue template pre-filled with system and version info, helping you report a bug in BuildCLI.",
        mixinStandardHelpOptions = true
)
public class BugCommand implements Callable<Integer> {

    private static final String ISSUE_URL = "https://github.com/BuildCLI/BuildCLI/issues/new?body=";

    @Override
    public Integer call() throws Exception {
        String buildCliVersion = getBuildCliVersion();
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        String javaVersion = System.getProperty("java.version");

        String body = """
                ### Describe the bug

                <!-- A clear and concise description of what the bug is. -->

                ### Steps to Reproduce

                <!-- Steps to reproduce the behavior: -->
                1. Go to '...'
                2. Run command '...'
                3. See error

                ### Expected behavior

                <!-- A clear and concise description of what you expected to happen. -->

                ### Environment
                - BuildCLI Version: %s
                - OS: %s
                - Architecture: %s
                - Java Version: %s
                """.formatted(buildCliVersion, os, arch, javaVersion);

        String encodedBody = URLEncoder.encode(body, StandardCharsets.UTF_8);
        String fullUrl = ISSUE_URL + encodedBody;

        openBrowser(fullUrl);

        System.out.println("Opened GitHub issue template in your browser. If it didn’t open, please visit:");
        System.out.println(fullUrl);

        return 0;
    }

    private String getBuildCliVersion() {
        // TODO: Retrieve actual BuildCLI version from manifest or config file
        return "1.0.0"; // Placeholder version
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                System.err.println("Desktop browsing not supported. Please open the following URL manually:");
                System.err.println(url);
            }
        } catch (Exception e) {
            System.err.println("Failed to open browser: " + e.getMessage());
        }
    }
}
