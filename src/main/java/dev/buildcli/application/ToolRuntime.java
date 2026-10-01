package dev.buildcli.application;

import dev.buildcli.application.tools.GitCommitTool;
import dev.buildcli.application.tools.GitReadTool;
import dev.buildcli.application.tools.ListFilesTool;
import dev.buildcli.application.tools.ReadFileTool;
import dev.buildcli.application.tools.RunCommandTool;
import dev.buildcli.application.tools.SearchTool;
import dev.buildcli.application.tools.Tool;
import dev.buildcli.application.tools.ToolContext;
import dev.buildcli.application.tools.WorkspaceLock;
import dev.buildcli.application.tools.WriteFileTool;
import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Capability;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import dev.buildcli.ports.ToolSpec.Param;
import dev.buildcli.ports.UserInterface;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool registry and executor. Every call goes through the same gate: the tool must exist, the agent must hold its
 * capability, the tool applies its own policy checks, output is scrubbed of secrets, and output that carries outside
 * content is delimited as untrusted data. Expected refusals come back to the model as text, never as exceptions.
 */
public final class ToolRuntime {
    static final String OUTPUT_TAG = "tool-output";

    private final Path workspace;
    private final UserInterface ui;
    private final Events events;
    private final List<Tool> tools;
    private final WorkspaceLock lock;
    private List<Path> protectedPaths = List.of();

    /** Folders the tools must never touch, whatever an agent is allowed to read or write; see {@link ToolContext#protect}. */
    public ToolRuntime protect(List<Path> paths) {
        this.protectedPaths = List.copyOf(paths);
        return this;
    }

    public ToolRuntime(Path workspace, UserInterface ui, Events events) {
        this(workspace, ui, events, defaultTools(), new WorkspaceLock());
    }

    /** @param lock shared by every run working on the same workspace at the same time */
    public ToolRuntime(Path workspace, UserInterface ui, Events events, WorkspaceLock lock) {
        this(workspace, ui, events, defaultTools(), lock);
    }

    public ToolRuntime(Path workspace, UserInterface ui, Events events, List<Tool> tools) {
        this(workspace, ui, events, tools, new WorkspaceLock());
    }

    public ToolRuntime(Path workspace, UserInterface ui, Events events, List<Tool> tools, WorkspaceLock lock) {
        this.workspace = workspace.toAbsolutePath().normalize();
        this.ui = ui;
        this.events = events;
        this.tools = List.copyOf(tools);
        this.lock = lock;
    }

    /** The built-in tools of 1.0. */
    public static List<Tool> defaultTools() {
        return List.of(new ReadFileTool(), new ListFilesTool(), new WriteFileTool(), new SearchTool(), new RunCommandTool(),
                new GitReadTool(), new GitCommitTool());
    }

    /** Only the tools an agent's capabilities resolve to: less to describe to the model, less it can misuse. */
    public List<ToolSpec> specsFor(Agent agent) {
        List<ToolSpec> specs = new ArrayList<>();
        for (Tool t : tools) {
            if (agent.can(t.capability())) {
                specs.add(t.spec());
            }
        }
        if (agent.can(Capability.AGENT_HANDOFF)) {
            specs.add(new ToolSpec("handoff",
                    "Delegate a well-defined piece of work to a teammate. Returns the teammate's result.",
                    List.of(new Param("to", "Name of the teammate", false, true),
                            new Param("objective", "What must be achieved", false, true),
                            new Param("brief", "Short context: decisions, constraints, relevant paths", false, false))));
        }
        if (agent.can(Capability.CHAT_POST)) {
            specs.add(new ToolSpec("send_message",
                    "Write a message as yourself, only because the user asked you to. 'to' is a group's name (it appears in that group "
                            + "for everyone), a teammate's name (a private chat between you two, which the user can read), or 'user' (your private "
                            + "chat with the person you work for: use it when someone asks you to talk to them, or to reach them).",
                    List.of(new Param("to", "A group name, a teammate's name, or 'user'", false, true),
                            new Param("text", "The message", false, true))));
        } else {
            specs.add(new ToolSpec("send_message",
                    "Write to the person you work for in your private chat with them, for example when a teammate asks you to talk to them. "
                            + "It appears there as a new message from you.",
                    List.of(new Param("to", "Always 'user'", false, true), new Param("text", "The message", false, true))));
        }
        specs.add(new ToolSpec("ask_user",
                "Ask the user a question and wait for the answer, when you need a decision you cannot reasonably make yourself. Offer 2 to 4 "
                        + "short options when the answers are a known set (the user can always type something else); leave 'options' out for an "
                        + "open question. Do not ask what you can find out by reading the project.",
                List.of(new Param("question", "One clear question", false, true),
                        new Param("options", "The likely answers", true, false))));
        return specs;
    }

    /** Never throws for expected failures. The event log gets the scrubbed raw result; the model gets it delimited. */
    public String execute(Agent agent, Task task, ToolCall call) {
        events.emit("ToolCalled", task.id, agent.name(), call.name() + " " + call.args());
        String result;
        Tool tool = tools.stream().filter(t -> t.name().equals(call.name())).findFirst().orElse(null);
        try {
            if (tool == null) {
                result = "ERROR: unknown tool '" + call.name() + "'";
            } else if (!agent.can(tool.capability())) {
                result = "DENIED: " + agent.name() + " does not have capability " + tool.capability();
            } else {
                ToolContext ctx = new ToolContext(workspace, request -> approve(agent, task, request), lock, agent.name(),
                        holder -> events.emit("WaitingForWorkspace", task.id, agent.name(), holder + " is changing the workspace"))
                        .onChange(ui::fileChanged).protect(protectedPaths);
                // tools that change the workspace take the exclusive lock themselves, after any approval
                result = tool.capability().equals(Capability.FILESYSTEM_WRITE) || tool.capability().equals(Capability.COMMAND_EXECUTE)
                        || tool.capability().equals(Capability.GIT_COMMIT) ? tool.execute(ctx, agent, call)
                        : lock.shared(() -> tool.execute(ctx, agent, call));
            }
        } catch (IllegalArgumentException e) {
            result = "ERROR: " + e.getMessage();
        } catch (Exception e) {
            result = "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        result = Redactor.redact(result);
        String status = result.startsWith("DENIED") ? "denied" : result.startsWith("ERROR") ? "error" : "ok";
        events.emit("ToolCompleted", task.id, agent.name(), status + ": " + abbreviate(result, 200));
        boolean external = tool != null && tool.returnsExternalContent() && status.equals("ok");
        return external ? wrapExternal(call.name(), result) : result;
    }

    /**
     * Delimits content that came from outside the runtime so the model can tell data from instructions. The closing
     * tag is neutralised inside the content so it cannot break out of the block.
     */
    static String wrapExternal(String toolName, String content) {
        String safe = content.replace("</" + OUTPUT_TAG, "</ " + OUTPUT_TAG);
        return "<" + OUTPUT_TAG + " tool=\"" + toolName + "\">\n" + safe + "\n</" + OUTPUT_TAG + ">";
    }

    private boolean approve(Agent agent, Task task, ApprovalRequest request) {
        events.emit("ApprovalRequested", task.id, agent.name(), request.summary());
        TaskStatus previous = task.status;
        task.status = TaskStatus.WAITING_APPROVAL;
        events.taskChanged(task);
        boolean granted = ui.approve(request);
        task.status = previous;
        events.taskChanged(task);
        events.emit(granted ? "ApprovalGranted" : "ApprovalDenied", task.id, agent.name(), request.summary());
        return granted;
    }

    static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "... [truncated " + (s.length() - max) + " chars]";
    }
}
