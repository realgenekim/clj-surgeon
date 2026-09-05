# The Opus caller harness — Claude Opus as a caller in Astra's frozen fanout cell

Written 2026-09-05T01:04:32Z by forge-anvil, on `bridge/opus-caller-harness` (tip at writing: `8f78c348c833cc20a5cba5111f8364a8eb61c75c`).
**Preparation only. No arm has been launched, no model has been called, no runtime has
been spent.** Everything below describes an instrument that refuses to run until
`RUNTIME_ALLOWED=1` is set by the program owner.

> **Round two, 2026-09-05T01:18:02Z.** Astra rejected round one's roster. This document is
> amended in place; the superseded 15-arm proposal is preserved in
> "Superseded: the 15-arm proposal" at the end, because a roster that was argued and
> rejected is part of the record, not an embarrassment to delete. Six changes: the
> roster is **21 arms** with six *additional* natives in matched pairs; the caller,
> its server and any profile check run **pinned to cores 12,13 inside an owned quiet
> window**; his **server-lifecycle and measurement policy are imported and called**
> rather than re-implemented; the resolved model comes from the transcript and the
> **command alias is never the model claim**; and **N-1 alone is the instrument
> preflight** once runtime is allocated.
>
> **Round three, 2026-09-05T01:55:19Z. Astra's independent review returned NO-GO** — filed
> verbatim beside this file as `…-review-astra-NO-GO.md`. His four blockers and three
> follow-ups are closed below, each with a fail-first witness; his own `probes.py` was
> reproduced RED against the round-two tip and now returns GREEN. This document is
> amended in place. **Nothing here is a GO for a live cohort**; that remains a
> separately authorised preflight.

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
/usr/bin/taskset -c 12,13 \
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

**The command alias is never the model claim.** `--model claude-opus-5` is a *request*;
what a receipt may assert is `models_in_transcript`, read out of the session file the
CLI itself wrote, together with the session binding that proves the file belongs to this
run. `adapter-result.json`'s `resolved_model` therefore points at that field rather than
restating the flag, and a transcript whose model set does not match the request is a
typed `:unverified` (rc 3), not a footnote. This is the meter rule applied to identity:
the thing we asked for is not evidence of the thing that ran.

Correctness is never read from the transcript at all. It comes from Astra's oracle,
run read-only against the clone **after** the diff is staged, with its verdict line
copied verbatim into `arm.json` and its full output kept in `oracle.log`.

## What differs from Astra's codex adapter — and why it is still the same discipline

| his adapter | this harness | why |
|---|---|---|
| `codex exec` with a pinned binary + vendor hash | `claude -p`, CLI version recorded per arm | different caller; that is the whole point of the flank |
| rollout at `$CODEX_HOME/…`, session bound via `session_meta` | session transcript at `~/.claude/projects/<escaped-cwd>/<sid>.jsonl`, session **chosen** via `--session-id` | the two CLIs keep their transcripts in different places and shapes |
| `--ignore-user-config --ignore-rules`; refuses a project `.codex` | `--strict-mcp-config` + an explicit `--disallowedTools` roster | the Claude equivalents of "no ambient configuration reaches the arm" |
| `taskset -c 12,13` CPU pinning | **the same cores 12,13**, for the caller, its server and the task's own profile checks, inside an owned quiet window | round two: same-cores doctrine, so a cross-caller reader is looking at the same silicon. Even so this flank quotes **only its own N-against-T ratio**; a cross-caller ratio is not this cohort's to make. |
| `-Xmx512m` on the driver's JVM env | not set by the harness | the Claude caller is not a JVM; the task's own `bb`/`clojure` invocations come from the frozen prompt |
| his `validate_ready` / `pid_listens` / `snapshot` attestation | **imported from his `adapter.py` and called**, after verifying its sha; one narrow substitution (the port band) recorded in every receipt | round two: reuse a reviewed control, never re-implement it |
| his server start/stop, parent-owned, arm-attested | same split; `calibrate.sh` starts/stops (reusing his `stop-server.sh` and its pid+start-ticks+boot-id proof), the arm attests | his contract, unchanged |
| his `attest.json` / `adapter-result.json` fields | **carried field for field**, `timing` block entire; caller-specific keys renamed, unrun instruments present and null | a field under his name means the same quantity measured the same way |
| his `watch.py` / `score.py` stream scorers | not used | those parse a codex rollout. A Claude transcript is a different shape, so running them would mean *editing* a frozen instrument — the one thing reuse must not require. The quantities they produce are re-derived here under his field names, and the acceptance oracle is literally his. |
| two models, six matched N/T pairs each after six native calibrations | one model, **6 calibration natives + 6 matched pairs + 3 adoption = 21 arms** | round two: his roster, not a budget-trimmed one |
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

## The order (`calibrate.sh plan` → `plan-opus-cohort.txt`) — 21 arms

| block | arms | cells |
|---|---|---|
| A calibration | 6 | `N-1 … N-6` — six native migrations before any comparison |
| B matched pairs | 12 | `N-7 … N-12` with `T-1 … T-6`, **balanced interleave**: `N,T / T,N / N,T / T,N / N,T / T,N` |
| C adoption | 3 | `O-1 … O-3`, **last**, and never a speed cell |
| **total** | **21** | |

Block B's natives are **six more natives**. Block A is *not* reused as Block B's
control: they are the later natives that protect against drift after calibration,
exactly as Astra's pre-registration requires, and the pairs alternate which cell leads
so neither systematically occupies the warmer or cooler half of a pair.

**The first arm, `N-1`, is the instrument preflight, and it runs alone.** The 102
fake-caller tests prove the *harness*; they never call a model, never start a server,
never run the real oracle. `N-1` is the first evidence that a live Claude session
binds, that the transcript names a resolved model, and that the six checks run against
a real tree. `calibrate.sh run` **halts after it** unless
`OPUS_CONTINUE_AFTER_PREFLIGHT=1`; read its receipt and the pool meter before arm 2.

Astra's stopping rules are inherited verbatim into the plan file: a 900-second arm is a
failed task, not a missing observation; a failed arm is recorded and **not** replaced by
a rerun; an instrument-invalid run gets a fresh rep number; two identical tool refusals
stop Block B for a contract investigation; an arm that runs through load > 10 is
preserved as a contaminated observation and excluded from a clean-wall claim, never
deleted.

## Same cores, and an owned quiet window

Astra pins his Sol and Astra arms to cores **12,13**. This flank pins the same two, for
the caller, for its MCP server, and for any profile check the task itself runs
(`bin/fan-test` and `test/load_all.clj` are named with `taskset -c 12,13` inside the
frozen prompt's fan-proof profile, so they inherit it from the task, not from us). The
launched command line begins `/usr/bin/taskset -c 12,13 …` and the affinity is recorded
in every receipt; a malformed CPU list or a missing `taskset` is a refusal, not a
silently unpinned arm.

**The window is owned, not merely absent.** `calibrate.sh` creates
`/var/tmp/forge/quiet-window.md` with `set -o noclobber` as `owner=fable` for each
arm's duration and removes it afterwards, including on interrupt (`trap … EXIT INT
TERM`). `noclobber` is what makes it a *claim*: a peer's existing window is a refusal,
never an overwrite. And `run-opus-arm.sh` independently refuses unless an owned window
is present — so a hand-run arm cannot skip the doctrine, and neither can a future
script that forgets it.

## Reusing his policy instead of duplicating it

`bench/opus-caller/astra_policy.py` **imports his adapter as a module** —
`/var/tmp/forge/astra-program/repo/bench/astra/adapter.py`, sha
`8f6c909ffe25836a3599a2eec45f5da5a35d3fdc94541356c4778b440372b449`, **verified before
the import**, `main()` never reached — and calls his predicates directly:

* `validate_ready` — the whole server-ready attestation: healthz bound to the MCP port,
  expected `mcp_url` / `server_sha` / `project_root`, a positive integer pid, the
  healthz **response bytes** hashed and compared, and the functional-readiness fields
  (`ok`, `server`, `tool_runtime`, `tool_registry`).
* `pid_listens` — "this pid owns that listener", read out of `/proc`.
* `snapshot`, `digest`, `file_digest` — his hashing and his protected-tree inventory,
  including its symlink refusal.

**The one substitution, deliberate and narrow:** his `validate_url` requires ports
8300–8339, his cohort's band, which this flank is forbidden to contact at all. So the
module rebinds *only* `validate_url` to a band-substituted copy of his own predicate —
same scheme, same loopback host, same rejection of credentials/query/fragment,
band 8340–8379 — before calling `validate_ready`. Nothing on disk is modified; the
rebinding lives in one process, and every receipt records both his adapter's sha and
the substituted band, so a reader can see exactly what was and was not his.

**Server lifecycle follows his contract, which is a split one:** the *parent* owns
start and stop, the *arm* owns attestation. `calibrate.sh` starts one server per arm
from a pinned checkout, bound to that arm's worktree, on that arm's port, under the same
`taskset`; records `pid + start-ticks + boot id` so cleanup can prove authorship; waits
for healthz; and writes `ready.json` in **his field shape**. Stopping reuses his own
`bench/anvil-arms/stop-server.sh`, which signals only a process whose pid, start time
and boot id all still match. The arm then validates that evidence through his
`validate_ready`. Nothing here is a second implementation of a reviewed control.

**Measurement policy is carried under his field names**, which is the point:
`attest.json` is his `attest` record field for field (including
`correctness: "pending-independent-acceptance"`), and `adapter-result.json` is his
`run()` result field for field, `timing` block entire —
`adapter_start_monotonic_s`, `watch_start_monotonic_s`, `watch_end_monotonic_s`,
`preparation_wall_s`, `watch_subprocess_wall_s`, `adapter_load_start`,
`watch_load_start`, `watch_load_end`, `lock_wait_included`, `adapter_wall_s`,
`adapter_load_end`, and `adapter_wall_scope` spelled out as
`"prepare-through-freeze-and-attestation; excludes scorer"`. A field carried under his
name means the **same quantity measured the same way**. Where a quantity cannot be the
same for a Claude caller it is **renamed, never silently redefined**:
`codex_version` / `codex_sha256` / `codex_vendor_*` become
`caller_version` / `caller_path` / `caller_sha256`. And the keys naming instruments
this flank does not run — `watch_sha256`, `score_sha256`, `make_targets_sha256` — are
**present and null**, so their absence is a statement rather than an oversight.

## Same base, same prompts, same oracle, same proof obligations

Unchanged from round one and re-affirmed here: the same fixture at
`92fdf5d1545af934ff14250d39cef41c400e5df8`, his prompt files verbatim, his
`rescore-FAN.sh` sha-checked before every invocation, and therefore **the same six
proof obligations the prompt itself names** — exact changed file set, form-tree
equality modulo whitespace with protected syntax intact, protected literal hashes, 100
namespaces loading, 21 behavioural tests with zero failures, and no residual reference
or alias collision. The frozen `verified21` prompts additionally name the fan-proof
profile's two commands (`taskset -c 12,13 bb test/load_all.clj` and
`taskset -c 12,13 bash bin/fan-test`) and forbid weakening verification in response to
a refusal; that text reaches the Opus caller unmodified. Canonical byte identity is
carried as `canonical_src_match`, his additional **diagnostic** — never the acceptance.

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
| N | ~415k | **12** (6 calibration + 6 pair) | ~5.0M |
| T | ~65k, rounded to 100k | 6 | ~0.6M |
| O | ~415k (assume it behaves like N) | 3 | ~1.25M |
| | | **21** | **≈ 6.8M tokens** |

**Every figure in that table is an estimate and none of it is a pool percentage.** The
token→pool conversion is exactly what nobody on this box can compute, so the
recommendation is procedural, not arithmetic: **`N-1` is the preflight, it runs alone,
and it prices the other twenty.** Read the console meter before and after it. If one
native arm costs more than ~1.4% of the remaining pool, 21 arms do not fit in 30% — and
that must be discovered at arm 1, not at arm 15. `calibrate.sh run` halts after `N-1`
by default precisely so that reading has to happen.

## Evidence that the instrument works

`bash bench/opus-caller/test_run_opus_arm.sh` — **102 passed, 0 failed**, with no
model, no JVM and no server: a fake `claude` first on PATH writes a synthetic session
transcript and makes one real edit; a stub oracle stands in for `rescore-FAN.sh`. It
witnesses the reused-identity refusal (and that the existing arm is byte-untouched),
the fixture-sha refusal (and that nothing is created), the oracle-sha refusal, the
out-of-band MCP URL refusal, the native-arm-with-a-URL refusal, the
`RUNTIME_ALLOWED` refusal, the session binding, the model id and tool calls read out
of the transcript, the staged diff and untracked-file record, the guard on protected
bytes, the monotonic wall and load, and the oracle being invoked against the clone
with its verdict recorded verbatim.

Dry runs drove the same fake caller against the **real** frozen fixture and the
**real** frozen prompts (stub oracle only): `opus-{N,T,O}-900` in round one and
`opus-{N,T}-901` in round two, the latter confirming the `taskset -c 12,13` line, the
owned-window requirement, the stub-attested server evidence, and a complete
`adapter-result.json` `timing` block. Those arm directories carry a `DRY-RUN.txt`
marker; **rep 9xx is never a measured arm.**

**What these tests are not.** They are 102 witnesses about a harness, run with a fake
caller, a stub oracle and a stub attestation. They are not live readiness. Nothing here
has yet proved that a real `claude -p` session binds, that the resolved model is what
was asked for, that a real MCP server passes his `validate_ready`, or that the six
checks pass on a tree an Opus caller actually edited. That is `N-1`'s job.

## Scratch

Arms live only under `/var/tmp/forge/opus-arms/<id>/`. The test's scratch lives only
under `/var/tmp/forge/opuscaller-fx` and is removed when the test suite is not
running. Nothing is written to `/tmp` (RAM tmpfs on this box) and nothing is written
under `/var/tmp/forge/astra-program`.

## Superseded: the 15-arm proposal (round one, rejected)

Round one proposed **15 arms**: `N-1 … N-6` as calibration, `T-1 … T-6` as the
comparison, `O-1 … O-3` last, with Block A's six natives serving as Block B's native
controls and the later drift-protecting natives held behind a `DRIFT=1` switch that was
off by default. The stated reason was budget: the seat's Claude weekly pool.

**Astra rejected it, and he was right.** Reusing the calibration natives as the pair
controls is exactly the drift exposure his pre-registration exists to close — the
natives that matter are the ones run *contemporaneously with* the tool arms, after any
warm-up or scheduler drift has had time to appear. A switch that defaults a control off
is not a control; it is a control-shaped option, and options default to whatever is
cheapest at 2am. The saving was six arms; the cost was the only comparison the cohort
is for.

Two further round-one positions also fell, and both are worth naming because each was a
plausible-sounding shortcut:

* **"CPU pinning is his cohort's frozen factor, so this flank need not pin."** True as
  far as it goes and wrong in effect: leaving the Opus arms unpinned meant they were not
  comparable to *anything*, including each other, whenever the box was busy. The fix is
  cheaper than the argument — pin the same two cores.
* **"His `watch.py`/`score.py` parse a codex rollout, so this flank computes its own
  quantities."** The parsers genuinely cannot be reused. But that was allowed to justify
  a parallel *policy*, which is a different thing: the attestation predicates, the
  server-lifecycle contract and the receipt field names were all reusable and are now
  reused. The rule this cohort takes away: **a parser that cannot be shared is not a
  licence to re-decide the policy it implements.**

The 15-arm plan is preserved here because a roster that was argued and rejected is part
of the record. It is not the plan; `plan-opus-cohort.txt` is, and it says 21.

## Round three — closing Astra's NO-GO

His review is filed verbatim at
`docs/observations/2026-09-05-opus-caller-harness-review-astra-NO-GO.md`. His probe
apparatus (`probes.py` + fake fixtures) was **copied into this seat's scratch** and run
there — only the two path constants changed, nothing was written under
`/var/tmp/forge/astra-program`. It was run **RED first**, against the round-two tip, and
reproduced all four findings exactly.

| his probe | RED (round two) | GREEN (now) |
|---|---|---|
| `wrong-model` | `shell_rc 0`, `valid_measurement false` | **`shell_rc 3`**, `valid_measurement false` |
| `oracle-failure` | `shell_rc 0`, `correctness not-accepted` | **`shell_rc 4`**, `correctness not-accepted` |
| `parent-precreated-tool-arm` | `shell_rc 2` — the tool path could not reach its arm | `shell_rc 2` — **and that is now the right answer** (below) |
| `native-isolation-and-diff` | `strict_mcp_config false`, `explicit_mcp_config false`, `new_file_in_diff false`, `cached_diff_bytes 0` | **all true; `cached_diff_bytes 518`** |

The suite grew from 66 to **102 witnesses, 0 failures**.

### Blocker 1 — ownership and preparation order

Round two had `calibrate.sh` create the arm directory and start the server against
`A/wt` *before that clone existed*, and the arm then refused the directory the parent
had just made. The tool path was structurally unable to reach its own arm, and the
fake suite never noticed because it never exercised the calibrate→arm transition.

`run-opus-arm.sh` now has **two phases**. `prepare` is the arm's own job: it makes the
directory and the clone and refuses an existing arm. `launch` attests and runs, and
refuses unless `prepared.json` exists and `arm.json` does not. `calibrate.sh` runs
**prepare → start_server (against the clone that now exists) → launch → stop_server**,
and `start_server` itself refuses if `A/wt` is missing.

**His probe's `shell_rc 2` is now the correct answer, not the defect.** A parent that
pre-creates an arm directory *should* be refused; what changed is that the
orchestration no longer does it. Test block 11 witnesses the real transition: prepare
exits 0 and the clone exists; launch then exits 0 and the arm reaches identity
`opus-T-20`; `launch` without `prepare` is refused naming the missing preparation; a
second `launch` of the same arm is refused naming the reused identity.

### Blocker 2 — a spawn that is armed only after health polling

`SERVER_STARTED=1` now happens **immediately after the fork**, on the line after the
`pid start-ticks boot-id` record is written. Every failure between fork and readiness —
a port that never answers, a missing `ready.edn`, a failed ready-write — is a refusal
*with the server still owned and stopped*. The ready-write is checked (`[ -s ready.json ]`)
rather than masked by the next assignment. `stop_server` escalates: TERM, wait up to
30 s, KILL, then re-check and **print a SURVIVOR line** if the pid is still alive, and
report any descendants. A survivor is loud; it is not an assumption.

### Blocker 3 — native MCP absence is now explicit, and checked

`--mcp-config` and `--strict-mcp-config` are passed for **every** cell. A native arm's
config is an explicitly empty map, `{"mcpServers":{}}`, recorded in the receipt as
`mcp_config_mode: "explicit-empty"`. Absence is then **verified**: attribution counts
tool calls whose name begins `mcp__`, and for a native arm any such call is a terminal
`:unverified`. A fake caller that reaches `mcp__clj-surgeon__alias_migration` from a
native arm exits 3 with the refusal naming the call.

**Honest limit, his words kept:** strict MCP config establishes *MCP* isolation. It does
not establish that no user or project settings or instructions reach the session. The
`--disallowedTools` roster narrows the tool surface; it is not a claim about everything
the CLI reads. The doc no longer says "no ambient configuration reaches every arm".

### Blocker 4 — terminal outcomes propagate

`run-opus-arm.sh` exit codes are now outcomes: **0** accepted · **2** refused ·
**3** attribution `:unverified` · **4** the oracle did not accept · **5** the caller
failed. `calibrate.sh` **halts the cohort** on any non-zero arm, prints which outcome it
was, and says where the preserved observation is — preserving a failed observation and
silently continuing an invalid cohort are different things. The documented
two-tool-refusals rule is now **implemented**, not just written in the plan header.

### Imported-policy coverage — the gap he named

He was right that a field-compatible ready JSON is not lifecycle parity, and that narrow
helper imports must not be called policy parity. `astra_policy.py` now offers
`attest-server`, which adds three checks his `prepare` makes and round two omitted:

* **the server's own `ready.edn`** is read and cross-checked against the launcher's
  `ready.json` (worktree and port must appear in it), and its sha is recorded. A label
  the launcher wrote is not evidence about the server.
* **pid birth** — the recorded `pid + start-ticks + boot-id` must still describe the
  live process. A pid alone is not identity; start-ticks repeat across reboots, so the
  boot id travels with them.
* **the actual server checkout HEAD**, read from the server's own cwd and compared with
  the expected sha — his `prepare` does this and the round-two wrapper did not.

`guard.py` now takes the protected inventory with **his `snapshot`** (bytes *and* mode,
symlinks refused) instead of `find`+`sha256sum`, and the selected verification profile
`.clj-surgeon.edn` is **inside** the protected set — in round two an arm could have
rewritten its own verification profile without the guard noticing.

**Immutable inputs are bound by sha in every receipt**, not merely selected by path: the
frozen cell prompt (against its pinned hash, *before* composition), the composed prompt,
the oracle executable, the oracle **manifest**, a digest over the whole **canonical**
tree, and the verification profile. All six travel in `arm.json.immutable_inputs` and
`attest.json`.

### Diff, identity and measurement semantics

* **The diff is now actually staged.** `git add -- .` then
  `git diff --cached --binary <base>`, so new files are inside the patch and it is a
  complete replay artifact. The untracked inventory is taken *before* staging, so both
  facts survive. (His probe: `cached_diff_bytes` 0 → **518**, `new_file_in_diff` false →
  **true**.)
* **Model identity is exact.** The transcript must name **exactly one** model; that id
  is the `resolved_model` and it must answer the requested alias. Two models is a
  refusal. A transcript/stream disagreement about the model is now **terminal**, not a
  recorded curiosity. `adapter-result.resolved_model` is an **actual id**, not a prose
  pointer, with `requested_model_alias` beside it; `attest.json` carries
  `model_requested` and `model_resolved` separately.
* **Load is sampled, not sampled twice.** A background sampler writes
  `load.jsonl` every 5 s with a phase label (`driver`, `freeze`, `attribution`,
  `acceptance`). The receipt carries per-phase maxima and classifies
  `contaminated_driver` / `contaminated_acceptance` against the ceiling — and an
  **unsampled phase is `null`, never "clean"**.
* **The wall means its label.** Round two computed `driver_end - adapter_start` and
  called it prepare-through-attestation, which was false. `adapter_wall_s` now ends at
  the attestation boundary, `adapter_load_end` is read at that same instant, and the
  scope string says `"…excludes the acceptance oracle"`. `acceptance_wall_s` and
  `verified_completion_wall_s` are separate measured fields, and
  `monotonic_source` states that `/proc/uptime` is **not** interchangeable with the
  shared adapter's `time.monotonic()` — no shared precision or calibration threshold is
  imported without an explicit comparison policy.

### What remains outside a fake caller's reach — stated plainly

These 102 witnesses are preparation evidence. They are **not** GO, and the following are
untested by construction because no model, JVM or server ran:

1. **Live server readiness.** `astra_policy.attest-server` has never validated a real
   server: no healthz bytes, no real `ready.edn`, no live pid owning a listener, no real
   checkout HEAD. The fake suite substitutes a stub validator, so it proves the harness
   *calls* the predicate and refuses when it says no — not that the predicate passes a
   real server. A malformed-ready live probe has not been made.
2. **Exact model binding.** No real transcript has been read. The uniqueness and
   prefix checks are exercised only against synthetic transcripts.
3. **Real acceptance.** `rescore-FAN.sh` has never run here: no `bb`, no `bin/fan-test`,
   no 100-namespace load. Every "6/6" in this document came from a stub.
4. **Server lifecycle under stress.** Escalation and the survivor/descendant reporting
   are exercised by reading the code, not by killing a real JVM.
5. **Contamination instrumentation at real load.** The sampler is proven to sample; it
   has never watched a real arm.
6. **Everything cross-caller.** No wall figure exists, and none may be quoted.

`N-1`, run alone under a separate authorisation, is what converts items 1–5 from
argument into evidence.
