package dev.buildcli;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.Events;
import dev.buildcli.application.Orchestrator;
import dev.buildcli.application.ToolRuntime;
import dev.buildcli.application.tools.WorkspaceLock;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Event;
import dev.buildcli.domain.Limits;
import dev.buildcli.domain.Permissions;
import dev.buildcli.infrastructure.StateStore;
import dev.buildcli.ports.ChatLog;
import dev.buildcli.ports.LlmGateway;
import dev.buildcli.ports.LlmMessage;
import dev.buildcli.ports.LlmReply;
import dev.buildcli.ports.ToolSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * A hand-run load test (not a unit test): {@code java -cp ... dev.buildcli.LoadProbe events|chat [args]}.
 * "events W N virtual|platform ENGINE" hammers the event log; "chat A M LATENCY_MS ENGINE" with ENGINE one of sqlite, sqlite-batched, h2,
 * h2-batched, memory-batched, none (no history); "chat" runs many agents through the real session, orchestrator and SQLite with a fake
 * model that takes a fixed time to answer, so what is measured is BuildCLI's own overhead.
 */
public final class LoadProbe {
    private LoadProbe() {}

    public static void main(String[] args) throws Exception {
        String what = args.length > 0 ? args[0] : "chat";
        Path dir = Files.createTempDirectory("loadprobe");
        try {
            if (what.equals("events")) {
                events(dir, Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3], args[4]);
            } else {
                chat(dir, Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), args[4]);
            }
        } finally {
            try (var s = Files.walk(dir)) {
                s.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    static String mb() {
        System.gc();
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / (1024 * 1024) + " MB heap";
    }

    /** {@code writers} concurrent writers append {@code perWriter} events each into the SQLite event log. */
    static StateStore open(Path dir, String engine) {
        boolean batched = engine.endsWith("-batched");
        return switch (engine.replace("-batched", "")) {
            case "sqlite" -> StateStore.open(dir.resolve("state.db"), StateStore.Backend.SQLITE, batched);
            case "h2" -> StateStore.open(dir.resolve("state.db"), StateStore.Backend.H2, batched);
            case "memory" -> StateStore.open(dir.resolve("state.db"), StateStore.Backend.MEMORY, batched);
            default -> StateStore.open(dir.resolve("state.db"), StateStore.Backend.SQLITE, false);
        };
    }

    static void events(Path dir, int writers, int perWriter, String kind, String engine) throws Exception {
        try (StateStore store = open(dir, engine)) {
            var done = new CountDownLatch(writers);
            long t0 = System.nanoTime();
            var pool = kind.equals("virtual") ? Executors.newVirtualThreadPerTaskExecutor() : Executors.newFixedThreadPool(writers);
            for (int w = 0; w < writers; w++) {
                int id = w;
                pool.execute(() -> {
                    for (int i = 0; i < perWriter; i++) {
                        store.append("run" + id, new Event(Instant.now(), "ToolCalled", i, "agent" + id, "read_file {path=src/Main.java}"));
                    }
                    done.countDown();
                });
            }
            done.await();
            store.list("nothing"); // a read waits for every queued write, so the time includes getting it all stored
            double s = (System.nanoTime() - t0) / 1e9;
            long total = (long) writers * perWriter;
            System.out.printf("events (%s): %d writers (%s) x %d = %d events in %.2f s = %,.0f events/s%n", engine, writers, kind, perWriter, total, s, total / s);
            pool.shutdown();
        }
    }

    static void chat(Path dir, int agents, int perAgent, int latencyMs, String engine) throws Exception {
        StateStore store = open(dir, engine);
        ChatLog log = engine.equals("none") ? ChatLog.NONE : store;
        List<Agent> team = new ArrayList<>();
        for (int i = 0; i < agents; i++) {
            team.add(new Agent("a" + i, "dev", "", Set.of(), Permissions.none()));
        }
        AtomicLong modelCalls = new AtomicLong();
        LlmGateway model = new LlmGateway() {
            @Override
            public LlmReply chat(Agent agent, List<LlmMessage> messages, List<ToolSpec> tools) {
                modelCalls.incrementAndGet();
                sleep(latencyMs);
                return new LlmReply("Done by " + agent.name() + ". " + "word ".repeat(40), List.of(), 120, 60);
            }

            @Override
            public LlmReply chatStreaming(Agent agent, List<LlmMessage> messages, List<ToolSpec> tools, Consumer<String> onText) {
                modelCalls.incrementAndGet();
                for (int i = 0; i < 10; i++) { // ten chunks, like a streamed answer
                    sleep(latencyMs / 10);
                    onText.accept("chunk " + i + " ");
                }
                return new LlmReply("Done by " + agent.name() + ". " + "word ".repeat(40), List.of(), 120, 60);
            }
        };
        var lock = new WorkspaceLock();
        var session = new ChatSession(team, List.of(), Limits.defaults(), (t, request, ui, cancelled, dispatcher) -> {
            Events events = new Events(store, "r" + System.nanoTime(), ui);
            var o = new Orchestrator(t, model, new ToolRuntime(dir, ui, events, lock), ui, events);
            o.cancelWhen(cancelled);
            o.dispatchWith(dispatcher);
            return o.run(request);
        }, dev.buildcli.ports.ChatStore.NONE, () -> 6, log);

        String before = mb();
        long t0 = System.nanoTime();
        for (int k = 0; k < perAgent; k++) {
            for (int i = 0; i < agents; i++) {
                session.submit("task " + k + " for a" + i, List.of(), "a" + i);
            }
        }
        long submitted = System.nanoTime();
        // what a screen does while this is going on: ask for the version and the messages, many times
        List<Long> frame = new ArrayList<>();
        while (session.busy() || session.queued() > 0) {
            long f = System.nanoTime();
            session.version();
            int n = session.messages().size();
            frame.add(System.nanoTime() - f + (n < 0 ? 1 : 0));
            Thread.sleep(16);
        }
        double wall = (System.nanoTime() - t0) / 1e9;
        double ideal = perAgent * latencyMs / 1000.0;
        long msgs = (long) agents * perAgent;
        frame.sort(null);
        System.out.printf("chat: %d agents x %d messages (model %d ms, state %s): %d answers in %.2f s (ideal %.2f s, overhead x%.2f) = %,.0f answers/s; "
                        + "submitting took %.0f ms%n", agents, perAgent, latencyMs, engine, msgs, wall, ideal, wall / ideal, msgs / wall,
                (submitted - t0) / 1e6);
        if (!frame.isEmpty()) {
            System.out.printf("  a screen asking every 16 ms: median %.2f ms, p99 %.2f ms, max %.2f ms (%d samples)%n", frame.get(frame.size() / 2) / 1e6,
                    frame.get((int) (frame.size() * 0.99)) / 1e6, frame.get(frame.size() - 1) / 1e6, frame.size());
        }
        System.out.printf("  model calls %d; heap %s -> %s; messages kept in memory %d; ok=%d%n", modelCalls.get(), before, mb(), session.messages().size(),
                session.messages().stream().filter(m -> m.state() == ChatSession.State.DONE).count());
        session.messages().stream().filter(m -> m.kind() == ChatSession.Kind.ERROR || m.kind() == ChatSession.Kind.SYSTEM).limit(2)
                .forEach(m -> System.out.println("  note: " + m.text()));
        session.close();
        store.close();
    }

    static void sleep(int ms) {
        try {
            Thread.sleep(Math.max(1, ms));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
