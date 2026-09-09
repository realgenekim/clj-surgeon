NO-GO

# Sol fence review, round 9: preservation brief

- reviewed: sealed candidate `e086997c95e77b4abeb365fee5b64df6012a1f42`
- branch tip accepted by the brief: `a8ec1923418d3d78bdf77d26dd336f91cfee662b`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 8 `NO-GO` on PB-FENCE-013
- excluded as directed: `make test`; verification used `bin/` only

## Blocking findings

### PB-FENCE-014 — `:namespace-usages` outside the `ns` span become fictitious load-order edits

The repair calls all of clj-kondo's `:namespace-usages` the require inventory. That inventory is not
restricted to the `ns`-form span which the repair says the private parser supplies. A top-level
`(comment (require ...))` is therefore mixed into A4's namespace inventory.

Probe: a `.clj` file kept its real `ns` form byte-identical and changed only this top-level form:

```clojure
;; base
(comment (require '[clojure.edn :as edn] '[clojure.walk :as walk]))

;; candidate
(comment (require '[clojure.walk :as walk] '[clojure.edn :as edn]))
```

The brief exits 3 with `require_reorders=1` and `reconcile_failures=2`. Its hard stop says the file's
“`:require` ORDER changed — that is load order”, although the namespace require list did not change
and the form is never executed. `kondo-requires` consumes every `:nsu` entry for the file; the parsed
`ns` span is never applied to that collection.

This is conservative for program safety, but not for the stated product: it manufactures a mandatory
review decision and falsely labels it a load-order change. It also breaks the claimed two-way
reconciliation, because that comparison is no longer between two inventories of the same thing.

Required repair: before ordering, grouping, or reconciling `:namespace-usages`, retain only occurrences
whose resolver position lies inside the parser-proven `ns`-form span. Add the top-level-comment pair as
a permanent control. The repair belongs in `bin/`, which the ship-fix contract classifies as
`HOLD reason=oracle-changed`; I did not patch it.

### PB-FENCE-015 — aliases and refers remain a private inventory beside the analyzer's

The §11 rule says that when the analyzer has an inventory, the brief keeps no second one. Requires now
come from `:namespace-usages`, but `index-file` still fills `:aliases` and `:refers` from
`preservation-scan/requires-of`. `head-info`, `resolve-site`, canonicalisation, and A5 consume those
private maps.

The exact repaired `#?@` replay pair demonstrates the disagreement:

- reordered pair: `require_reorders=1`, `require_sort_broken=1`, `reconcile_failures=0`, exit 3;
- control pair: `require_reorders=0`, `require_sort_broken=0`, `reconcile_failures=0`, exit 3;
- both pairs: the two valid `str` and `set` call sites are reported as aliases the `ns` form does not
  define (`unresolved_sites=2`, `undefined_alias_sites=2`).

Counting the private map's blindness as an obligation prevents a false `clear: true`, but it is not
sufficient. It makes every touched spliced-alias file carry false A5 failures, leaves canonicalisation
using a different namespace model from A4, and directly violates the candidate's stated no-second-
inventory rule. Aliases and refers must come from the authoritative, ns-span-filtered
`:namespace-usages` inventory per analysed platform. If platform maps cannot be combined without losing
meaning, the file must be refused with that precise reason rather than declaring valid aliases undefined.

This repair also belongs in `bin/`; I did not patch it.

### PB-FENCE-016 — Section D still says analysed `.cljc` branches were not elaborated

The generated Section D still contains:

> reader conditionals. `.cljc` branches are compared as text; no platform was elaborated.

That is the exact stale statement called out in round 8 and contradicts the implemented clj-kondo
`:clj`/`:cljs` analysis and reconciliation. The local-binding warning was corrected, but the reader-
conditional sentence was not.

## Requested probes and controls

The eight prior hostile canonicalisation probes were rerun against the current candidate: direct
`wrong-binding`, `are-binding`, `uppercase-alias`, `kind-collision`, `referred-testing`,
`refer-all-shadow`, `use-shadow`, and `refer-all-external`. Every row exits 3 and reduces relocated
preservation from the clean control's 99 to 98; none regressed to false certification.

Positional require probes:

| probe | result |
|---|---|
| two requires on one line, reversed | exit 3; `require_reorders=1`; correct |
| duplicate require sequence `[set string set]` → `[set set string]` | exit 3; `require_reorders=1`; correct |
| require sequence reversed inside top-level `comment` | exit 3; `require_reorders=1`, `reconcile_failures=2`; false load-order/reconciliation signal, PB-FENCE-014 |

The clean Cell C control remains 99/141 relocated bodies preserved, 32/64 in-place bodies preserved,
zero require reorders, and zero reconciliation failures.

## Other requested checks

- **Independence:** the three implementation files require no `clj-surgeon.*` namespace.
  `preservation-brief` reads a receipt only after its A/B derivations, for Section C.
- **Wrong-binding falsifier:** the direct same-token/different-resolution row remains non-clear and
  drops preserved bodies 99 → 98.
- **Section-B-only defects:** promotion, forward-reference, and require-order signals remain hoisted in
  the pre-A hard-stop block and force exit 3; a reviewer is explicitly told not to stop at Section A.
- **Real-tree provenance:** `preservation-replay` builds its main scratch repository by `git archive`
  from the real Curtaincall CFP trees, not a synthetic source seed.
- **Preregistration:** §9 honestly fixes 21 specimens × 3 arms = 63 observations, gives each protected
  class two observations per arm, declines to estimate a catch rate, and preregisters strict dominance.
- **Table ratchet:** `bin/preservation-tables-test` exits 0: 202 core functions, 72 modelled/frozen
  Vars, 31 claims, 38 external-resolver witnesses, 274 generated shadows, 269 actually applied.
- **Exact delta:** `5ec9edc5..a8ec1923` changes only `bin/preservation-brief` (+104/-15),
  `bin/preservation-replay` (+55/-1), and `bin/preservation-tables-test` (+2/-1).
  `git diff --check` is clean; `bin/preservation_scan.clj` did not change.

The complete 23-row sequential replay was not claimed: its real-tree setup, shapes, clean control, and
direct wrong-binding row completed before the bounded run was stopped; the remaining seven requested
historical probes and the new positional controls were then run concurrently against the same generated
real-tree fixture.

## Decision

PB-FENCE-013 is closed: analysed `#?@` require reorder is now caught in positional order. The candidate
still does not meet its own proof-burden contract. It treats namespace usages outside the `ns` form as
load-order edits, retains private alias/refer inventories beside analyzer data, and emits a stale Section
D scope statement. Do not ship this candidate as a proof-burden reducer.
