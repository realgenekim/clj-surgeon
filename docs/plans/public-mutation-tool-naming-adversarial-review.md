# Adversarial Review: Public Mutation Tool Names and Shapes

**Status:** Brain Fleet adversarial lane. No product change is authorized.

**Owning issue:** `clj-surgeon-x9d`

**Base:** `6db86d1a4d9ceb6b904f5a5141162d687f287e3e`

## Recommendation

Keep `edit_clojure` and `apply_clojure_changes` for the next release. Do not
ship a cosmetic rename.

The names are imperfect, but both names now have strong behavior evidence.
`edit_clojure` achieved 10/10 exact fresh Sol/high selections after its thin
schema became visible. `apply_clojure_changes` is the one-shot entrance behind
the measured Sol, Fable, Opus, Terra, and Spark extraction results. The best
matched results range from 6.37x through 9.69x faster than correct native
controls.

A rename has no measured prize and has several direct regression paths. The
current source also uses the public operation string as an internal behavior
key. A cosmetic rename can disable the terminal response, distort telemetry,
or break recovery without changing the transaction kernel.

First make the public name a projection of a stable internal operation kind.
Then run a clean-context name-and-shape tournament. Rename only if a challenger
improves route selection and complete verified task time without slowing the
frozen Sol route.

## STE100 Explanation of the Current Boundary

The current tool names do not state the boundary. Use these stable terms in
instructions and experiments:

| Preferred term | Exact meaning |
|---|---|
| **exact edit batch** | The caller already knows files, owners, old forms, replacements, counts, computed programs, or owner deletions. |
| **prepared transaction** | The change needs retained semantic evidence, a specialized operation, or a verification gate that must participate in rollback. |
| **public tool name** | The MCP catalog name selected by the caller. |
| **operation kind** | The stable internal behavior identity. It must not depend on a public alias. |
| **terminal mutation evidence** | Proof that this mutation is complete. It does not prove that the complete user request is finished. |

In controlled language:

```text
Use edit_clojure for one complete exact edit batch.

Use apply_clojure_changes when the request uses a prepared basis, extraction,
a specialized action, or a verification gate that must roll back with source.

Use native patching for prose, a new file, or one small visible text edit.
```

This wording is more accurate than “light” and “heavy.” Those words describe
implementation cost, not the caller's decision.

## Current Contract and Its Ambiguities

| Surface | Current public shape | Strength | Ambiguity |
|---|---|---|---|
| `edit_clojure` | `edits`, `programs`, or `delete_owners`; no `verify` | Small exact gesture; preserves unrelated bytes; one guarded transaction | “Edit” also describes every operation in the other tool. Programs and multi-owner deletion are larger than the name suggests. |
| `apply_clojure_changes` | `basis+decisions`, `changes+expect`, compact inputs, or `extraction`; optional `verify` | Complete operation compiler; specialized actions; rollback-gated verification | “Apply” suggests that a separate plan already exists. Direct extraction deliberately removed that plan. “Changes” does not explain why compact edits belong elsewhere. |

The overlap is real. `apply_clojure_changes` accepts the compact fields that
also select `edit_clojure`. The narrow tool omits `verify`, while the broad tool
accepts it. Therefore an exact replacement can belong to either tool depending
on verification policy. No pair of cosmetic verbs makes that exception
obvious.

The raw catalog size increases the cost of visible compatibility aliases:

| Tool | Schema JSON characters | Approximate description source characters |
|---|---:|---:|
| `edit_clojure` | 3,348 | 1,281 |
| `apply_clojure_changes` | 18,281 | 4,115 |

A visible alias for the broad tool can duplicate roughly 22,000 raw catalog
characters before protocol framing. It can also produce two search results for
one behavior.

## Evidence That Constrains the Decision

### Proven positive behavior

- A fresh Sol/high cohort selected `edit_clojure` and completed the exact
  nested edit in 10/10 trials. Each trial used one successful mutation.
- The direct extraction route removed a public plan phase and saved 12.070
  seconds, or 24.2% complete wall.
- The terminal-response contract reduced the promoted extraction median from
  25.066 to 19.216 seconds. The 5.850-second saving was 23.3%.
- Matched cross-caller results retained one `apply_clojure_changes` call:
  Sol 9.69x, Fable 8.97x, Opus 6.37x, and Terra 7.56x versus correct native
  controls.
- Claude callers paid one deferred `ToolSearch`. Sol retained a direct
  first-action route. A migration must not force Sol through Claude's catalog
  discovery path.

### Recent natural-history window

The privacy-safe receipt for `2026-08-27T23:26:30Z` through
`2026-08-28T05:09:42.111127Z` reports:

| Measure | Result |
|---|---:|
| Codex `edit_clojure` calls | 14 |
| Codex `apply_clojure_changes` calls | 18 |
| Surgeon-using Codex turns | 24 |
| Median next boundary after Surgeon | 8.207 seconds |
| Median MCP server wall | 0.154 seconds |
| Claude Surgeon calls in this narrow window | 0 |

This window proves current use, not route correctness. It does not provide a
matched Claude naming comparison. Use the retained cross-caller benchmark for
Claude capability and discovery evidence.

The service receipt labels every mutation telemetry event as
`apply_clojure_changes`, while the caller transcript distinguishes both public
tools. This is a current instrumentation limitation. A name experiment must
repair or bypass that conflation before it uses service telemetry as the route
authority.

### A prior stop result

An edit-only profile removed 90.7% of catalog text and was 15.6% slower in its
small cohort. Therefore catalog size alone is not a causal speed model.
Nevertheless, visible aliases still create selection ambiguity and stale-name
risk. Treat catalog bytes as a mechanism measurement, not a verdict.

## Hidden Couplings That Make a Rename Dangerous

1. **Terminal response.** `exact-terminal-response` requires
   `operation == "apply_clojure_changes"`. A public rename can remove the
   measured 5.850-second receipt gain if operation identity changes with it.
2. **Telemetry.** The mutation recorder currently emits
   `tool="apply_clojure_changes"` for all mutation requests. A rename can make
   caller and service counts less comparable.
3. **Recovery.** Recovery checks and remedies name
   `apply_clojure_changes` explicitly.
4. **Outcome registry.** Public outcome classes are keyed by exact tool name.
5. **Benchmark gates.** Codex and Claude harnesses match exact tool names and
   action geometry. A rename can look like zero adoption or route failure.
6. **Installed routing.** Repository, Codex, and Claude packages contain exact
   names. Stale installed copies can disagree with the live catalog.
7. **Long-lived clients.** The server emits `tools/list_changed`, but a Codex
   turn can retain old model-visible schema text. Claude's deferred search also
   selects an exact tool name.
8. **Visible aliases.** A compatibility alias duplicates schema, creates two
   valid choices, and can add a model decision boundary.
9. **Historical receipts.** Durable results and scorers contain the current
   names. Migration must preserve their interpretation.
10. **Tool-name cardinality.** The broad name appears in 101 repository files;
    the narrow name appears in 51. Excluding plans and observations still
    leaves 43 and 24 files respectively.

## Option Portfolio

| Option | Why it might be right | Main cost or failure mode | Decision |
|---|---|---|---|
| A. Keep both names; front-load role sentences | Preserves all measured routes. Changes only explanatory text after a controlled test. | The names remain imperfect. The verifier exception still needs instructions. | **Recommend now.** |
| B. `edit_clojure` + `refactor_clojure` | “Edit” and “refactor” are familiar to coding models. Extraction maps naturally to refactor. | Compact programs and owner deletion can be refactors. Insertions and verified exact edits can be ordinary edits. The name implies architectural judgment that the kernel does not own. | Admit only as a tournament challenger. |
| C. `edit_clojure` + `apply_clojure_transaction` | “Transaction” advertises atomicity, rollback, and verification. | Both tools are transactions. “Apply” still implies a prior plan. Longer name adds no routing rule. | Reject unless clean callers select it better. |
| D. `edit_clojure_exact` + `apply_clojure_changes` | Makes the narrow stale-guarded contract explicit. | Throws away a 10/10 proven name. Programs are computed rather than literal exact replacements. | Reject as default; useful challenger only. |
| E. `edit_clojure` + `apply_clojure_plan` | Prepared `basis+decisions` maps cleanly to a plan consumer. | Direct extraction must not pay for planning. The current broad tool accepts complete direct decisions. The name would teach the obsolete route. | Reject. |
| F. One `change_clojure` tagged union | Removes the catalog choice and alias problem. | Recreates the overloaded surface that failed compact-edit discovery. The current broad schema is already 18,281 characters. | Reject without new causal evidence. |
| G. `edit_clojure`, `extract_clojure`, `rename_clojure`, and other operation tools | Each name maps to a visible user action. | Schema proliferation, more discovery choices, stale catalogs, and duplicated outcome laws. It can tax Sol to help less capable callers. | Reject for the current mission. |
| H. `edit_clojure` for direct decisions; `apply_clojure_plan` only for returned plans | Creates a clean direct-versus-prepared boundary. Inspect can return the exact plan call, so callers need not choose it. | Requires a real schema repartition. Moving extraction and specialized direct changes into `edit_clojure` can destroy its small-schema advantage. | Valuable architecture experiment, not a rename. |

Only B is worth a pure naming tournament against A. H is a separate public
shape experiment. Do not mix those factors.

## Frozen Decision Cards for a Naming Tournament

Use these retained task shapes. Do not show implementation vocabulary such as
“compact” or “heavyweight.”

| Card | Complete supplied decision | Correct current route |
|---|---|---|
| 1 | Replace one exact subtree in one named owner. | `edit_clojure` |
| 2 | Replace several exact subtrees across files. | `edit_clojure` |
| 3 | Run one bounded computed relation across selected forms. | `edit_clojure` |
| 4 | Delete several exact named owners. | `edit_clojure` |
| 5 | Insert complete sibling forms after one owner. | `apply_clojure_changes` |
| 6 | Rename a destructured local binding while preserving its external key. | `apply_clojure_changes` |
| 7 | Move 15 supplied owners to a new namespace with exact caller decisions. | `apply_clojure_changes` |
| 8 | Fill decisions in a snapshot-bound `next_call` from inspection. | `apply_clojure_changes` |
| 9 | Apply an exact replacement with a repository verifier inside rollback. | `apply_clojure_changes` |
| 10 | Change one visible prose line. | Native patch |

Card 9 is the adversarial crossover. A name pair that cannot route it from the
published descriptions has not explained the real contract.

Run each card in fresh Sol/high and Fable/high contexts. Add Opus only after
the harness proves exact catalog and prompt parity. Keep the current names as
the control. Do not use repository routing text that gives the answer.

## Falsifiable Gates

A candidate name or shape advances only when all gates pass:

1. **Selection:** at least 95% correct first-route selection on cards 1-9.
2. **Native boundary:** card 10 remains native in every trial.
3. **One shot:** no increase in failed calls, schema correction, or tool
   switching.
4. **Sol protection:** no more than 5% regression in Sol median complete wall,
   time to first mutation, or input tokens on the frozen extraction and exact
   edit controls.
5. **Claude value:** Fable must remove a discovery or correction action, or
   improve complete wall by at least 10%. Aesthetic preference is not enough.
6. **Terminal relay:** every eligible exact-verification result still returns
   the terminal response byte-for-byte.
7. **Telemetry:** public name, internal operation kind, request shape, and
   canonical outcome are separately observable.
8. **Stale client:** an old cached caller either succeeds through a hidden
   compatibility route or receives one deterministic recovery action. It must
   not mutate twice.
9. **Catalog:** no duplicate visible tool has the same behavior and schema.
10. **Rollback:** the immutable current tag restores installed skills, CLI,
    catalog, and shared runtime in one announced window.

Stop after two correct counterbalanced Sol/Fable pairs when the candidate has
less than 10% wall difference and identical action geometry. That result is
parity, not a reason to rename.

## Migration and Rollback Plan

Do not start migration until a challenger passes the gates.

### Ratchet 1: separate identity from presentation

Introduce a closed internal operation kind, for example:

```clojure
:operation-kind :exact-edit-batch | :prepared-transaction
:public-tool-name "edit_clojure" | "apply_clojure_changes"
```

Terminal response, telemetry, recovery, and outcome classification must use
the internal kind or explicit capabilities. They must not branch on the public
string. Preserve current response fields during this ratchet. Prove byte and
timing parity before a name experiment.

### Ratchet 2: shadow the candidate catalog

Generate a candidate catalog and routing package from the same immutable
commit. Run fresh clients against an isolated MCP server. Do not install or
reload the shared runtime. Compare exact schema hashes, action geometry,
terminal relay, and results with the current catalog.

### Ratchet 3: choose one compatibility mechanism

Prefer an unlisted call alias only if the MCP registry can dispatch it without
advertising a duplicate schema. Test a stale client that calls the old name.
Record alias use in telemetry.

If an unlisted alias is impossible, do not publish both names by default.
Schedule a session-bound cutover and require fresh clients. A visible duplicate
is acceptable only if a separate cohort proves no Sol or Claude regression.

### Ratchet 4: publish once

Before publication, record:

- exact candidate and rollback commits;
- installed CLI, Codex skill, and Claude skill receipt hashes;
- shared MCP PID and CWD;
- old and new catalog hashes;
- one true mutation result and one safe refusal;
- one pre-existing session continuity proof.

Run one announced `make install` and one announced hot reload. If either result
is inconclusive, mark state unverified and stop. Do not retry blindly.

### Rollback

Restore the immutable prior release with `make install`, then perform one
announced hot reload from that ref. Verify the old catalog, one bounded read,
and one safe mutation in an isolated workspace. Preserve all candidate
receipts. Do not reinterpret failed candidate runs as old-name runs.

## Final Adversarial Judgment

The current problem is not proven to be the spelling of either name. The
product has already won its hardest benchmarks with these names. The remaining
confusion comes from a non-disjoint public boundary and from a broad tool whose
name, schema, telemetry identity, and terminal behavior are coupled.

The safest next move is a Kent Beck move: make renaming cheap and reversible
before renaming. Separate operation identity from presentation, freeze the ten
decision cards, and make the current names defeat a real challenger. Until a
challenger wins, preserving the measured Sol path is the better design.

## Evidence Sources

- `docs/plans/one-shot-editor-gesture.md`
- `docs/plans/selective-compiled-scalpel.md`
- `docs/plans/cross-caller-mcp-extraction-benchmark.md`
- `docs/plans/cli-public-operation-envelope-gap-analysis.md`
- `docs/observations/2026-08-24-anvil-round-two-and-startup-memory-synthesis.md`
- `docs/observations/2026-08-26-three-day-speed-option-portfolio.md`
- `docs/observations/2026-08-27-captains-log-terminal-proof-ended-the-second-plan.md`
- `docs/observations/2026-08-27-captains-log-the-model-boundary-dwarfed-the-scalpel.md`
- `/tmp/clj-surgeon-agent-usage-20260827T232630Z-20260828T050942111127Z.json`
