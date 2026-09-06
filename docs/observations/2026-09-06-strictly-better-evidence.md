# Strictly better, or native — the evidence behind the routing plate

Maintainer document. The always-loaded plate carries only the trigger, the complete
calls, the real prerequisites, the receipt scope and the escape rule. Everything a
router does NOT need in order to act — ratios, fixtures, caveats, and how the meter
is operated — lives here.

## The rule, and what "strictly better" does and does not claim

> Route automatically only when the task matches a witnessed contract and the
> complete receipt path is available; otherwise use native. On one clear refusal
> repair once, then native fallback with a receipt. Meter complete verified wall,
> first-attempt success, fallback and unknown telemetry; retire a route when
> evidence no longer clears its native control.

Strictly better means a better EXPECTED complete verified wall on an ELIGIBLE task,
with a bounded escape. It is NOT a per-invocation guarantee: learning, queue,
provider and refusal costs are real, and the meter charges them to the route.
Native being the performance default outside the two routed classes is a default,
not an impossibility claim: an explicit user request or a separately approved
experiment may use any other capability.

## The two automatically routed classes

| class | native | tool | ratio | n | correctness |
|---|---|---|---|---|---|
| batched known-intent fan-out, informed route (cohort I) | 101.2 s | 57.8 s | 1.75x WIN | 4 pairs | tool 4/4, native 3/4 |
| whole-repository alias migration (Astra replication) | 42.9 s | 31.1 s | 1.38x WIN | 6 controls + 3 pairs | 3/3 and 6/6 |

Fan-out boundary: the route was mandated, not chosen; the native witness failure was
a known layout false negative, so there is NO quality-superiority claim. `owner_counts`
is a later usability change with no measured additional wall gain.

Alias boundary: the fixture was a FIXED NO-COLLISION synthetic repository (114 files,
21 targets, 63 qualified uses, zero collisions), so collision resolution is NOT
witnessed. The preregistered 1.5x prediction was MISSED (pairwise 1.163x / 1.412x /
1.218x). Controls ran serially and pairs concurrently, so the 11.8 s median gap
against a 6.9 s fresh-control 2SD threshold is an operational threshold, not a formal
significance claim. Receipts: `2026-09-06-astra-alias-replication.md`.

## Why reading is a supporting call and not a routed class

Served discovery (cohort J) was wall-neutral against the same fan-out, and its arms
scored 0/4 on a spelling-sensitive witness caused by the route itself. One hand-probe
match batch over 20 files returned 59 sites with their owning forms in 0.33 s (0.24 s
on the rebuilt server) — a latency figure, not a wall win. The house rule's "~150x
more token-efficient than reading files" is an OLDER measurement carried in doctrine;
it is deliberately absent from the plate, because no supporting comparison was made
here and tokens are not the prime meter.

Known paper cut: the root-level `expect` is required by the evaluator while the field
description marks it optional, and it refused 2/2 extract-E arms that omitted it. The
plate's example therefore shows `expect` at the root.

## The measured losses that make native the default

| class | native | tool | ratio | note |
|---|---|---|---|---|
| whole feature, admit gate | 130.6 s | 135.1 s | 1.03x LOSS | both correct |
| whole feature, typist client | 125.0 s | 229.0 s | 1.83x LOSS | 16 outer actions vs 7 |
| extract-to-namespace | 98.0 s | 105.5 s | 1.08x LOSS | 4/4 correct; server time 2% of wall |
| served discovery on the fan-out | 101.2 s | 59.7 s | wall-neutral vs cohort I | falsified as a separate win |

Extraction's ethnography (`2026-09-06-extract-E-ethnography.md`) is the reason the
receipt section tests VALUES: both tool arms re-verified natively — more than native
did — because a receipt said "written bytes read back and verified" and, in the same
block, "caller proof · structural candidates only; not semantic completeness". A
receipt that disclaims semantics buys no verification credit.

## Executed example receipts (2026-09-06 ~20:03Z)

Each plate example is a schema example INSTANTIATED against a fixture and executed
there, and the plate publishes it byte-identical to the request that ran (verified
with `diff -q` against the retained files). The service was
`http://127.0.0.1:8171/mcp`; the fixture was a fresh scratch copy of the `fanout-B`
seed (33 Clojure files) with `workspace_root` pointed at that copy. A router
substitutes the task inputs — `workspace_root`, lib, Var, file, form, path and every
count. Request and response JSON retained at
`/var/tmp/forge/fable-strict-fx/examples/`.

| example | result | figures |
|---|---|---|
| `inspect_clojure` supporting read | ok, `read_complete=true`, `next_action=none` | 3 requests / 3 files / 12 matches, 74.6 ms server, 71.5 ms client wall |
| `apply_clojure_changes` fan-out | ok, atomic commit, bytes read back and verified | 3 edits / 2 files, 293.94 ms server, 297.5 ms client wall |
| `alias_migration` | ok, atomic commit, bytes read back and verified | 2 files / 3 sites, 1 alias bound, 0 collisions, 628.12 ms server, 632.1 ms client wall |

One earlier draft of the alias example was REFUSED by the live server, and the
example was corrected rather than the refusal explained away. Verbatim:

```
alias_migration
  refused · unknown-verification-profile · 1.20 ms

✓ source unchanged
→ Unknown verification profile: clojure -M:test
facts · configured_profiles=["fast" "full"] · mutation_attempted=false · next_action="correct_request" · source_unchanged=true · verify="clojure -M:test" · write_authority=false
remedy · Name a profile this workspace configures, or omit verify.
```

A hard-coded test command is not a repository-independent proof contract, so `verify`
was dropped from the published example entirely — matching the shape that was
actually measured. A follow-up attempt with `verify: "fast"` migrated 2 files / 3
sites and then ROLLED BACK when that profile failed (`files_restored=2`,
`files_still_migrated=0`), which is the intended behaviour and is why the plate says
to run the repository's own required checks afterward rather than delegating them to
a profile string.

## Operating the meter

Required measurements per routed class: first-attempt success, refusal rate, fallback
rate, and complete request-to-verified wall. Discovery, schema repair and any second
read are charged to the route; tool runtime is never subtracted from a wall. Each
class also carries a periodic preregistered native pair.

- A CORRECTNESS failure suspends the class immediately.
- A WALL loss is assessed against that class's own controls. One noisy pair is not a
  permanent ban.
- UNKNOWN telemetry means unknown performance. Use native pending investigation; do
  not record it as a measured loss, and do not require a fresh cohort for every
  missing row.
- The usage collector's coverage of complete task wall and fallback is itself
  unproven. Until a receipt exists, these are required measurements with unknowns
  retained as unknown.

## Source receipts

`2026-09-06-gene-report-1950z-crank.md` (block ledger and the vs-native table),
`2026-09-06-astra-alias-replication.md`, `2026-09-06-extract-E-ethnography.md`,
`2026-09-06-max-utility-plan.md`, `2026-09-06-fanout-I-result.md`,
`2026-09-06-fanout-J-ethnography.md`, and the peer text review at
`/var/tmp/forge/astra-strict-2-review.md`.
