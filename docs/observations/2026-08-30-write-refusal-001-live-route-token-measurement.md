# WRITE-REFUSAL-001 live-route token measurement

Date: 2026-08-30

Verdict: **GO to Gene's install decision, not an install or performance
claim.** Exact candidate `9af88fba` adds no catalog or request cost and leaves
ordinary success behavior unchanged after named dynamic receipt fields are
excluded. On the realistic 27-owner refusal it adds 5,241 response bytes and
1,656 `o200k_base` input tokens so the caller receives the complete known
match universe. Whether that evidence removes a recovery turn for this write
family is unmeasured.

## Scope correction

The candidate implements generic write `expect-count-mismatch`. It does not
change read-side `batch-form-selection-failed`. The often-quoted 191 firings,
2.72 MB, and 33.5 minutes belong to that different read refusal. They are not
used to price this slice. This correction was accepted before the valid run.

## Exact subjects and route

- Control: `b445a8c3595d70f6f05b6edccb9b1a924539a195`, tree
  `b1e21af8073e66283f82f4036583bfe2971c4b0a` — the installed prepared-request
  surface.
- Candidate: `9af88fbae9ee720613599feaf8cf58432c5898bb`, tree
  `6f9bc30316eb6417977c07c86caf8eb146dfbdb8`.
- Transport: two private 512 MiB MCP stdio processes, JSON-RPC 2.0 protocol
  `2024-11-05`; telemetry and nREPL off.
- Token proxy: local `tiktoken==0.9.0`, encoding `o200k_base`.
- `environment_valid=true`, `route_adherent=true`, all semantic validity fields
  true. Shared runtime, registration, install, and reload were untouched.

## Relay-quotable result

| Surface | Control | Candidate | Delta |
|---|---:|---:|---:|
| Full `tools/list` | 50,550 B / 11,402 T | 50,550 B / 11,402 T | **0 B / 0 T** |
| Exact `edit_clojure` tool | 7,800 B / 1,784 T | 7,800 B / 1,784 T | **0 B / 0 T** |
| 27-owner request | 554 B / 178 T | 554 B / 178 T | **0 B / 0 T** |
| 27-owner refusal response | 973 B / 250 T | 6,214 B / 1,906 T | **+5,241 B / +1,656 T** |
| 129-owner request | 1,783 B / 587 T | 1,783 B / 587 T | **0 B / 0 T** |
| 129-owner refusal response | 984 B / 251 T | 24,477 B / 7,614 T | **+23,493 B / +7,363 T** |

`T` means `o200k_base` proxy tokens, not provider billing tokens.

The 27-owner candidate response contains all 27 ordered, source-free rows with
exact aggregate/per-file/per-form counts, snapshot guards, and no authority.
The 129-owner response returns 128 rows, reports exactly one omitted row, and
adds a non-executable, non-authoritative continuation bound to the selector and
snapshot. Its complete serialized wire response is 24,477 bytes, below the
32,768-byte public framing budget.

## No-cue success

Both arms accepted byte-identical 237-byte / 72-token requests and committed
the same one-owner edit with the same read-back hash. Raw responses were 908 B
/ 300 T and 913 B / 297 T because elapsed time, receipt hash, receipt UUID, and
private arm paths are run-specific. After excluding exactly those named
dynamic fields, both normalized structured results are byte-identical:

`b3fb1f3f997e57ba4d05d8791a62bd181b0ad634d0e25adfe4684369586b7f92`

No `write_refusal_evidence` cue appears on success.

## Price model and claim boundary

The added refusal payload is caller input/prefill, not model-emitted output.
It is therefore on the cheap side of the measured prefill/decode asymmetry, but
it persists in context and is not free. The realistic result remains under
2,000 proxy tokens; the bounded 129-owner result remains under 8,000. The
catalog and all requests add zero tokens.

This zero-model measurement proves price, completeness, bounding, and no-cue
behavior. It does not prove fewer recovery reads, lower wall time, higher
one-shot correctness, or adoption. Those claims require product-shaped caller
evidence for this exact write-refusal family.

## Invalid attempt retained

The first complete run passed every semantic gate but correctly refused its
own no-cue verdict because the harness had not normalized per-run receipt hash
and UUID fields. Its raw payloads remain under `invalid-attempt-1` and no value
from it is scored. Commit `222df610` made that instrumentation repair explicit;
the entire experiment then reran from fresh arm workspaces.

## Evidence

- Valid report SHA-256:
  `6e06ddf33d040550de31a7d8b4dfb51478eeb277c86512fbd6257c9c5027ed76`
- Valid manifest SHA-256:
  `2886fedb0a204da63a6527285070fb6ffd502c4fb22b9804d2116534c9d61eb7`
- Measurement script SHA-256:
  `8fde5042ab2a51786a8193252f86418e9378799395bb4af6ce24eeee207bae04`
- Control/candidate `tools/list` SHA-256:
  `92d3409421c0ff046aeaf6f922395c302f3f18b644e8c6ef4612461733a19645`
- Control 27-owner response SHA-256:
  `24ffaa734737a05379dd54e7afe26553354f657fab47cf8a4760124beb58dfbb`
- Candidate 27-owner response SHA-256:
  `bc80939234ee09cd5ae1d40a3a6f39b90d3c3967a8ea60509476644a2734e92e`
- Candidate 129-owner response SHA-256:
  `c53ed37c32a872c2e41c1374466253808af4e792966218dbbf882c19fab3bf46`

All raw requests, responses, commands, stderr streams, fixtures, receipts,
normalized success results, the JSON report, and the manifest are retained in
`bench/results/2026-08-30-write-refusal-001-live-route/`.

## Exact replay

From this branch, with the two exact clean detached worktrees:

```sh
/Users/genekim/anaconda3/bin/uv run \
  --no-project \
  --with tiktoken==0.9.0 \
  python dev/experiments/measure_write_refusal_001_live_route.py \
  --control-worktree /private/tmp/clj-surgeon-write-refusal-control \
  --candidate-worktree /private/tmp/clj-surgeon-write-refusal-candidate \
  --result-dir /private/tmp/clj-surgeon-write-refusal-replay
```

The harness refuses a pre-existing result directory, a dirty or wrong
commit/tree, any semantic mismatch, any source mutation on refusal, any
authority leak, a catalog/request delta, or a no-cue success difference after
the named dynamic exclusions.

## Read-side owner-selection follow-up

Installed `b445a8c` already has ratified `MCP-OP-READ-DIAG-003`,
`MCP-OP-READ-HYP-001..002`, and `MCP-OP-READ-PARITY-001`: selector refusals
return the complete bounded name-only owner vocabulary and non-authoritative
hypotheses. `MCP-OP-READ-NORM-003` only preserves request identity through that
refusal. The old per-failure location list still has a hard limit of 10 in
`show_form.clj`, but the complete owner-name universe is separately present;
retained evidence found 137/137 complete owner lists and still 119 recovery
reads. Thus basic candidate completeness is addressed, while caller
consumption/trust remains unresolved. A new leaf should target that consumption
mechanism or a proven duplicate-location gap, not re-ratify “return all owner
names.”
