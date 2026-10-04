# Tools

An agent asks for **capabilities**; the runtime resolves them to tools. A tool is only described to an agent that holds
its capability, and a call to any other tool is refused.

| Capability | Tool | What it does | Limits |
|---|---|---|---|
| `filesystem.read` | `read_file` | Reads a text file | Output capped at 4,000 characters; checked against the read globs |
| `filesystem.read` | `list_files` | Lists a directory (`/` marks directories) | Hides what the read globs exclude; 200 entries |
| `filesystem.write` | `write_file` | Creates or overwrites a file | Must match the write globs; **always shown to you as a unified diff and approved** |
| `search` | `search` | Case-insensitive text search, `path:line: text` | 30 matches; skips `.git`, `target`, `node_modules`, `build`, binaries and files over 1 MB; alphabetical order |
| `git.read` | `git_read` | `status`, `diff` (optionally of one path) or `log` (last 20) | Fixed argv; fsmonitor, external diff and textconv disabled |
| `git.commit` | `git_commit` | Commits the named paths with a message | **Always approved by you**, shown with the diff; the agent must be able to read every path; repository hooks run |
| `command.execute` | `run_command` | Runs an argv array in the workspace | Allow list by argv prefix, otherwise approval; timeout (default 30 s) kills the process tree; scrubbed environment; output capped |
| `agent.handoff` | `handoff` | Delegates a task to a teammate | The runtime validates the target, depth (default 3) and count per attempt (default 3) |
| `chat.post` | `send_message` | Writes as the agent in a group it belongs to, or privately to a teammate (a chat between the two agents that you can read but not write in) | Only because you asked; a teammate you took contact away from with `/reach` cannot be written to; replies between agents stop after the hop limit |
| (none: every agent) | `ask_user` | Asks you a question and waits: a form with options and "Something else…", or a text box | Only when the agent needs a decision it cannot make itself; Esc skips and it must decide alone |

Tool results are scrubbed of secrets. Results that carry outside content are delimited as `<tool-output tool="...">` data.
Refusals are returned to the model as text beginning `DENIED` or `ERROR` so it can adapt. See the
[security model](../security-model.md).
