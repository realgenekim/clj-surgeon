# Typed MCP change entrance

**Status:** Implemented experiment; recovery-round revalidation passed

**Motivating evidence:**

- [The intent compiler won its first matched race](../observations/2026-08-06-captains-log-the-intent-compiler-won-its-first-matched-race.md)
- [A representative reroll turned the race into a tie](../observations/2026-08-06-captains-log-a-representative-reroll-turned-the-race-into-a-tie.md)
- [To beat `apply_patch`, become a native tool](../observations/2026-08-06-captains-log-to-beat-apply-patch-become-a-native-tool.md)

## Outcome

Expose the shipped structural transaction compiler as one typed local MCP tool:

```text
apply_clojure_changes(files, owners, exact targets, replacements, counts)
  -> one compact verified receipt
```

A clean Codex caller with a complete structural decision must be able
to mutate and verify it in one typed tool action. It must not read a skill,
construct EDN, quote source through a shell, create a manifest, choose a receipt
path, or run a duplicate source read or diff after success.

The existing transaction compiler remains the authority. The MCP server loads
that kernel once and calls it in-process; it is not a second compiler and not a
new edit language. A subprocess adapter may exist as a contract oracle, but it
is not the performance path.

## Reference architecture verdict

Use `../itrev-mcp-server` as the local reference implementation. Reuse these
proven patterns:

- `io.github.bhauman/clojure-mcp` as a pinned library;
- one server with custom tool maps;
- pure tool schema and parameter normalization separate from I/O;
- callback-level handler tests;
- no protocol output on stdout except MCP messages;
- JSON-RPC `initialize`, `tools/list`, and `tools/call` smoke tests;
- an embedded development nREPL whose redefined handler Vars take effect on
  the next MCP call without reconnecting the client;
- explicit Codex and Claude registration commands.

Do not copy its corpus, database, source watcher, dynamic tool list, embedding
dependencies, or telemetry daemon. Those solve different problems. Copy the
embedded nREPL/hot-handler seam because it materially shortens frontier
development; disable it explicitly during clean benchmarks so it cannot hide
startup cost or change the caller surface.

The reference is a protocol model, not a performance assumption. The first
cold stdio experiment showed that Codex can choose native tools before a JVM
server becomes available. The primary product route is therefore one
loopback-only persistent Streamable HTTP server. Stdio remains a protocol
smoke-test and fallback entrance.

## Primary hypothesis and kill gate

The current binary is not slower than native patching at the action boundary:
recent medians were 569 ms for Surgeon and 594 ms for native patch actions. The
remaining cost is model ceremony around the CLI.

The typed entrance should remove two model boundaries:

```text
CLI
read skill -> compose/escape EDN -> :change! -> duplicate diff -> answer

MCP
apply_clojure_changes -> answer
```

The MCP entrance passed the keep gate. Four counterbalanced correct runs of the frozen
`decision-batch-edit` capsule:

- finished at a 24.530-second median, versus 43.190 seconds for native and
  36.396 seconds for the current CLI-and-skill route;
- use one source-bearing action;
- perform zero source reads and zero failed mutations;
- create no temporary manifest;
- emit no more source than the CLI transaction;
- preserve every existing parse, refusal, rollback, and inverse guarantee.

The improvement was 18.660 seconds versus native and 11.866 seconds versus the
current CLI. All assisted runs were exact and verified.

Record server initialization, namespace-load time, and tool latency separately,
but judge complete task wall. The normal product route is a persistent HTTP
server shared by fresh local agent sessions. Report both cold readiness and
warm task wall. Do not prewarm one treatment lane while charging another for
initialization.

Metadata alone produced zero MCP calls in four no-hint runs. A one-sentence
project `AGENTS.md` rule produced 4 / 4 adoption, exact results, and a
27.432-second median without loading the skill. Activation and mechanism remain
separate reported gates.

## Bitter-Lesson boundary

MCP changes transport, not judgment.

- The model chooses files, owners, targets, replacements, and counts.
- The existing compiler finds exact syntax and proves the future state.
- The adapter performs only schema validation, JSON-to-EDN translation,
  project-root confinement, one in-process kernel call, and receipt
  normalization.
- The tool never infers scope, guesses counts, expands dependencies, chooses an
  architecture, or repairs a refused plan.

Use native patching for one unique prose or textual edit. `apply_clojure_changes` is for a
complete structural decision where owner scope, repeated syntax, transaction
atomicity, parsing, or inverse recovery removes real work.

## Public contract

### Server

The write experiment exposes exactly one tool named
`apply_clojure_changes`. It has no resources, prompts, read tools, shell tool,
or hidden default tools. A later read experiment can add one separate
`inspect_clojure` tool without changing this mutation contract.

Server instructions must fit the important rule in their first 512 characters:

> Use `apply_clojure_changes` only when the files, named owner forms, exact before forms,
> exact replacements, and positive counts are already known. It applies one
> guarded structural transaction and returns complete mutation verification.
> After `verification_complete: true`, do not reread or diff unless the user
> explicitly requested aggregate review. Prefer native patching for prose or
> one unique text edit.

The server working directory is the project root. All source paths are relative
to it. Absolute paths, `..` traversal, and paths whose real target escapes the
root refuse before the CLI runs.

### Tool input

The JSON shape is intentionally narrower than the full EDN transaction schema:

```json
{
  "changes": [
    {
      "id": "body-class",
      "files": ["src/ui.clj"],
      "forms": ["shell", "reader"],
      "find": ":body",
      "replace": ":body.page",
      "expect": {
        "matches": 2,
        "each_form": 1
      }
    }
  ],
  "expect": {
    "changes": 1,
    "edits": 2,
    "files": 1
  }
}
```

Required fields:

- top-level `changes` and `expect`;
- per change: `id`, non-empty `files`, non-empty `forms`, `find`, `replace`,
  and `expect.matches`;
- aggregate positive `changes`, `edits`, and `files`.

Optional distribution guards are positive `expect.each_form` and
`expect.each_file`. Unknown keys refuse. Empty arrays, duplicate IDs, blank
strings, non-integer counts, and non-positive counts refuse.

The adapter translates one change into the existing kernel shape:

```clojure
{:id :body-class
 :in ["src/ui.clj"]
 :forms [shell reader]
 :find ":body"
 :do [:replace ":body.page"]
 :expect {:matches 2 :each-form 1}}
```

It calls `intent-transaction/execute-change!` in-process and supplies a
generated receipt path outside the source tree. The caller cannot supply
commands, environment variables, receipt paths, raw EDN, or arbitrary CLI
flags.

### Successful result

Success is compact JSON data, not source or a diff:

```json
{
  "ok": true,
  "operation": "apply_clojure_changes",
  "committed": true,
  "change_count": 6,
  "match_count": 6,
  "changed_file_count": 2,
  "verification_complete": true,
  "verified": {
    "whole_files": true,
    "file_count": 2,
    "read_back_hashes": {
      "src/a.clj": "...",
      "src/b.clj": "..."
    }
  },
  "undo_receipt": "/tmp/.../receipt.edn",
  "next_action": "none"
}
```

Absolute source paths from the CLI are normalized back to project-relative
paths. Success returns no source bodies and no unified diff.

### Refusal and transport failure

A kernel refusal remains structured and is returned as an MCP tool error:

```json
{
  "ok": false,
  "error_type": "change-distribution-mismatch",
  "error": "...",
  "source_unchanged": true,
  "remedy": "Correct the declared scope or count and call apply_clojure_changes once."
}
```

Preserve stable kernel fields rather than flattening them into prose. A
non-EDN stdout payload, zero exit with an error receipt, nonzero exit with a
success receipt, missing receipt publication after reported commit, or process
launch failure becomes a distinct adapter error. Stderr is diagnostic evidence
and never protocol stdout.

## Safety invariants

- MCP cannot widen the existing CLI's mutation capability.
- The project root and executable are server configuration, never tool input.
- Every source path is relative, canonicalized, and contained by the project
  root before execution.
- The adapter runs one fixed executable vector without a shell.
- JSON strings are data and never interpolated into a command line.
- Refusal leaves every source byte unchanged and publishes no inverse receipt.
- Success requires CLI success, `:committed true`, complete read-back proof,
  and an existing inverse receipt.
- Receipt normalization cannot remove or reinterpret kernel refusal fields.
- Nothing writes to stdout except the MCP protocol after server startup.
- The server exposes no read, shell, evaluation, prompt, or resource surface.

## Implementation shape

### Functional core

Add a Babashka-compatible namespace for public pure functions:

- `validate-tool-params`;
- `tool-params->transaction`;
- `normalize-success-receipt`;
- `normalize-refusal`;
- `classify-kernel-result`.

These functions take maps and strings and return data. They do not touch files,
spawn processes, or call MCP callbacks.

### Imperative shell

Add thin JVM namespaces for:

- canonical root/path confinement;
- generated temporary receipt directory;
- one in-process call to `intent-transaction/execute-change!`;
- MCP callback success/error handling;
- persistent HTTP and stdio server startup;
- loopback binding, origin filtering, health, and readiness;
- an optional embedded development nREPL whose handler Vars remain live.

The production adapter never shells to the CLI. A focused subprocess oracle may
compare the same normalized request with the CLI during tests, using fixed argv
and stdin, so semantic drift is detected without putting process startup on the
product route.

### Repository surfaces

- pin `clojure-mcp`, `rewrite-clj`, SCI, nREPL, CIDER nREPL, and `slf4j-nop`
  under JVM aliases;
- add `make mcp-test`, `make mcp-smoke`, and `make mcp-serve`;
- add an explicit branch-local Codex development installer and removal target;
- add a development launcher that preserves the caller's working directory;
- add an MCP lane to the representative benchmark without exposing the skill;
- keep raw protocol transcripts and benchmark events out of Git.

Registration must follow current client contracts. Codex uses a Streamable
HTTP URL registered through `codex mcp add`. Stable `make install` does not
enable MCP. The explicit `make install-mcp-codex-dev` target creates
branch-coupled entrances, starts the local session-persistent service, and
registers the URL for this experiment.

## Contract-exhaustive test matrix

### Pure parameter and translation tests

- valid singleton and six-change/multi-file documents;
- optional `each_form`, `each_file`, and both together;
- missing top-level or per-change required keys;
- unknown keys at every level;
- empty changes, files, or forms;
- blank IDs, paths, owners, find, or replacement;
- duplicate change IDs;
- absolute paths, parent traversal, and normalized escape attempts;
- zero, negative, non-integer, and boolean counts;
- aggregate and per-change key spelling translation;
- punctuation-bearing Clojure owner names;
- exact preservation of `#()`, metadata, comments, commas, and Unicode inside
  `find` and `replace` strings.

### Pure result classification tests

- zero exit plus verified success;
- nonzero exit plus structured kernel refusal;
- zero exit plus `:error` receipt;
- nonzero exit plus apparent success;
- malformed, empty, or multiple stdout documents;
- committed result without read-back proof;
- committed result without published receipt;
- relative normalization of one and several source hashes;
- source hash outside the project root;
- preservation of stable refusal fields and remedies.

### Boundary tests

- direct-kernel and CLI-oracle results normalize to the same contract;
- working-directory confinement rejects absolute, `..`, and symlink escape;
- real copied decision-batch fixture commits exact accepted bytes;
- wrong owner distribution refuses with bytes unchanged and no receipt;
- generated receipt exists after success and supports real `:undo-change!`;
- callback uses MCP success on commit and MCP error on refusal;
- redefining the handler Var through the embedded nREPL changes the next call
  without restarting the MCP process;
- stdout armor survives an intentional stray `println`;
- stdio and HTTP `initialize`, `tools/list`, and `tools/call` expose only
  `apply_clojure_changes` in the write-only experiment;
- cold server readiness and warm tool latency are recorded.

### Clean-agent experiment

Give fresh callers only the tool schema and the frozen task prompt. Withhold the
skill, CLI help, implementation, plan, tests, and expected command. Assert:

- voluntary `apply_clojure_changes` use;
- one typed mutation call;
- no source read, shell command, manifest, help call, or post-success diff;
- exact accepted bytes and compact verified receipt;
- first-attempt success;
- unchanged repository outside declared targets.

The four-replicate native, CLI, assisted MCP, no-hint MCP, and project-rule MCP
lanes are complete. Incorrect runs remain in the correctness denominator and
never contribute to efficiency medians.

### Recovery-round elimination extension

A later four-replica MCP lane exposed one systematic regression. Every caller
used an obsolete string `owner` field, received a fail-closed refusal in about
10 ms, and then spent a 20.460-second median interval constructing the current
`forms` request. The successful six-edit transaction took 348.954 ms median.
The complete MCP turn was 50.619 seconds versus 47.215 seconds for native.

This extension tests whether contract congruence removes that recovery round.
It does not change the frozen task or infer a counterfactual by subtraction.

1. Update the direct-change description and example to name `id`, `files`,
   `forms`, `find`, `replace`, and `expect` exactly.
2. Validate the published example with the production validator.
3. Reject obsolete direct-change field names in contract tests.
4. Reload the live tool registry and inspect the published contract.
5. Run four clean MCP replicas and four fresh counterbalanced native replicas.
6. Record time to first mutation, direct tool time, refusal-to-retry time,
   success-to-final time, complete wall, actions, tokens, and correctness.

The extension passes only when MCP produces 4 / 4 exact results, accepts all
four first calls, uses one mutation action per run, removes discovery and
post-success source actions, improves the flawed MCP median by at least 10
seconds, and beats the fresh native median by at least 5 seconds.

If the first calls are correct but the wall gate fails, do not tune the
subsecond kernel. First identify whether request construction or final-response
generation dominates. Test a more compact request representation only when
request construction dominates and retain it only after a measured five-second
gain with unchanged safety guarantees.

### Recovery-round result

The corrected contract passed the extension gate. Five of five MCP runs used
one mutation call and produced exact bytes. Four of five native controls were
valid and correct; the invalid control is retained in the correctness record.

| Route | Median wall | Correct efficiency runs |
|---|---:|---:|
| One-shot MCP | **27.976 s** | 5 / 5 |
| Fresh native control | 68.932 s | 4 / 5 |

The MCP route removed the 20.460-second median recovery interval, beat native
by 40.956 seconds, and reduced complete wall time by 59.4%. This establishes a
2.46x median advantage for the frozen complete-decision task. It does not yet
establish the same advantage for exploratory read-decide-edit work.

## Documentation and release checklist

- Update `docs/vision.md` with the benchmark tie, transport bottleneck, typed
  entrance experiment, and explicit native-patch home turf.
- Document server startup, schema, registration, approval mode, refusal, and
  removal in `README.md` and `make help`.
- Add a focused MCP README only if the main README becomes harder to scan.
- Update `CHANGELOG.md` when the experiment passes its mechanism gate.
- Keep activation evidence explicit. Tool metadata alone did not cause
  adoption; one project rule did. Do not hide that distinction inside a large
  skill.
- Record startup, mechanism, clean-caller, and benchmark findings in a new
  Captain's Log.

## Verification gates

- Standard Clojure Style on every changed Clojure file;
- pure MCP contract tests under the normal Babashka suite;
- focused JVM MCP tests;
- stdio protocol smoke with exact one-tool registry;
- existing 533-test repository suite with no weakened assertion;
- clj-kondo on changed source and tests;
- `git diff --check` and no tracked raw protocol/benchmark logs;
- one real copied-fixture call and inverse restoration;
- four clean Codex MCP callers in the counterbalanced paid gate;
- clean Claude mechanism acceptance before calling the feature complete.

## Explicit non-goals

- dependency, move, rename, extraction, or undo MCP tools;
- raw EDN or arbitrary transaction passthrough;
- captures, insertion, deletion, wrapping, or algebra expansion;
- OAuth, remote execution, multi-user service, or a permanent login daemon;
- source watcher, dynamic tools, or telemetry daemon;
- registration as a side effect of stable `make install`;
- replacing the CLI or making MCP required;
- claiming a speed win from mechanism success alone.

## Definition of done

One repo-owned persistent server exposes one typed
`apply_clojure_changes` tool that invokes the existing guarded transaction
kernel in-process, rejects path escape, commits the real six-edit fixture
exactly, returns terminal compact verification, and restores exact originals
through its inverse. Pure, JVM boundary, protocol, and complete repository
tests pass. Four clean assisted callers used it in one action without the
skill. A one-sentence project rule then produced 4 / 4 adoption. The
development server supports nREPL handler reload without making nREPL a client
requirement. The Captain's Log reports readiness, direct tool latency, and
complete task wall separately.
