NO-GO

# Sol fence review, round 8: preservation brief

- reviewed: sealed candidate `65cb317de454b3315d6b727b6c9781c77aea85d7`
- branch tip accepted by the brief: `5ec9edc5bd10360f39ae24e03fdb09041d2e3814`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 7 `NO-GO` on PB-FENCE-012
- excluded as directed: `make test`; verification used `bin/` only

## Blocking finding

### PB-FENCE-013 — an analysed `#?@` require-order change is reported clear

The PB-FENCE-012 repair closes the exact unanalysed-platform omission, but the requested
`#?@` attack exposes another mismatch between the advertised CLJC scope and the private
`ns`-form parser. Given a valid `.cljc` namespace whose require clause is:

```clojure
(:require #?@(:clj [[clojure.set :as set]
                     [clojure.string :as str]]
                :cljs [[clojure.set :as set]
                       [clojure.string :as str]]))
```

and a candidate that reverses those two libraries in both analysed branches, the brief exits
0 with `clear: true`. It reports `ns_edits=1`, but `require_reorders=0` and
`require_sort_broken=0`; the hard-stop list is empty. A4 parses each complete `#?@(...)`
expression as one synthetic library, printing one giant removed value and one giant added
value. B4 then says the require order did not change.

That is a semantic load-order change inside the analyzer's declared scope, precisely the class
the brief says it inventories and hoists. A reviewer following the opening result is told no
decision remains. The checker therefore still cannot certify CLJC files carrying analysed
reader conditionals in an `ns` form.

Retained witness:

- base: `36ac33598ad5b1eaaec68f9ce66c58eb2c09f0c7`
- candidate: `055bfe874167665d3394a1ccc04190d5a7fac8e7`
- brief: `/var/tmp/forge/sol-r8.EQ93nm/feature-probes/spliced-require-reorder.md`
- machine result: `/var/tmp/forge/sol-r8.EQ93nm/feature-probes/spliced-require-reorder.json`
- recoverable object-database fixture: `/var/tmp/forge/sol-r8.EQ93nm/feature-probes/spliced-require-reorder`

Required repair: structurally expand analysed `#?@` branches while inventorying `ns` clauses,
or refuse such a file. The repair belongs in `bin/`, which the ship-fix contract classifies as
`HOLD reason=oracle-changed`; I did not patch it.

## PB-FENCE-012 repair and requested feature probes

- **Exact unanalysed regression:** deleting `#?(:bb ^:probe (def hidden 1))` now exits 3,
  with one reconciliation failure and `unanalysed_platforms [":bb"]`.
- **Analysed control:** `#?(:clj (def seen 1) :cljs (def seen 2))` has zero reconciliation
  failures and two reader-conditional occurrences across the two trees. The separate edit to
  `visible` is correctly raised as an in-place change.
- **Namespaced keyword:** `:foo/bar` is named in `unanalysed_platforms` and refuses the file.
- **Symbol feature:** the invalid non-keyword feature fails closed through the analyzer-failure
  path and is not certified.
- **Conditional nested in a definition body:** changing an analysed branch is not silently
  compared. It is raised as changed/frozen. The resolver emits the surrounding owner once per
  analysed language, so the report counts two in-place obligations; conservative, but not an
  escape.
- **Section D discrepancy:** contrary to the round-8 brief, the generated report still says
  “`.cljc` branches are compared as text; no platform was elaborated.” It also retains the stale
  warning that local bindings can be canonicalised as owners. Those statements contradict the
  implemented analyzer/reconciliation path and should be corrected with the functional repair.

## Seven prior hostile probes

All seven were rerun against the candidate on the real Cell C-derived replay tree:

| probe | result |
|---|---|
| `are-binding` | `clear=false`; preserved 99 → 98; changed/frozen body |
| `uppercase-alias` | `clear=false`; preserved 99 → 98; changed/frozen body |
| `kind-collision` | `clear=false`; preserved 99 → 98; omitted/kind and frozen signals |
| `referred-testing` | `clear=false`; preserved 99 → 98; changed/frozen body |
| `refer-all-shadow` | `clear=false`; preserved 99 → 98; changed/frozen body |
| `use-shadow` | `clear=false`; preserved 99 → 98; changed/frozen body |
| `refer-all-external` | `clear=false`; preserved 99 → 98; changed/frozen body |

The direct wrong-namespace canonicalisation falsifier also drops preserved bodies 99 → 98,
and the local-shadow substitution is raised as changed rather than preserved. The candidate
therefore does not regress the earlier resolution repairs.

## Other requested checks

- **Section-B-only defects are loud:** silent promotion increments the promotion count and is
  hoisted as `PRIVACY CHANGES`; the form-order defect hoists two new forward references; the
  require-order defect hoists a broken sorted-order signal. All exit nonzero above section A.
- **Independence:** the three implementation files require no `clj-surgeon.*` namespace.
  `preservation-brief` reads a receipt only after its A/B derivations, for section C.
- **Real-tree provenance:** `preservation-replay` initializes its scratch repository from
  `git archive` of Curtaincall CFP `d9205abc` and `65ad613b`; the main replay is not a synthetic
  source seed.
- **Table ratchet:** `bin/preservation-tables-test` exits 0: 202 core functions, 72
  modelled/frozen Vars, 31 claims, 38 external-resolver witnesses, 274 generated shadows, and
  269 shadows actually applied.
- **Cell C control:** clean replay remains 99/141 relocated bodies preserved, 32/64 in-place
  bodies preserved, and zero reconciliation failures.
- **Preregistration:** section 9 explicitly says 21 specimens x 3 arms = 63 observations,
  guarantees two observations per protected class per arm, declines to estimate a catch rate
  at that denominator, and preregisters strict dominance instead. The co-primary is honest
  before any run.
- **Exact candidate delta:** `33e65cef..5ec9edc5` changes only
  `bin/preservation-brief` (+53/-7), `bin/preservation-replay` (+37/-2),
  `bin/preservation-tables-test` (+1/-1), and `bin/preservation_scan.clj` (+37/-0).
  `git diff --check` is clean; no other candidate path changed.

The bounded replay was stopped after the seven requested historical probes completed, before
repeating its final metadata and two CLJC rows; the two CLJC rows were then run directly on
small object-database fixtures. No complete fresh 21-row replay is claimed.

## Decision

Do not ship this candidate as a proof-burden reducer. The round-7 omission is closed and all
seven earlier hostile resolution probes remain fail-closed. But an analysed reader-conditional
require reorder receives the strongest possible false result: exit 0, `clear: true`, “no
decision was left open.” That violates A4/B4 and the repair's claim that certification scope
matches the analyzer's.


> END RECEIPT (fence-run): worktree HEAD at review exit = 65cb317de454b3315d6b727b6c9781c77aea85d7 = fenced sha.
