package dev.buildcli.ports;

import dev.buildcli.domain.Event;
import java.util.List;

public interface EventStore {
    void append(String runId, Event event);

    List<Event> list(String runId);
}
