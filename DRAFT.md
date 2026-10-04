# [RFC] BuildCLI 1.0: rebuild as a local runtime for AI engineering teams

> **BuildCLI: your local AI engineering team.**
> A local, open-source runtime that executes a configurable team of AI agents, driven from your terminal.

## TL;DR

BuildCLI has been quiet for a while and has no user base to protect. AI assistants are moving from *one chatbot* to *teams of agents you work with*. Hosted products such as xAI's [Grok Bot](https://x.ai/news/introducing-grok-bot) show the idea, but they are closed, cloud-hosted and tied to one vendor.

I propose that we **rebuild BuildCLI from scratch** to win users with a clear, new promise, and ship **1.0** as a **local runtime for AI engineering teams**:

```text
Developer
    │
    ▼
BuildCLI Runtime
    │
    ├── Architect
    ├── Developer
    ├── Reviewer
    └── QA
```

The differentiator is not *"BuildCLI calls several models"*. It is:

> **A local, configurable engineering team where the runtime, not the LLM, controls execution, permissions and stopping.**

**This RFC is optimized for delivery.** 1.0 is the smallest product that still delivers the promise: a *team* of agents that hand work to each other, safely, on your machine. Everything else is explicitly postponed (see [Scope](#scope-of-10) and [Later](#later-1x-and-beyond)).

I will build the first spike and the M0/M1 foundations; the team takes it from there. The plan is split into workstreams so several people can work in parallel from week 2.

## Foundational decisions

1. **100% local.** No BuildCLI server, account or telemetry. Data only leaves your machine when you configure a remote LLM provider or tool. With local models (Ollama) everything runs offline.
2. **Java-first.** Java 21, distributed as a **single JAR**.
3. **Provider-neutral via [LangChain4j](https://github.com/langchain4j/langchain4j)**, kept behind a port.
4. **Agents defined by files** (Markdown/YAML) in `.buildcli/`, versionable. `AGENTS.md` is read as project context. Compatibility with the shared `.agents/` convention comes in **1.4**.
5. **Teams from day one.** Direct, and handoff between agents, are in 1.0.
6. **Tools as an abstraction**; built-ins in 1.0, connectors and MCP later.
7. **TUI-first, with [TamboUI](https://github.com/tamboui/tamboui).** The terminal UI is the product's main interface from 1.0, not an add-on.

And one principle behind all the others:

> **The runtime is deterministic; the LLM is not.** The LLM decides *what* to do. The runtime decides *whether it may, who does it, and when it stops*.

## Why rebuild from scratch

From reading the current code (`cli`, `core`, `plugin`, `hooks`; Java 21, picocli 4.7.6):

| Area | Current state |
|---|---|
| AI abstraction | `AIService#generate(AIChat)`: single-shot, one system + one user message, returns `String` |
| Conversations | No history, no participants, no tool calls, no streaming |
| Providers | Only Ollama and JLama, chosen by a `switch` in `GeneralAIServiceFactory`, on `langchain4j` 0.36.2 |
| UI | Plain stdout (`SystemOutLogger`, `BeautifyShell`, `InteractiveInputUtils`) |
| Design | Heavy static state and coupling (see #489, #491, #494, #495) |

Evolving `AIService#generate()` into an agent runtime would produce an accidental architecture. There are no users to migrate, so:

- **Keep the current code on a `legacy` branch** (tag `v0.14.0`). No support commitment; it stays as reference.
- **Start a clean codebase on `main`.**
- **Port useful ideas later as tools** (Maven, Docker, changelog, doctor), only if the team wants them.
- **Close or re-scope** issues that target the old code; re-create the useful ones as 1.0 issues.

## Core concepts

```text
Agent · Team · Task · Handoff · Tool · Policy · Event
```

There is **no Session object in 1.0**. A run starts from a **root Task** and is identified by a `run id`; events and usage are grouped by it. Sessions (multi-run workspaces with a background TUI) come later.

**Chat is a view over these objects, not the central object.**

### Agent

An agent separates **identity, responsibility, instructions, capabilities and permissions**. It is not bound to a model: "Ana" is the entity; the model is runtime configuration.

```yaml
---
name: ana
role: architect

description: >
  Owns architecture, boundaries and technical trade-offs.

instructions: |
  You are the software architect.
  You do not implement production code.
  Challenge unnecessary complexity.

capabilities:
  - filesystem.read
  - search
  - git.read
  - agent.handoff

permissions:
  filesystem:
    read: ["**"]
    write: []
---
```

- Behaviour comes from **role + instructions + capabilities + permissions**. *"Ana cannot modify production code"* is enforced by the runtime. `personality` (optional) is style only.
- **Capabilities, not tool names.** An agent asks for `filesystem.read`; the runtime resolves the tool.
- `memory` is **not** in 1.0 (see [Later](#later-1x-and-beyond)).

### Team

A named group of agents, one of them the **lead**, plus runtime configuration (models, budget).

```yaml
# .buildcli/teams/backend.yaml
name: backend
lead: ana
agents: [ana, bruno, carla]
runtime:
  default: { provider: ollama, model: qwen3-coder }
  ana:     { provider: openai-compatible, model: <model-id> }
budget:
  max_tokens_per_task: 30000
```

```bash
buildcli run --team backend "Add pagination to the orders endpoint"
```

The **lead** receives the user's request, breaks it into tasks and hands them off. A solo dev gets the architect/reviewer/QA they don't have; a team lead commits the team's conventions with the repo.

### Task: the unit of execution

A task has an objective, an owner, inputs (brief, artifacts, references), a status (`PENDING`, `RUNNING`, `WAITING_APPROVAL`, `ESCALATED`, `FAILED`, `DONE`) and a result. Tasks are persisted and traceable. Tasks form a tree under the root task.

### Handoff: a system primitive

An agent does not chat with another one; it creates a handoff, which is a new task:

```text
Task #42
 ├── from: ana
 ├── to: bruno
 ├── objective: implement repository abstraction according to ADR-012
 ├── brief: decisions, constraints, open questions
 └── artifacts: ADR-012.md, src/main/java/.../OrderRepository.java
```

Bruno receives an objective and a brief, not a transcript. When he finishes, the result returns to Ana through the same task. The runtime validates that the target agent exists, is in the team, and that the handoff has the required fields. **Collaboration becomes traceable and cheap in tokens.**

## Interaction modes

| Mode | Flow | In 1.0 |
|---|---|---|
| **Direct** | User → Agent | Yes |
| **Handoff** | Agent A → Task → Agent B | Yes |
| **Team discussion** | Bounded multi-agent debate (`/discuss`) | No: 1.1 |

Agents never talk freely. Every interaction is a task with an owner.

## Execution model

**One agent runs at a time in 1.0.** The orchestrator runs tasks sequentially: the lead delegates, the target agent runs, the result returns. This is a deliberate simplification:

- Deterministic runs, easy to test and replay.
- No file lock or merge problems (only one writer exists at any moment).
- Parallel execution, file ownership and task workspaces are **1.1+**, when there is real evidence that the sequential model is too slow.

Run limits, enforced by the runtime: max handoff depth, max steps per task, max tokens per task, timeout per tool call. When a limit is reached, the task fails with an explicit reason instead of looping.

### Failure handling: retry, then escalate

When a task or handoff fails (agent error, invalid handoff, tool failure, limit reached), the runtime, not the LLM, applies a fixed policy:

1. **Retry up to 3 times.** Each retry is a new attempt of the same task, and the failure reason is added to the agent's context so it can correct itself.
2. **After the 3rd failed retry, escalate to the user.** The task moves to `ESCALATED` and the TUI asks the user to choose: **retry** (optionally with extra guidance), **skip/close** the task, or **abort** the run.
3. Retries count against the task's token budget; a `LimitReached` on tokens escalates immediately, without retrying.
4. Approval denials by the user are **not** failures and are never retried automatically.

Every attempt is recorded (`TaskRetried`, `TaskEscalated`), so the trace shows what was tried before the user was involved. The retry count (default 3) is configurable per team.

## Runtime vs. LLM responsibilities

| The runtime decides (deterministic) | The LLM decides (non-deterministic) |
|---|---|
| Who can execute a task | What to do and how to solve it |
| Which tools are available to whom | Which tool to use |
| Whether a handoff is well-formed and allowed | Whether to hand off, and to whom |
| Whether approval is required | The content of the work |
| Max depth, steps, tokens, timeouts | |
| Whether a task is finished or failed | |

Note the boundary: the runtime validates the **form and permissions** of a decision; it does not judge its **quality**. That is what the Reviewer/QA agents and the human approval are for.

## Tools

**Tool** is the central abstraction. The agent only knows `ToolDefinition`, `ToolExecution` and `ToolResult`. Internally: `Tool → Tool Registry → Tool Executor`, with a policy check before every execution.

**1.0 built-ins:** `filesystem` (read, write, search), `git` (read; commit gated by approval), `command` (under a policy), and `agent.handoff`.

MCP and connectors are **1.x**. The registry is designed so they plug in as additional providers, but they are not implemented in 1.0.

## Security

"100% local" does not mean safe. Reading and writing files, running commands and network access are potentially dangerous, so every agent runs under an explicit policy:

```yaml
permissions:
  filesystem:
    read:  ["**"]
    write: ["src/**"]
  command:
    allow: [["mvn", "test"], ["./mvnw", "-q", "verify"]]
    timeout: 10m
  network: false
```

Rules for 1.0:

- **Commands are structured argv, never a shell string.** No pipes, `;`, `&&` or expansion. The allow list matches the argv prefix.
- **Approval is the default.** Writes (shown as a diff), `git commit`, and any command not in the allow list need explicit human approval. Relaxing this is a per-policy decision.
- **Honest limits.** An allowed `mvn test` still runs project code (plugins, test code). The command policy reduces accidents; it is **not a sandbox**. Container/sandbox execution is planned for 1.x, and the docs must say so.
- **Untrusted by default.** Agents, teams and policies coming from a cloned repo require approval before first use. Policy files are never editable by agents.
- **Prompt injection.** Content read from files, tool output and external text is treated as untrusted data and delimited as such in prompts. It can never widen an agent's permissions: permissions come only from the policy.
- **Secrets** are never written intentionally to logs/events; known patterns (API keys, tokens) are redacted from tool output. Redaction is best-effort and documented as such.

## Context: `AGENTS.md` and `.buildcli/`

| Path | Responsibility | Committed? |
|---|---|---|
| `AGENTS.md` | **Project context**: build commands, architecture, conventions. Not runtime config. | Yes |
| `.buildcli/agents/`, `.buildcli/teams/` | BuildCLI agents, teams and policies | Optional |
| `~/.buildcli/` | Global agents, teams, provider config | n/a |
| `~/.buildcli/projects/<project-id>/` | **Operational state**: tasks, events, artifacts | **Never** in the repo |

BuildCLI never writes runtime state into the project tree. The shared `.agents/` convention is **not** read in 1.0; support (with a mapping for BuildCLI's capabilities and permissions) is planned for **1.4**.

## Persistence

| Kind of data | Storage |
|---|---|
| Human configuration (agents, teams, `AGENTS.md`) | Markdown / YAML |
| Runtime state (tasks, handoffs, events, tool calls, usage) | Embedded SQL database (**SQLite or H2, decided by the spike**) |
| Artifacts (ADRs, patches, reports) | Filesystem |

### Event log

An append-only event log from day one:

```text
RunStarted · AgentInvoked · ToolCalled · ToolCompleted
TaskCreated · TaskCompleted · TaskFailed · TaskRetried · TaskEscalated · HandoffCreated
ApprovalRequested · ApprovalGranted · ApprovalDenied · LimitReached
```

The same log feeds the **CLI/TUI output, history, audit and usage metrics**, and later enables `buildcli replay <run>`. The schema is versioned from the first release.

## Token efficiency

A team costs more than one agent, so cost is designed in, not bolted on:

- Direct mode by default; handoff **briefs**, not transcripts.
- Scoped context per task: objective, brief, referenced artifacts. Never the whole run.
- References instead of content (paths, line ranges); tool output trimmed.
- Minimal tool schemas: an agent only receives the tools its capabilities resolve to.
- A **hard token budget per task** (`max_tokens_per_task`), enforced by the runtime.
- Right model per agent (cheap/local for simple roles).

Usage is derived from the event log and shown per agent and per run (`buildcli usage`). Token-saving tips, `/context` inspection and multi-level budgets are 1.x.

## Architecture

```text
                    ┌──────────────┐
                    │  TUI (Tambo) │
                    └──────┬───────┘
                    ┌──────▼───────┐
                    │ Orchestrator │  ← sequential, limits, approvals
                    └──────┬───────┘
              ┌────────────┼────────────┐
        ┌─────▼─────┐ ┌────▼─────┐ ┌────▼────┐
        │   Agent   │ │   Task   │ │ Handoff │
        │  Runtime  │ │  Engine  │ │ Engine  │
        └─────┬─────┘ └────┬─────┘ └─────────┘
       ┌──────▼────────────▼──────┐
       │       Tool Runtime       │  ← policies, approvals
       └──────────┬───────────────┘
        ┌─────────┼──────────┐
      Files      Git       Command
                  │
          ┌───────▼────────┐
          │  LLM Gateway   │
          │  (LangChain4j) │
          └───────┬────────┘
        Ollama · OpenAI-compatible
```

### Code structure (ports and adapters)

```text
domain          Agent, Team, Task, Handoff, Message, Tool, Policy, Event
application     orchestrator, task engine, handoff engine, use cases
ports           LlmGateway, ToolProvider, EventStore, AgentRepository, UserInterface
infrastructure  LangChain4j, database, filesystem, picocli, TamboUI
```

- LangChain4j stays behind `LlmGateway` and never leaks into the domain.
- **A single Maven module** with well-separated packages, boundaries enforced by ArchUnit. Modules are extracted once the boundaries are proven.
- **TamboUI is the 1.0 interface**, behind the `UserInterface` port. The port exists because TamboUI describes itself as experimental with a changing API: the version is **pinned**, the dependency is isolated in one package, and the runtime is tested through a headless `UserInterface` (scripted approvals and answers), never through the real terminal.
- Non-interactive commands (`task`, `usage`, `doctor`, `run --headless`) print plain text so scripts and CI work; this is output formatting, not a second UI to maintain.

### CLI and TUI

`buildcli` with no arguments opens the TUI, which is the main entry point: team panel, task/handoff tree, approvals with diffs, escalations, and a usage status bar. Subcommands are shortcuts and scripting entry points:

```bash
buildcli                            # opens the TUI
buildcli init                       # create .buildcli/ with a sample team
buildcli agent list | create <name>
buildcli team list | create <name>
buildcli run  --team backend "<request>"    # lead + handoffs
buildcli run  --agent ana    "<request>"    # direct
buildcli task list | show <id>
buildcli usage [--run <id>]
buildcli doctor
```

## Scope of 1.0

The 1.0 is done when **the demo scenario below works end to end** on a real project, with a local model, on Linux, macOS and Windows.

> **Demo scenario.** Team `backend` = lead *Ana* (architect), *Bruno* (developer), *Carla* (reviewer).
> In the TUI: `run --team backend "Add pagination to the orders endpoint"`:
> 1. Ana reads the code and hands off an implementation task to Bruno, with a brief.
> 2. Bruno proposes file changes; the user approves the diff in the TUI; he runs `mvn test` (allowed by policy).
> 3. Bruno's result returns to Ana, who hands off a review task to Carla.
> 4. Carla reads the diff and reports; Ana closes the run with a summary.
> 5. If a task fails, it is retried up to 3 times and then escalated to the user in the TUI.
> 6. `buildcli task list` and `buildcli usage` show the full trace, per-agent tokens and cost.

| Area | In 1.0 | Not in 1.0 |
|---|---|---|
| Core | Agent, Team, Task, Handoff, Tool, Policy, Event | Session, Memory |
| Modes | Direct, handoff (sequential) | Team discussion, parallel agents |
| AI | LangChain4j 1.x; **Ollama + one OpenAI-compatible provider**; streaming; tool calling | More providers |
| Tools | filesystem, git, command (policy), agent.handoff | MCP, connectors, worktrees |
| Context | `AGENTS.md` (project context), `.buildcli/` | `.agents/` compatibility (1.4) |
| Persistence | Embedded DB + filesystem, versioned schema | Replay |
| UI | **TamboUI TUI** + plain-text scripting commands | Background sessions |
| Safety | Approvals, command policy, untrusted agents, per-task token budget, run limits, retry (3) then escalate | File locks, sandbox/containers |
| Observability | Event log, `task` and `usage` commands | Tips, `/context` |

## Later (1.x and beyond)

- **1.1:** team discussion (bounded, with a decision record), file ownership and parallel agents, task workspaces (git worktrees), agent memory.
- **1.2:** MCP client and connector SPI; container sandbox for commands; more providers.
- **1.3:** Sessions and background TUI, `buildcli replay`, token-saving tips, multi-level budgets, Maven/Docker/changelog tools ported from legacy.
- **1.4:** `.agents/` compatibility: read the shared agent-definition convention, with a documented mapping for BuildCLI's capabilities and permissions, so projects configured for other agent tools work with zero setup.
- **Deliberately postponed:** autonomous long-running agents, scheduled agents, RAG, marketplace, remote/distributed agents.

## Roadmap

Each milestone has **acceptance criteria**; a milestone is closed only when they pass.

### M0: Spike and reset (2 weeks)

*Goal: prove the core loop and settle the technical unknowns before committing `main`.*

- Spike on a separate branch: two agents, a handoff, one tool under a command policy, an approval **rendered in a TamboUI view**, events stored in the DB, Ollama via LangChain4j.
- Evaluate **TamboUI** as the 1.0 interface: streaming output, diff rendering, input handling, Windows terminals/WSL, resize, and how it behaves inside a single JAR. Pin the version and list the gaps to work around or contribute upstream.
- Decide **SQLite vs. H2** (JAR size, Windows/WSL/ARM, startup time).
- Measure tool-calling reliability with 2–3 local models; pick the **minimum supported models**.
- Then: tag `v0.14.0`, create `legacy`, clean `main`, CI, ArchUnit, contribution guide, issue triage.

**Accept when:** the spike completes a handoff scenario with a local model in ≥ 8 of 10 runs, the approval view works in TamboUI on Linux, macOS and Windows terminals, the TamboUI/DB decisions are documented, and `main` builds in CI on Linux/macOS/Windows.

### M1: Domain and runtime core

- Domain model, event store with versioned schema, loading of agents/teams/policies (`AGENTS.md` as context, `.buildcli/`), fake/recorded `LlmGateway` and headless `UserInterface` for deterministic tests.
- Orchestrator with limits (depth, steps, tokens, timeout), sequential task engine, handoff validation, **retry (3) then escalate**.

**Accept when:** a scripted (fake-LLM) team completes the demo scenario deterministically in tests, every limit has a test that fails the task with the right reason, and a task that fails 4 times in a row ends in `ESCALATED` with the retry events recorded.

### M2: LLM gateway and tools

- LangChain4j port (Ollama + OpenAI-compatible), streaming, tool calling.
- Tool runtime: filesystem, git, command (argv policy), approvals with diffs, redaction.

**Accept when:** the demo scenario runs against a real local model; a policy-denied command and an unapproved write are both blocked and logged; prompt-injection test cases (malicious file content) cannot widen permissions.

### M3: TUI, CLI and preview (0.x)

- **TamboUI app**: team panel, task/handoff tree, streaming agent output, approvals with diffs, escalation dialog, usage status bar.
- Scripting commands (`init`, `agent`, `team`, `run --headless`, `task`, `usage`, `doctor`).
- Packaging (single JAR, `install.sh`/`install.bat` updated). **Public 0.x preview.**

**Accept when:** a new user can install, run `buildcli init`, and complete the demo scenario (including approving a diff and resolving an escalation) in under 10 minutes following only the README, on Linux, macOS and Windows.

### M4: Hardening → 1.0.0

- Docs (concepts, agent/team/policy reference, security model with its limits), sample teams, schema stability commitment, bug bash from preview feedback.

**Accept when:** the preview feedback P1s are closed, and the security model doc has been reviewed by at least two maintainers.

### Workstreams (so the team can parallelize after M0)

| Workstream | Depends on | Milestones |
|---|---|---|
| A. Domain + event store + config loading | M0 | M1 |
| B. Orchestrator + task/handoff engine | domain API from A | M1 |
| C. LLM gateway + streaming + tool calling | port defined in M0 | M2 |
| D. Tool runtime + policies + approvals | domain API from A | M2 |
| E. CLI / interactive UI / TUI | ports defined in M0 | M3 |
| F. Docs, packaging, CI, sample teams | none | all |

## Risks

| Risk | Mitigation |
|---|---|
| **Scope and maintenance capacity** | Sequential execution, no Session/Memory, features moved to 1.x; every milestone has acceptance criteria and can ship on its own. |
| **Weak tool calling in small local models** | Measure in M0; declare minimum supported models; support a remote OpenAI-compatible provider from 1.0. |
| **Command policy is not a sandbox** | Argv-only commands, approval by default, honest docs, container sandbox in 1.2. |
| **Prompt injection through files/tool output** | Untrusted-content delimiting; permissions come only from the policy; dedicated tests in M2. |
| **TamboUI is experimental and it is the 1.0 interface** | Evaluated in the M0 spike before committing `main`; version pinned; dependency isolated behind `UserInterface`; runtime tested headless; gaps worked around or contributed upstream. If the M0 evaluation fails, we decide with data whether to fix upstream or reconsider, before M1 starts. |
| **SQLite native libs in a single JAR** | Settled in the M0 spike (SQLite vs. H2). |
| **Windows/WSL differences** (paths, terminal, native libs) | CI on Linux/macOS/Windows from M0; explicit acceptance criterion. |
| **Rewrite loses momentum** | A public 0.x preview at M3; the spike is a working demo that can be shown before the RFC is approved. |

## Open questions

1. **Capability naming:** the initial set and how it maps to built-in tools (and later MCP).
2. **Minimum supported local models** and a shared benchmark scenario for them (answered in M0).
3. **Schema versioning policy** for agent/team files and the event DB after 1.0.
4. **`.agents/` field mapping** (for 1.4): which fields of the shared format to support first, and how BuildCLI's capabilities and permissions coexist without breaking other tools. Not needed before 1.4, but the agent file format in 1.0 should avoid choices that block it.

Decided: failed tasks are retried up to 3 times and then escalated to the user (see [Failure handling](#failure-handling-retry-then-escalate)); the TUI is TamboUI from 1.0; `.agents/` compatibility is 1.4.

## Non-goals

- Any hosted/cloud service, account or telemetry. BuildCLI stays MIT and 100% local.
- Replacing Maven/Gradle or becoming an IDE.
- Another thin LLM wrapper, or a product clone. Grok Bot is only a reference for the agent-team UX.

## Next steps

1. Review this RFC and agree on the direction (and on the rewrite) in this thread.
2. I start **M0**: the spike on a separate branch, with a demo the team can run.
3. Create a GitHub Project with the milestones and workstreams above, and split M1 into small issues, including `good first issue`s.
4. Decide the open questions above as the spike produces data.
