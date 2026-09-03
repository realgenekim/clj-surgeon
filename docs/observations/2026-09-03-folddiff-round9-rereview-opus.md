# Fold-diff round 9 — Opus executed review of curtaincall-cfp bridge/fold-diff-tool at b223f64e (2026-09-03T19:27Z; Sol refused the brief)

Verdict: **GO-WITH-FIX** for the production read (pin stays 347fe6d3; tip not blessed). Round-8 findings 1–3 CLOSED with independent reproductions; gates green with selected counts (56 selected; the unqualified focus selects 0 of 1486). OPEN, live: candidate phase accepts DIFF_EXIT 1 as a verdict — a candidate that fails to COMPILE (`No such var: store-pg/read-lines-with-seq`) exits 1 with no VERDICT/FAILED/REFUSED line. OPEN: three scan shapes fail open (`eval`+`read-string`; a resolver reached through another resolver; syntax-quoted macro bodies); allowlist pins a fn shape not the write-guard context; case 5 accepts exit {0,1,2,3}. Round 10 launched.

## Opus verdict, verbatim

# Round-nine executed re-check — curtaincall-cfp, branch bridge/fold-diff-tool @ b223f64e

Reviewer: independent Opus seat (the previous round-nine reviewer was killed by an API limit
after attack (a); this is a fresh, complete execution of the whole brief).

Checkout: `/home/forge/tmp/sol/folddiff9-wt`, `git rev-parse --short HEAD` → `b223f64e`.
The previous reviewer had left `.codex/config.toml` deleted plus an untracked
`.codex/config.toml.disabled-by-sol-yolo`; both were reverted/removed before any work
(`git checkout -- . && rm -f .codex/config.toml.disabled-by-sol-yolo` → `git status --porcelain`
empty). Nothing was committed, stashed or pushed. Final `git status --porcelain` is empty and
`git worktree list` shows only this checkout.

**Repository store under `data/`: PRESENT at start** —
`-rw-r--r-- 1 forge forge 149047 Sep  3 13:01 data/store/events.jsonl` (gitignored via
`.gitignore:20:data/`). After the full unit suite it was 150118 bytes; the suite appends to it.

Fixtures: `/tmp/folddiff9-sol-fx` only.

---

## VERDICT: **GO-WITH-FIX** for the production read pinned at 347fe6d3

All three of Sol's round-eight findings are CLOSED, each with an independent reproduction below,
and every gate is green. Two new defects and one unbacked claim came out of the round-nine
attacks. One of them — finding 1 below — is **live, reachable with no injection at all, and is
the exact class this branch exists to close**: a candidate worktree whose namespace fails to load
makes the wrapper exit **1**, the code it publishes as DIFFERENCES FOUND. It is not a hazard to
the *specific* pinned read (that read's candidate is the branch tip, and `make compile-check`
exits 0 in this same gate run, while the risky side — origin/main — is on the emit path, which
*is* guarded at `bin/fold-diff-checkpoint:753`). So the read is safe to take **once finding 1 is
fixed**, and it must be fixed before any script is allowed to read this gate's exit code.

---

## Prior findings — re-run

### 1. CLOSED — the required-var scan no longer fails open through bound intermediates

`bin/fold-diff-required-vars.bb:145` (`quoted-symbol` rejects a bare symbol) and
`bin/fold-diff-required-vars.bb:155` (`literal-target` requires both halves to be reader-produced).

My own scenario, `/tmp/folddiff9-sol-fx/f1-bound-intermediate.clj`:

```clj
(let [var-name "checkpoint-path"
      target   (symbol checkpoint-ns-name var-name)
      f        (requiring-resolve target)]
  (checkpoint/validate (f path)))
```

```
$ bb bin/fold-diff-required-vars.bb /tmp/folddiff9-sol-fx/f1-bound-intermediate.clj
:UNRESOLVABLE
(requiring-resolve target)
  — its namespace and name are not both symbols the reader produced at the call site. …
    Its binding site here is: "(let [var-name \"checkpoint-path\" target (symbol …)] …)"
$ bin/fold-diff-checkpoint --self-test-required-vars-guard …f1-bound-intermediate.clj
REFUSED :required-vars-unresolvable
guard exit=2
```

Own-copy control (`f1-owncopy-only-validate.clj`, defines only `validate`):
`--self-test-missing-vars … validate checkpoint-path` → prints `checkpoint-path`, so the
own-copy check was never the defect. The shipping file still scans clean:
`bb bin/fold-diff-required-vars.bb src/cfp_scheduler_killer/fold_diff.clj` →
`:OK / checkpoint-path / validate / write-checkpoint!`, exit 0, and it contains no
`symbol`/`read-string`/`load-string`/`eval` call (grep empty).

### 2. CLOSED — a crashed scan is a typed FAILED and the reserved exit 4

`bin/fold-diff-checkpoint:135` (`SCAN_CRASHED_EXIT=4`), `:168` (any non-zero scanner exit).

```
$ PATH=/tmp/folddiff9-sol-fx/fakebin:$PATH BASELINE_REF=HEAD~1 CANDIDATE_REF=HEAD \
    CHECKPOINT_PATH=…/checkpoint.json FOLD_DIFF_DATA_DIR=…/data bin/fold-diff-checkpoint
==> baseline   HEAD~1 (d3560920…)
==> candidate  HEAD (b223f64e…)
FAILED :required-vars-scan-crashed bin/fold-diff-required-vars.bb exited 47
  … NOTHING WAS COMPARED … Exit 4 is reserved for exactly this and is never 0/1/2/3.
  What the scanner printed:
    fake bb: simulated crash
EXIT=4
```

No `REFUSED` line, no worktree opened. `:NO-ALIAS` and an unexpected status line route to the
same code (`bin/fold-diff-checkpoint:171-208`).

### 3. CLOSED — refs resolve before `:data-dir-with-postgres`

`bin/fold-diff-checkpoint:459-460` (`resolve_ref` for both refs) now precede the data-dir refusal
at `:484`.

```
BASELINE_REF=no/such/ref  + FOLD_DIFF_DATA_DIR + STORE_BACKEND=postgres → REFUSED :baseline-ref-unknown,  exit 2
CANDIDATE_REF=also/no/such + same                                        → REFUSED :candidate-ref-unknown, exit 2
both refs valid            + same                                        → REFUSED :data-dir-with-postgres, exit 2
```

Neither ref refusal is masked, and the data-dir refusal alone is unchanged.

---

## Round-nine attacks

### (a) The allowlist — both predicted outcomes confirmed; the residual is NOT enforced

Pin: `bin/fold-diff-required-vars.bb:108-109`, matched against the printed text of the nearest
enclosing binder (`pin-key`, `:257`).

**(a1) pinned text reproduced verbatim + a symbol forged elsewhere → REFUSED.**
`/tmp/folddiff9-sol-fx/a1-pinned-plus-forge.clj` carries
`(fn [sym] [sym (try (requiring-resolve sym) (catch Throwable _ nil))])` byte-for-byte plus
`(defn sneaky [ns-name v] (symbol ns-name v))`:

```
:UNRESOLVABLE
(requiring-resolve sym)
  — this file BUILDS symbols of its own — (symbol ns-name v) — so even a pinned dynamic
    resolver form could be handed a name that appears nowhere in its source
guard exit=2
```

**(a2) pinned text reproduced, no forging, symbols imported from another ns → residual, scan
passes.** `/tmp/folddiff9-sol-fx/a2-pinned-no-forge.clj` (`*write-vars*` built with
`(into ['…store/append!] extra-write-vars)` where `extra-write-vars` is `:refer`red from
`scratch.othervars`):

```
:OK
validate
guard exit=0
```

**Can the residual ever produce exit 0 with a missing required var?** For the *shipping*
`fold_diff.clj`, no — every symbol reaching that allowlisted form comes from `*write-vars*`, and
`src/cfp_scheduler_killer/fold_diff.clj:434` throws `:write-guard-incomplete` when any of them
fails to resolve. **But nothing checks that this stays true.** The pin matches a *fn shape*
(`bin/fold-diff-required-vars.bb:261`), not the write-guard context: the identical `(fn [sym] …)`
mapped over a list of *read* vars imported from another namespace scans `:OK` exactly as (a2)
does, and `call-with-writes-forbidden` never sees those symbols. `grep -rn known-dynamic-resolvers
test/ bin/test-fold-diff-checkpoint` returns nothing — no witness binds the allowlist entry to
`call-with-writes-forbidden`, so the header's containment argument
(`bin/fold-diff-required-vars.bb:57-62`) is documentation, not a control. See finding 3.

### (b) Reader shapes — all conservative or fail-closed; no defect found

| shape | result |
|---|---|
| `` `checkpoint/checkpoint-path `` (syntax-quote) | `:OK` + `checkpoint-path` (over-counts — conservative) |
| `#'cfp-scheduler-killer.store-checkpoint/checkpoint-path` | `:OK` + `checkpoint-path` |
| `(var cfp-scheduler-killer.store-checkpoint/checkpoint-path)` | `:OK` + `checkpoint-path` |
| `:cfp-scheduler-killer.store-checkpoint/checkpoint-path` (ns keyword) | ignored — correct, a keyword is not a var reference |
| `::checkpoint/checkpoint-path` (auto-resolved keyword) | read error → **scan exit 3** → wrapper exit 4 |
| `#=(clojure.core/symbol …)` (eval reader) | `EvalReader not allowed when *read-eval* is false` → **scan exit 3** |
| `(#'requiring-resolve target)` (var-quote at the head) | `:UNRESOLVABLE`, guard exit 2 |

### (c) The exit-code table — every code witnessed; **exit 1 is still shared with a failure**

| exit | meaning | witness |
|---|---|---|
| 0 | IDENTICAL | `BASELINE_REF=HEAD CANDIDATE_REF=HEAD`, private 154-row checkpoint + private data dir → `VERDICT: IDENTICAL — the candidate ref's folds project exactly what the baseline ref's do.` `B_EXIT=0` |
| 1 | DIFFERENCES | shipped tool, baseline projection with one `[:rooms … :name]` value perturbed → `RELATION :rooms — 1 differing path(s) … VERDICT: 1 difference(s)`; `C_EXIT=1`. The wrapper passes this through verbatim at `bin/fold-diff-checkpoint:820`. |
| 1 | **a candidate JVM that never loaded** | **`BASELINE_REF=HEAD CANDIDATE_REF=bd581934…`** → baseline emits fine (`fold-source-digest: 5293fb7b…5fa0`), then `Syntax error compiling at (cfp_scheduler_killer/store_checkpoint.clj:113:15). No such var: store-pg/read-lines-with-seq` and **`E_EXIT=1`** — no `VERDICT:` line, no `FAILED` line, no `REFUSED` line. Reproduced again under injection (a shim `clojure` that execs the real one for `--emit-baseline` and exits 1 otherwise): `D_EXIT=1`. |
| 2 | typed refusal | `BASELINE_REF` unset → `REFUSED :baseline-ref-unset`, exit 2 (and all 16 kinds, case 13) |
| 3 | a phase started and failed | self-test case 4, `FOLD_DIFF_XMX=not-a-heap-size` → `FAILED :baseline-emit-failed`, exit 3 (gate 5 green) |
| 4 | the pre-flight scan could not run | fake `bb` exiting 47 → `FAILED :required-vars-scan-crashed`, exit 4 |

**Yes — a verdict is still shared with a failure.** The baseline-emit phase is guarded
(`bin/fold-diff-checkpoint:753`, `{0,2,3}` only), but the candidate phase accepts `{0,1,2,3}`
(`:813-814`) and re-emits the code unchanged (`:820`). `clojure -M` exits **1** on a namespace
load failure — measured directly: `clojure -M -e "(require 'no.such.namespace)"` → exit 1 — so
the very case the driver's own comment at `:809-812` names as caught ("a compile failure in the
checked-out worktree") is the case that slips through spelled DIFFERENCES. The self-test already
owns this fixture: case 5 uses `bd581934…` as the *baseline* (where it is caught, exit 3); the
mirror case with it as the *candidate* was never written.

### (d) `--refusal-order` vs case 13's oracle — the oracle is real

`bin/test-fold-diff-checkpoint:744` holds a hand-written table; `:763` reads the wrapper's.
I swapped rows 3 and 4 of `refusal_order()` in `bin/fold-diff-checkpoint` (published table only;
the code order untouched) and ran the self-test:

```
--- case 13: THE WHOLE REFUSAL ORDER -- all 16 kinds, in order, … ---
✗ --refusal-order disagrees with this test's hand-written oracle:
3,4c3,4
<  3  :candidate-ref-unknown          driver          …
<  4  :data-dir-with-postgres         driver          …
---
>  3  :data-dir-with-postgres         driver          …
>  4  :candidate-ref-unknown          driver          …
F_EXIT=1
```

The file was restored byte-for-byte (`git status --porcelain` empty afterwards). Note the two
halves are independent: the provocation half uses hard-coded ordinals, so a mutation of the
*code's* order is caught by `expect_row 2/3/4` and a mutation of the *table* by the oracle diff.
Both directions are covered.

### (e) Vacuity of the 18 self-test cases — none is green vacuously

Every case makes at least one content assertion beyond an exit code. Two nuances worth naming,
neither a false green:

- **case 2** (`bin/test-fold-diff-checkpoint:170`) is a deliberately *vacuous comparison* (both
  sides the same commit) — but it is not a vacuous *test*: it asserts `VERDICT: IDENTICAL`, the
  driver's `both sides are the same commit` note (`:182`) and the tool's `BYTE-IDENTICAL` note.
- **case 5**'s exit assertion (`:306`) accepts any of `{0,1,2,3}` and so discriminates nothing on
  the exit code; it earns its keep from the `checkpoint-source (baseline): candidate-fallback`
  line and the digest equality it asserts, both printed before either JVM starts.

Cases 0a/0b pin exact sets and exact negatives; 7a/7b/9/9b/10/11/12/13/8/8b each assert a named
`REFUSED`/`FAILED` kind plus a control that the neighbouring refusal still fires alone.

---

## Gates — ran-lines verbatim (all under `/home/forge/bin/suite-run`)

```
###### GATE 1: focused fold-diff-test   (bin/kaocha unit --focus cfp-scheduler-killer.fold-diff-test)
56 tests, 226 assertions, 0 failures.
GATE1_EXIT=0
```
Selected-test count: **56 selected, 56 ran**. Control: the unqualified
`bin/kaocha unit --focus fold-diff-test` prints
`WARNING: All 1486 tests were skipped. Check for misspelled settings in your Kaocha test
configuration or incorrect focus or skip filters.` and exits 0 — the fully qualified ns is
required, as the brief says.

```
###### registry (--focus cfp-scheduler-killer.intent-contract-test
                 --focus cfp-scheduler-killer.intent-registry-architecture-test
                 --focus cfp-scheduler-killer.intent-witness-identity-architecture-test)
9 tests, 1083 assertions, 0 failures.
A_EXIT=0
```
(`intent-registry-architecture-test` alone is `2 tests, 622 assertions, 0 failures.`)

```
###### GATE 3: full unit   (bin/kaocha unit)
1102 tests, 13436 assertions, 0 failures.
GATE3_EXIT=0
```
Repository store under `data/` **PRESENT** for this run (149047 bytes before, 150118 after).

```
###### GATE 4: compile-check
Compiling application namespaces...
✓ Application namespaces compiled successfully
GATE4_EXIT=0
```

```
###### GATE 5: bin/test-fold-diff-checkpoint
✓ bin/test-fold-diff-checkpoint: 18 cases, all green
GATE5_EXIT=0
```

---

## Findings for round ten

1. **[OPEN, live]** `bin/fold-diff-checkpoint:813-820` — the candidate phase accepts
   `DIFF_EXIT` 1 as a verdict, so a candidate worktree whose namespace fails to load exits 1,
   the code published as DIFFERENCES FOUND.
   Witness, no injection: `BASELINE_REF=HEAD CANDIDATE_REF=bd581934bf63e2ead0ae046820b53c4714beb5d7`
   → `Syntax error compiling at (cfp_scheduler_killer/store_checkpoint.clj:113:15). No such var:
   store-pg/read-lines-with-seq`, `E_EXIT=1`, with no `VERDICT:`, `FAILED` or `REFUSED` line
   anywhere. (`clojure -M -e "(require 'no.such.namespace)"` → exit 1, so the driver's own
   comment at `:809-812` names as *caught* exactly the case that escapes.) Fix: require a
   positive receipt — exit 1 without a `VERDICT:` line from the candidate is
   `FAILED :candidate-emit-failed`, exit 3 — and add the mirror of case 5 with `bd581934…` as
   the *candidate*.

2. **[OPEN]** `bin/fold-diff-required-vars.bb:285` — the forging net only fires when a dynamic
   entry also exists (`(and (seq dynamic) (seq forging))`), so a file that reaches a var purely
   by forging is not refused.
   Witness: `/tmp/folddiff9-sol-fx/c2-eval-readstring.clj`, whose only checkpoint reference past
   `validate` is `(eval (read-string "(cfp-scheduler-killer.store-checkpoint/checkpoint-path)"))`
   → `:OK`, derived set `validate`, `--self-test-required-vars-guard` exit 0.

3. **[OPEN]** `bin/fold-diff-required-vars.bb:204` — a resolver is recognized only when the verb
   is the literal head of the call, so a resolver obtained through another resolver is invisible.
   Witness: `/tmp/folddiff9-sol-fx/c1-indirect-resolver.clj`,
   `(((ns-resolve 'clojure.core 'requiring-resolve) (symbol "cfp-scheduler-killer.store-checkpoint" "checkpoint-path")) p)`
   → `:OK`, `derived-vars=[validate]`, and
   `--self-test-missing-vars f1-owncopy-only-validate.clj validate` prints nothing — the own copy
   that defines *only* `validate` is judged COMPLETE, guard exit 0. This is round-eight finding 1's
   exact composite, reached through a different shape.

4. **[OPEN]** `bin/fold-diff-required-vars.bb:201` — the quoted-datum branch skips syntax-quoted
   macro bodies, so a macro that *emits* a resolver call is never seen.
   Witness: `/tmp/folddiff9-sol-fx/c5-macro-emits.clj`,
   ``(defmacro resolve-var [sym-expr] `(requiring-resolve ~sym-expr))`` used as `((resolve-var nm) p)`
   → `:OK`, derived set `validate`, guard exit 0. Findings 2-4 together falsify the header's and
   the registry's claim that the scan "FAILS CLOSED on every reference it cannot read"
   (`bin/fold-diff-required-vars.bb:18`, `docs/intent/registry.edn` FOLD-DIFF-013 round-eight
   amendment). None of them can fool today's `fold_diff.clj` — case 0a pins its set to exactly
   `{checkpoint-path, validate, write-checkpoint!}` — so these are regression holes, not a live
   false green.

5. **[PARTIAL]** `bin/fold-diff-required-vars.bb:108` + `:261` — the allowlist pin matches the
   printed text of a *fn shape*, not the write-guard context, and nothing binds the residual's
   containment claim to `src/cfp_scheduler_killer/fold_diff.clj:434`.
   Witness: `/tmp/folddiff9-sol-fx/a2-pinned-no-forge.clj` reproduces the pinned binder verbatim
   with its symbol list `:refer`red from another namespace and scans `:OK / validate`, exit 0;
   `grep -rn known-dynamic-resolvers test/ bin/test-fold-diff-checkpoint` returns nothing.

6. **[NOTE, no action required for this read]** `src/cfp_scheduler_killer/store.clj:958-960`
   resolves `'cfp-scheduler-killer.store-checkpoint/hydrate!` dynamically from a *sibling*
   namespace, and the scan reads only `fold_diff.clj`, so `hydrate!` is outside the derived set.
   It is not reachable on the diff path (`fold_diff.clj` uses `store/fold`, never `store/load!` —
   grep of `store/` in that file), so it cannot produce a false-complete verdict today; it is a
   scope limit of "derived from fold_diff.clj's own source" worth stating in the header.

7. **[NOTE]** `bin/test-fold-diff-checkpoint:306` — case 5's exit assertion accepts `{0,1,2,3}`
   and discriminates nothing; tighten it once finding 1 lands (with `bd581934…` as candidate the
   correct code becomes 3, and that is the mirror witness finding 1 asks for).
