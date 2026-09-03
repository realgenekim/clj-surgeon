# Fold-diff round 10 — Opus executed re-check of curtaincall-cfp bridge/fold-diff-tool at 034fba53 (2026-09-03T20:29Z)

Verdict: **GO-WITH-FIX for the production read at 347fe6d3 — 034fba53 may NOT replace the pin yet.** All round-9 findings addressed (finding 1 closed on the reviewer's own reproduction; seven resolver-through-head shapes refused; truncated stdout → exit 4; five exit codes witnessed and disjoint; oracle = `--refusal-order`, row 9 provokes; 034fba53 load-bearing). BLOCKER (live): `bin/test-fold-diff-checkpoint` asserts only that the log EXISTS, but case 13 rows 13/16 need >80 rows — on the tree as found (5-line log) it exits 1; green only after an unrequested `make seed-demo`. Regression holes: `phase_spoke` requires a contract line but never that it AGREE with the exit code (a candidate printing `REFUSED …` + exit 0 → IDENTICAL); `literal-collection-def?` ignores a REBIND of `*write-vars*` (alter-var-root/binding to an imported set scans `:OK`). Notes: stale summary text (exit 3 vs 4), a bare `VERDICT:` accepted, SIGPIPE 141 outside the table, `VERDICT:` + exit 2 published as REFUSED unnamed. Round 11 launched.

## Opus verdict, verbatim

# Round-ten executed re-review — curtaincall-cfp, branch bridge/fold-diff-tool @ 034fba53

Reviewer: independent Opus seat. Checkout `/home/forge/tmp/sol/folddiff10-wt`,
`git rev-parse HEAD` → `034fba53b88e4160f80c92b848be56fb7b4425a5` (detached), `git worktree list`
shows only this checkout. Nothing was committed, stashed or pushed; `git status --porcelain` is
empty at start and at end. Every driver mutation used for a fails-first check was made on a COPY
under `/tmp/folddiff10-sol-fx/`, never on a tracked file. Fixtures: `/tmp/folddiff10-sol-fx` only.

**Repository store under `data/`: ABSENT at start** — `ls data/` → "No such file or directory"
(`.gitignore` covers `data/`). The full unit suite CREATED `data/store/events.jsonl` at 1071 bytes
/ **5 lines**, and that turned out to matter — see finding 3. After `make seed-demo` it is 149 lines.

---

## VERDICT: **GO-WITH-FIX** for the production read pinned at 347fe6d3.

**034fba53 MAY NOT REPLACE THE PIN as it stands.** Not because of anything it broke — all five
round-nine findings and both notes are addressed, three of them completely — but because the gate
this branch ships **does not pass on the tree as found**: `bin/test-fold-diff-checkpoint` exits 1
on a fresh worktree, and only reaches its 22-case green after a `make seed-demo` the gate never
asks for and never checks (finding 3). A merge gate whose own self-test is red on checkout cannot
be the thing a script reads an exit code from. That is a ten-line fix, and it is the only blocker.

**The pinned read itself is safe to take with 034fba53's driver**, and it is materially safer than
b223f64e's: round-nine finding 1 is closed with my own reproduction, the candidate side no longer
spells a compile failure `1`, and every way a phase can die now lands on the reserved 4.

---

## Prior findings 1–5 and notes 6–7 — my own re-runs

### 1. CLOSED — a candidate that never compiled is exit 4, not 1
`bin/fold-diff-checkpoint:777` (`phase_spoke`), `:802` (`CANDIDATE_CONTRACT_RE`), `:908-914`.

My own re-run of the round-nine witness, real candidate ref, real `clojure` for the candidate
phase (baseline shimmed so the run reaches it):

```
BASELINE_REF=HEAD CANDIDATE_REF=bd581934bf63e2ead0ae046820b53c4714beb5d7 → EXIT=4
  Syntax error compiling at (cfp_scheduler_killer/store_checkpoint.clj:113:15).
  No such var: store-pg/read-lines-with-seq
  FAILED :candidate-diff-crashed — the candidate diff exited 1 without printing any line of its own
    contract (no 'VERDICT:', no 'REFUSED :', no 'FAILED :'), so it died before it reached one
```

Round nine got `E_EXIT=1`, no line of any kind. Closed.

### 2. CLOSED — opaque code is refused on its own
`bin/fold-diff-required-vars.bb:145` (`opaque-code-verbs`), `:439-451` (checked FIRST, unconditionally).

`/tmp/folddiff10-sol-fx/c2-eval-readstring.clj` → `:UNRESOLVABLE`, offending form
`(eval (read-string "(cfp-scheduler-killer.store-checkpoint/checkpoint-path)"))`;
`--self-test-required-vars-guard` → `REFUSED :required-vars-unresolvable`, exit 2.
(Round nine: `:OK`, derived set `validate`, exit 0.)

### 3. CLOSED — a resolver reached through another resolver is a resolver
`bin/fold-diff-required-vars.bb:301` (`resolver-producing-call?`), `:315` (`resolver-call?`).

`/tmp/folddiff10-sol-fx/c1-indirect-resolver.clj` → `:UNRESOLVABLE`, guard exit 2.

### 4. CLOSED — syntax-quoted macro bodies are walked
`bin/fold-diff-required-vars.bb:347-359` (the quoted-datum branch removed).

`/tmp/folddiff10-sol-fx/c5-macro-emits.clj` (``(defmacro resolve-var [s] `(requiring-resolve ~s))``)
→ `:UNRESOLVABLE clojure.core/requiring-resolve`, guard exit 2.

Control for 2–4: the shipping `src/cfp_scheduler_killer/fold_diff.clj` still scans `:OK` with
exactly `checkpoint-path / validate / write-checkpoint!`, exit 0.

### 5. PARTIAL — the pin now carries context, but `:literal-def` does not see a REBIND
`bin/fold-diff-required-vars.bb:180` (`known-dynamic-resolvers`), `:241` (`literal-collection-def?`),
`:430` (`pinned?`).

The shape it was filed for is closed: `/tmp/folddiff10-sol-fx/a2-pinned-no-forge.clj` (the pinned
`(fn [sym] …)` verbatim, `*write-vars*` `:refer`red from another ns) now returns `:UNRESOLVABLE`
naming both halves of the context, guard exit 2 — round nine had it `:OK / validate`, exit 0.

The residual moved rather than closed. See finding 2 below.

### 6. PARTIAL — the sibling-namespace boundary is now STATED but not ratcheted
`bin/fold-diff-checkpoint:136-152`, and the matching `SCOPE, STATED` clause in
`docs/intent/registry.edn` FOLD-DIFF-013.

I re-derived the claim rather than trusting it: `grep -n 'store/' src/cfp_scheduler_killer/fold_diff.clj`
shows `store/fold`, `store/postgres?`, `store/store-path`, `store/empty-state` and the two
`'cfp-scheduler-killer.store/append!` quoted symbols — **no `store/load!`** — so
`store.clj:959`'s dynamic `hydrate!` resolve is genuinely off the diff path today. But nothing
enforces it: `grep -n "load!" test/cfp_scheduler_killer/fold_diff_test.clj bin/test-fold-diff-checkpoint`
returns **nothing**. The boundary is a comment guarding a property a one-line edit can break.

### 7. CLOSED — case 5's exit assertion discriminates
`bin/test-fold-diff-checkpoint:312` — `[ "$CASE5" -eq 4 ]`, was `{0,1,2,3}`.

---

## Round-ten attacks

### (a) The receipt-line discriminator — which wins, and is a partial verdict accepted?

Driven with a shim `clojure` on PATH (`/tmp/folddiff10-sol-fx/fakebin/clojure`) so the phase's
printed line and its exit code can be varied independently. Baseline held at a clean receipt.

| candidate prints | exits | driver EXIT | published as |
|---|---|---|---|
| `VERDICT: IDENTICAL` | 7 | **4** | `FAILED :candidate-diff-crashed` — "printed a line of its own contract but exited 7" |
| `VERDICT: IDENTICAL` | 137 (SIGKILL) | **4** | same |
| (nothing) | 1 | **4** | `FAILED :candidate-diff-crashed` — "exited 1 without printing any line" |
| `VERDICT: 1 difference(s)` | 2 | **2** | `==> REFUSED during phase: candidate-emit (diff)` |
| **`REFUSED :checkpoint-invalid`** | **0** | **0** | **IDENTICAL** |
| **`FAILED :oom`** | **1** | **1** | **DIFFERENCES FOUND** |
| `VERDICT:` (bare, no content, no newline) | 1 | **1** | DIFFERENCES FOUND |

**Which wins:** neither — both are required, but only as a CONJUNCTION of two independent tests.
A missing receipt always wins (exit 4). A receipt plus a number OUTSIDE `{0,1,2,3}` also gives 4.
A receipt plus a number INSIDE that set is passed through **without ever checking that the line
and the number agree**. That is the last two rows: a phase that names a refusal gets published as
the tool's strongest positive claim, and a phase that names its own failure gets published as
DIFFERENCES FOUND — the exact sentence round nine's finding 1 was about, one row over.

**Is a partial verdict ever accepted:** yes. `printf 'VERDICT:'` with no verdict text and no
trailing newline satisfies `phase_spoke` (`printf '%s\n'` supplies the newline) and exits 1 →
driver exit 1. The discriminator tests a line PREFIX, not a complete verdict.

Not reachable at today's tool — `src/cfp_scheduler_killer/fold_diff.clj:207` is the sole `refused`
constructor and always sets `:exit 2`, and `main*`'s catch always returns 3 — and the tool TRAVELS
(both phases run this tree's `fold_diff.clj`), so a foreign ref cannot supply the mismatch. A
regression hole, not a live hazard. Filed as finding 1.

### (b) A phase whose stdout is truncated

| baseline prints | exits | driver EXIT | published as |
|---|---|---|---|
| `wrote …` + `fold-source-digest:` | 9 | 4 | `FAILED :baseline-emit-crashed` (receipt, wrong number) |
| `wrote …` only (digest lost) | 0 | **2** | `REFUSED :baseline-digest-unreadable` — row 9, correct |
| (nothing) | 0 | 4 | `FAILED :baseline-emit-crashed` |
| (nothing) | 141 (SIGPIPE) | 4 | `FAILED :baseline-emit-crashed` |

**Answer: exit 4, and never a false REFUSED.** The one truncation that produces a refusal produces
the RIGHT one — the emit really did print no digest line, which is precisely what row 9 says.
No truncation produces a verdict code. One aside: if the DRIVER's own stdout is a broken pipe
(`bin/fold-diff-checkpoint | head -1`) the script dies at `printf` under `set -e` with **141**,
which is outside the documented 0–4 table (note 6 below).

### (c) "Head PRODUCED a resolver" — all seven shapes REFUSED, none slipped

Every one returns `:UNRESOLVABLE`; none derives a silently smaller set.

| shape | refused at |
|---|---|
| `(-> 'x requiring-resolve)` | bare-symbol net, binder = the whole `defn` |
| `(apply requiring-resolve [sym])` | bare-symbol net |
| `(partial requiring-resolve)` | bare-symbol net |
| `(comp deref requiring-resolve)` | bare-symbol net |
| `(letfn [(rr [s] (requiring-resolve s))] ((rr sym) p))` | `(requiring-resolve s)`, binder = the `letfn` |
| `((clojure.core/requiring-resolve sym) p)` | `core-verb?` accepts the `clojure.core` qualifier |
| `#'clojure.core/requiring-resolve` then `(rr sym)` | bare-symbol net inside the var form |

The net that catches most of them is `bin/fold-diff-required-vars.bb:376-381` — a resolver verb
anywhere OTHER than a call head — not the new `resolver-producing-call?`. That is fine (it is the
conservative direction), but worth knowing: `resolver-producing-call?` earns its keep only for the
`(((ns-resolve 'clojure.core 'requiring-resolve) …) …)` shape, where the verb IS a head.
`(-> 'cfp-scheduler-killer.store-checkpoint/checkpoint-path requiring-resolve)` is refused even
though its target is literal — conservative, and correct per the header's own doctrine.

### (d) `:literal-def *write-vars*` — a REBIND slips

**SLIPPED.** `literal-collection-def?` (`bin/fold-diff-required-vars.bb:241`) looks for a top-level
`def`/`defonce` whose last form is a literal collection of quoted symbols. It never looks for a
rebinding, so the literal `def` can stand while the value that actually reaches the tolerated
resolver is computed elsewhere:

```
/tmp/folddiff10-sol-fx/d1-literal-def-then-rebound.clj      → :OK, derived set: validate
   (def ^:dynamic *write-vars* ['…/append!])                  ;; satisfies :literal-def
   (alter-var-root #'*write-vars*
     (constantly (vec (concat *write-vars* extra-write-vars))))  ;; imported from scratch.othervars

/tmp/folddiff10-sol-fx/d2-literal-def-binding-rebind.clj    → :OK, derived set: validate
   (binding [*write-vars* (vec (concat *write-vars* extra-write-vars))] (resolve-all))
```

And on the real file, which is what matters: `/tmp/folddiff10-sol-fx/real/d3-real-mutated.clj` is
the shipping `fold_diff.clj` with `'cfp-scheduler-killer.store-checkpoint/write-checkpoint!` moved
out of the literal and re-introduced through an `alter-var-root` over an imported set. It scans
**`:OK`, deriving `checkpoint-path / validate` — `write-checkpoint!` is gone from the required
set**, so a ref whose own `store_checkpoint.clj` lacks it would be judged complete. Case 17 does
not cover this: both of its shapes DELETE the literal (`:754`, `:766`), and neither keeps it while
rebinding. The docstring at `:244` even asserts the property it does not test — "Nothing computed,
imported, `into`-ed or **rebound** qualifies."

### (e) The exit-code table — every code witnessed, no verdict shares a code with a failure

| exit | meaning | my witness |
|---|---|---|
| **0** | IDENTICAL | `BASELINE_REF=HEAD CANDIDATE_REF=HEAD`, private 149-row checkpoint + private data dir → `fold-source-digest: 5293fb7b…5fa0`, `VERDICT: IDENTICAL — the candidate ref's folds project exactly what the baseline ref's do.`, `DRIVER_EXIT=0` |
| **1** | DIFFERENCES | real emitted baseline with one `:rooms … :name "Main Stage"` value perturbed → `RELATION :rooms — 1 differing path(s)`, `VERDICT: 1 difference(s) — merging the candidate ref changes what production's existing log MEANS.`, `TOOL_EXIT=1`; the driver re-emits it verbatim at `bin/fold-diff-checkpoint:923` |
| **2** | typed refusal | `BASELINE_REF` unset → `REFUSED :baseline-ref-unset`, exit 2 (and all 16 kinds via case 13) |
| **3** | the tool named its own failure | `FOLD_DIFF_MAX_PATHS=not-a-number` → `FAILED :bad-env — clojure.lang.ExceptionInfo: FOLD_DIFF_MAX_PATHS="not-a-number" is not a number`, `TOOL_EXIT=3` |
| **4** | NO VERDICT | `CANDIDATE_REF=bd581934…` → `FAILED :candidate-diff-crashed`, exit 4; and `FAILED :baseline-emit-crashed` for every silent-death baseline shape in (b) |

**No verdict shares a code with a failure, for today's tool.** But that disjointness is a property
of `fold_diff.clj` (one `refused` constructor at `:207`, one catch returning 3), not a property the
driver enforces — see (a). Outside the table: 141 on a broken driver stdout.

### (f) Case 13's oracle vs `--refusal-order`, and row 9

**Still equal, still mutation-sensitive.** I extracted the hand-written oracle straight out of
`bin/test-fold-diff-checkpoint:1002-1019` and diffed it against the shipped
`bin/fold-diff-checkpoint --refusal-order`: **identical, 16 rows**. Against a COPY of the driver
with rows 3 and 4 of `refusal_order()` transposed (published table only), the same diff prints the
2-line disagreement and exits 1 — the oracle is a real, independent witness, not a tautology.
The tracked file was never touched (`git status --porcelain` empty throughout).

**Row 9 provokes correctly after 034fba53, and 034fba53 is load-bearing.** My independent
reproduction of the row-9 shape (emit prints `wrote …`, no `fold-source-digest:`, exits 0):

```
shipped 034fba53 driver                        → EXIT=2   REFUSED :baseline-digest-unreadable
same run, BASELINE_CONTRACT_RE requiring the
digest line (the builder's pre-fix regex)      → EXIT=4   FAILED :baseline-emit-crashed
```

That is exactly the regression 034fba53's message describes — a published refusal relabelled a
crash — and case 13's oracle is what caught it. The full self-test confirms it end to end:
`row 9  :baseline-digest-unreadable`, all 16 rows in the published order.

---

## Gates — ran-lines verbatim, all under `/home/forge/bin/suite-run`

```
###### GATE 1: bin/kaocha unit --focus cfp-scheduler-killer.fold-diff-test
56 tests, 226 assertions, 0 failures.                    GATE1_EXIT=0
###### GATE 1b control: bin/kaocha unit --focus fold-diff-test   (unqualified)
WARNING: All 1486 tests were skipped. …                   GATE1B_EXIT=0
###### GATE 2: --focus …intent-contract-test --focus …intent-registry-architecture-test
                --focus …intent-witness-identity-architecture-test
9 tests, 1083 assertions, 0 failures.                     GATE2_EXIT=0
###### GATE 3: bin/kaocha unit
1102 tests, 13436 assertions, 0 failures.                 GATE3_EXIT=0
###### GATE 4: make compile-check
✓ Application namespaces compiled successfully            GATE4_EXIT=0
###### GATE 5: bin/test-fold-diff-checkpoint   (AS FOUND, 5-line log)
✗ row 13: expected :baseline-other-checkpoint to be the FIRST refusal, got '<none>' (exit 0)
                                                          GATE5_EXIT=1
###### GATE 5: bin/test-fold-diff-checkpoint   (after `make seed-demo`, 149-line log)
✓ bin/test-fold-diff-checkpoint: 22 cases, all green      GATE5_EXIT=0
```

Selected-test count for gate 1: **56 selected, 56 ran** — the unqualified control skips all 1486,
so the fully qualified namespace is doing the selecting.

---

## Findings for round eleven

1. **[OPEN, regression hole]** `bin/fold-diff-checkpoint:777` + `:908-931` — `phase_spoke` requires
   a contract line to be PRESENT but never that it AGREE with the exit code, so the two can
   contradict each other inside `{0,1,2,3}` and the number silently wins.
   Witness: a candidate phase printing `REFUSED :checkpoint-invalid` and exiting 0 makes the driver
   exit **0 — IDENTICAL**; one printing `FAILED :oom` and exiting 1 makes it exit **1 — DIFFERENCES
   FOUND**. Not reachable at today's tool (`src/cfp_scheduler_killer/fold_diff.clj:207` is the only
   `refused` constructor and pins `:exit 2`; `main*`'s catch returns 3), so no hazard to the pinned
   read. Fix: bind the line to the code — `VERDICT:` ⇒ `{0,1}`, `REFUSED :` ⇒ 2, `FAILED :` ⇒ 3,
   anything else ⇒ `phase_crashed` — which is finding 1's own doctrine ("the number is not
   evidence") applied in both directions.

2. **[OPEN, regression hole]** `bin/fold-diff-required-vars.bb:241` — `literal-collection-def?`
   checks for a literal `def` and never for a REBIND, so the allowlist exemption survives an
   `alter-var-root` or `binding` that replaces `*write-vars*` with a computed, imported set.
   Witness: the shipping `fold_diff.clj` with `write-checkpoint!` moved out of the literal and
   re-added via `(alter-var-root #'*write-vars* (constantly (vec (concat *write-vars*
   extra-write-vars))))` scans `:OK` deriving only `checkpoint-path / validate` —
   `write-checkpoint!` vanishes from the required set
   (`/tmp/folddiff10-sol-fx/real/d3-real-mutated.clj`). Case 17's two shapes both delete the
   literal (`bin/test-fold-diff-checkpoint:754`, `:766`) and cannot see this. Fix: refuse when the
   `:literal-def` var is also the target of `binding`/`alter-var-root`/`set!`/`with-redefs`
   anywhere in the file, and add the rebind shape as case 17's third fixture.

3. **[OPEN, LIVE — the merge blocker]** `bin/test-fold-diff-checkpoint:32-38` — the self-test
   asserts only that `data/store/events.jsonl` EXISTS, while case 13's rows 13 and 16 need a log of
   at least 81 lines (`build_checkpoint … 80` silently returns the whole log below that, making
   `cp-prefix` identical to `cp-full`).
   Witness: on this worktree `data/` was absent; the unit suite left a 5-line log; the gate then
   failed with `✗ row 13: expected :baseline-other-checkpoint to be the FIRST refusal, got '<none>'
   (exit 0)` — a message that names the wrong cause. Green (22 cases) only after `make seed-demo`
   → 149 lines. Fix: assert the floor with the reason (`[ "$(wc -l < "$LOG")" -ge 81 ] || die "case
   13 rows 13 and 16 need >80 rows; run make seed-demo"`), or have the test seed its own log.
   Until then a fresh checkout cannot reproduce this branch's own green.

4. **[NOTE]** `bin/test-fold-diff-checkpoint:1281` — the closing summary still says "a forced
   baseline-emit failure is exit 3" while case 4 (`:329`, `:347`) asserts exit **4**. A second copy
   of the truth, disagreeing with the code, inside the file whose case 13 exists to stop exactly
   that.

5. **[NOTE]** `bin/fold-diff-checkpoint:802` — a partial verdict is accepted: `CANDIDATE_CONTRACT_RE`
   matches a line PREFIX, so a bare `VERDICT:` with no verdict text and no trailing newline plus
   exit 1 is published as DIFFERENCES FOUND. Tightening the RE to
   `^VERDICT: (IDENTICAL|[0-9]+ difference)` costs nothing and is subsumed by finding 1's fix.

6. **[NOTE]** `bin/fold-diff-checkpoint:826` / `:906` — `printf '%s\n' "$…_OUT"` to a closed stdout
   (`bin/fold-diff-checkpoint | head -1`) kills the script under `set -e` with **141**, outside the
   documented 0–4 table. Witness: `PIPE_EXIT=141`. Harmless (141 is not a verdict), but the header's
   exit table claims to be exhaustive.

7. **[NOTE]** `bin/fold-diff-checkpoint:916-923` — a candidate that prints `VERDICT: 1 difference(s)`
   and exits 2 is published as `==> REFUSED during phase: candidate-emit (diff)`, exit 2. Fail-safe,
   but the contradiction is never named in the transcript, which is the one place an operator would
   look. Same fix as finding 1.
