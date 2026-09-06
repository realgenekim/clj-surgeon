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
