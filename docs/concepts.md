# Concepts

BuildCLI runs **agents** on your machine and you talk to them in a chat. This page explains the pieces and how a request unfolds.

## The pieces

| Concept | What it is |
|---|---|
| **Agent** | An identity with a role, instructions, **capabilities** and **permissions**. Not tied to a model: "ana" is the agent, the model is configuration. Defined in a Markdown/YAML file ([reference](reference/agents.md)). |
| **AgentFather** | A built-in contact, not an agent and not a model, that creates and manages agents by conversation (`/newagent`). It follows a script and acts only after you say yes. |
| **Group** | A chat you put agents in, like a messaging group: members and **admins**. A message nobody is @mentioned in goes to an admin; an @mention goes to that member. Kept per project, outside the project tree. |
| **Task** | The unit of execution: an objective, an owner, a status (`PENDING`, `RUNNING`, `WAITING_APPROVAL`, `ESCALATED`, `FAILED`, `DONE`) and a result. Persisted and traceable. |
| **Handoff** | How agents collaborate: an agent creates a *child task* for a teammate with an objective and a short brief, and gets the result back. In a group they also talk by @mention, and when you ask they can write in a group or privately to each other (`send_message`); a hop limit stops them from talking forever. |
| **Tool** | What an agent can do (read, search, write, run a command, git, hand off). Agents ask for *capabilities*; the runtime resolves the tool ([tools](reference/tools.md)). |
| **Policy** | The permissions the runtime enforces: which paths may be read or written, which commands may run, what needs approval. |
| **Event** | One entry of the append-only log of a run. The UI, history, audit and usage are all derived from it. |

**The chat is a view of structured data.** What you see in the terminal UI is a rendering of tasks, handoffs, tool calls and
approvals, which are structured data.

## The principle

> The LLM decides *what to try*. The runtime decides *whether it may, who does it, and when it stops*.

Everything that must be reliable is deterministic code, not a prompt: permissions, approvals, limits, handoff validation,
retries, redaction. The model can be wrong or be tricked by the files it reads; it cannot be given more power
([security model](security-model.md)).

## Anatomy of a run

1. You send a message. A **root task** is created for the agent that receives it (in a group, an admin or the one you @mentioned).
2. The lead works with its tools. To delegate it makes a **handoff**: the runtime checks the target exists, is a teammate,
   and that the depth and count limits allow it, then runs the teammate **to completion** and returns the result.
3. Each tool call goes through the same gate: the agent must hold the capability, the tool applies the agent's policy,
   anything needing approval is shown to you (writes as a diff), output is scrubbed of secrets and delimited as data.
4. When the lead replies with no further tool call, the root task is `DONE` and the run ends.

In the chat, different agents work **in parallel** and each agent handles one conversation at a time. They share the workspace
through a lock: reads are shared; writes, git commits and commands are exclusive, and a write is refused if the file changed
since the agent read it. A headless `run` still executes one task at a time.

## When something goes wrong

A failed task (a provider error, an invalid reply, too many steps) is **retried up to three times**, continuing the same
conversation so side effects already done are not repeated; a failure caused by the agent's own behaviour is explained to
the model. If it still fails, the task is **escalated to you**: retry, skip it, or abort the run. A token budget overrun
escalates immediately. A denied approval is not a failure and is never retried.

## Interaction modes

- **Direct** (`buildcli run --agent NAME`): you talk to one agent, which cannot hand off.
- **Group** (`buildcli run --group NAME`, the default when there is one): its admin leads and delegates through handoffs.

## Where things live

| What | Where |
|---|---|
| Agents of a project | `<project>/.buildcli/agents/` |
| Project context for agents | `<project>/AGENTS.md` (information only, never configuration) |
| Your own agents | `~/.buildcli/agents/` |
| Your groups, who may contact whom | `~/.buildcli/projects/<id>/chats.yaml`, `reach.yaml` |
| Runs, tasks, events, usage, chat history | `~/.buildcli/projects/<id>/state.db` (SQLite; or `state.mv.db` with H2, or nothing on disk with `memory`: see Settings > State database), never inside the project |
| Which project definitions you approved | `~/.buildcli/trust.json` |

## Choosing a model

The runtime bounds what a model can do but cannot make a weak one reliable. Agents that use tools need a capable model:
7B and larger for local use, or any hosted OpenAI-compatible model. A 3B model passed a simple scenario once and then
failed after small prompt changes ([findings](m2-real-model-findings.md)). Qualify a model before relying on it by
trying your own tasks with it several times: one run proves little, especially at temperature 0.
