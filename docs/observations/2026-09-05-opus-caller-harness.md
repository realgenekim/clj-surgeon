# The Opus caller harness — Claude Opus as a caller in Astra's frozen fanout cell

Written 2026-09-05T01:04:32Z by forge-anvil, on `bridge/opus-caller-harness` (tip at writing: `8f78c348c833cc20a5cba5111f8364a8eb61c75c`).
**Preparation only. No arm has been launched, no model has been called, no runtime has
been spent.** Everything below describes an instrument that refuses to run until
`RUNTIME_ALLOWED=1` is set by the program owner.

## What this is, and what it is not

Astra's fanout cohort asks one question — *does the caller model change the native
versus `alias_migration` result on a 21-owner alias migration* — and answers it for
`gpt-5.6-sol` and `gpt-6-astra` through `codex exec`. This harness adds a third
caller, **Claude Opus through `claude -p` on the seat's subscription**, to the *same
cell*: same fixture at the same sha, same task prompts, same six-check acceptance
oracle, same proof obligations, same one-arm-at-a-time load-gated slot.

It is **not** a second experiment with its own fixture. Nothing under
`/var/tmp/forge/astra-program` is written, moved, or copied by anything here; his
frozen inputs are read by absolute path and every one of them is **sha-checked before
use**, so a harness run against a changed input refuses instead of producing a number.

## The frozen inputs, by path and sha

| what | path | sha / value |
|---|---|---|
| fixture repo | `/var/tmp/forge/astra-program/verified21/base-repo` | HEAD `92fdf5d1545af934ff14250d39cef41c400e5df8` (checked every run) |
| fixture provenance | his `verified21/base-preparation.json` | from `fanout-k1/repo-21` @ `65fe39a9…`, 21 owners, 100 namespaces |
| N prompt | `verified21/prompts/fanout-native.txt` | `409e71963a9522773ddf32f6dd923f9e6de4cbc5b4b84b84cddaeb05710efadd` |
| T prompt | `verified21/prompts/fanout-tool.txt` | `150d072777421ee1ca90da62690205f161d33e552f305e7320c48e86a3e4ce1f` |
| common prompt (base of cell O) | `verified21/prompts/fanout-common.txt` | `e5fafb6e1722272c18b72b5730f6bffbd3bf4b6bb35619b98df7fd803b85b6e9` |
| acceptance oracle | `astra-program/repo/bench/fanout/rescore-FAN.sh` | `97486d75ff5f051b831c7997452d31ca15560fc85476ae1d57e12c69b9560eaa` — **verified before every invocation; a mismatch is a refusal, not a warning** |
| oracle fixtures | `verified21/oracle/{manifest-21.edn,canonical-21}` | hashes in his `verified21/FROZEN.json` |
| his reference adapter | `astra-program/repo/bench/astra/adapter.py` | `8f6c909f…` (read as the model for this one; not invoked, not modified) |

## The exact CLI invocation

Composed per arm and written verbatim to `<arm>/command.txt`. From the T-cell dry run:

```
claude -p --model claude-opus-5 --dangerously-skip-permissions \
  --session-id <uuid chosen by the harness> \
  --output-format stream-json --verbose \
  --add-dir <arm>/wt \
  --mcp-config <arm>/mcp.json --strict-mcp-config \
  --disallowedTools Task Skill ToolSearch WebFetch WebSearch SendMessage ListAgents \
      Monitor TaskOutput TaskStop NotebookEdit Artifact EnterWorktree ExitWorktree Workflow
```

* **cwd is the clone**, and the prompt arrives on **stdin**. Both are load-bearing:
  `claude -p … <text>` as a trailing positional exits 1 ("Input must be provided
  either through stdin or as a prompt argument"), and `--add-dir` grants *access*,
  not a working directory — the task's `src/` and `bin/fan-test` are relative paths.
  (Measured in the earlier E-CALLER cohort, `/home/forge/tmp/arms/ecaller`.)
* **The MCP server is attached on the command line, per arm**, never through a
  config file someone could forget to revert:
  `{"mcpServers":{"clj-surgeon":{"type":"http","url":"…"}}}` written to
  `<arm>/mcp.json` and passed with `--mcp-config`, plus `--strict-mcp-config` so
  the seat's own MCP servers cannot leak into a measured arm. Cell N gets neither
  flag and is *refused* if an MCP URL is supplied at all.
* **The URL must be loopback HTTP on 8340–8379**, this cohort's own band. It is not
  Astra's 8300–8339 and it is not any of 7888 / 7890 / 7894 / 7895 / 8171 / 8173–8175;
  the harness never contacts any of those, and contacts nothing at all until
  `RUNTIME_ALLOWED=1`.
* `--dangerously-skip-permissions` is required, not cosmetic: measured in the E-CALLER
  cohort, without a bypass every `Edit` returns `permission_denied` and the file is
  byte-unchanged — a silent zero on the primary meter.
* `--disallowedTools` removes `Task` above all. A subagent's writes never appear in
  this run's transcript, so a delegated write would be an **unmeasured** write. The
  same list is applied identically to N, T and O.

## The attribution path, end to end

This is the piece that has no counterpart in the codex adapter and had to be built.

1. **The session identity is chosen, not discovered.** The harness generates a UUID and
   passes `--session-id`. Nothing has to be scraped out of a log to learn which session
   ran.
2. **The CLI writes its transcript to
   `~/.claude/projects/<escaped-cwd>/<session-id>.jsonl`**, where the escaping replaces
   every non-alphanumeric character of the absolute cwd with `-` (verified against the
   seat's existing project directories; e.g. `/var/tmp/forge/opus-arms/opus-N-900/wt` →
   `-var-tmp-forge-opus-arms-opus-N-900-wt`). If that exact path is absent the harness
   falls back to a *bounded* `find … -maxdepth 2 -name "<sid>.jsonl"` under the projects
   root — never a `/home/forge` scan.
3. **The run is bound to that file by path and sha256**, both recorded in
   `<arm>/arm.json` after the caller exits.
4. **Model id and every tool call come out of that file**, into `<arm>/calls.json`:
   per call the tool `name`, the call `id`, a sha256 **digest of the arguments**, and
   the timestamp, plus a `by_name` histogram. Arguments are digested rather than
   copied so a large edit payload does not become a second copy of the diff.
5. **A second witness, and the disagreement is the signal.** The captured
   `--output-format stream-json` stdout (`<arm>/run.log`) is parsed independently for
   the same quantities, including its `init` record's resolved model, session id and
   attached MCP servers. `attribution.json` records `sources.agree_models` and
   `sources.agree_tool_call_count`; a disagreement is recorded, never averaged away.
6. **Four typed `:unverified` outcomes**, each exiting 3 with the reason named: no
   readable transcript; unreadable transcript lines; transcript session ids that are
   not exactly the requested one; a model set that does not match the requested model.
   The caller's own summary is never a counting or correctness authority for anything.

Correctness is never read from the transcript at all. It comes from Astra's oracle,
run read-only against the clone **after** the diff is staged, with its verdict line
copied verbatim into `arm.json` and its full output kept in `oracle.log`.

## What differs from Astra's codex adapter — and why it is still the same discipline

| his adapter | this harness | why |
|---|---|---|
| `codex exec` with a pinned binary + vendor hash | `claude -p`, CLI version recorded per arm | different caller; that is the whole point of the flank |
| rollout at `$CODEX_HOME/…`, session bound via `session_meta` | session transcript at `~/.claude/projects/<escaped-cwd>/<sid>.jsonl`, session **chosen** via `--session-id` | the two CLIs keep their transcripts in different places and shapes |
| `--ignore-user-config --ignore-rules`; refuses a project `.codex` | `--strict-mcp-config` + an explicit `--disallowedTools` roster | the Claude equivalents of "no ambient configuration reaches the arm" |
| `taskset -c 12,13` CPU pinning, `-Xmx512m` | not pinned; one arm at a time through `slot -t` | CPU pinning is his cohort's frozen factor. **Opus arms are therefore not wall-comparable to his Sol/Astra arms**; they are comparable to *each other*, N against T, which is the only ratio this flank may quote. |
| his own `watch.py`/`score.py` meters | not used | those are frozen instruments of his cohort; reusing them would mean editing them for a second stream shape. The wall, load and call counts here are computed independently and the oracle is shared. |
| two models, six matched N/T pairs each | one model, 6 N + 6 T + 3 O (see the plan) | budget — stated in the plan file, not hidden |
| n/a | **cell O**, the tool merely available | the optional-adoption cell, run last, never a speed cell |

**The prompt composition, stated exactly.** Every cell is the frozen file *verbatim*,
followed by an identical caller stanza (`prompts/claude-caller-common.txt`: names the
caller's native tools, forbids delegating the write to a subagent), followed by the
cell stanza. T adds `prompts/T-tooling-claude.txt`, which only *names* the mandated
operation the way this caller sees it (`mcp__clj-surgeon__alias_migration`) plus the
server URL — it adds no obligation Astra's text does not already carry. O is built on
his **common** text (which has no TOOLING paragraph at all) plus
`prompts/O-tooling.txt`: the same server is attached, the tool exists, "you are not
asked or expected to use it." Measured composed sizes against the real frozen prompts:
N 2,811 B, T 3,478 B, O 3,053 B.

## The order (`calibrate.sh plan` → `plan-opus-cohort.txt`)

Block A: six native calibrations, `N-1 … N-6`, before any comparison, no favourable
run selected as the baseline. Block B: six tool arms, `T-1 … T-6`. Block D last:
three optional-adoption arms, `O-1 … O-3`. Every arm goes through `~/bin/slot -t`
(refuses above 1-minute load 10, honours the shared quiet window, one of the box-wide
slots shared with Astra). `calibrate.sh run` refuses without `RUNTIME_ALLOWED=1`.

**The one deviation from his pre-registration, stated plainly:** his design runs six
matched N/T *pairs* after calibration, costing six further native arms. Here Block A's
six natives serve as the native controls for Block B, and the later drift-protecting
natives are Block C (`N-7 … N-9`), **off unless `DRIFT=1`**. With `DRIFT=0` this
cohort is exploratory on drift and must say so; a speed claim needs `DRIFT=1` and the
extra three arms.

Astra's stopping rules are inherited verbatim into the plan file: a 900-second arm is a
failed task, not a missing observation; a failed arm is recorded and **not** replaced by
a rerun; an instrument-invalid run gets a fresh rep number; two identical tool refusals
stop Block B for a contract investigation.

## Headroom — what a cohort costs, and what is still unmeasured

**The seat's Claude weekly pool was 30% remaining at 21:53Z on 2026-09-04** (the
figure supplied with this task; it is the last observation, not a live reading — there
is no pool meter on this box, and by the meter rule it should be re-read at the
console before allocation). Every Opus arm spends that pool. The Surgeon usage watcher
in `/var/tmp/forge/usage-watch.log` measures *tool* calls, not the subscription pool,
and is not a substitute.

The dry run with the fake caller costs zero tokens by construction, so it cannot
produce a token figure. What it *does* pin exactly is the harness's own contribution:
the composed prompt (2.8–3.5 kB, ≈700–900 tokens) and a 90-file, 412 kB `src/` tree.
The rest is estimated from the **measured** `claude -p` arms of the earlier E-CALLER
cohort on this same fixture family (Sonnet, 2026-09-04):

| cell | measured there | tool calls | transcript bytes |
|---|---|---|---|
| N | `ecaller-N2-N-1` | 10 | 84,973 |
| T | `ecaller-T2-T-1` | 2 (one `alias_migration`) | 13,011 |

Estimate, with the arithmetic shown so it can be argued with: unique conversation
tokens ≈ bytes/4; turns ≈ tool calls + 2; billed input ≈ turns × (system+tools ≈ 12k +
half the conversation); output ≈ a third of the conversation; and a **2× factor for
Opus over Sonnet** on conversation volume.

| cell | est. tokens/arm | arms | est. total |
|---|---|---|---|
| N | ~415k | 6 | ~2.5M |
| T | ~65k (round to 100k) | 6 | ~0.6M |
| O | ~415k (assume it behaves like N) | 3 | ~1.25M |
| | | **15** | **≈ 4.3M tokens** |

**Every figure in that table is an estimate and none of it is a pool percentage.** The
token→pool conversion is exactly what nobody on this box can compute, so the
recommendation is procedural, not arithmetic: **run `N-1` alone, read the pool at the
console before and after, and let that one arm price the other fourteen.** If one
native arm costs more than ~2% of the remaining pool, the 15-arm cohort does not fit
in 30% and the budget must be cut before Block A finishes, not after.

## Evidence that the instrument works

`bash bench/opus-caller/test_run_opus_arm.sh` — **38 passed, 0 failed**, with no
model, no JVM and no server: a fake `claude` first on PATH writes a synthetic session
transcript and makes one real edit; a stub oracle stands in for `rescore-FAN.sh`. It
witnesses the reused-identity refusal (and that the existing arm is byte-untouched),
the fixture-sha refusal (and that nothing is created), the oracle-sha refusal, the
out-of-band MCP URL refusal, the native-arm-with-a-URL refusal, the
`RUNTIME_ALLOWED` refusal, the session binding, the model id and tool calls read out
of the transcript, the staged diff and untracked-file record, the guard on protected
bytes, the monotonic wall and load, and the oracle being invoked against the clone
with its verdict recorded verbatim.

A second dry run drove the same fake caller against the **real** frozen fixture and the
**real** frozen prompts (stub oracle only) as `opus-{N,T,O}-900`; those arm
directories carry a `DRY-RUN.txt` marker and rep 9xx is never a measured arm.

## Scratch

Arms live only under `/var/tmp/forge/opus-arms/<id>/`. The test's scratch lives only
under `/var/tmp/forge/opuscaller-fx` and is removed when the test suite is not
running. Nothing is written to `/tmp` (RAM tmpfs on this box) and nothing is written
under `/var/tmp/forge/astra-program`.
