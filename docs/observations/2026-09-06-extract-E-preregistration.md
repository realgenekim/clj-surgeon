# Preregistration — cell extract-E (DRAFT, expectations blank)

**Task.** Extract five non-database forms (`instance`, `database`, `iam-jdbc-url`, `get-ds`,
`deep-merge`) from `src/maven/db.clj` into a new `maven.db-helpers`
(`src/maven/db_helpers.clj`) in the-gene-maven @0eecb55a, updating 18 caller files: 14 pure
callers (alias retarget only) and 4 mixed callers (add a second require, requalify only the
moved-name sites). Full statement: `intent.txt`. Actor dossier: `dossier.txt`.

**Seed.** `seed/` — pristine copy of `cell-prep/out/fanout-B/seed` (37 files, repo + `proof/`).
Every arm starts from a fresh copy; no arm shares a workspace.

## Arms

| arm | prefix | route |
|---|---|---|
| **N** | `runner-b/prefixes/N.txt` (verbatim, unmodified dossier) | native only — shell/read/apply_patch; scripts allowed; Surgeon forbidden. Fresh **gpt-6-astra** seat. |
| **E** | `prefixes-proposal/E.txt` (substituted for the native prefix by `runner-b.dossier_for`) | one `inspect_clojure`, then ONE `apply_clojure_changes` with the `extraction` argument; one repair permitted from the refusal; bounded native fallback recorded as **zero tool-committed sites**. |

No codex actors. No JVM while `/var/tmp/forge/quiet-window.md` names an owner other than `fable`.

## Schedule

`N1, E1, E2, N2` — in that order; the pairs `(N1,E1)` and `(E2,N2)` may run in parallel.
Order alternates so a drift in machine load cannot land on one arm.

## Primary measure

**Wall** = fresh seed copy → actor terminal, **proof-inclusive** (the arm's own verification
runs inside the measured window). Reported per run and as a per-arm median of 2.

## Correctness (both required)

1. `python3 -I proof-e/run.py gate <ws>` — the repo's own tests: **58 tests / 190 assertions,
   0 failures, 0 errors**.
2. `python3 -I proof-e/run.py candidate <ws>` — `proof-e/witness.clj`: moved forms
   byte-identical in the destination and absent from the source; every caller requires
   `maven.db-helpers`; the mixed four keep `[maven.db :as mdb]` with their
   `mdb/emit-event!` / `mdb/rebuild-projection!` counts unchanged; every file outside
   {source, destination, 18 callers} byte-identical to seed. **WITNESS GREEN** required.

Witness calibration (run 2026-09-06): baseline seed = **RED, 24 typed failures**
(`destination-missing` ×1, `form-still-defined-in-source` ×5, `caller-missing-destination-require`
×18); hand-driven reference = **GREEN, 0 failures**, gate 58/190.

## Hypothesis (registered before any arm runs)

**E's median wall is more than 30 s below N's median wall, with 2/2 E runs correct**
(gate pass + witness GREEN). E is falsified if either E run is incorrect, if the median gap
is ≤ 30 s, or if E's route degrades to the native fallback in ≥ 1 run.

## Expectations

- N1: _______   N2: _______
- E1: _______   E2: _______

(Each seat records its own wall/outcome prediction before starting; blank at registration.)

## Frozen apparatus

`seed/`, `intent.txt`, `dossier.txt`, `prefixes-proposal/E.txt`, `proof-e/witness.clj`,
`proof-e/run.py`. Nothing in `runner-b/` was modified.


## Expectations (written before any actor; 19:25Z)
- Fable: native must move 5 forms by hand, create the ns, and update 18 callers (4 mixed) with scripts or edits, then run the gate: 150–300 s, correctness risk on the four mixed callers. E: orientation 20–40 s + plan probe 0.3 s + one submit 2.1 s + gate: 45–90 s if the actor fills the returned plan on the first repair; 2/2 correct. Expect E median < N median by well over 30 s — this is the class with no native verb.
- Astra: (his line, verbatim when posted)
