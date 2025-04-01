package dev.buildcli.cli.utilsfortest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.extension.*;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LogbackExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private TestAppender testAppender;

    @Override
    public void beforeEach(ExtensionContext context) {
        // Get the @LogbackLogger annotation from the test class
        LogbackLogger annotation = context.getRequiredTestClass().getAnnotation(LogbackLogger.class);

        if (annotation == null) {
            throw new IllegalStateException("The annotation @LogbackLogger is required.");
        }

        Class<?> loggerClass = annotation.value();

        testAppender = new TestAppender();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        testAppender.setContext(loggerContext);
        testAppender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
        logger.addAppender(testAppender);

    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (testAppender != null) {
            testAppender.stop();
            Logger logger = (Logger) LoggerFactory.getLogger(context.getRequiredTestClass());
            logger.detachAndStopAllAppenders();
        }
    }

    public List<ILoggingEvent> getLogs() {
        return testAppender.list;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(List.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return getLogs();
    }
}
