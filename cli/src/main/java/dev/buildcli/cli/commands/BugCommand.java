package dev.buildcli.cli.commands;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.JavaUtils;
import dev.buildcli.core.utils.OS;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;

@Command(
        name = "bug",
        description = "Opens a GitHub issue template pre-filled with system and version info, helping you report a bug in BuildCLI.",
        mixinStandardHelpOptions = true
)
public class BugCommand implements BuildCLICommand {
    private static final Logger logger = LoggerFactory.getLogger(BugCommand.class);
    private static final String ISSUE_URL = "https://github.com/BuildCLI/BuildCLI/issues/new?body=";

    @Override
    public void run() {
        String buildCliVersion = getBuildCliVersion();
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
                """.formatted(buildCliVersion, OS.getOSName(), OS.getArchitecture(), JavaUtils.getJavaVersion());

        String encodedBody = URLEncoder.encode(body, StandardCharsets.UTF_8);
        String fullUrl = ISSUE_URL + encodedBody;

        openBrowser(fullUrl);

       logger.info("Opened GitHub issue template in your browser. If it didn’t open, please visit:");
       logger.info(fullUrl);
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
                logger.warn("Desktop browsing not supported. Please open the following URL manually:");
                logger.warn(url);
            }
        } catch (Exception e) {
            logger.error("Failed to open browser: {}", e.getMessage(), e);
        }
    }
}
