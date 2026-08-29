# Captain's Log: Location Tolerance Met a Different Schema Cliff

**Decision:** STOP after the frozen four-run pilot. Do not expand to BAAB and
do not claim a live performance win for injective compact-location
normalization.

**Date:** 2026-08-29 UTC

**Seat:** `dev-a` on `anvil-server`

The result is ugly and useful. The fresh model did not repeat the historical
location mistakes that motivated the candidate. All four first calls selected
the already-valid location spelling `within.namespace=true`. Instead, every
first call used `old`/`new` or `before`/`after` where the public schema requires
`from`/`to`.

The strict control recovered twice. The candidate recovered once and failed
once after changing `old`/`new` to the still-invalid `before`/`after`. The
candidate therefore missed every promotion gate: it was 1/2 correct, 0/2
first-call exact, and removed no refusal/recovery turn.

This is not evidence that source-proved location normalization is unsafe. Its
retained replay and permanent falsifiers remain green. It is evidence that the
fresh caller hit an earlier, orthogonal schema cliff, so the treatment never
had a causal opportunity to help.

## Frozen identities and admission

| Item | Exact identity |
|---|---|
| product control | `24056d28fc42f071fb8948bc339a03e716eac4ef` |
| product candidate | `4904d4ea52c1e1330bc9f8a04c8a5e393af9a758` |
| control harness | `026d0356de64d28faea7acb5c3064adc3f785322` |
| candidate harness | `9b92d252bac342a74e1d9928bff8e35d69a65fb1` |
| controller | `c0b77ab00f50bdce1aa849d0c2389c34898a6d9a` |
| shared derived `bench` tree | `bb24490bccec7e6c77198d93f8eec0cdf0fad694` |
| binary harness diff | `58b082b1daff3c66f4b401462598383f351461e5878081f692279c559dc76976` |
| task SHA-256 | `789809060a52d647197cf1fb5ade2cc0a76992209a0223991c7a51179f44d8e1` |
| capsule SHA-256 | `7d985f4d30acdf871f615b174e0f6c37338539253e6591cf898f96c26f39d4b9` |
| scorer SHA-256 | `ff2751e5c61c0eb8f6beebe7a2821d197973c356942d14942f0a4ce04cd2ce6b` |

Control harness depth was one commit. Candidate harness depth was two commits.
Explicit product and harness commit/tree admission passed for both. The two
actual Codex client-registry preflights each exposed exactly one tool,
`edit_clojure`, and produced no prompt or model events. Model, reasoning,
fixture, prompt suffix, scorer, harness bytes, and seat were identical.

## Immutable A/B/B/A result

| Run | Arm | Wall | Exact bytes | Meaning | First call | Final transaction |
|---|---|---:|---:|---:|---|---|
| 01-A | control | 91.914 s | yes | yes | refused `old`/`new` | second call, 51 edits / 9 files |
| 02-B | candidate | 113.770 s | no | no | refused `old`/`new` | second call also refused `before`/`after`; source unchanged |
| 03-B | candidate | 86.945 s | yes | yes | refused `before`/`after` | second call, 51 edits / 9 files |
| 04-A | control | 99.202 s | yes | yes | refused `old`/`new` | second call, 51 edits / 9 files |

All four calls selected `edit_clojure` first. There were no shell calls,
native file tools, source reads, or partial writes. Every refusal reported the
source unchanged. Each successful transaction was atomic, read back all nine
files, matched the nine future hashes, reported 51 edits, and ended with
`verification_complete=true` and `next_action=none`.

Control was 2/2 exact and meaning-preserving, with a 95.558-second median.
Candidate was 1/2 exact and meaning-preserving. Its all-attempt midpoint was
100.358 seconds, 5.0 percent slower than control, but that number is not an
efficiency estimate because half the candidate tasks did not complete.

## Native comparison

The closest retained matched-native cohort for this same 51-edit, nine-file
workload has two exact native completions at 220.772 and 473.051 seconds, a
346.912-second midpoint; its third native arm was route-rejected after 306.824
seconds. Against that retained correct-native midpoint:

- the exact control midpoint is 3.63 times faster;
- the candidate's one exact completion is 3.99 times faster; and
- no candidate multiple is publishable because candidate correctness was only
  1/2.

An earlier retained cohort had exact native completions at 135.080 and 269.173
seconds, a 202.127-second midpoint. The exact control remains 2.12 times faster
against that more conservative denominator. The conclusion does not depend on
choosing the flattering native cohort: structural batching remains faster,
but this candidate did not improve the live route.

## Where the time went

The MCP server was not the bottleneck. Refusals took 3.9 to 10.1 milliseconds;
successful 51-edit transactions took 1.047 to 1.140 seconds. The caller spent
31 to 55 seconds materializing each large request and 3 to 10 seconds
interpreting each refusal. One schema miss therefore created roughly another
complete model-sized payload, not a millisecond-scale correction.

The refusal also has an avoidable product defect. It lists the allowed fields
but says, “Correct the named field and call `apply_clojure_changes` once,” even
though the only advertised tool is `edit_clojure`. It does not give the exact
injective mapping:

```text
old    -> from
new    -> to
before -> from
after  -> to
```

The failed candidate caller invented `before`/`after` after seeing that
refusal. That is direct evidence that a generic “correct the field” remedy is
not cheap enough for a 51-edit request.

## Next smallest hill

Do not broaden fuzzy normalization. Test one closed, injective field-vocabulary
ratchet:

1. accept exactly one complete pair from `from`/`to`, `old`/`new`, or
   `before`/`after`;
2. lower aliases to authoritative `from`/`to` before location normalization;
3. refuse mixed, partial, duplicated, or conflicting pairs before reading or
   writing source;
4. preserve the exact requested pair and emitted pair as bounded evidence; and
5. replay these four captured calls plus adversarial mixed-pair falsifiers
   before another fresh model cohort.

That is the same principle as the location candidate: tolerate only a
mechanically injective spelling whose meaning is exact. If this ratchet is
safe, rerun a fresh whole cohort. Do not selectively retry this one.

## Raw evidence

Archive:

`/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-29/clj-surgeon-location-abba-20260829T060610Z.tar.gz`

SHA-256:

`d109fa0bef5c40a9cdb9313bfa5ff9e361258d338e9fd80c5ba92c8d81b5eded`

The archive contains the exact two-ref Git bundle, controller and admission
verifier, client-registry receipts, prompts, events, full MCP requests and
responses, event clocks, telemetry, start/final hashes, semantic and exact
scores, and the raw SHA-256 manifest. It excludes only redundant cloned
checkouts, whose immutable refs and trees are recorded in admission evidence.
