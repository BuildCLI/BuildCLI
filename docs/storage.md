# The state database

BuildCLI keeps runs, tasks, the append-only event log and the chat history in one database per project, under
`~/.buildcli/projects/<id>/` and never inside the project. You choose the engine in **Settings > General > State database**, or with
`BUILDCLI_STORAGE`, which wins over the setting:

| Engine | Where | Good for | Watch out |
|---|---|---|---|
| `sqlite` (default) | `state.db` | A stable file format you can inspect with `sqlite3`; readable from several terminals at once (`buildcli runs` while the chat is open) | Needs a native library per OS |
| `h2` | `state.mv.db` | Pure Java; a little faster | One BuildCLI at a time (a second one fails with a clear message); the file format changes across H2 major versions; not covered by the native-image metadata |
| `memory` | nothing on disk | Fast, throwaway sessions | Everything is gone when BuildCLI closes; other processes cannot see it |

Each engine keeps its own history: switching does not move it. The setting takes effect on the next start.

## Writes are batched

Writes return at once and one writer thread applies them in groups, one transaction each. What piles up while a group is being
committed becomes the next group, so a quiet database commits every write within a few milliseconds and a busy one amortises the
commit. Reads wait for what was written before them, so they never see an old state. A failed write is reported by the next write
and when the database closes. After a crash only what was still waiting in the queue (milliseconds of work) is lost. The queue holds
100 000 writes; when it is full, writers wait instead of memory growing.

## Measured

`LoadProbe` (a hand-run program under `src/test`): 1 000 virtual-thread writers, 200 000 events, 3 runs, Linux, JDK 25.

| | events per second |
|---|---|
| SQLite, one commit per event (before batching) | ~7 000 |
| SQLite, batched | ~90 000 (87 to 96 thousand) |
| H2 file | 90 000 to 115 000 |
| H2 file, batched | 107 000 to 128 000 |
| H2 memory, batched | 109 000 to 166 000 |

Batching is what matters (about 13 times); H2 on top adds 1.2 to 1.4 times. The H2 numbers use H2's default sync behaviour, which
may lose up to about a second of writes on a crash; an earlier measurement with H2's `WRITE_DELAY=0` (durable on every write) found
it level with SQLite (about 0.4 ms per event each). Never use SQLite's default sync mode for the event log: its fsync per insert was
about 40 times slower, which is why the database runs in WAL mode with `synchronous=NORMAL`.

A whole chat with 1 000 agents answering 5 messages each (a fake model that takes 200 ms): 8.8 s with SQLite writing one commit at a
time, 4.1 s with batching, 3.6 s with H2 and 2.9 s in memory. Model calls, not BuildCLI, are the real limit on how many agents
you can run.

## Why SQLite is the default

The event log is meant to outlive versions and to be inspected, and SQLite's file format is stable. Switching engines is cheap (the
store takes a JDBC URL), so this stays reversible.
