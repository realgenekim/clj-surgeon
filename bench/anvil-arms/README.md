# `bench/anvil-arms` — the meter for the E3 / E6 arm cohorts

The apparatus that
`docs/observations/2026-09-04-e3-e6-prestaged.md` says must exist **before** the word
"go". Its decision tree opens with:

> Are `attest.sh`, `watch.py` and `score.py` written and smoke-tested (PF-5)?
> no → write them FIRST. **A cohort without a meter is a rumour.**

Nothing here has run a live arm. It has been proved end to end against a fake
driver (`make anvil-arms-self-test`, 55 assertions, ~11 s).

## The pieces

| file | spec | what it does |
|---|---|---|
| `attest.sh` + `_attest_write.py` | A.4 | writes `attest.json` / `attest.edn` **before** any driver starts; exits 2 with `ATTEST-MISMATCH` on any fail-closed condition |
| `watch.py` | A.10 | wraps the driver, tails its JSONL, writes `watch.jsonl` (one record per model return and per tool call) and `run.json`; aborts typed on zero returns |
| `score.py` | A.7 + A.10 | predicates over `rollout.jsonl` + `watch.jsonl` → `receipt.json` + `receipt.md`; computed counts only |
| `run-arm.sh` | B.6 §5 | one arm-run: attest → watch(driver) → freeze diff → score |
| `run-cohort.sh` | B.3 / C.4 | n slots per arm, serial, **mirrored order** |
| `stop-server.sh` | A.5 | stops **only** the server this arm-run spawned, by recorded pid **and** recorded start time |
| `_make_targets.py` | A.10 | resolves the worktree's Make targets with `make -n` at attest time, so a test runner behind `make verify` is metered as one |
| `fake-driver.sh` | PF-5 | a synthetic rollout, so the chain is provable with no arm-run budget |
| `self-test.sh` | PF-5 | 21 cases / 162 assertions; `make anvil-arms-self-test` |
| `prompts/` | B.4 | the four E3 arm prompts, extracted from the doc, hashed |

## The prompts are not typed by hand

`prompts/build-prompts.py` extracts the B.4 fenced blocks from the pre-registration
doc and, for the two rung-L arms, derives the prompt from
`docs/observations/2026-09-02-acid-rung-L/L-prompt-main.md` by the three edits B.4.4
names — each assertion fails loudly if the source text moved.
`build-prompts.py --check` rebuilds into a temp dir and diffs; the self-test runs it,
so prompt drift is a test failure and not a surprise on the morning of a cohort.

Installed hashes (`prompts/MANIFEST.sha256`):

```
9ab5267a77a2a02bb5bf4e4833d2bcbcb5055550f2830bdff092f316baa638f6  E3-P-N.md
6062621cb9600df4b20f2c8763051c151cda1df65832fcd523f7098e4d4c6f08  E3-P-T.md
e834f3d2ec28faa82430d8545489d0449f37d21093e479ae2e5b8b0dcfc2ff18  E3-L-N.md
6f81be22f3027cb7fa99eae4c86591ed14ea888c4be4c67017ed53ef473861f2  E3-L-T.md
```

E6's two prompts (C.5) are **not** installed: rung Lb has to be built first
(C.2/C.7 step 1, "deletions only"), and building it is part of E6's own pre-flight.

## Usage

```bash
ROOT=/home/forge/tmp/arms/e3
MAIN_SHA=$(git -C /home/forge/src/clj-surgeon rev-parse origin/main)

# one arm
bash bench/anvil-arms/run-arm.sh \
  --root "$ROOT" --exp e3 --rung P --arm T --slot 1 \
  --prompt bench/anvil-arms/prompts/E3-P-T.md --port 7907 \
  --expected-server-sha "$MAIN_SHA" --server-src "$ROOT/server-src" \
  --worktree-src /home/forge/acid/fanout/repo-21 --churn-band 47,71,27,41

# a cohort: serial, mirrored  N-1 T-1 T-2 N-2 N-3 T-3
bash bench/anvil-arms/run-cohort.sh \
  --root "$ROOT" --exp e3 --rung P --arms N,T --n 3 \
  --prompt-dir bench/anvil-arms/prompts --prompt-prefix E3-P --ports "T=7907" \
  --expected-server-sha "$MAIN_SHA" --server-src "$ROOT/server-src" \
  --worktree-src /home/forge/acid/fanout/repo-21
```

Exit codes worth knowing: `run-arm.sh` 2 = attestation refused, or `--root` outside
`/home/forge/tmp/arms` (no driver ran); `watch.py` 4 = zero returns, 5 = idle or wall
cap, 6 = incomplete run (a tool call whose result never arrived), 7 = the rollout
could not be bound to the driver's own announced session; `score.py` 2 = no
attestation or `attest_ok=false`, 3 = missing / empty / **invalid** rollout or watch
stream — **and no receipt is written, and any stale one is deleted**;
`run-cohort.sh` 64 = `--n` is not a positive integer, and it stops on the first
refused arm with a `COHORT-ABORT` line.

## Boundaries this code enforces, not merely documents

- A tool/free-choice arm's port must be one of `7907 7908 7909 7910`. `run-arm.sh`
  refuses anything else *before* anything is contacted, so 7888 / 7894 / 7895 / 7906
  are unreachable by construction, not by care.
- `ss -ltn` **lists**; it never connects. Ports outside the cohort range are recorded
  in `listeners_observed` and are never a reason to refuse a native arm — otherwise
  another seat's live server would make every native control impossible.
- Only a server pid this script wrote into `ready.edn` is ever signalled. No pid,
  no signal, and a warning instead.
- `diff.patch` is frozen with a plain `git diff <base>` (never a negative pathspec:
  `git add -A -- . ":!.cpcache"` is the shell trap that cost this program every FAN
  diff), and a non-zero rc writes `DIFF-FAILED rc=<n>` into `driver.log` rather than
  letting an empty diff pass for a clean one.

## Where the doc was ambiguous, and what was chosen

Six places. Each is reversible in one line.

**1. `:server-project-head` vs `:expected-server-sha` — the doc's refusal is
self-contradictory as written.** A.4 defines `:server-project-head` as
`git -C <ready-project-root> rev-parse HEAD` and then refuses when it "does not start
with `:expected-server-sha`". But A.5 binds `:project-dir` to the **arm's worktree**,
so that HEAD is the rung base (`ab267f9`, or the FAN tag), never the clj-surgeon main
sha the runner expects. Taken literally, every tool arm would refuse.
*Chosen:* capture **both** identities and check each against the thing it can actually
mean.
  - `server_project_head` = the ready-file project root's HEAD, checked against the
    arm's `worktree_head` (the served project must be *this* arm's worktree at the
    pinned base).
  - `server_sha` = the HEAD of `/proc/<port-pid>/cwd` — the server process's own
    working directory, i.e. the clj-surgeon checkout it is running — checked against
    `expected_server_sha`. `healthz` reports the *served* project and not the server's
    source (`mcp_http_server.clj`'s `readiness` map), so `/proc` is the only identity
    readable **from the running server itself**, which is what A.10 demands. If it is
    unreadable, `server_sha` is `"unverified"` and the arm refuses.

**2. "Arm N with any reachable Surgeon port → refuse."** On this box 7888, 7894, 7895
and 7906 always have listeners and are forbidden to contact, so the literal reading
refuses every native arm forever. *Chosen:* arm N refuses if it was handed an MCP url,
or if any port **this apparatus owns** (7907–7910) is listening — a stale arm server
is the contamination the rule is actually about. Every other listener is recorded in
`listeners_observed` and ignored. The proof string is written verbatim into
`mcp_absent_proof`, so a reader can see exactly what was and was not checked.

**3. B.4.4 says "exactly three edits", but two places mandate `TURNS:`.** Edit 1
removes `## Reporting` item 5; the Ground-rules bullet
("End your final message with a line of the exact form `TURNS: <n>`") is not named.
Leaving it would re-mandate the withdrawn null instrument in the same prompt that just
removed it. *Chosen:* rewrite that bullet to `TOOLCALLS: <n>` as part of edit 1, and
assert no `TURNS:` survives. Recorded here because it is one more edit than the doc
says.

**4. Where the RITUAL block goes.** "Immediately before `## Verify`" does not say
whether to add markdown separators. *Chosen:* the block verbatim with one blank line
either side and no `---` rule, so the inserted bytes are exactly A.8's.

**5. `attest.edn` vs `attest.json`.** A.3/A.4 name `attest.edn`; A.10's receipt is
JSON and the task's brief says `attest.json`. *Chosen:* write **both** from one source
of truth. `attest.json` is canonical (score.py reads it); `attest.edn` is the same map
rendered for a human or a Clojure reader.

**6. `watch.jsonl`'s one-record-per-call.** A.10's example shows a single `call`
record carrying `elapsed_ms` and `outcome`, which cannot be known until the tool
returns. *Chosen:* buffer the call and emit one consolidated record when its output
arrives; a call still open when the driver ends is emitted with
`outcome: "no-output"`. Records are therefore ordered by *completion*, and every one
carries `n` (return ordinal) and `seq` (call ordinal) so order is recoverable.

## What the scorer refuses to do

- Print a verdict word over a missing number. A missing or empty `rollout.jsonl`, or
  one with zero assistant returns, is **exit 3 with no receipt written** — not a zero
  row. (`verdict-label-was-a-noun`.)
- Read a stream it cannot read. Every non-empty line must be a well-formed JSON
  object, a file with no terminating newline is a truncated record, call ids must be
  unique with no output before its own call, and the watcher's `ms_since_start` must
  be non-decreasing with return/call ordinals dense from 1. A duplicated or reversed
  stream aborts instead of producing two "independent" witnesses that agree because
  they were derived from the same corrupted bytes.
- Score a run whose last action has no outcome. A tool call whose result never
  arrived is `incomplete-run`: typed, nonzero, no receipt.
- Leave the previous answer standing. Every abort **deletes** `receipt.json` and
  `receipt.md`; a refusal that leaves a stale receipt in the directory is not a
  refusal.
- Resolve a disagreement silently. Returns and tool calls are re-derived twice —
  once from the raw rollout, once from the watcher — and any difference is written
  into `meter.sources` and `notes`.
- Score a human judgement. A.6's `class`, `agent_visible` and
  `next_call_sent_verbatim` are `"unverified"` in every receipt: they are read by a
  person, not computed. `next_call_present`, `returns_to_recover` and
  `abandoned_route` **are** computed.
- Call a gate green from its absence. No `gate.json` → `gate.green` is
  `"unverified"` and a note says so.

## Still owed before "go" (not this apparatus's job)

PF-1 merge state · PF-2 the N=21 rung and its tag · PF-3 rung P's canonical churn
band · **PF-4 the G1 hand-drive of `alias_migration` / `ls-tree`** (non-negotiable,
and the cheapest thing in the pre-registration) · PF-6 `uptime`. PF-5 — the scorer
smoke — is what `make anvil-arms-self-test` discharges for the *predicates*; running
them once over a real PF-4 rollout is still the doc's instruction, and the fake driver
does not substitute for it.
