NO-GO

# Sol fence review, round 10: preservation brief

- reviewed: sealed candidate `e970f56e6cdcf0193c7b1d0511f36ee0b969bd47`
- branch tip accepted by the brief: `de83b75db8cffe9b8e670153daed6e890af3d477`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 9 `NO-GO` on PB-FENCE-014/015/016
- excluded as directed: `make test`; verification used `bin/` only

## Blocking findings

### PB-FENCE-017 — a second `ns` form is misclassified as runtime usage, not refused

The span filter uses only `(sc/ns-form forms)`, which returns the first `ns` form. Every resolver
namespace usage outside that single span is then labelled a comment, REPL block, or runtime call that is
"not load order" (`bin/preservation-brief:351-370`, hard stop at lines 1702-1704).

A file with two top-level `ns` forms falsifies that statement. I planted this pair:

```clojure
(ns first.ns
  (:require [clojure.string :as str]))

(ns second.ns
  (:require [clojure.edn :as edn]
            [clojure.set :as set]))

(defn f [] (set/union #{(edn/read-string "1")} #{2}))
```

The candidate reverses the two requires in `second.ns`. The brief reports `require_reorders=0`,
`reconcile_failures=0`, `dynamic_requires=4`, and `undefined_alias_sites=2`. Those usages are not dynamic:
they are static require clauses in a second namespace form and their order is load order. The result is
conservative only accidentally; it gives the reviewer two false explanations and says the inventories
reconciled.

Required repair: refuse a file with more than one live `ns` form, or prove and associate every namespace
usage with the correct `ns` span. Do not call a usage in another `ns` form runtime/dynamic or "never load
order". This belongs under `bin/`, which the ship-fix contract forbids me to patch.

### PB-FENCE-018 — `sc/children` remains a private structural inventory for `declare` placement

The claimed grep invariant does not hold. `index-file` calls `sc/children` to rediscover the names and
positions of `declare` forms (`bin/preservation-brief:372-376`). That private result directly suppresses
the B4 forward-reference hard stop (`bin/preservation-brief:1526-1542`); it is not either side of a
reconciliation.

The concrete falsifier is a valid metadata-wrapped early declaration:

```clojure
(ns probe)

^:reviewed (declare later)

(defn early [] (later))

(defn later [] 1)
```

Against a base where `later` precedes `early`, clj-kondo reports the declaration and the brief itself
prints `declares=1`, but the private `sc/children` path misses its placement. The brief then reports
`new_forward_refs=1` and hard-stops with "owner used before it is defined, no `declare`". That is a false
load-order obligation: the declaration is present before the use.

Required repair: derive declaration positions from the resolver's `:var-definitions` facts already kept
with row/column, or reconcile any private placement result before it can affect B4. Add the metadata-
wrapped declaration pair as a permanent control. This also belongs under `bin/`, so I did not patch it.

## Closed round-9 findings and requested replay

- PB-FENCE-014 closes on the original comment-require pair: both rows have
  `require_reorders=0`, `reconcile_failures=0`, and `undefined_alias_sites=0`; the four base/candidate
  occurrences are retained as `dynamic_requires=4` obligations.
- PB-FENCE-015 closes on the repaired resolver path. The `#?@` reorder has
  `require_reorders=1`, `require_sort_broken=1`, zero unresolved sites, zero undefined aliases, and zero
  reconciliation failures. Its control has zero reorder/sort/reconciliation/alias signals.
- PB-FENCE-016 closes. `bin/preservation-tables-test` R6 rejects all four retired sentences and requires
  Section D to name `:clj` and `:cljs`; the rung exits 0.
- The eight hostile canonicalisation probes — `wrong-binding`, `are-binding`, `uppercase-alias`,
  `kind-collision`, `referred-testing`, `refer-all-shadow`, `use-shadow`, and `refer-all-external` — all
  remain non-clear and reduce Cell C relocated preservation from the clean control's 99 to 98.
- The clean Cell C control remains 141 moved, 99 relocated bodies preserved, 32/64 in-place bodies
  preserved, zero require reorders, zero dynamic requires, zero unresolved/undefined-alias sites, and zero
  reconciliation failures.

The complete replay table printer was interrupted after the requested rows had emitted their JSON; the
interruption landed during `meta-owner`, so its final aggregate exited 1 for that missing unrelated row.
I do not count that partial aggregate as a passing 25-row replay. The requested hostile rows and the two
require controls named above completed and were read directly from their generated JSON.

## Other requested checks

- **Independence:** none of the three implementation files requires `clj-surgeon.*` or reads source under
  `src/`. Receipt access remains after the A/B derivations and is used only for C.
- **Owner-canonicalisation falsifier:** direct same-token/different-resolution substitutions remain out of
  the preserved count, as shown by every 99 -> 98 hostile row above.
- **Section-B-only defects:** promotion and load-order findings are in the hard-stop block above A and force
  non-clear output; their text is not skippable without skipping the verdict itself.
- **Real-tree provenance:** the main replay repository is populated by `git archive` of Curtaincall CFP
  `d9205abc` and `65ad613b`, then defects are planted on that real candidate.
- **Preregistration:** section 9 fixes 21 specimens x 3 arms = 63 observations, gives every protected class
  two observations per arm, expressly declines to estimate a catch rate, and preregisters strict dominance
  as the safety co-primary before any run.
- **Span attacks:** an `ns` form that is not first correctly detects the planted require reorder
  (`require_reorders=1`, no reconciliation/alias failure). A `:bb` require branch inside the `ns` form
  refuses the `.cljc` file with two reconciliation failures and `unanalysed_platforms=[:bb]`. The second-
  `ns` attack fails as PB-FENCE-017 describes.
- **Grep audit:** `sc/alias-map`, `sc/refer-map`, `sc/imports-of`, and `sc/ns-name-of` have no callers.
  `sc/form-owner` and `sc/requires-of` serve reconciliation. `sc/children` at lines 372-376 is the remaining
  non-reconciled structural reader and is independently falsified by PB-FENCE-018.
- **Exact delta:** `a8ec1923..de83b75d` changes only `bin/preservation-brief` (+88/-26),
  `bin/preservation-replay` (+55/-1), and `bin/preservation-tables-test` (+39/-1). `git diff --check` is
  clean.

## Decision

PB-FENCE-014/015/016 are repaired, but the candidate still manufactures review burden from private or
misassociated structure. A second namespace form is described as dynamic non-load-order usage, and a
resolver-confirmed early `declare` is missed by an unreconciled private parser and reported as a load-order
break. Both contradict the candidate's own proof-burden and grep-audit claims. Do not ship.


> END RECEIPT (fence-run): worktree HEAD at review exit = e970f56e6cdcf0193c7b1d0511f36ee0b969bd47 = fenced sha.
