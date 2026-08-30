# Splice-by-reference synthetic screen — preregistration

Date frozen: 2026-08-30, before the first model call.

This is rung 2, a **synthetic screen**. It can kill the splice-by-reference
hypothesis. It cannot mint a product performance claim; that would require a
fresh-fixture replication and then a matched serial Anvil acid test at an exact
product commit on a real historical decision.

## Empirical reach and prior evidence

The release-branch write-side emission study measured 630,138 canonical bytes
over 195 MCP writes. `find` plus `edits.from` re-quoted 162,345 bytes, 28.5% of
write leaf bytes. In the strict same-file population, 83.6% of leaf bytes were
copyable from the immediately preceding read. The unit of value here is one
completed multi-edit task, not one edit leaf, because the model emits and
recovers at task boundaries.

The prior zero-model splice screen at
`db937f514b3cca6e81fe8b994afb8301b9d484f1` killed a one-anchor insertion
grammar and found that 65.2% of echo lives inside true replacements. It also
showed that weakening the caller-belief guard or overloading `after` creates a
wrong-subject path. This screen therefore tests a different surviving idea:
keep byte-exact replacement semantics, but let a write point to old bytes the
server just returned.

The reach estimate is repository-local and circular: clj-surgeon telemetry is
heavy with its own development. No fleet generalization will be made.

## Frozen mechanism

Product source is the exact tree at
`origin/release/closed-relations-published`. Every episode uses a fresh copy of
the same synthetic Clojure fixture and a local isolated product MCP. No product
source or product test is changed.

An experiment-only stdio proxy in `bench/splice_reference_proxy.py`:

1. passes ordinary `inspect_clojure` reads to the product MCP;
2. in arm R only, labels the four manifest spans `s1` through `s4`, binds each
   label to the returned per-file SHA-256, and returns readable file, owner,
   offsets, anchor hash, preview, and source identity;
3. accepts `from_ref` plus `to` in arm R, verifies label, file, owner, snapshot,
   exact-one cardinality, and the fixture's target-content identity;
4. refuses unknown, stale, ambiguous, or wrong-target labels with a typed,
   `source_unchanged=true`, `mutation_attempted=false` result;
5. lowers a valid reference to ordinary `from` plus `to` before the unchanged
   product `edit_clojure` validator and compiler see it; and
6. appends the complete resolved readable identity to the product receipt.

That final receipt is the pointer-to-identity law. A short pointer is never
treated as evidence by itself.

## Frozen fixture and arms

The task reads four exact owners and replaces four 100+ byte subforms in one
atomic edit: retry, cache, alert, and rollout policies. The prompt, fixture,
expected bytes, owner order, and requested semantic changes are identical in
both arms.

- **Arm Q:** ordinary read result and ordinary `edit_clojure`; the model must
  re-quote each old span.
- **Arm R:** the same read result plus snapshot-bound labels, and the same tool
  schema with one additional `from_ref`/`to` value-pair branch.

Both arms expose only `inspect_clojure` and `edit_clojure`. Each episode is a
fresh ephemeral `codex exec` session on the ChatGPT subscription, exact model
`gpt-5.6-sol`, reasoning `high`, no API key, read-only sandbox, fresh Codex
home, fresh product MCP process, and fresh fixture copy.

## Sub-ceiling pilot

The pilot order is Q then R. No cohort call launches unless all conditions
hold:

- an ideal conventional versus reference serialization can differ by at least
  25% in `o200k_base` mutation-request tokens;
- Q completes exact bytes through a four-anchor conventional request;
- R completes exact bytes through four references and zero quoted old spans;
- R emits fewer mutation-request tokens than Q; and
- wrong-subject is zero.

This is an instrument gate, not cohort evidence. Pilot episodes are never
pooled with the cohort.

## Frozen cohort and counting

The serial interleaved order is:

`Q R R Q | R Q Q R | Q R R Q | R Q Q R`

There are exactly eight attempts per arm. Failed attempts remain in their
assigned slots and are never rewritten or silently replaced.

The primary unit is **canonical mutation-request arguments per completed
task**, summed across every `edit_clojure` attempt including retries. Canonical
JSON is UTF-8, key-sorted, compact, and Unicode-preserving. Tokens use
`tiktoken==0.11.0`, explicit `o200k_base`. The primary comparison is the
percentage reduction from the Q-arm median to the R-arm median. UTF-8 bytes
are a co-reported physical count. Total MCP request tokens, including the
identical read, are secondary.

Predeclared validity fields per episode are `environment_valid`,
`semantic_correct`, `exact_bytes`, `route_adherent`, and `completed_task`.
Exact final bytes and product `verification_complete=true` are required.
Shell or native file-change actions fail route adherence. A valid cohort
requires eight completed attempts per arm; otherwise no performance verdict is
reported.

Turns are reported as MCP round trips, inspect calls, edit calls, shell calls,
and file-change calls. Reference use is strict only when one four-edit request
uses `from_ref` for every edit and quotes no old anchor. Mixed use is reported
separately and does not count toward the adoption gate.

Wrong-subject is the count of any attempted label whose readable file/owner or
target-content identity does not match the intended span. It is a loud typed
failure before product mutation, but it still counts as wrong-subject and kills
the hypothesis. Exact bytes do not erase a prior wrong-subject attempt.

## Registered predictions and kill criteria

Predictions, frozen before launch:

- median mutation-request tokens: **40% lower in R than Q**, plausible screen
  range 32–48%;
- median canonical mutation-request bytes: **42% lower**, plausible range
  35–50%;
- exact completed tasks: **8/8 in each arm**;
- strict reference use: **7/8 R attempts (87.5%)**;
- wrong-subject: **0**;
- median MCP round trips: **2 in each arm**; no turn-count improvement is
  predicted because the proxy changes emission, not the read→write sequence.

The hypothesis is killed by any one of:

- less than 25% median emitted mutation-token reduction;
- any wrong-subject attempt; or
- strict reference use in fewer than half of R attempts.

Secondaries cannot rescue a failed primary. A null at this sample size is not
equivalence. If the screen survives, the only allowed next claim is that the
mechanism earned a fresh-fixture replication.

Price-model line: emitted output decodes at the prior measured approximately
56.5 tokens/second (and mutation bytes correlated at 3.5237 ms/byte); input
prefill is roughly 1,284 times cheaper and is not folded as if it were output.
This screen reports raw counts first and does not convert them into a product
speedup.

## Evidence retention and replay

Every episode retains prompt, raw Codex JSONL stream, stderr, proxy duplex
stream, proxy receipts, product MCP telemetry, product inverse receipts,
before/after workspace bytes, score, command identity, and wall time. The run
root retains the immutable config, pilot gate, cohort summary, and a SHA-256
manifest over every retained file.

Replay, after substituting the committed protocol SHA and exact subscription
auth file:

```sh
/private/tmp/splice-reference-screen-venv/bin/python \
  bench/run_splice_reference_screen.py --run \
  --output docs/observations/evidence/splice-reference-screen-20260830/raw \
  --auth-file /Users/genekim/.codex/auth.json \
  --expected-head <frozen-protocol-commit>
```

## Forward-only v2 launcher amendment

Frozen after the stopped v1 pilot and before any v2 model call.

The v1 Q and R episodes both exited in under 0.09 seconds with an empty event
stream and the same `No such file or directory` stderr. The runner constructed
the episode workspace beneath a relative output root, changed its working
directory to that workspace, and then passed the same relative path to Codex's
`-C`; Codex therefore looked for the workspace below itself and exited before
loading the MCP or emitting a tool request. Both episodes remain scored as
`environment_valid=false`, the v1 pilot gate remains closed, and its raw files
and manifest remain under the original `raw/` result root. They are not pooled,
replaced, or relabeled.

Protocol v2 changes exactly one launcher fact: resolve the caller-supplied
output root to an absolute path before constructing per-episode workspaces.
Fixture, prompt, proxy, schemas, scorer, schedule, model, reasoning, validity
rules, registered magnitudes, and kill criteria are unchanged. V2 writes to a
new `raw-v2/` root and starts again with a fresh Q→R pilot. This amendment does
not use or respond to any model behavior; v1 produced none.
