# Agents and project context (schema 1)

BuildCLI reads its configuration from files you can commit with the project. Everything is validated strictly when
loaded: a typo in a key or a permission is an **error** (never silently ignored), and **all** problems are reported at
once.

## Where files live

| Path | What | Committed? |
|---|---|---|
| `AGENTS.md` (project root) | Project **context** for humans and agents: build commands, architecture, conventions. Read as information only; it can never change an agent's role or permissions. Truncated at 8,000 characters. | Yes |
| `.buildcli/agents/*.md`, `*.yaml` | Agent definitions of the project | Optional |
| `~/.buildcli/agents` | Global (per-user) agent definitions. `BUILDCLI_HOME` overrides `~/.buildcli`. | n/a |
| `~/.buildcli/projects/<id>/` | Operational state keyed by project: the database (runs, tasks, events, chat history), your groups (`chats.yaml`), who may contact whom (`reach.yaml`) and your settings. **Never inside the project tree.** | Never |

Project definitions override global ones with the same name. Definitions from a project are **untrusted until you approve them** (see the [security model](../security-model.md)). (`.agents/` compatibility is planned for 1.4.)

## Agent

Either a Markdown file with YAML front matter (the body is appended to `instructions`) or a plain YAML file.

```markdown
---
schema: 1
name: bruno
role: developer
instructions: |
  You implement tasks with your tools, then report what you did.
capabilities: [filesystem.read, filesystem.write, command.execute]
permissions:
  filesystem:
    read: ["**"]          # default: the whole workspace
    write: ["src/**"]     # default: nothing
  command:
    allow:                # argv arrays, never shell strings
      - ["mvn", "test"]
      - ["./mvnw", "-q", "verify"]
    timeout: 10m          # 500ms | 30s | 10m | 1h (default 30s)
---
Prefer small, focused changes.
```

| Key | Required | Notes |
|---|---|---|
| `schema` | yes | Must be `1`. |
| `name` | yes | `[a-z][a-z0-9_-]*` |
| `role` | yes | Free text (architect, developer, reviewer...). |
| `instructions` | no | Behaviour and constraints. |
| `capabilities` | no | See below. Unknown names are rejected. |
| `permissions` | no | `filesystem.read`, `filesystem.write` (glob lists), `command.allow`, `command.timeout`. |
| `description`, `personality`, `memory` | no | Accepted; `personality` is style only, `memory` arrives in 1.1. |

**Capabilities**: `filesystem.read`, `filesystem.write`, `search`, `git.read`, `git.commit`, `command.execute`, `agent.handoff`, `chat.post` (see [tools](tools.md)).
An agent asks for capabilities, never for tool names; the runtime resolves the tool.

Notes on permissions:

- `command.allow` entries match the **start** of the argv. `["mvn", "test"]` allows `mvn test -q` but not `mvn deploy`.
  A command that is not on the list needs your approval each time.
- An allowed command still runs project code (plugins, test code). The allow list reduces accidents; it is **not a
  sandbox**.
- `permissions.network` is **rejected** in 1.0: it cannot be enforced without the command sandbox planned for 1.2, and
  an unenforced permission would be misleading.

## Groups, models and limits are not agent files

A **group** is a chat you put agents in (members and admins): you create it in the chat (`/newgroup`, or the sample agents button) or
`buildcli init` makes one, and it is kept per project outside the project tree, so a cloned repository cannot define who talks to
whom. The **model** of each agent is a setting (Settings > Models, `/model`, or `--model` on the command line). Run **limits** are
fixed: 3 retries before escalating to you, 12 model calls per attempt, handoffs nested 3 deep, 3 handoffs per attempt and 30 000
tokens per task.

Providers: `ollama`, `openai` (any OpenAI-compatible endpoint), and the hosted ones listed by `buildcli provider list`.

## Versioning

Every file carries `schema`. A file with a newer schema than the running BuildCLI is refused with a clear message.
The state database is versioned separately (`PRAGMA user_version`); a database written by a newer BuildCLI is refused
and left untouched.
