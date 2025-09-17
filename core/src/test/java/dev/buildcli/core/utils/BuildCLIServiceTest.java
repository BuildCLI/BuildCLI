package dev.buildcli.core.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.domain.git.GitCommandExecutor;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.core.utils.console.input.InteractiveInputUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import utilsfortest.TestAppender;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static dev.buildcli.core.utils.BeautifyShell.content;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class BuildCLIServiceTest {

    @Mock
    private GitCommandExecutor gitExec;

    private ByteArrayOutputStream outContent;
    private BuildCLIService buildCLIService;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        buildCLIService = new BuildCLIService(gitExec, new PrintStream(outContent));
    }

    @Test
    void testWelcome_ShouldPrintOfficialBanner_WhenBannerIsEnabledAndNoPathIsConfigured() {
        BuildCLIConfig config = BuildCLIConfig.empty();
        config.addOrSetProperty("buildcli.logging.banner.enabled", "true");

        try (MockedStatic<ConfigContextLoader> mockedLoader = Mockito.mockStatic(ConfigContextLoader.class)) {
            mockedLoader.when(ConfigContextLoader::getAllConfigs).thenReturn(config);

            buildCLIService.welcome();
        }

        String lineSeparator = System.lineSeparator();
        String styledLine = String.format("|  .-.  \\|  ||  |,--.|  |' .-. ||  |    |  |   |  |       %s%n", content("Built by the community, for the community").blueFg().italic());

        String expectedOutput = ",-----.          ,--.,--.   ,--. ,-----.,--.   ,--." + lineSeparator +
                "|  |) /_ ,--.,--.`--'|  | ,-|  |'  .--./|  |   |  |" + lineSeparator +
                styledLine.trim() + lineSeparator + // Use the formatted string and trim trailing newline from printf
                "|  '--' /'  ''  '|  ||  |\\ `-' |'  '--'\\|  '--.|  |" + lineSeparator +
                "`------'  `----' `--'`--' `---'  `-----'`-----'`--'" + lineSeparator +
                lineSeparator;

        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    void checkUpdates_shouldDoNothing_whenAlreadyUpToDate() {
        when(gitExec.findGitRepository(anyString())).thenReturn("/fake/path/to/repo");
        when(gitExec.checkIfLocalRepositoryIsUpdated(anyString(), anyString())).thenReturn(true);

        buildCLIService.checkUpdatesBuildCLIAndUpdate();

        assertEquals("", outContent.toString());
    }

    @Test
    void checkUpdates_shouldShowOutdatedMessage_whenUpdateAvailable() {
        Logger logger = (Logger) LoggerFactory.getLogger("BuildCLI");
        TestAppender testAppender = new TestAppender();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        testAppender.setContext(context);
        testAppender.start();
        logger.addAppender(testAppender);

        try {
            when(gitExec.findGitRepository(anyString())).thenReturn("/fake/path/to/repo");
            when(gitExec.checkIfLocalRepositoryIsUpdated(anyString(), anyString())).thenReturn(false);

            try (MockedStatic<InteractiveInputUtils> mockedInput = Mockito.mockStatic(InteractiveInputUtils.class)) {
                mockedInput.when(() -> InteractiveInputUtils.confirm(anyString())).thenReturn(false);

                buildCLIService.checkUpdatesBuildCLIAndUpdate();
            }

            List<ILoggingEvent> logs = testAppender.list;
            String expectedOutdatedMessage = "ATTENTION: Your BuildCLI is outdated!";
            boolean outdatedFound = logs.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(msg -> msg.contains(expectedOutdatedMessage));

            assertTrue(outdatedFound, "The 'outdated' message was not logged.");

        } finally {
            logger.detachAppender(testAppender);
            testAppender.stop();
        }
    }
}