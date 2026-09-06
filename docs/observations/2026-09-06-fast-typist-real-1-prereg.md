# Preregistration — real-1 A/B (written BEFORE any A/B call was made)

Written: see the commit/mtime of this file. Nothing below is edited after the
first arm runs; observed numbers go in the report, not in here.

## What is being compared

One frozen dossier (`bin/typist-dossier-real-1.md`, sha256 pinned in every
receipt), one mission (`real-1`), one judging path (apply -> gate ->
independent acceptance), two arms, interleaved N,F,N,F,N,F,N,F:

- **arm N** — `--arm N --runs 1` — one careful author, `codex exec -m
  gpt-5.6-sol`, medium reasoning, cold each time. 4 rounds.
- **arm F** — `--arm F --k 5 --provider openrouter` with
  `TYPIST_OPENROUTER_ORDER=Cerebras` — five parallel fast typists,
  `openai/gpt-oss-120b`, no upstream fallback. 4 rounds.

Everything is charged: process start (T0) through apply, the JVM/bb gate, and
acceptance. `first_verified_s` is wall from process start to the first
candidate that passes apply AND gate AND acceptance.

## The mission, in one line

Rename `finding-identity` -> `finding-fingerprint` and the private `field` ->
`finding-field` across all 10 call sites in the REAL
`src/clj_surgeon/diagnostic_delta.clj` at f2efc87c, without touching the local
`let` binding named `identity` or the docstring sentence that uses the word.

## Measured gate cost (step 1, already run, quiet window held)

bb gate on `clj-surgeon.diagnostic-delta-test`: 0.062 / 0.066 s (two cold runs,
1-min load 1.26). JVM gate, same namespace, full `:clj-surgeon/mcp-test`
classpath: 0.744 / 0.788 s (load 0.93).

## Predictions

P1. **The gate will NOT dominate.** The brief expected it to; with the cheapest
    honest gate (bb, 0.07 s) it cannot. Predicted judging overhead per
    candidate (fresh copy + git init + gate + acceptance) < 1.0 s, so < 5 s for
    a k=5 round — a minority of either arm's wall.
P2. **Arm N median first_verified_s: 45-120 s.** Cold `codex exec` startup plus
    a careful read of a 94-line real file.
P3. **Arm F median first_verified_s: 8-25 s.** Cerebras-served gpt-oss-120b,
    k=5 in parallel.
P4. **F is faster than N on the median by at least 3x.**
P5. **N verifies in 4/4 rounds. F verifies in at least 3/4 rounds** (>=1 of the
    5 candidates verified).
P6. **The dominant failure signature on arm F will be the two traps** — a
    candidate that renames the local `identity` binding or the docstring
    sentence — showing up as `accept_ok=false` with a TRAP assertion, or as a
    `gate_ok=false` unresolved-symbol from a missed call site.

## Falsifier

If arm F's median `first_verified_s` is NOT lower than arm N's, or if F
verifies in <=1 of 4 rounds, then the fast-typist advantage measured on the
five-file toy fixtures does not carry to a real repository file, and the
prototype's headline ratio is a fixture artifact. Either outcome is reported
as-is; the mission is not retuned after seeing results.

## Budget

20 OpenRouter calls (4 rounds x k=5), 4 codex exec invocations. Cap 40.

---

# Preregistration — whole-file arm (written BEFORE any whole-file A/B call)

Written 2026-09-06, after the real-1 diff-form result (0/20 arm-F candidates
applied; the loss was whitespace fidelity in CONTEXT lines, not the change) and
BEFORE any whole-file network call. Nothing below is edited after the first
whole-file arm runs.

## What is being compared

The SAME mission, the SAME preimage, the SAME bb gate, the SAME independent
acceptance witness, and a dossier derived from the frozen one by changing
exactly three spans (the statement of what to emit, the proof-gate sentence
about how the candidate reaches the tree, and THE RULE) —
`bin/typist-dossier-wholefile-gen` is the derivation and the diff is three
hunks. Only the EDIT FORM differs:

- **diff** (already run, ab-1): a unified diff.
- **whole-file** (this arm): the COMPLETE new contents of every authorized file
  inside one ```clojure fence each, judged additionally by a CHANGED-REGION
  BUDGET of 3x the known-good change size, applied BEFORE the gate.
  real-1: budget 78 changed lines (known-good 26). fanout: budget 99 (33).

Arm F only, `--k 5 --provider openrouter`, `TYPIST_OPENROUTER_ORDER=Cerebras`,
`openai/gpt-oss-120b`, no upstream fallback. real-1 x4 rounds (20 candidates),
fanout x2 rounds (10 candidates).

## Predictions (recorded in the ab-1 result doc before this file was written)

W1. **real-1 whole-file: >= 40% of single candidates verified** (>= 8/20).
W2. **real-1 whole-file verifies in at least 3 of 4 rounds.**
W3. **fanout whole-file: >= 30% of single candidates verified** (>= 3/10), and
    at least 1 of 2 rounds verified. Fanout is harder (three files, two traps,
    a new arity) and the whole-file form multiplies the bytes that must be
    reproduced exactly by three, so the per-candidate rate is predicted lower
    than real-1's.
W4. **The budget refuses at least one candidate across the 30**, or refuses
    none because the model stayed inside the change — either is reported; the
    budget exists so that a rewrite cannot buy a pass silently.
W5. **The dominant whole-file failure signature will NOT be `apply_ok=false`**
    (there is no context to mis-render). It will be `gate_ok=false` or
    `accept_ok=false`: a dropped or reworded docstring, a reformatted form, or
    one of the two traps renamed.

## Falsifier

**If fewer than 20% of real-1 whole-file candidates verify (< 4/20), the
whole-file form does not rescue the fast typist on a real repository file**, and
the real-1 loss is about the model's fidelity to a real file, not about the
patch format. Reported as-is; the mission is not retuned after seeing results.

## Budget

30 OpenRouter calls (4x5 real-1 + 2x5 fanout). Session cap 40; 0 spent so far
today on this arm.

---

# gate-dominated arms — real-2 / real-2j (written BEFORE any call in this batch)

Written 2026-09-06 by fable, after real-1 whole-file (16/20 candidates, median
first_verified 1.89 s, cold Sol 3/4 at 29.7 s) and BEFORE any real-2 network
call. Nothing below is edited after the first arm runs.

## What changes, and what does not

ONE thing changes: the proof profile. Same preimage bytes (the same two real
files at f2efc87c), same change, same two do-not-change traps, same authorized
file, same independent acceptance witness, same whole-file edit form, same
changed-region budget. The dossier is derived from the frozen real-1 dossier by
`bin/typist-dossier-real-2-gen`, which replaces EXACTLY ONE line: the shell
command under `## Proof gate` (diff confirmed: 1 line, both forms).

- **real-1** (already run): `bb -cp src:test`, one focused namespace. 0.06 s.
- **real-2**: the SAME namespace on the JVM, `:clj-surgeon/mcp-test` classpath
  baked into the profile (a candidate workspace is a standalone two-file
  project and cannot resolve deps itself). Measured ~0.75 s cold in the survey.
- **real-2j**: four real test namespaces of this repository —
  `diagnostic-delta-test`, `mcp-change-buffer-test`, `mcp-compact-location-test`,
  `ns-isolation-test`. `mcp-change-buffer` requires `clj-surgeon.diagnostic-delta`
  directly, so the set genuinely exercises the change. ~6.5 s in the fake-arm
  ratchet.

  Two namespaces were measured and REJECTED, and the reasons are recorded
  because they bound how heavy an honest gate can be here:
  `analyzer-contract-test` (5.1 s alone) asserts on clojure.test's global report
  counters and FAILS when run after another namespace — order-dependent, not a
  gate; `admit-patch-test` (17.3 s alone, which would have hit the 20 s anchor)
  reads repository-relative fixtures and errors 10x when cwd is a candidate
  workspace. `mcp-alias-migration-test` is 69.7 s — too slow for the budget.
  **So the honest heavy gate available in this repository for a standalone
  two-file candidate workspace is ~6.5 s, not 20 s.** That is reported as the
  finding it is; nothing was retuned to reach a number.

## Serial or parallel?

**The runner gates candidates SERIALLY.** `judge_candidates` is a plain `for`
loop over candidates: fresh copy -> apply -> gate -> acceptance, one at a time.
Only the MODEL calls are parallel (k=5 threads). So a k=5 round pays
`model_wall + 5 x judge_wall`, and `first_verified_s` for candidate i pays the
judging of every candidate before it. Predictions below assume that.

## Predictions

G1. **real-2 (0.75 s gate), whole-file k=5, first-verified median: 4-9 s.**
    real-1's whole-file median was 1.89 s against a 0.06 s gate; each candidate
    now costs ~0.75 s more, and the first verified candidate is rarely c0.
G2. **real-2j (~6.5 s gate), whole-file k=5, first-verified median: 12-30 s.**
G3. **Ratio vs cold Sol.** Sol on real-1 was 29.7 s median with a 0.06 s gate;
    under the heavy gate Sol pays ONE candidate's gate, the typist pays up to
    five. Predicted ratio F:N — **~10x at the 0.75 s gate** (Sol ~30.5 s vs F
    ~3 s... call it 5-15x), and **falling to 1.5-3x at the ~6.5 s gate**
    (Sol ~36 s vs F ~15-25 s). The recorded expectation in the brief was ~10x
    at 0.75 s and 2-3x at a 20 s gate; the gate available is 6.5 s, so the
    bottom end is predicted at 1.5-3x rather than 2-3x.
G4. **The advantage shrinks monotonically with gate cost**, and shrinks FASTER
    than the gate grows, because k=5 multiplies the gate for F and not for N.
G5. **Verification rate is unchanged by the gate.** Same model, same prompt
    modulo one line: real-2 predicted >= 3 of 4 rounds verified, real-2j >= 2
    of 3, per-candidate rate in the 50-90% band real-1 whole-file showed.

## Falsifier

If F's median first_verified_s under the ~6.5 s gate is not materially higher
than under the 0.75 s gate, the serial-gating model of the runner is wrong and
the decomposition must be re-derived from the receipts before any ratio is
quoted.

## Budget

Cap 30 OpenRouter calls: 4 rounds x k=5 (real-2) + 3 rounds x k=5 (real-2j) =
35 — OVER. Therefore real-2 runs 4 rounds and real-2j runs 3 rounds only if
under cap; the batch is planned at 30 and the 7th round is dropped if the cap
binds. Codex (Sol): 3 calls on real-2j, cap 4.

## warm native comparator (arm NW) — preregistered 2026-09-06T01:17:34Z, before any codex call

Astra's specification, verbatim: "six identical native variance controls before interleaved
comparisons, same frozen dossier/acceptance, warmed harness with fresh preimage per trial,
native free to use any script/batched patch. Freeze what warm means (resident process/context
and loaded source), record full start-to-independent-acceptance time and all failed attempts,
and retain" everything.

FROZEN DEFINITION OF WARM (agreed with Astra; corrected by Astra's 2026-09-06 protocol note
BEFORE any scored trial ran — no trial has run on a reused session, so nothing is void):
one orientation call `codex exec -m gpt-5.6-sol --skip-git-repo-check` in a scratch copy of the
real-1 preimage, prompt = the frozen dossier + "Read the file now and reply READY; do not edit
yet". That session holds the dossier and the source; it solves nothing. Per trial the session is
NOT resumed and evolved — it is FORKED (`codex exec fork <id> "<trial prompt>"`), so no trial
ever sees another trial's solution while the stateless typist sees none. Arm label:
**context-warm / process-cold** (:warm_kind "fork"). Workspace path fixed across trials so the
loaded context stays valid; preimage restored IN PLACE per trial (everything but .git deleted and
re-copied); the model's own in-place edit read back with `git diff`; then the runner's ordinary
gate + independent acceptance. Clock per trial = fork SPAWN → independent acceptance pass
(process start charged). Warm-up wall recorded as :warmup_wall_s and NOT charged to any trial.

PREDICTIONS (recorded now, no tuning after results):
- NW median first-verified on real-1 under the bb gate (0.06 s): **6–12 s** — Sol's model turn
  without the cold context load.
- F whole-file k=5 median stays ~**1.9 s** (A/B 2 measured 1.89 s).
- Warm ratio NW/F therefore lands **3–6x** (against 15.7x cold in A/B 2).
- NW verified rounds: ≥ 8/10 (cold Sol was 3/4 in A/B 1).
FALSIFIER: NW median under **3 s**. (If the fork replays orientation nearly free and the model
turn is that short, the "warm native is still several times slower" reading is wrong.)

PLAN: 6 NW variance controls (`--arm NW --runs 1` ×6, consecutive, same frozen session), then 4
interleaved rounds NW, F (F = `--arm F --k 5 --edit-form whole-file --provider openrouter
--dossier bin/typist-dossier-real-1-wholefile.md`, Cerebras pinned). All under one quiet window,
every timed command via `SLOT_OWNER=fable /home/forge/bin/slot -t`, 1-min load beside each timing.
Caps: codex ≤12 calls total (1 warm-up + 10 trials = 11), OpenRouter ≤20 (4 rounds x k=5 = 20).
Output: ab-4.log. Every attempt including failures is retained under /var/tmp/forge/typist-real-fx.
