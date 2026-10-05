# Changelog

All notable changes to BuildCLI. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased] - 1.0.0

BuildCLI is rebuilt from scratch as a local runtime for teams of AI agents. The previous CLI (0.14.0) remains on the
`legacy` branch and the `v0.14.0` tag. Design: `docs/rfc/0001-buildcli-1.0.md`.

### Fixed
- **The approval mode chosen in Settings applies at once.** It was only read at start, so choosing `auto` in F2 kept asking until the
  next restart. The setting is now called "Approval mode" (not "... at start").
- Choosing a chat from the list closes the settings / connect / info screen that was over it.
- Tab on a command only fills it in (`/diff `); Enter runs it. The `/` menu says so.
- An empty project opens on the welcome screen, not on "Connect a model" (there is no agent to connect one to).
- **Tab did nothing**: the toolkit used it to move focus and never passed it on, so it did not complete the `/` and `@` menus. It does now.
- **Up/Down through what you sent** also works after a restart: the history is rebuilt from the saved messages.
- **Asking an agent to talk to a teammate** no longer fills your private chat with the two of them: the request and the teammate's work
  show in a chat between the two (read-only for you, like the ones made by `send_message`). In a group it stays in the group.
- In groups and in chats between agents, every message says who wrote it.
- Notes and the private chats between agents could not be opened: the screen went back to the default chat on the next frame.

### Changed
- **Git runs through JGit, inside BuildCLI.** `git_read`, `git_commit` and `/diff`, `/status`, `/log` no longer start a `git`
  program: nothing needs to be installed, and nothing a repository configures (external diff, textconv, fsmonitor) can run.
  A diff never shows files git does not track. Commit hooks still run. A hook has no time limit now, and `buildcli doctor` no
  longer looks for `git`.
- **The screen is drawn when something changes, not on a timer.** The chat session tells the screen about every change
  (message, typing, presence, question) and requests that arrive together become one frame, at most one every 16 ms. The only
  clock left runs while something moves (a spinner, a toast) and stops by itself. Idle CPU is about the same as before
  (0.5% in a 20 s measurement on the JVM); the difference is that nothing wakes up to look for changes.
- `StateStore`, `SettingsView` and `ConnectView` were split (`StateDb`, `StateSchema`, `SettingsDialogs`, `KeyEntry`,
  `ProviderStatus`) and `ChatScreen` lost its viewer, its answer dialog and its `/` and `@` menu (`ViewerPane`, `PendingDialog`,
  `SlashMenu`; 1800 -> 1300 lines); behaviour is the same.

### Removed
- **Teams are gone**: groups (chats with members and admins) are the only way to put agents together. Removed `buildcli team`, team
  files (`.buildcli/teams/`), `run --team` (use `--group`) and the per-team model and limits; models are chosen per agent in the
  settings, and limits are the defaults.
- The M0 spike: the hidden `bench` and `demo` commands, the `eval` package and its findings document (replaced by `docs/storage.md`).

### Added
- **Code blocks are coloured by language** (TamboUI's highlighter: Java, JSON, YAML, shell, Python, JS/TS, SQL, Go, Rust, XML/HTML),
  with a palette per theme, in answers while they stream and in files opened with `/open`. Unknown languages stay plain. A code
  block is a rectangle: its lines are padded to the widest one instead of ending where each line of text ends.
- **Your own colours in `~/.buildcli/theme.css`** (`$bg: #101820;`, `$light-bg: …`, `$agent-1: …`), read with TamboUI's CSS
  parser. A mistake in the file is shown on the first screen and skipped.
- **English and Portuguese** (`ui.language`: auto, en, pt). `I18n.t("English text")` looks the Portuguese up in `i18n/pt.tsv` and falls back to English, so a missing text still shows; `I18nTest` checks that settings and commands are translated and that placeholders match. Tests run with `-Duser.language=en`.
- **Mark messages** (right click, or Alt+M): copy, forward to another chat, delete (the agents forget them too).
- **Rounded bubbles** and the **BuildCLI logo's colours** (navy and cream) instead of WhatsApp's greens.
- An agent can write to **you** in its own private chat (`send_message` to `user`), also when a teammate asked it to.
- **Modes** `manual` / `edits` / `auto` (Shift+Tab, `/mode`, the pill in the chat header; starting mode in Settings): which questions are answered for you. `trust` is never skipped.
- **Drafts** like a messenger: unsent text waits in its chat and shows as "Draft: ..." in the chat list.
- **Context for groups** (optional): a text and files the group's agents read (`/context`, group info screen).
- **About you** (`/me`, Settings): your name, what the agents should know about you and how to deal with you; agents stop saying "the user".
- **Sample teams for non-technical work**: writing desk (writer, editor, researcher) and office (assistant, analyst, planner) via `/samples`; neutral defaults and ideas when the folder is not a git repository.
- **`/editagent`** in AgentFather: change an existing agent's role, capabilities, instructions, write folders and the commands it may
  run without asking, each after a yes. Folders outside the project, `.buildcli/` and `.git/` are refused.
- The chat list scrolls (mouse wheel, and it follows the chat you pick) and says how many chats are hidden.
- The message history (Up/Down) is per chat.
- Menu and list text that does not fit ends in "…" instead of being cut off.
- **Agents can ask you a question** (`ask_user`): a form with numbered options and "Something else…" (or just a text box for an open
  question). Up/Down or a number chooses, Enter confirms, Esc skips and the agent decides by itself.
- The chat list is ordered by the latest message.
- **AgentFather**: a built-in contact (always in the chat list, no model needed) that creates agents step by step (`/newagent`),
  lists them (`/agents`), deletes them after a yes (`/deleteagent`) and adds the sample agents (`/samples`). The empty chat offers it.
- `scripts/tmux-screenshot.py` turns a colour capture of a terminal into the README screenshot.
- **Search your chats** (`Ctrl+K`, `/chats`, or the box above the chat list): by name, role or what was said; starts a chat with an agent that has none.
- **State database you can choose**: Settings > General > State database (or `BUILDCLI_STORAGE`): `sqlite` (default), `h2`
  (file; one BuildCLI at a time) or `memory` (fast, forgotten on exit). Writes to the event log, tasks and chat history are now
  applied in the background in batches (SQLite: about 7 000 to 90 000 events/s in a synthetic test); reads always see what was written before them.
- **Agents that speak for you**: capability `chat.post` gives an agent a `send_message` tool to write in a group it is in, or
  privately to a teammate. A private chat between two agents appears in the chat list (`ana ↔ bruno`), read-only for you.
- **Who may contact whom**: `/reach @bruno @ana off` takes contact away (the agent is told it cannot, and the teammate is
  not on its list); `/reach` shows it; kept per project in `reach.yaml` in the state directory.
- **Notes to yourself**: a private chat that no agent reads.
- The sample team is now the maintainers' (wheslley, breno, matheus, dumildes) in the group `maintainers`; their files are drafts.
- Creating an agent: Esc goes back one step, and capabilities are a list to tick.
- Chat TUI in the style of WhatsApp Web with the behaviour of ChatGPT: chat list, bubbles, ticks, typing, streamed
  markdown replies, `/` commands and `@` mentions, git diff and file viewer, mouse, image and audio attachments.
- Agents as people: an inbox and one virtual thread each; one conversation at a time per agent, in parallel across
  agents; presence (reading, thinking, typing, busy in another chat); read receipts.
- Groups with members and admins, direct chats, agents talking to each other by @mention (paused after N messages),
  a warning for agents that are in no chat. Groups are kept per project outside the project tree.
- Settings screen (F2): providers, a model per agent from the providers' live model lists, agents, themes (dark,
  light, contrast), Enter behaviour; saved for this project or for all projects, outside the project tree.
- Providers: OpenRouter, DeepSeek, Kimi/Moonshot, Groq, Mistral, Gemini, Together, LM Studio and your own
  OpenAI-compatible endpoints; `buildcli provider list|add|test`. Provider errors are one readable line; rate limits
  and server errors are retried.
- **Runtime**: agents, teams, tasks and handoffs; sequential execution; retry three times then escalate to the user; run
  limits (steps, depth, handoffs, tokens).
- **Configuration**: agents (Markdown/YAML) and teams under `.buildcli/` and `~/.buildcli`, `AGENTS.md` as context, strict
  schema-1 validation that reports every problem at once.
- **Tools**: `read_file`, `list_files`, `write_file` (approval with a unified diff), `search`, `git_read`, `git_commit`,
  `run_command` (argv allow list, timeout, scrubbed environment).
- **Security**: workspace confinement including symlinks; secret redaction of tool output, events and stored state; tool
  output delimited as untrusted data; approval of project-defined agents bound to a digest of their files; terminal escape
  and bidirectional-text sanitization; Windows `cmd.exe` argument guard; bounded file reads; YAML size limit. See
  `docs/security-model.md` for what is **not** protected.
- **Models**: LangChain4j behind a port; Ollama and any OpenAI-compatible endpoint; per-agent model routing; streaming.
- **State**: SQLite (WAL) with versioned migrations for runs, tasks, events and per-agent usage, kept outside the project.
- **Terminal UI** (TamboUI) and CLI: `init`, `agent`, `team`, `run`, `runs`, `task`, `usage`, `doctor`, `config`.
- **Distribution**: single jar, `install.sh` / `install.ps1` with SHA-256 verification, CI on Linux, macOS and Windows.

- **Connect a model inside the app** (`/connect`, opened by itself on first run): choose a provider, see what it still needs,
  **type or paste its API key there** (masked, checked with the provider before it is saved, kept only in your own
  BuildCLI folder with owner-only permissions), pick a model, send a test message, and save it as the default for this
  project or for all projects. Also `buildcli provider login/logout`. Environment variables still work and win.
  Tools can never read or write BuildCLI's own folder, so an agent cannot see the keys.
- **See and undo what an agent changed**: a card after each run that wrote files (`ana changed 2 files +12 -3`) with Review
  and Undo (`/review`, `/undo`); undo shows the diff first and leaves alone any file changed since. Content of files that
  may hold secrets is never written to the database.
- **"Always here" on approvals** (key `A`): approve a kind of request from one agent in one chat for the rest of the session;
  never for commits; `/revoke` takes it back.
- **Copy and find**: a copy button on every code block and `/copy` (system clipboard tool, OSC 52 as a fallback); Ctrl+F or
  `/find` searches the chat with highlights.
- **Window title and bell** when an agent needs you or a long job ends (setting to turn the sound off).
- **Windows**: the mouse works (clicks, wheel, drag) and each frame is drawn in one write (scroll frames 48 ms -> 1 ms).
- `--help` on every subcommand.
- **Native executable (spike)**: `scripts/build-native.sh` builds a GraalVM native executable: starts in 40-230 ms and the terminal UI
  uses about half the memory of the JVM (44 MB idle, 61 MB after a real model reply, against 99 and 127 MB). Image thumbnails and audio
  waveforms are not available in it. See `docs/native-image.md`.
- **`/model`**: `/model provider:model` sets the default model of this project, `/model @agent provider:model` one agent's,
  and `/model` alone opens the Models settings.
- **First run in two clicks**: an empty chat now offers "Add the sample team" (ana, bruno, carla in a group `backend`,
  the same files as `buildcli init`), next to "Create your own agent" and "Connect a model and provider". Also in Settings > Agents.

### Fixed
- `ChatSession.pending()` could throw while the screen was drawn if a question was answered at that moment.
- A letter pressed while a dialog was open was typed into the message box, after which Y and N stopped working.
- Installer launchers failed on Java 21 (`-Xlog:aot` exists only since JDK 24).
- Tests that waited on fixed sleeps made CI on macOS fail; a global JUnit timeout now makes any hang fail with its stack.

### Known issues
- The TUI has only been driven on Linux; macOS and Windows terminals are unverified.
- No model smaller than 7B has been shown reliable for tool use, and no larger model has been benchmarked yet
  (`docs/m2-real-model-findings.md`). The "demo scenario against a real local model" criterion is open.
- The command allow list is not a sandbox; there is no network permission until the sandbox planned for 1.2.
