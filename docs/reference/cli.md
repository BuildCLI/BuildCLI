# CLI reference

`buildcli` with no arguments opens the terminal UI on a terminal, and prints the help otherwise.
Exit codes: **0** success, **1** the work failed or was aborted, **2** usage or configuration error.

| Command | What it does |
|---|---|
| `buildcli init [--model provider:model]` | Creates `.buildcli/` with four sample agents (wheslley, breno, matheus, dumildes), a group `maintainers` (kept outside the project) and an `AGENTS.md`. Never overwrites existing files. The files it writes are pre-approved; anything you add later asks again. |
| `buildcli agent list` / `show <name>` / `create <name> [--role R] [--capabilities a,b] [--global]` | Inspect and scaffold agents. |
| `buildcli run [REQUEST...]` | Runs a request. Options below. |
| `buildcli runs [--limit N]` | The recent runs of this project. |
| `buildcli task list [--run ID]` / `task show <n> [--run ID]` | The tasks and handoffs of a run (default: the latest), and one task with its events. |
| `buildcli usage [--run ID] [--json]` | Tokens per agent for a run, derived from the event log. |
| `buildcli provider list` / `login <provider> [--stdin]` / `logout <provider>` / `add <name> --url <url>` / `test <provider:model>` | The places models come from. `login` saves the provider's API key on this computer (asked hidden, or the first line of standard input with `--stdin`) so no variable has to be set before starting; `logout` forgets it. A set environment variable wins over a saved key. Inside the app, `/connect` does the same step by step. |
| `buildcli doctor` | Checks Java, git, the state directory, the configuration and the model providers. Exits 1 if something is broken; an unreachable Ollama is only a warning. |
| `buildcli config` | Where configuration and state live, and the provider endpoints. For each key it says only whether it is set, and whether from the environment or saved by you (never the key). |

## `run`

```bash
buildcli run --group maintainers "add a health endpoint"    # a group: its admin receives it and may delegate via handoffs
buildcli run --agent breno "check the CI workflow"        # direct: one agent, no handoffs
buildcli run --headless --approve writes "fix the typo"    # plain text, no TUI
```

| Option | Meaning |
|---|---|
| `--group NAME` / `--agent NAME` | The group (default: the only group) or a single agent. A group run goes to its first admin. |
| `--model provider:model` | Model for agents with none chosen in the settings, e.g. `ollama:qwen2.5:7b` or `openai:gpt-x`. |
| `--headless` | Plain-text output. Used automatically when there is no terminal. |
| `--approve ask\|none\|writes\|all` | How approvals are decided **without the TUI**. `ask` prompts on the console (default on a terminal); `none` denies everything that needs approval (default without a terminal); `writes` approves file writes only; `all` approves everything, only for throwaway workspaces. |
| `--no-stream`, `--threads N`, `--temperature T` | Model settings. `--threads` is Ollama's `num_thread` (default 4; Ollama's own default of 16 was ~50x slower on a WSL2 machine). |

Before anything runs, agents defined by the project are checked against the trust file. The first time, or after any change
to `.buildcli/`, you are shown what they may do and asked to approve. Without the TUI this is decided like any other
approval: `ask` prompts you, `all` approves it, and `none` and `writes` **deny** it, so an unattended script can never be
talked into trusting changed definitions.

### The terminal UI (the chat)

A chat list on the left (your groups, "You (notes)", a direct chat per agent, and read-only chats between agents), the
conversation on the right with who is reading, thinking, typing or waiting for you, and approvals as a dialog (writes show
the diff). A task that failed three retries asks you to retry, skip or abort. Type `/` for the commands, `@` to mention an
agent. `F2` opens the settings, the mouse works, and `Enter` sends (Settings > General changes that).

| Command | What it does | Key |
|---|---|---|
| `/diff [--staged] [path]` | Show uncommitted changes (git diff) | `Ctrl+G` |
| `/status` | Show git status |  |
| `/log` | Show recent commits |  |
| `/open <file>` | Open a file in the viewer | `Ctrl+O` |
| `/attach <file>` | Attach an image or audio file (or paste its path) |  |
| `/tasks` | Show what the agents are doing: tasks and handoffs | `Ctrl+T` |
| `/stop` | Stop the work in this chat | `Ctrl+X` |
| `/retry` | Send the last failed message again |  |
| `/copy [message]` | Copy the last code block (or the whole last answer) |  |
| `/find [text]` | Search this chat | `Ctrl+F` |
| `/chats [text]` | Search your chats and agents by name, role or what was said | `Ctrl+K` |
| `/review` | See the files agents changed in this chat |  |
| `/undo` | Put back the files an agent changed last (shows them first) |  |
| `/revoke` | Stop approving automatically in this chat (shows what was allowed) |  |
| `/queue [clear]` | Show or drop messages waiting their turn |  |
| `/settings` | Providers, models, agents, theme and more | `F2` |
| `/connect` | Connect a provider and choose the default model |  |
| `/model [@agent] [provider:model]` | Show the models, or change the default (or one agent's) model |  |
| `/reach [@agent @other on|off]` | Show or set which agents may contact each other |  |
| `/dm @agent` | Open a direct chat with an agent |  |
| `/newgroup <name> [@agents]` | Create a group with some agents |  |
| `/add @agent` | Add an agent to this group |  |
| `/remove @agent` | Remove an agent from this group |  |
| `/admin @agent` | Make an agent admin of this group |  |
| `/dismiss @agent` | Dismiss an admin of this group |  |
| `/rename <name>` | Rename this group |  |
| `/info` | Group or contact info |  |
| `/sidebar` | Show or hide the chat list | `Ctrl+B` |
| `/clear` | Delete this chat's messages (asks you to confirm) |  |
| `/help` | Keys, commands and tips |  |
| `/quit` | Leave BuildCLI | `Ctrl+C` |

### Environment

`OLLAMA_HOST` (default `http://localhost:11434`), `OPENAI_BASE_URL`, the API-key variables of the providers
(`OPENROUTER_API_KEY`, `OPENAI_API_KEY`, ...; never printed, and a set one wins over a key saved with `provider login` or in the app),
`BUILDCLI_HOME` (default `~/.buildcli`), `BUILDCLI_MOUSE=0` (turn the mouse off), `BUILDCLI_STORAGE=sqlite|h2|memory` (the state database; overrides the setting).
