# M2: real-model findings (qwen2.5:3b)

**Status: the M2 acceptance criterion "the demo scenario runs against a real local model" is NOT met.** Everything else
in M2 is covered by deterministic tests; this page records what was tried against the only model available on the
development machine (`qwen2.5:3b`, CPU, Ollama 0.21.2), so the team can pick it up with a stronger model.

## What happened

The first prototype completed the scenario 5/5 with this model. After the M1/M2 changes the same scenario fails: after the
`write_file` result, **Bruno replies with an empty message** (no text, no tool call), before and after the runtime's
nudge, and the task is retried and escalated.

The HTTP bodies sent to Ollama were captured with a logging proxy and compared with the first prototype's jar's. The requests are
identical (same model, options, task prompt, conversation, tool-call and tool-result messages) except for two things
the new code adds: a paragraph in the system prompt about `<tool-output>` data, and a fourth tool (`list_files`).

## Experiments (temperature 0, same scenario)

| Variant | Result |
|---|---|
| First-prototype jar (3 tools, short prompt), run twice, once through the proxy | **OK**, 5,618 tokens, identical each time |
| Full M2 code (streaming) | fails: empty reply after the write |
| Full M2 code, `--no-stream` | fails the same way, so streaming is not the cause |
| Without the `<tool-output>` paragraph (keeps `list_files`) | fails |
| Without `list_files` (keeps the paragraph) | fails |
| Lead scripted to hand off the first prototype's exact objective text (removes the lead's wording as a variable) | fails |
| Stronger nudge that restates the objective | fails |
| Extra system instruction "never reply with an empty message" | fails |
| Tool result sent as a user message instead of a `tool` message | no longer empty, but the model **narrates** ("Now I will run cat...") and stops without calling the tool: a different failure |

(A direct probe of the Ollama API with hand-built messages returned empty for every variant, including the first prototype's
configuration, so it did not reproduce the framework's request format and proved nothing.)

## Interpretation

Each of the two differences removed alone still fails; removed together the request equals the first prototype's and passes. The most
economical reading is that **a 3B model is not robust to small changes in its prompt or tool list**: the first prototype's result was a
fragile configuration, not evidence of reliability (which was already flagged as "n=1" then). This
was not proven to be the only cause.

## Consequences

- Do not claim support for small local models yet. The RFC's open question "minimum supported local models" stays
  open; the working assumption should be 7B and above until measured.
- Model qualification needs more than one passing run. Use temperature above 0 and at least 10 runs:

  ```bash
  # measures the developer agent alone (the lead is scripted, removing one source of variance)
  java -jar target/buildcli.jar bench --runs 10 --temperature 0.5 -v \
      --fixed-objective "Create out/greeting.txt with content 'hello from bruno' and then run cat out/greeting.txt to verify." \
      --provider openai --url http://<host>:11434/v1 --model <7b-or-larger>
  ```

- The runtime guardrails (one nudge on an empty reply, handoff cap, retry that keeps the conversation) are kept: they
  recover some failures and bound the rest. They did not recover this one.
- Not adopted: sending tool results as user messages (changes the failure, does not fix it) and prompt additions
  (no measurable effect).
