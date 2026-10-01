# BuildCLI

> **Your local AI engineering team.**
> An open-source runtime that lets you chat with a set of AI agents that work on your project from your terminal. 100% local: no
> account, no server, no telemetry. With a local model (Ollama) everything runs offline.

**Status: pre-1.0.** BuildCLI is being rebuilt from scratch as a runtime for agents you chat with (the previous CLI is on the
[`legacy`](../../tree/legacy) branch, tag `v0.14.0`). The design is in [`docs/rfc/0001-buildcli-1.0.md`](docs/rfc/0001-buildcli-1.0.md).
Read [Known limitations](#known-limitations) before relying on it.

![AgentFather, the built-in helper, creating an agent called rita step by step; rita then appears in the chat list](docs/images/agentfather.png)

*AgentFather creating an agent, step by step, with no model connected: it asks, shows what it is about to do, and only creates
the agent after you say yes. The new agent shows up in the chat list.*

## The idea

```text
You ── chat ──▶ BuildCLI Runtime ──┬── wheslley (architect)
                                   ├── matheus  (developer)
                                   ├── breno    (devops)
                                   └── dumildes (innovator) ...
```

You open a chat, like a messaging app, with your agents: a **group** for several of them and a **direct chat** with each
agent. Agents are people-like: each reads one conversation at a time, different agents work **in parallel**, they
**@mention** and **hand off** work to each other, and you see who is reading, thinking, typing or waiting for you. The
**runtime, not the LLM,** enforces what each agent may do: permissions, approvals, limits, and when a task has failed.

- **Handoffs instead of chatter:** an agent delegates a task with an objective and a short brief, not a transcript.
- **Policies, not prompts:** "the reviewer cannot modify code" is enforced by the runtime ([security model](docs/security-model.md)).
- **You stay in control:** file writes are shown as diffs, commands outside the allow list ask first, and a task that
  fails is retried three times and then handed to you.
- **Everything is an event:** one append-only log in a local database (SQLite by default; H2 or memory if you prefer,
  see [Settings](#settings)) feeds the UI, history and usage.

## Install

Requires **JDK 21 or newer**.

```bash
# Linux and macOS
curl -fsSL https://github.com/BuildCLI/BuildCLI/releases/latest/download/install.sh | sh

# Windows (PowerShell)
irm https://github.com/BuildCLI/BuildCLI/releases/latest/download/install.ps1 | iex
```

The installer verifies the jar's SHA-256, installs it under `~/.buildcli/bin` and adds a `buildcli` launcher; it needs
no administrator rights. Or build it yourself: `mvn verify` produces `target/buildcli.jar` (`java -jar target/buildcli.jar`).

## Quick start

```bash
cd my-project
buildcli               # opens the chat; on first run it offers to connect a model and to add the sample agents
```

Everything can be done **inside the app**: `/connect` picks a provider (OpenRouter, DeepSeek, Ollama, ...), asks for the
API key (typed hidden, checked before it is saved, stored owner-only on your computer, or taken from the usual environment
variable) and lets you choose a model; the empty chat has buttons for the sample agents, to create your own agent, and to talk to [AgentFather](#agentfather).
From the shell, the same setup is `buildcli init` (the sample agents and an `AGENTS.md`), `buildcli provider login <name>`
and `buildcli doctor` (checks Java, git, your configuration and whether Ollama has the model).

The sample agents are **wheslley** (architect, leads the group), **matheus** (developer), **breno** (devops) and
**dumildes** (innovator) in the group `maintainers`. Their files are drafts based on each maintainer's git history; edit
them in `.buildcli/agents/`.

Without the chat:

```bash
buildcli run --group maintainers --headless --approve ask "add a health endpoint to the API"
buildcli run --agent matheus --headless "fix the typo in the README"
buildcli runs          # the runs of this project
buildcli task list     # the tasks and handoffs of the latest run
buildcli usage         # tokens per agent
```

`init` uses `ollama / qwen2.5:7b` by default (`buildcli init --model provider:model` to change it); pull the model first
(`ollama pull qwen2.5:7b`), use `/connect`, or point agents at any OpenAI-compatible endpoint. Every command is documented
in the [CLI reference](docs/reference/cli.md); agent files in [agents](docs/reference/agents.md);
ready-to-copy agents in [`examples/`](examples). New to the ideas? Read [concepts](docs/concepts.md); stuck? [troubleshooting](docs/troubleshooting.md).

## AgentFather

Like Telegram's BotFather, **AgentFather** is a contact that is always in your chat list and creates and manages agents by
conversation. It is not a model: it follows a script, so it works before you have connected anything, and it never creates or
deletes anything until you reply *yes*.

- `/newagent [name]` asks for a name, a role, what the agent may do (a list to pick from) and one sentence on how it should
  work, shows a summary, and creates it. `back` goes one question back, `cancel` stops.
- `/agents` lists your agents with their model and capabilities; `/deleteagent <name>` deletes one (after a yes);
  `/samples` adds the sample agents.
- `/editagent <name>` changes an agent you already have, one setting at a time (each after a yes): its role, what it may do, how
  it works, the folders it may write in and the commands it may run **without asking**. Folders must be inside the project (never
  `.buildcli/` or `.git/`). It rewrites the agent's file, so comments in the file's header are lost.
- A new agent can read the whole project and has no folder to write in and no command allowed; give it more with `/editagent`.
  Every write still shows you a diff and asks first.

## In the chat

- **Groups, direct chats and notes**: `/newgroup`, `/dm @agent`, members and admins; a message with no @mention goes to an
  admin, one with @mentions goes to those agents. "You (notes)" is a private chat that no agent reads.
- **Agents that speak for you**: give an agent the `chat.post` capability and ask it, in a direct chat, to write in a group
  or to a teammate. A private chat between two agents shows up as `ana ↔ bruno`: you can read it, not write in it.
- **Who may contact whom**: `/reach @bruno @ana off` stops one agent from contacting another (they are told they cannot).
- **You stay in control**: file writes show as diffs; `/review` lists what agents changed and `/undo` puts it back (files
  changed since are left alone); approvals can be allowed "always here" and taken back with `/revoke`.
- **Search your chats** (`Ctrl+K`, or click the box above the list): by name, role or something said in a chat, like a
  messaging app; an agent you have no chat with yet is offered as "Start a chat".
- **Work with the output**: `/copy` copies a code block, `Ctrl+F` searches the chat, `/diff`, `/status`, `/log`, `/open`,
  image and audio attachments, a title and bell when an agent needs you, mouse support (Windows included).
- **Models**: `/model` shows or changes the default model, or one agent's.
- **Modes**: `manual` asks before every write, command and commit; `edits` writes files without asking (you can undo) and still
  asks for commands; `auto` asks for nothing. Shift+Tab or `/mode` changes it; the pill in the chat header shows it. Starting mode
  in Settings. What an agent may touch is still set by its own permissions, and trusting a project is always asked.
- **Messages**: right-click a message (or Alt+M for the newest) to mark it, click more, then copy, forward to another chat or
  delete (the agents forget deleted messages too). Unsent text waits in its chat as a "Draft:" in the list.
- **About you and your groups**: `/me name|about|style` tells every agent who you are and how to deal with you (kept on this
  computer). `/context` gives a group background to read: a text and text files.
- **English and Portuguese**: Settings › General › Language (`auto` follows your computer). The screens, menus, commands, dialogs and
  AgentFather are translated; some messages from providers and errors stay in English. Agents answer in the language you write in.
- **Not only for code**: `/samples writing` and `/samples office` add teams for writing and office work (no commands, no git).

## Settings

`F2` (or `/settings`): providers, a model per agent, agents (a step-by-step form where `Esc` goes back and capabilities
are ticked from a list), theme, and **State database**: `sqlite` (default, a file readable from several terminals), `h2`
(a file, one BuildCLI at a time) or `memory` (fastest, forgotten when BuildCLI closes). Writes go to the database in the
background in batches; in a synthetic load test that took the event log from about 7 000 to about 90 000 events/s with
SQLite (see [`docs/storage.md`](docs/storage.md)). `BUILDCLI_STORAGE` overrides the setting.

**Your own colours.** Put variables in `~/.buildcli/theme.css` (read when BuildCLI starts):

```css
$accent: #ff8800;        /* every theme */
$light-bg: #fffdf5;      /* only the light theme; also $dark-… and $contrast-… */
$agent-1: #4dd0e1;       /* the colours agents get, 1 to 8 */
```

The names are `bg sidebar panel field selected me them pill code error-bg dialog text dim faint accent green blue red amber
on-me on-me-dim tick code-text line on-accent add-bg add-fg del-bg del-fg danger`. Anything it does not understand is
reported on the first screen and skipped.

There is also an experimental **native executable** (GraalVM): about half the memory and 5 to 10 times faster to start, built
on Linux only so far. See [`docs/native-image.md`](docs/native-image.md).

## Known limitations

Be aware of these, they are stated plainly on purpose:

- **Model quality decides everything.** Tool-using agents need a capable model. `qwen2.5:3b` was unreliable in our
  tests (see [`docs/m2-real-model-findings.md`](docs/m2-real-model-findings.md)); use 7B or larger, or a hosted model.
  The runtime bounds what a weak model can do, but it cannot make it reliable. Support for specific models is not yet
  claimed.
- **Not a sandbox.** The command allow list reduces accidents; a command you allow still runs project code, and there is
  no network permission until the sandbox planned for 1.2. See the [security model](docs/security-model.md).
- **Terminals:** the TUI was driven on Linux, and on the Windows JVM through WSL; the maintainer confirmed the mouse in
  Windows Terminal. It has not been driven on macOS, and the Windows ACL branch of the saved-keys file is untested. CI
  builds and tests on Linux, macOS and Windows, including the installers.
- **Real-model coverage is thin:** the chat, `send_message`, undo and the sample agents were tested mostly with scripted models;
  a hosted model passed the scenario, and small local models are unreliable with tools.
- **No cost figures:** usage is reported in tokens; no prices are assumed.
- **Parallel agents share one workspace:** reads are shared, but writes and commands take an exclusive lock, so two agents
  never change files at the same time. There are no sessions or long-term memory yet (see the RFC).
- **Scale:** many agents are cheap to hold (one virtual thread each), but the model calls, not BuildCLI, are the real limit;
  the measurements are in [`docs/storage.md`](docs/storage.md) and `LoadProbe`.

## Architecture

Ports and adapters, enforced by tests: `domain` ← `ports` ← `application` ← `infrastructure`, with LangChain4j, TamboUI,
JDBC and Jackson confined to `infrastructure`. See [`.github/CONTRIBUTING.md`](.github/CONTRIBUTING.md).

## Contributing

Contributions are welcome. Read [`.github/CONTRIBUTING.md`](.github/CONTRIBUTING.md) and the RFC first.

## License

[MIT](LICENSE)
