NO-GO

# Sol fence review, round 11: preservation brief

- reviewed: sealed candidate `45e0d099775e7966f97e92c58179b70d5eba1900`
- branch tip accepted by the brief: `95b0ff3f5490fa97b7b0f6091105a68371f07939`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 10 `NO-GO` on PB-FENCE-017/018
- excluded as directed: `make test`; verification used `bin/` only

## Blocking finding

### PB-FENCE-017 remains open — refusal does not suppress the false namespace explanations

The new namespace-definition count correctly hard-stops a file with two namespaces, but the
brief still derives and publishes semantic inventories from the first private-parser `ns` span.
On the exact round-10 two-`ns` probe the JSON is:

```text
clear=false
reconcile_failures=2
ns_count_failures=2
dynamic_requires=4
undefined_alias_sites=2
require_reorders=0
```

The four `dynamic_requires` are `clojure.edn` and `clojure.set` in the second namespace, in
both trees. The report puts them in its hard-stop list and B4 table as namespace usages
“OUTSIDE the `ns` form (never load order)” and says they are a comment, REPL block, or runtime
call. It also says the candidate's `edn` and `set` call sites name aliases the `ns` form does
not define. Both statements are false: they are static requires and aliases in the second
live namespace form.

The cause is still visible in `bin/preservation-brief`: `outside-usages` is every resolver
namespace usage outside the private scanner's first `ns` span (lines 375 and 387), and the
global dynamic/undefined-alias inventories do not exclude a file whose namespace-count
reconciliation failed (lines 1503 and 1598). The new hard stop therefore sits alongside the
old false explanations instead of refusing the file whole for all downstream inventories.

Round 10's required repair explicitly said not to call a usage in another `ns` form
runtime/dynamic or “never load order.” That falsifier still fires. This is blocking for a tool
whose purpose is to shrink the reviewer's proof burden: the hard-stop header says every listed
signal needs a human, so false B4 and A5 signals are not rendered harmless by also listing the
namespace-count refusal.

The requested `.cljc` attack with `#?(:clj (ns distinct.a) :cljs (ns distinct.b))` does detect
two distinct namespace names in each tree (`ns_count_failures=2`, `reconcile_failures=2`). An
`in-ns`-only file detects zero namespace definitions in each tree and produces the same two
failure counts. Both fail closed. They also expose a related inaccurate Section D sentence:
`unanalysed_platforms=[]`, but Section D uses the total reconciliation-failure count to say
that two files carrying an outside platform were refused (lines 2244–2246). These files were
refused for namespace count, not platform coverage.

The repair belongs under `bin/`. The ship-fix contract classifies any patch touching `bin/` as
`HOLD reason=oracle-changed`, so I did not leave a GO-WITH-FIX patch.

## PB-FENCE-018 and requested probes

PB-FENCE-018 closes on the metadata-wrapped declaration. The exact probe reports `declares=1`,
`new_forward_refs=0`, `reconcile_failures=0`, and one expected in-place body obligation.

R7 passes on the sealed candidate with exactly 14 allowed private-scanner calls. In an isolated
scratch copy I added `(when false (sc/children nil))` after the scanner require. The table test
exited 1 with exactly one failure: `R7 unlisted private-scanner read — sc/children ... is not on
the span/bytes/tokens/positions allowlist`. No repository file was changed by this mutation.

The eight requested same-token/different-resolution canonicalisation probes all remain
non-clear and reduce Cell C relocated preservation from 99 to 98:

- `wrong-binding`
- `are-binding`
- `uppercase-alias`
- `kind-collision`
- `referred-testing`
- `refer-all-shadow`
- `use-shadow`
- `refer-all-external`

Together with `two-ns` and `meta-declare`, these are the ten requested probes.

## Five controls

- Clean Cell C remains 141 moved, 99 relocated bodies preserved, and 32 of 64 in-place bodies
  preserved; namespace count, reconciliation, require reorder, dynamic require, undefined alias,
  stale-site, and forward-reference counters are zero.
- `shapes` reconciles with no namespace-count, resolver, alias, or forward-reference failures.
- `cljc-analysed` reports no namespace-count, reconciliation, or unanalysed-platform failure.
- `spliced-require-control` reports no require reorder, sort break, reconciliation failure, or
  undefined alias.
- `comment-require-control` reports no require reorder, sort break, reconciliation failure, or
  undefined alias; its four comment-form namespace usages remain the expected B4 obligations.

## Other required checks

- Independence remains intact: none of the three implementation files requires
  `clj-surgeon.*` or reads source under `src/`. Receipt parsing starts after the A/B derivations
  and is used for C disagreement.
- The replay scratch repository is built by `git archive` of the real Curtaincall CFP trees
  `d9205abc` and `65ad613b`; the reviewed rows used branches created from those archived trees.
- The Section-B-only defects remain in the hard-stop block above A, so their placement is loud.
- The section 9 preregistration is unchanged by this repair delta; it still fixes 21 specimens
  across three arms, gives each protected class two observations per arm, declines a catch-rate
  estimate, and names strict dominance as the safety co-primary.
- `de83b75d..95b0ff3f` changes only `bin/preservation-brief` (+38/-10),
  `bin/preservation-replay` (+68/-2), and `bin/preservation-tables-test` (+37/-1).
  `git diff --check` is clean.
- `bin/preservation-tables-test` exits 0 on the sealed candidate.

## Decision

PB-FENCE-018 is repaired, and the distinct-name and zero-namespace counters fail closed. But
PB-FENCE-017's decisive falsifier still produces the same false dynamic/load-order and alias
explanations from a second namespace form. Do not ship this candidate.


> END RECEIPT (fence-run): worktree HEAD at review exit = 45e0d099775e7966f97e92c58179b70d5eba1900 = fenced sha.
