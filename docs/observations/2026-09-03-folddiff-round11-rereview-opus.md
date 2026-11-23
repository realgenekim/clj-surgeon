# Fold-diff round 11 — Opus executed re-check of curtaincall-cfp bridge/fold-diff-tool at 3d344432 (2026-09-03T22:03Z)

Verdict: **GO-WITH-FIX — and 3d344432 MAY REPLACE THE 347fe6d3 PIN.** Round-10 blocker closed on the reviewer's first command on a fresh clone: `bin/test-fold-diff-checkpoint` 25 cases, exit 0, `data/` absent before and after, no seeding. Findings 1–3 and notes 4–7 CLOSED. Remaining regression holes (hardening, filed to the ledger, no new round under the 50% functional floor): `ns-unmap` + `refer` replaces the pinned var with no rebinding verb; a rebind in a sibling file via `load`/`require` is invisible (single-file scan); `names-var?` inspects only direct args. Notes: `LOG=` override vs case 0; precondition advice; last-contract-line rule fail-safe; a possibly dead `driver-stdout-unwritable` path.

## Opus verdict, verbatim

# Round-eleven executed re-review — curtaincall-cfp, branch bridge/fold-diff-tool @ 3d344432

Reviewer: independent Opus seat. Checkout `/home/forge/tmp/sol/folddiff11-wt`,
`git rev-parse HEAD` -> `3d344432f4e6040ae0c96c20910c027ad0ddcbb6` (detached); `git worktree list`
shows only this checkout. Nothing was committed, stashed or pushed; `git status --porcelain` was
empty at start and at end. Every driver mutation used for a fails-first check was made on a COPY
under `/tmp/folddiff11-sol-fx/`, never on a tracked file. Fixtures: `/tmp/folddiff11-sol-fx` only.
`make seed-demo` was NOT run at any point.

**Repository store under `data/`: ABSENT at start, and still ABSENT after the full 25-case
self-test.** `ls data/` -> "No such file or directory" before the first command of this review and
again after `bin/test-fold-diff-checkpoint` returned 0. That is the point of round eleven's first
commit, and it holds. (`data/store/events.jsonl` does appear later, created by the FULL KAOCHA UNIT
SUITE in gate 3 — 5 rows — which is exactly the ambient state that made round ten's gate red. It no
longer matters: the checkpoint suite neither reads nor creates it. `git status --porcelain` is
empty throughout; `data/` is gitignored.)

---

## VERDICT: **GO-WITH-FIX** for the production read pinned at 347fe6d3.

**3d344432 MAY REPLACE THE PIN.** Round ten's single merge blocker — the gate's own self-test being
red on a fresh checkout — is closed on my own run: `bin/test-fold-diff-checkpoint` on this clone,
with no `data/` store and no seeding, is **25 cases, all green, exit 0, 4m48s**, and leaves `data/`
still absent. Every one of round ten's findings 1-3 and notes 4-7 is closed by my own reproduction.
The `-WITH-FIX` is for round twelve and touches nothing the pinned read can reach: three sibling
shapes of round-ten finding 2 still slip past `rebinds-var?`, and the self-test's documented `LOG=`
hand-drive override is now unusable while the file still documents it.

---

## Round-ten findings 1-3 and notes 4-7 — my own re-runs

### 1. CLOSED — a phase's receipt now binds its exit code
`bin/fold-diff-checkpoint:881` (`phase_claim`), `:898` (`phase_receipt_line`),
`:1029-1033` (baseline arm), `:1101` + `:1106-1111` (candidate arm).

My own shim `clojure` (`/tmp/folddiff11-sol-fx/rig/shimbin/clojure`), written independently of the
suite's, real driver, real refs, 120-row private log and private data dir:

| candidate prints | exits | driver EXIT at 034fba53 (round ten) | driver EXIT at 3d344432 |
|---|---|---|---|
| `REFUSED :checkpoint-invalid` | 0 | **0 — IDENTICAL** | **4** |
| `FAILED :oom` | 1 | **1 — DIFFERENCES FOUND** | **4** |
| bare `VERDICT:` (no verdict text) | 1 | **1 — DIFFERENCES FOUND** | **4** |
| `VERDICT: 1 difference(s)` | 2 | 2, contradiction unnamed | **4** |
| `VERDICT: IDENTICAL` (control) | 0 | 0 | **0** |
| baseline `REFUSED :checkpoint-invalid` | 0 | 4 (round ten) | **4** |

Each of the four disagreements names itself, verbatim from my run:

```
FAILED :candidate-diff-contract-mismatch REFUSED :checkpoint-invalid — injected 0
FAILED :candidate-diff-contract-mismatch FAILED :oom — injected 1
```

### 2. CLOSED — a REBIND of the pinned var voids the pin
`bin/fold-diff-required-vars.bb:258` (`rebinds-var?`), wired in at `:320`; `:241` (`rebinding-verbs`).

My own reconstruction of the round-ten witness — this tree's shipping `fold_diff.clj` with
`'cfp-scheduler-killer.store-checkpoint/write-checkpoint!` deleted from the `*write-vars*` literal
and re-introduced by `(alter-var-root #'*write-vars* (constantly (vec (concat *write-vars*
extra-write-vars))))` over an imported set (`/tmp/folddiff11-sol-fx/real/d3-real-mutated.clj`):

```
round ten (034fba53) : :OK, derived set: checkpoint-path validate      exit 0
round eleven         : REFUSED :required-vars-unresolvable             exit 2
                       "…does not define *write-vars* as a LITERAL collection of quoted symbols"
```

Control: the unmodified `src/cfp_scheduler_killer/fold_diff.clj` still scans `:OK` with exactly
`checkpoint-path / validate / write-checkpoint!`, exit 0. Three sibling shapes still slip — new
findings 1-3 below.

### 3. CLOSED — the self-test owns its log; a fresh checkout reproduces its green
`bin/test-fold-diff-checkpoint:60-70` (builds the log under `$TMP` from the committed fixture),
`:84-89` (the >80 floor, asserted with its reason), `:93` (`export FOLD_DIFF_DATA_DIR`),
`:112-152` (case 0).

**My run on this fresh clone with NO `data/` store, the first command of the review:**

```
log: /tmp/fold-diff-selftest-A3yOVW/data/store/events.jsonl (120 rows, from resources/judge-sandbox/events.jsonl)
FOLD_DIFF_DATA_DIR: /tmp/fold-diff-selftest-A3yOVW/data
…
✓ bin/test-fold-diff-checkpoint: 25 cases, all green
real 4m47.755s      EXIT=0
ls data/  ->  No such file or directory        (before AND after)
```

Round ten got `✗ row 13 … got '<none>' (exit 0)`, exit 1, on the same starting condition.

My 5-row-log scenario, driven by name (`LOG=/tmp/folddiff11-sol-fx/log5.jsonl`, the shape the unit
suite used to leave behind):

```
EXIT=3
PRECONDITION: store log has 5 rows, case 13 needs >80 — run make seed-demo
```

The misnamed row-13 red is gone; the floor names the real cause and uses the reserved 3.

### 4. CLOSED — the stale summary text
`bin/test-fold-diff-checkpoint:1610` now reads "a forced baseline-emit failure is exit 4 (NO
VERDICT," and case 4 (`:429`) asserts 4. The two copies agree.

### 5. CLOSED — a partial verdict is no longer accepted
`bin/fold-diff-checkpoint:980` (`CANDIDATE_CONTRACT_RE`) is still a PREFIX match, so `phase_spoke`
still admits a bare `VERDICT:`; it is `phase_claim`'s `/^VERDICT:/ { claim = "" }` arm (`:887`)
that kills it. Witness: `printf 'VERDICT:'` + exit 1 -> **exit 4**, `FAILED
:candidate-diff-contract-mismatch`. Closed at the claim rather than at the RE, which is fine —
but the RE is no longer the place a reader should look for the contract.

### 6. CLOSED, and WIDER than filed — no `set -e` failure is spelled 1
`bin/fold-diff-checkpoint:208` (`verdict_guard`), `:228` (armed before the worktrees), `:678`
(chained ahead of `cleanup`, `$?` read first), header table `:88-96`.

```
SIGPIPE at its default,  driver | head -1        PIPESTATUS0=141
SIGPIPE ignored (trap '' PIPE), driver | head -1 PIPESTATUS0=4
                                                 FAILED :driver-aborted — the driver itself failed
                                                 (`set -e`) before reaching a verdict …
```

The second row is the one round ten's note did not reach, and it was a real DIFFERENCES FOUND
before this commit. Note which handler caught it: `verdict_guard`, not `emit_captured` — see new
note 7.

### 7. CLOSED — the contradiction is named in the transcript
`VERDICT: 1 difference(s)` + exit 2 is now `FAILED :candidate-diff-contract-mismatch VERDICT: 1
difference(s) — injected 2`, exit 4, on stderr with the full both-directions contract table
(`bin/fold-diff-checkpoint:945-963`), instead of a silent `==> REFUSED during phase`.

---

## Round-eleven attacks

### (a) The self-built log — deterministic, and rows 13/16 provoked for real

**Deterministic: yes, by construction and by measurement.** The fixture is tracked at this commit
(`git cat-file -p 3d344432:resources/judge-sandbox/events.jsonl | sha256sum` ==
`sha256sum resources/judge-sandbox/events.jsonl` == `d8ec652d8a7fe31f…`, 3246 rows), and the
suite's construction (`head -n 120` + the inline python checkpoint builder) is byte-stable across
two independent builds:

```
run1 log=42ce9fd07d327a0f checkpoint=ebe526e4c9e0f569 rows=120
run2 log=42ce9fd07d327a0f checkpoint=ebe526e4c9e0f569 rows=120
```

**Rows 13/16 are provoked for real, and the >80 floor is load-bearing, not decoration.** Two runs:

```
FIXTURE_ROWS=79, floor in place (shipped file)
  -> EXIT=3  PRECONDITION: store log has 79 rows, case 13 needs >80

FIXTURE_ROWS=79, floor lowered to 0 on a SCRATCH COPY (/tmp/folddiff11-sol-fx/test-nofloor.sh)
  -> ✗ row 13: expected :baseline-other-checkpoint to be the FIRST refusal,
       got 'REFUSED :log-shorter-than-checkpoint' (exit 2)
     EXIT=1
```

So case 13 really does go red one row short of the floor, and the floor is exactly what converts
that misleading red into a named precondition. In the 120-row default run rows 9-16 all pass, so
the case is mutation-sensitive in both directions.

### (b) `phase_claim` reads the LAST contract line — which wins, and can data impersonate a receipt?

**The LAST line wins, and yes, a column-0 data line placed AFTER the verdict does speak for the
phase — in the fail-safe direction only.** Measured, real driver, shim candidate:

| candidate prints | exits | driver EXIT |
|---|---|---|
| `VERDICT: IDENTICAL …` then `REFUSED :x` at column 0 | 0 | **4**, `FAILED :candidate-diff-contract-mismatch REFUSED :x 0` |
| `REFUSED :x` at column 0 then `VERDICT: IDENTICAL` | 0 | **0** (last wins; the data line is ignored) |
| `RELATION …` + `      baseline : "REFUSED :x"` + `VERDICT: IDENTICAL` | 0 | **0** |

**Can a real data line ever reach column 0?** No, on three independent grounds, all verified on a
REAL perturbed run rather than argued: every differing-path value is emitted indented
(`src/cfp_scheduler_killer/fold_diff.clj:587-591`, `  %s` for the path and `      baseline : %s` /
`      this tree: %s` for the values); the values go through `show` (`:504`), which is
`(truncate (pr-str v) chars)` and therefore escapes any embedded newline; and the verdict is the
last thing emitted (`:622`) apart from 9-space-indented gap notes (`:628-632`). My real perturbed
run's tail:

```
RELATION :events — 1 differing path(s)
  [:events "enterprise-ai-summit-charlotte-2026" :name]
      baseline : "PERTURBED-BY-REVIEWER"
      this tree: "Enterprise AI Summit"

SUMMARY
  :events                            1
  total                              1

VERDICT: 1 difference(s) — merging the candidate ref changes what production's existing log MEANS.
```

**Verdict on (b): "last wins" is the right choice and it is safe today.** The residual is a
denial-of-verdict, never a false verdict: a data line that got to column 0 after the verdict would
turn a true IDENTICAL into exit 4, which is the reserved no-verdict code. Filed as note 6.

### (c) `verdict_guard` — the real DIFFERENCES path is untouched, and `trap - EXIT` cannot swallow a verdict

**A legitimate exit-1 verdict still reaches the caller as 1.** Two independent witnesses:

```
shim candidate: VERDICT: 3 difference(s) + exit 1          -> driver EXIT=1
REAL run, both JVMs real, baseline EDN perturbed in one
  :events … :name value by a pass-through shim              -> driver EXIT=1
     VERDICT: 1 difference(s) — merging the candidate ref changes what production's log MEANS.
```

**Ordering cannot swallow a real verdict.** `trap - EXIT` at `:1085` disarms the guard immediately
after `emit_captured "$DIFF_OUT"` and BEFORE the only `exit 1` in the file. I re-derived "only" as
a fact rather than reading the comment: `grep -n '^\s*exit ' bin/fold-diff-checkpoint` returns
nineteen exits, of which the literals are `0` (x4), `2` (x6) and `"$PHASE_CRASHED_EXIT"` (x7); the
sole variable exits are `exit "$DIFF_EXIT"` at `:1117` (after the disarm) and the exit-2/3
pass-through at `:1056`. No `exit 1` is reachable while the guard is armed, so the guard can only
ever remap a `set -e` abort.

**Both SIGPIPE dispositions witnessed:** default -> 141, ignored -> 4 with `FAILED :driver-aborted`
(quoted in finding 6 above).

### (d) `rebinds-var?` — six shapes, three refused, one correctly ignored, three SLIPPED

All run through `bin/fold-diff-checkpoint --self-test-required-vars` against a scratch copy of the
shipping `fold_diff.clj` with `write-checkpoint!` removed from the `*write-vars*` literal, so a
`:OK` answer that omits `write-checkpoint!` is the round-ten finding-2 signature exactly.

| shape | result |
|---|---|
| `(def ^:redef *write-vars* …)` + `alter-var-root` | **REFUSED** exit 2 |
| `(declare *write-vars*)` before the literal, no rebind | `:OK`, full set — correct, `declare` writes nothing |
| `(declare *write-vars*)` + `alter-var-root` | **REFUSED** exit 2 |
| `(alter-meta! …)` / `(vary-meta …)` only | `:OK`, full set — correct, metadata is not a value |
| `#_(alter-var-root #'*write-vars* …)` (reader discard) | `:OK` — **correctly ignored**: the reader never produces the form and it never runs |
| `(comment (alter-var-root #'*write-vars* …))` | **REFUSED** exit 2 — over-conservative, safe direction |
| **`(ns-unmap *ns* '*write-vars*)` + `(refer 'other :only '[*write-vars*])`** | **`:OK`, derived set `checkpoint-path validate` — SLIPPED** |
| **`(alter-var-root (val (find (ns-interns 'cfp-scheduler-killer.fold-diff) '*write-vars*)) …)`** | **`:OK`, `checkpoint-path validate` — SLIPPED** |
| **`(load "fold_diff_extra")`, and a plain `:require` of a sibling ns that rebinds** | **`:OK`, `checkpoint-path validate` — SLIPPED** |

The two slips that are not merely a source-level gap were proved to substitute the value at
runtime, under `bb`:

```
ns-unmap + refer :  before: [a/one a/two]      after : [scratch/EXTRA-ONLY]
(load "…")       :  before: [a/one a/two]      after : [SUBSTITUTED/only]
```

Filed as new findings 1-3.

### (e) Case 0 vs `LOG=` — the documented hand-drive override is dead

```
LOG=<5-row file>    -> EXIT=3  PRECONDITION: store log has 5 rows, case 13 needs >80
LOG=<100-row file>  -> EXIT=1  ✗ the suite's log is /tmp/folddiff11-sol-fx/log100.jsonl —
                                 outside its own temp workdir /tmp/fold-diff-selftest-YmQrGX.
                                 This suite must not read (or depend on the contents of) the
                                 repo's gitignored data/ store; …
```

So there is no row of `LOG=` that reaches a case: undersized logs die at the floor, valid logs die
at case 0. The suite does not name the conflict — it says the log must not come from the repo's
`data/` store, and mine was in `/tmp`. `bin/test-fold-diff-checkpoint:56-59` still advertises the
override in the present tense. Filed as note 4.

### (f) The exit-code table — all six witnessed, and the header matches

| exit | my witness |
|---|---|
| **0** | `BASELINE_REF=HEAD CANDIDATE_REF=HEAD`, private 120-row log -> `VERDICT: IDENTICAL`, and self-test case 2 |
| **1** | real perturbed baseline, real JVMs both sides -> `RELATION :events — 1 differing path(s)`, `VERDICT: 1 difference(s)` |
| **2** | `BASELINE_REF` unset -> `REFUSED :baseline-ref-unset` |
| **3** | `FOLD_DIFF_MAX_PATHS=not-a-number` -> `FAILED :bad-env — clojure.lang.ExceptionInfo: FOLD_DIFF_MAX_PATHS="not-a-number" is not a number` |
| **4** | five receipt/exit disagreements, plus `FAILED :baseline-emit-crashed` on a bad `-Xmx`, plus SIGPIPE-ignored |
| **141** | `driver | head -1` with SIGPIPE at its default |

The header's table (`bin/fold-diff-checkpoint:64-96`) agrees with every row, including 141 being
outside it, and it now states the both-directions receipt contract. No verdict shares a code with a
failure.

---

## Gates — verbatim, all under `/home/forge/bin/suite-run`

```
###### GATE 1: bin/kaocha unit --focus cfp-scheduler-killer.fold-diff-test
56 tests, 226 assertions, 0 failures.                        GATE1_EXIT=0
###### GATE 1b control: bin/kaocha unit --focus fold-diff-test  (unqualified)
WARNING: All 1486 tests were skipped. …                      GATE1B_EXIT=0
###### GATE 2: --focus …intent-contract-test --focus …intent-registry-architecture-test
                --focus …intent-witness-identity-architecture-test
9 tests, 1083 assertions, 0 failures.                        GATE2_EXIT=0
###### GATE 3: bin/kaocha unit
1102 tests, 13436 assertions, 0 failures.                     GATE3_EXIT=0
###### GATE 4: make compile-check
✓ Application namespaces compiled successfully                GATE4_EXIT=0
###### GATE 5: bin/test-fold-diff-checkpoint  (AS FOUND, fresh clone, NO data/ store, no seeding)
✓ bin/test-fold-diff-checkpoint: 25 cases, all green         GATE5_EXIT=0   (4m47.755s)
```

Selected-test count for gate 1: **56 selected, 56 ran**; the unqualified control skips all 1486, so
the fully qualified namespace is what selects.

---

## Findings for round twelve

1. **[OPEN, regression hole]** `bin/fold-diff-required-vars.bb:241` (`rebinding-verbs`) +
   `:258` (`rebinds-var?`) — a var can be REPLACED without any rebinding verb, by unmapping it and
   referring another namespace's var of the same name. Witness:
   `/tmp/folddiff11-sol-fx/real/e3-nsunmap-refer.clj` (this tree's `fold_diff.clj`, `write-checkpoint!`
   out of the literal, plus `(ns-unmap *ns* '*write-vars*)` and
   `(clojure.core/refer 'scratch.othervars :only '[*write-vars*])`) scans **`:OK`, derived set
   `checkpoint-path validate`** — `write-checkpoint!` gone from the required set, so a ref whose own
   `store_checkpoint.clj` lacks it would be judged complete. Runtime proof under `bb`:
   `before: [a/one a/two]` -> `after : [scratch/EXTRA-ONLY]`. Fix: treat `ns-unmap` and `refer` /
   `refer-clojure` naming the pinned var as rebinds, and add the shape to case 17.

2. **[OPEN, regression hole]** `bin/fold-diff-required-vars.bb:258` — `rebinds-var?` scans only the
   ONE file it was handed, so a rebind in a second file of the SAME namespace is invisible.
   Witness: `/tmp/folddiff11-sol-fx/real/e7-load-sibling.clj` (`(load "fold_diff_extra")`, and plain
   `load` is in neither `opaque-code-verbs:145` nor `rebinding-verbs:241`) and
   `e7b-require-sibling.clj` (a plain `:require` of a sibling ns) both scan **`:OK`,
   `checkpoint-path validate`**. Runtime proof: `(load "probe_extra")` -> `after : [SUBSTITUTED/only]`.
   Fix: add `load` to the opaque set (it runs code this scan never reads, exactly like `load-file`),
   and say in the pin that `:literal-def` vouches only for a single-file namespace.

3. **[OPEN, regression hole]** `bin/fold-diff-required-vars.bb:248` (`names-var?`) inspects only the
   DIRECT argument nodes of a rebinding verb, so the var can be reached through one level of
   expression. Witness: `/tmp/folddiff11-sol-fx/real/e6b-nested-no-forge.clj`,
   `(alter-var-root (val (find (ns-interns 'cfp-scheduler-killer.fold-diff) '*write-vars*)) (constantly …))`
   scans **`:OK`, `checkpoint-path validate`**. (The `(symbol "*write-vars*")` variant IS refused,
   but by the forging net at `:132`, not by this check — so the coverage is accidental.) Fix: make
   `names-var?` search the rebinding call's whole subtree, not its top-level args.

4. **[NOTE]** `bin/test-fold-diff-checkpoint:56-59` documents `LOG=` as a live hand-drive override
   ("remain overridable … the >80 floor below is enforced for those runs too, by NAME"), but case 0
   at `:135-143` fails every log outside `$TMP`, so no `LOG=` value can reach a case. Witness:
   `LOG=<100-row file>` -> exit 1, `✗ the suite's log is /tmp/… — outside its own temp workdir`,
   with the reason given as "must not read … the repo's gitignored data/ store" when the log was in
   `/tmp`. Two copies of the truth disagreeing inside the file whose case 13 exists to stop that.
   Fix: either delete the `LOG=` sentence, or let case 0 accept an explicit `LOG=` and assert only
   that the DEFAULT is `$TMP`-owned — and name the conflict when both are given.

5. **[NOTE]** `bin/test-fold-diff-checkpoint:85-88` — the floor's message says "run make seed-demo"
   and then "(or leave LOG unset and this suite builds its own N-row log…)" even when `LOG` was
   already unset and the actual lever is `FIXTURE_ROWS`. Witness: `FIXTURE_ROWS=79` ->
   `PRECONDITION: store log has 79 rows, case 13 needs >80 — run make seed-demo`, advice which, if
   followed, lands on finding 4's exit 1. Fix: branch the message on whether `LOG` was supplied.

6. **[NOTE]** `bin/fold-diff-checkpoint:881` — `phase_claim` takes the LAST contract line, so a
   column-0 line inside a phase's DATA that appears after the verdict speaks for the phase.
   Witness: `VERDICT: IDENTICAL …` followed by `REFUSED :x` at column 0, exit 0 -> exit 4,
   `FAILED :candidate-diff-contract-mismatch REFUSED :x 0`. Unreachable today — every report value
   is indented (`fold_diff.clj:587-591`) and passes through `pr-str` (`:504`), and the verdict is
   emitted last (`:622`) — and the failure direction is a denied verdict, never a false one. Worth
   one anchored guard (only accept a receipt in the last N lines, or require the report's own
   column-0 vocabulary to be disjoint) if the report ever gains a free-form trailer.

7. **[NOTE]** `bin/fold-diff-checkpoint:88-91` — the header says a closed stdout "dies at `printf`
   under `set -e`". In my SIGPIPE-ignored witness the first failing write was one of the driver's
   own `echo`s well before `emit_captured`, so the run landed on `verdict_guard`'s
   `FAILED :driver-aborted` (`:208`) and never on `emit_captured`'s `FAILED
   :driver-stdout-unwritable` (`:923`). The widened guard is what makes note 6 safe; the sentence
   naming `printf` is narrower than the code that saves it, and `emit_captured`'s dedicated message
   may in practice be dead. Fix: say "any write to stdout", and check whether `emit_captured` is
   still reachable.

---

## Verdict

**GO-WITH-FIX for the production read pinned at 347fe6d3, and 3d344432 MAY REPLACE THE PIN.**

Round ten withheld the pin for exactly one reason — a merge gate whose own self-test was red on
checkout — and that is closed on my own first command against this clone: 25 cases, all green, exit
0, no `data/`, no seeding. Findings 1 and 2 of round ten are closed on my own reproductions, notes
4-7 are closed, and every exit code including 141 is witnessed and matches the published table. The
three open items are all in `bin/fold-diff-required-vars.bb`, all require an edit to this tree's own
`fold_diff.clj` to exploit, and the tool TRAVELS — both phases run the tree under test — so no
foreign ref can reach them. They cannot affect the pinned read; they are round twelve's work.
