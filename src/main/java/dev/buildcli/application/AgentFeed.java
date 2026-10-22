package dev.buildcli.application;

import dev.buildcli.application.ChatSession.Kind;
import dev.buildcli.application.ChatSession.Live;
import dev.buildcli.application.ChatSession.Message;
import dev.buildcli.application.ChatSession.State;
import dev.buildcli.domain.Event;
import dev.buildcli.domain.Task;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the orchestrator reports while agents work, turned into what a chat shows: the text an agent is still writing, one
 * line per tool it uses, a message when it hands work to a teammate, its status ("thinking", "working") and the token counts.
 * Its methods run on the agents' threads.
 */
final class AgentFeed {
    private static final int MAX_EVENTS = 300;
    private static final Pattern PATH_ARG = Pattern.compile("path=([^,}]+)");
    private static final Pattern ARGV_ARG = Pattern.compile("argv=\\[([^\\]]*)\\]");
    private static final Pattern TO_ARG = Pattern.compile("to=([^,}]+)");

    private final MessageStore transcript;
    /** What each agent is doing, the way a person's status reads. */
    private final Map<String, String> state;
    private final Runnable changed;
    /** The chat an agent works for right now (callbacks on other threads ask by agent), and the chat of the current thread. */
    private final Function<String, String> threadOf;
    private final Supplier<String> threadNow;
    /** Writes a note in the chat the current thread works for. */
    private final Consumer<String> system;

    /** Text each agent is still writing, and where. Guarded by itself. */
    private final Map<String, StringBuilder> live = new LinkedHashMap<>();
    private final Map<String, String> liveThread = new HashMap<>();
    private final List<Event> events = new ArrayList<>();
    private final AtomicInteger inputTokens = new AtomicInteger();
    private final AtomicInteger outputTokens = new AtomicInteger();

    AgentFeed(MessageStore transcript, Map<String, String> state, Runnable changed, Function<String, String> threadOf,
            Supplier<String> threadNow, Consumer<String> system) {
        this.transcript = transcript;
        this.state = state;
        this.changed = changed;
        this.threadOf = threadOf;
        this.threadNow = threadNow;
        this.system = system;
    }

    // ---- what the front end reads ----

    /** What an agent is writing in {@code thread} right now, or null. */
    Live live(String thread) {
        synchronized (live) {
            for (var e : live.entrySet()) {
                if (!e.getValue().isEmpty() && thread.equals(liveThread.get(e.getKey()))) {
                    return new Live(e.getKey(), e.getValue().toString(), thread);
                }
            }
        }
        return null;
    }

    List<Event> events() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }

    int inputTokens() {
        return inputTokens.get();
    }

    int outputTokens() {
        return outputTokens.get();
    }

    // ---- what the orchestrator reports ----

    void onTaskChanged(Task t) {
        Run run = Run.CURRENT.get();
        if (run == null) {
            return;
        }
        Task copy = new Task(t.id, t.parentId, t.from, t.to, t.objective, t.brief);
        copy.status = t.status;
        copy.tokens(t.tokens());
        copy.attempts(t.attempts());
        synchronized (run.tasks) {
            run.tasks.put(t.id, copy);
        }
    }

    void onText(String agent, String delta) {
        changed.run();
        synchronized (live) {
            live.computeIfAbsent(agent, k -> new StringBuilder()).append(delta);
            liveThread.put(agent, threadOf.apply(agent));
        }
        state.put(agent, "typing");
    }

    void onEvent(Event e) {
        changed.run();
        synchronized (events) {
            events.add(e);
            if (events.size() > MAX_EVENTS) {
                events.remove(0);
            }
        }
        switch (e.type()) {
            case "AgentInvoked" -> {
                inputTokens.addAndGet(e.inputTokens());
                outputTokens.addAndGet(e.outputTokens());
                state.put(e.agent(), "thinking");
            }
            case "AgentReplied", "TaskFailed", "TaskCompleted", "LimitReached", "TaskEscalated" -> flushLive(e.agent());
            default -> { }
        }
        switch (e.type()) {
            case "ToolCalled" -> {
                flushLive(e.agent());
                state.put(e.agent(), "working");
                if (!e.payload().startsWith("handoff")) {
                    transcript.add(new Message(transcript.nextId(), Kind.ACTIVITY, e.agent(), describeTool(e.payload()), Instant.now(),
                            State.RUNNING, List.of(), threadOf.apply(e.agent())));
                }
            }
            case "ToolCompleted" -> transcript.completeActivity(e.agent(), e.payload());
            case "TaskCreated" -> handoffStarted(e);
            case "WaitingForWorkspace" -> state.put(e.agent(), "waiting for the workspace");
            case "TaskFailed" -> system.accept(e.agent() + " had a problem: " + e.payload());
            case "TaskRetried" -> system.accept(e.agent() + " is trying again (" + e.payload() + ").");
            case "TaskEscalated" -> state.put(e.agent(), "needs you");
            case "TaskSkipped" -> system.accept(e.agent() + "'s task was closed by you.");
            default -> { }
        }
    }

    private void handoffStarted(Event e) {
        Run run = Run.CURRENT.get();
        Task t = null;
        if (run != null) {
            synchronized (run.tasks) {
                t = run.tasks.get(e.taskId());
            }
        }
        if (t == null || t.from.equals("user")) {
            return;
        }
        run.handedOffTo.add(t.to);
        // a handoff reads like one teammate writing to another in the chat
        transcript.add(new Message(transcript.nextId(), Kind.AGENT, t.from, "@" + t.to + " " + t.objective
                + (t.brief == null || t.brief.isBlank() ? "" : "\n" + t.brief), Instant.now(), State.NONE, List.of(), threadNow.get()));
    }

    // ---- text an agent was writing becomes a message ----

    void flushLive(String agent) {
        String text;
        String thread;
        synchronized (live) {
            StringBuilder sb = live.get(agent);
            if (sb == null || sb.isEmpty()) {
                return;
            }
            text = sb.toString().strip();
            thread = liveThread.getOrDefault(agent, threadNow.get());
            sb.setLength(0);
        }
        if (!text.isEmpty()) {
            transcript.add(new Message(transcript.nextId(), Kind.AGENT, agent, text, Instant.now(), State.NONE, List.of(), thread));
        }
    }

    void flushAll() {
        List<String> agents;
        synchronized (live) {
            agents = new ArrayList<>(live.keySet());
        }
        agents.forEach(this::flushLive);
    }

    /** A short human line for "write_file {path=x, content=...}" without dumping the content. */
    static String describeTool(String payload) {
        int sp = payload.indexOf(' ');
        String name = sp < 0 ? payload : payload.substring(0, sp);
        String args = sp < 0 ? "" : payload.substring(sp + 1);
        Matcher argv = ARGV_ARG.matcher(args);
        if (argv.find()) {
            return "ran " + argv.group(1).replace(",", "");
        }
        Matcher to = TO_ARG.matcher(args);
        if (name.equals("send_message") && to.find()) {
            return Orchestrator.isPerson(to.group(1).strip()) ? "wrote to you" : "wrote to " + to.group(1).strip();
        }
        if (name.equals("ask_user")) {
            return "asked you a question";
        }
        Matcher path = PATH_ARG.matcher(args);
        if (path.find()) {
            String verb = switch (name) {
                case "write_file" -> "wrote";
                case "read_file" -> "read";
                case "list_files" -> "listed";
                default -> name;
            };
            return verb + " " + path.group(1).strip();
        }
        return name + (args.isBlank() ? "" : " " + ToolRuntime.abbreviate(args, 120));
    }
}
