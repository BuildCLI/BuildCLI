package dev.buildcli.cli.utilsfortest;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class TestAppender extends ListAppender<ILoggingEvent> {

    public TestAppender() {
        super();
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }
}
