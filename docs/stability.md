# Stability policy (proposed for 1.0, for maintainer review)

What a user can rely on across 1.x releases, and what may change. BuildCLI follows [Semantic Versioning](https://semver.org).

## Stable within 1.x

| Surface | Promise |
|---|---|
| **Agent file schema (`schema: 1`)** | Files that load today keep loading. New *optional* keys may be added. Removing or changing the meaning of a key requires `schema: 2`, and 1.x keeps reading schema 1. A newer schema than the running version is refused with a clear message, never guessed at. |
| **Permissions semantics** | A permission never becomes broader than documented. Security fixes may make enforcement *stricter* (for example, refusing a path that was wrongly allowed); that is not a breaking change. |
| **CLI commands, options and exit codes** | Documented commands and options keep working; `0` success, `1` the work failed or was aborted, `2` usage or configuration error. Breaking changes only in a new major version. |
| **State database** | Migrations are ordered and forward-only (`PRAGMA user_version`). A released migration is never edited. A database written by a newer version is refused and left untouched, so downgrading cannot corrupt it. |
| **Event type names** | Only added, never renamed or removed. Consumers must ignore types they do not know. |
| **`--json` output of `buildcli usage`** | Existing fields keep their names and meaning; fields may be added. |

## Not stable

- The **terminal UI** layout, colours and keys (the documented key bindings aside).
- The **text** of human-readable output and messages.
- **Hidden development commands** (`bench`, `demo`).
- **Prompts and tool descriptions** sent to models. They may change in any release, which can change how a given model
  behaves (small models are sensitive to this; see the [findings](m2-real-model-findings.md)). Pin your BuildCLI version
  when you depend on a particular model's behaviour.
- Tool names and arguments as seen by the model.
- The content of `~/.buildcli/trust.json`.

## Deprecation

A documented feature is deprecated for at least one minor release (with a warning) before removal in the next major.
