# Troubleshooting

Start with `buildcli doctor`: it checks Java, git, the state directory, your configuration and the model providers.

| Symptom | Cause and fix |
|---|---|
| `no model configured for agent 'x'` | No model is chosen for that agent and there is no default. Type `/connect` in the chat, set one in Settings (F2), or pass `--model provider:model`. |
| `Ollama is not reachable` | Start Ollama, or set `OLLAMA_HOST` if it runs elsewhere. Only needed for the `ollama` provider. |
| `model 'x' is not pulled` | `ollama pull x`. |
| Generation is extremely slow | Ollama's default of 16 threads was about 50x slower than 4 on a WSL2 machine. BuildCLI defaults to `--threads 4`; tune it to your physical cores. |
| The agent replies with nothing, loops, or announces an action without doing it | The model is too small for tool use. Use 7B or larger, or a hosted model ([findings](m2-real-model-findings.md)). |
| `The project's agent definitions were not trusted, so nothing was run` | A project agent is new or changed. Run with the TUI or `--approve ask` to review and approve it. `--approve none` and `writes` deliberately never trust. |
| Nothing is written in `--headless` mode | Without a terminal, approvals default to `none`. Use `--approve writes` for file writes, or `ask` on a terminal. |
| `The configuration has N problem(s)` | Every problem is listed with its file. Keys are validated strictly, so a typo is an error. See [agents and teams](reference/agents-and-teams.md). |
| `the state database is schema version N but this BuildCLI only understands up to M` | The database was written by a newer BuildCLI; upgrade. It was not modified. |
| `an argument contains a character that cmd.exe treats specially` (Windows) | Commands like `mvn` run through `cmd.exe`; arguments containing `& \| < > ^ %` or quotes are refused. Pass them another way. |
| The terminal looks garbled after a crash | Run `reset`, then use `--headless`. |
| A warning about native access on start | Use the installed launcher or the jar's manifest (`java -jar buildcli.jar`); on JDK 24+ without it pass `--enable-native-access=ALL-UNNAMED`. |
