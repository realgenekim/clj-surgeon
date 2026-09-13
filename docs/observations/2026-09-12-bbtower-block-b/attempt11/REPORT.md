Fast coordinator makespan: 23,620 ms (fast.log).

# Block B attempt11 — verifier repairs

Verdict: GO (build verification; Fable ratification and independent acceptance owed).
Branch: bb-rewrite-tower-local. Starting tip:
`b2049db5148cc1020c79ecf1cbaa01dfe160514b`. Implementation tip:
`800b756ec522ae43177aa82e3fe3d78c6843c325`.

## Findings and fixes

1. **Computed bb ceiling: 343,102 ms.**
   `bb test/clj_surgeon/bb_ceiling.clj docs/observations/2026-09-12-bbtower-block-b/attempt10/ceiling-run/receipt.edn`
   prints `{:bb-runtime-sum-ms 240989, :fast-cadence-sum-ms 42143, :ceiling-ms 343102}`
   ([ceiling-derivation.log](ceiling-derivation.log)). Both sums use the shipped
   manifest, from one unchanged archived calibration receipt. Integer ceiling
   arithmetic is `(bb * 60000 + fast - 1) quot fast`; no frozen map participates.
   The new named witness calls the same computation and compares the registered
   constant. This remains a NEW bb budget requiring Fable's ratification.
2. **Spec boundary: accept 343,102; refuse 343,103.** The spec identifies the
   script and definition. A named test parses the actual boundary sentence,
   requires exactly one declaration, and compares both ends with the constant.
   Original-state JVM red: two failures, zero errors, naming both defects
   ([ceiling-spec-jvm-red.log](ceiling-spec-jvm-red.log)). A temporary constant
   change to 343103 yields six failures, zero errors, including exact-boundary
   enforcement ([ceiling-plus-one-plant-red.log](ceiling-plus-one-plant-red.log)).
3. **Every top-level form in all three declared owner files is scanned.**
   The named-form filter is removed entirely. Extraction covers direct,
   qualified and requiring-resolve refusal calls, literal maps, keyed calls,
   conditional values, and def/defn-/do/defmethod wrappers. The shared files
   already emit nine non-probe kinds: these are explicitly registered under
   `:owner-refusals`, checked in both directions by file, and kept distinct from
   the seven probe protocol rows. No source forms or kinds are excluded from
   extraction. This is literal source completeness, not dynamic dataflow proof.
   The verifier's helper plant yields two failures and names
   `mcp_hot_verify.clj` / `:hot-verify-helper-unregistered`
   ([verifier-plant-red.log](verifier-plant-red.log), [patch](verifier-plant.patch)).
   The independent second plant changes a literal in `verify!`, yielding three
   failures and naming `mcp_hot_verify.clj` /
   `:attempt11-verify-profile-unregistered`; the reverse check also names the
   displaced kind ([second-plant-red.log](second-plant-red.log), [patch](second-plant.patch)).
   Both plants were restored; no src/ change remains.

## Verification

| Check | Result | Evidence |
|---|---|---|
| Restored focused JVM tests | 69 tests, 534 assertions, zero failures/errors | [restored-green.log](restored-green.log) |
| Formatter | Five changed Clojure files formatted | [format.log](format.log) |
| Paved clj-kondo | Zero errors/warnings | [lint.log](lint.log) |
| Census | Exactly two added test names, no deleted names | [census-regenerate.log](census-regenerate.log), implementation commit |
| make test-fast, once | Exit 0; 23,620 ms makespan; fast sum 42,909 / 60,000 ms; bb sum 15,455 / 343,102 ms; bb makespan 12,361 ms; zero isolation violations, 94 namespaces | [fast.log](fast.log), [receipt](fast-run/receipt.edn) |
| make test-integration, once | Exit 0; sum 57,140 / 240,000 ms; 168 tests, 3,798 assertions; zero isolation violations, seven namespaces | [integration.log](integration.log) |
| landing-gate-prewarm, once | Exit 0; passed, no problems; 384,159 ms total; all seven stages exit 0; no repair/retry | [gate.md](gate.md), [receipt](prewarm-receipt.edn) |
| Final receipt validation | Current source digest matches the passed prewarm receipt; all seven exits checked; calibration reproduces | [final-verification.log](final-verification.log) |

The prewarm MCP pool charges fast 43,500 / 60,000 ms and integration
66,883 / 240,000 ms; its bb sum is 14,464 / 343,102 ms. The bb pool charges
234,230 / 343,102 ms, with 85,384 ms makespan. The no-argument bb diagnostic
passes in 217,260 ms. These are separate pool measurements; overlapping sums
must not be added. All values are in [prewarm-1.log](prewarm-1.log).

The direct integration entrance labels itself SERIAL/NOT-A-GATE; its result
proves that requested check, not landing authority. Live logs stayed outside
the worktree. The attempt11 directory was held outside during suites to avoid
the inherited unbounded dirty-worktree CLI receipt behavior documented in
attempt10. The accidental bb inner run of the JVM isolation witness is retained
as diagnostic evidence, not accepted as a clean red (see [dogfood.md](dogfood.md)).

## Commits and limits

- `76627051104a423d1717b4eb39b0c34670c7ec34`: computed calibration, constant,
  spec boundary and witnesses, census additions.
- `800b756ec522ae43177aa82e3fe3d78c6843c325`: all-form refusal coverage,
  explicit shared-owner registration, tech-tree update.
- The containing commit archives attempt11 last; no push, tag, install or
  main-branch operation was performed.

Every source edit and plant is listed in [dogfood.md](dogfood.md); commands and
archival procedure are in [commands.md](commands.md).

Least sure: timings are single observations, not performance certification.
The registry proves literal vocabulary completeness for declared owner files;
it cannot prove arbitrary dynamically computed kinds or undeclared owner files.
No disagreement with the verifier's three findings or Fable's shipped-manifest
definition. Shared-owner registration is explicit because scanning the entire
files also sees their pre-existing non-probe verification/configuration kinds.

Owed: Fable's ratification of 343,102 ms and independent acceptance. The earlier
encounter score remains owed (attempt10 recorded encounter-not-found); this
attempt changes no receipt-boolean behavior. The inherited unbounded dirty-tree
CLI receipt and the verifier's V5 implementation-link scope note remain outside
these three repairs. Prewarm is not landing authority or performance certification.
