# Opus red-team review — fable/clj-splice-r1 (tip 4be489fe, base 04648059)

**VERDICT: GO-WITH-FIX** — land after M1–M3 below. No HIGH finding. The consolidation
from 50 deftests / 1,149 assertions to 29 / 515 survives adversarial mutation: in 7 of 8
reintroduced defects the *named destination group* went red for the right reason, and the
8th class is guarded by a different retained witness. The three-function fence is clean,
the byte contract holds against an independent Python computation and across three
runtimes, and E4 reaches `02332a74…` through both verbs via the real bb CLI entrance.

Reviewer worktree: `/var/tmp/forge/splice-fx/wt-redteam` (branch `fable/clj-splice-redteam`
@ 4be489fe, removed at end). Builder worktree read-only throughout. All JVMs `-J-Xmx1g`,
one at a time, load-gated at 14. Temp under `/var/tmp/forge/splice-fx/redteam`.

---

## Findings by severity

### MEDIUM

**M1 — `:payload-parse-error` is missing from `docs/intent/insert-forms/refusals.edn`, and
nothing can detect the omission.**

It is a live refusal type: emitted at `src/clj_surgeon/insert_forms_plan.clj:260`, carries its
own remedy at line 58, is specified as `INSERT-FORMS-003`, and is asserted by
`insert-forms-parse-stages` and by the E4 group. The remedy `case` names 21 types;
`refusals.edn` has 20 rows.

```
$ grep -c ':type ' docs/intent/insert-forms/refusals.edn      # 20
$ grep -n 'payload-parse-error' docs/intent/insert-forms/refusals.edn   # (no output)
$ grep -rn 'payload-parse-error' src/ | head -2
src/clj_surgeon/insert_forms_plan.clj:58:    :payload-parse-error "Repair the payload syntax at the reported line and column."
src/clj_surgeon/insert_forms_plan.clj:260:      (refuse! (case kind :source :source-parse-error :payload :payload-parse-error :candidate-parse-error)
```

The registry check in `test/clj_surgeon/splice_envelope_test.clj:37-46` validates only the
rows that are present (field presence, distinct `:type`, `native_failure`/class rule). It
never compares the row set against the vocabulary the verb actually emits, so a refusal type
can be added or forgotten with the gate green. `rename-alias` is a superset and is fine.

*Fix:* add the row, and add a completeness assertion (`emitted-types ⊆ registry-types`)
derived from the remedy table or the `refuse!` call sites — otherwise this recurs.

---

**M2 — `clj-splice/splice` batch overlap refusal is input-order dependent.**

The docstring promises "intervals must not overlap" — a property of the *set* of edits — but
`splice-edits` sorts by start only (`(sort-by (comp first first) edits)`) and then checks
`(<= previous start)`. For equal starts the outcome depends on argument order.

```
$ bb /var/tmp/forge/splice-fx/redteam/edge.clj
A [[5 10]] then [[5 5]] => REFUSED :clj-splice/invalid-interval
B [[5 5]]  then [[5 10]] => "01234YXabcdef"
E [[3 3]"A"] [[3 3]"B"] => "012AB3456789abcdef"
F [[3 3]"B"] [[3 3]"A"] => "012BA3456789abcdef"
```

A and B are the same edit set in two orders: one refuses, one applies. E/F apply two
point-insertions in argument order with no documented rule. Genuinely overlapping intervals
are refused in both orders (C/D), so this is narrow, and neither current caller can reach it
(rename passes sorted disjoint token spans; insert passes one edit). But this is the public
contract of a library explicitly designed to be extracted, and it is unwitnessed.

*Fix:* make the refusal a function of the set (detect any pair with `start < prev_end`, or
define and document a total order for equal-start edits), plus a witness for both orders.

---

**M3 — `column-one-reference-addresses` does not exercise a column-one path; the branch it
names is dead code.**

Reintroducing the column-one defect left every destination green. The `(dec end-row)`
correction in `src/clj_surgeon/splice_projection.clj` is guarded by `(and (= 1 end-col)
(> end-row row))`. Probing all 12 frozen corpora plus 6 crafted multi-line sources
(`deadcode.clj`), the **only** node that ever satisfies it is the root `:forms` node (id 0):

```
HIT rename-f3-B.clj :forms 1 1 1435 1      # and the same for every other file
DEADCODE-PROBE-DONE
```

The only consumer of `:end_line` from this projection is `rename_alias_plan/site`, which is
built from alias-reference tokens — always single-line, so `(> end-row row)` is false. Mutating
`(= 1 end-col)` → `(<= 1 end-col)` produced 0 failures across all 8 destination groups.

Separately, the witness asserts only `(is (integer? (get-in site [:address :preorder])))`,
never a value. Shifting every preorder by +1 also left all 8 destinations green — it was
caught by `rename-alias-e4-verbatim`, which pins `[537 4661]`. So the *class* is guarded, but
not by the witness whose retired name claims it.

*Fix:* either delete the vestigial branch (it cannot fire for any consumer), or give the
witness a source that actually produces `end-col = 1` on a consumed node; and pin a preorder
*value* in the preservation-address group rather than relying on E4 alone.

### LOW

**L1 — `unsupported-source` conflates a semantic hazard with capability refusals.** It is
classed `:semantic` with `native_failure` naming lossy UTF-8 decoding, and the row itself
admits "This legacy type also includes capability-only BOM, conditional and extension
refusals." `bounded-input-path-encoding` drives `#=(…)`, `#?(…)` and a leading BOM through
this same type. Every `:none` in both files is confined to `:protocol`/`:resource`/`:capability`
(verified for all 46 rows), so task E's letter is satisfied — but a caller who hits the BOM
refusal reads a `native_failure` about malformed UTF-8 that did not occur.

**L2 — mixed-newline insertion emits a locally inconsistent separator.** Verified byte-exact
preservation of all original bytes, but the "first source newline" policy can insert CRLF next
to LF-terminated neighbours:

```
before: (ns demo)\r\n(defn a [] 1)\n(def s "in\r\nstring")\r\n(defn zz [] 2)\n
after : (ns demo)\r\n(defn a [] 1)\n(defn inserted [] 42)\r\n(def s "in\r\nstring")\r\n(defn zz [] 2)\n
```
Deterministic and documented (build report least-sure #2); flagged only because the retained
witnesses prove preservation, not local style.

**L3 — `recount` is a pure alias**: `(defn recount [candidate] (spans candidate))`. Verified
`(= (recount s) (spans s))`. The fence's third function carries no behaviour of its own.

**L4 — the frozen corpora sit on the library's source path.** `libs/clj-splice/bb.edn` has
`:paths ["src" "test"]`, so the 12 fixtures are Clojure source on the classpath, with four
duplicate ns declarations (`demo`×2, `f1`×2, `f2`×2, `cfp-scheduler-killer.views.schedule`×2)
and two files with no ns at all. Nothing requires them today.

```
$ ~/bin/clj-kondo --lint libs/clj-splice/src src/clj_surgeon/{splice_projection,rename_alias_plan,insert_forms_plan}.clj
linting took 125ms, errors: 0, warnings: 0
$ ~/bin/clj-kondo --lint libs/clj-splice/test/clj_splice/{core_test,test_runner}.clj
linting took  19ms, errors: 0, warnings: 0
$ ~/bin/clj-kondo --lint libs/clj-splice/test          # includes fixtures/
linting took 221ms, errors: 12, warnings: 14           # all inside fixtures/
```

**L5 — corpora stored twice.** `libs/clj-splice/test/clj_splice/fixtures/` and
`test-fixtures/clj-splice/` hold the same 12 files. All 12 verified byte-identical today and
matching the SHAs pinned in `scope.md`; no test compares the copies, so a future edit to one
drifts silently.

---

## A. Witness-reintroduction table (8 rows)

Baseline control, unmutated: all eight destination groups green, 152 assertions.
Each row = one source-level defect in my worktree, one JVM, all eight destinations run,
then `git checkout -- .`.

| # | Retired witness | Destination group | Defect reintroduced | Result |
|---|---|---|---|---|
| 1 | `rename-alias-prefix-trap` | `rename-alias-test/rename-alias-role-selection` | alias matched by bare prefix: `(str/starts-with? text (str alias "/"))` → `(str/starts-with? text alias)` | **RED 8p/10f** — `event/x` counted as a 4th reference; `:expect-count-mismatch`, `actual_count 4` vs 3. Right reason. |
| 2a | `column-one-reference-addresses` | `rename-alias-test/rename-alias-preservation-address` | column-one `end_line` correction mis-applied: `(= 1 end-col)` → `(<= 1 end-col)` | **GREEN 37p/0f — NOT CAUGHT.** Branch is dead code (M3). |
| 2b | `column-one-reference-addresses` | same | every `:address {:preorder}` shifted +1: `(rest nodes)` → `nodes` | **GREEN in destination**; caught by `rename-alias-e4-verbatim`: `(not (= [537 4661] [538 4662]))`. Class guarded, wrong witness (M3). |
| 3 | `candidate-form-preservation-refuses` (structure guard) | `rename-alias-test/rename-alias-candidate-integrity` | `form-proof` preservation check made tautological: `(when-not (every? true? (map preservation […]))` → `(when-not true` | **RED 4p/2f** — mutant returned `:state "committed" :committed true` and wrote corrupted bytes; witness caught both the missing refusal and the changed file. Right reason. |
| 4 | `rename-alias-scope-guards-counts` (E4 count refusal) | `rename-alias-test/rename-alias-stale-and-counts` | expect-count refusal removed: `(when-not (= expected actual)` → `(when-not true` | **RED 14p/4f** (+ `rename-alias-preservation-address` RED 26p/9f/2e). `:ok true` where `:expect-count-mismatch` was required. Right reason. |
| 5 | `insert-forms-after-prefix-named-defn` | `insert-forms-test/insert-forms-exact-root-anchor` | owner name matched by prefix in `owner?` | **RED 24p/10f** — anchor `a` now also matches `ab`; `:anchor-multiple-matches` with both candidates named. Right reason. |
| 6 | `insert-forms-deftest-nested-testing` | `insert-forms-test/insert-forms-body-anchor` | `testing_path` search no longer scoped to the selected container (searches the whole owner subtree) | **RED 17p/1f** — two `"inner"` labels found (lines 3 and 5); `:anchor-multiple-matches`. Right reason. |
| 7 | `insert-forms-unbalanced-payload-refuses` | `insert-forms-test/insert-forms-parse-stages` | payload parse stage skipped (`:payload` failures no longer refuse) | **RED 6p/3f** (+ E4 group RED 33p/1f) — expected `:payload-parse-error`, got `:payload-form-count-mismatch`. Right reason. |
| 8 | `insert-forms-stale-hash-refuses` | `insert-forms-receipt-test/insert-forms-snapshot-guard` | guard hash comparison removed: `(when-not (= hash (sha source))` → `(when-not true` | **RED 8p/4f** (+ E4 and rename-stale groups RED) — mutant returned `:ok true` with a full candidate on a stale guard. Right reason. |

**7/8 caught in the named destination for the right reason. Row 2 is the exception and is
covered by M3.** Two earlier mutants were mine, not the branch's: an inverted stale-hash
mutant (`when-not false` = always refuse) and an inert nested-testing mutant; both were
rewritten (rows 6, 8) and are reported above in their corrected form.

Reproduce any row:
```bash
cd /var/tmp/forge/splice-fx/wt-redteam
python3 /var/tmp/forge/splice-fx/redteam/apply.py <index>   # 0..11, see defects.py
TMPDIR=/var/tmp/forge/splice-fx/redteam clojure -J-Xmx1g -M:clj-surgeon/test-deps \
  -e "$(cat /var/tmp/forge/splice-fx/redteam/run8.clj)"
git checkout -- .
```

Widened control: with 2b applied, all 24 retained deftests across
`rename-alias-test`, `insert-forms-test`, both receipt tests, `splice-envelope-test`,
`receipt-booleans-test` and both parity tests were run — 23 green, only
`rename-alias-e4-verbatim` red. That run also serves as an independent confirmation of the
build's "24/458" focused inventory.

---

## B. Byte-contract table

| # | Check | Result |
|---|---|---|
| B1 | `spans` over a source with a 2-byte (`é`), 3-byte (`ह`), 4-byte (`😀`) scalar before the anchor, plus CRLF, a bare CR, and CR/CRLF inside a string literal — versus an independent `python3` UTF-8 + bracket-matching computation | **EXACT MATCH.** roots `[(15,24),(25,41),(42,51)]`, gaps `[(0,15),(24,25),(41,42),(51,52)]`, per-root SHA-256 identical in both computations (`7839afe2…`, `d284a47a…`, `bbd77c20…`), total 52 bytes. |
| B2 | Reassembly law `g0 f0 … f(N-1) gN` over bytes, on E4 and both complete red-team corpora (12 files) | **PASS 12/12**, including E4 `rename-f3-B.clj` (35 roots, 80,217 bytes). Byte-array equality, not string equality. |
| B3 | `splice` at `start=end` inside a multi-line string literal | **ACCEPTED, bytes exact.** The contract promises interval replacement, not literal awareness ("Surgeon authorizes the interval"); prefix and suffix verified byte-identical. Protection against a bad interval lives in Surgeon's `candidate-structure-mismatch`, which I confirmed live (task D). |
| B3b | `splice` endpoint inside a 4-byte scalar | **REFUSED** `:clj-splice/encoding-boundary` at bytes 9/10/11; accepted at the scalar boundary 8. |
| B4 | `#_#_1 2 3` effective roots | **physical 2 / effective 1**, effective tag `:token` (the `3`), `recount` = 1. Matches the spec exactly. Also `#_1 2` → 1, `#_#_#_1 2 3 4` → 1, `1 2 3` → 3. |
| B5 | `#::ev{}` and `#?@(…)` synthetic-node intervals | **CORRECT, with the original slice verified.** `#::ev{}` → `:map-qualifier` bytes [1,5) = `"::ev"`; `#:é{}` → [1,4) = `":é"`; `#?(:clj 1)` → `:conditional-prefix` [1,2) = `"?"`; `#?@(:clj [1])` → [1,3) = `"?@"`. The implementation asserts `(= (subs source a b) (node/string n))` and refuses on mismatch. |
| B6 | Identical span hashes on JVM / bb 1.13.219 / bb 1.12.209 over the corpora + Unicode/newline/synthetic extras | **IDENTICAL: `c70ac4a1954f92e055653772a1c848cd1ebfba6641d22ec4cfdc0499ac5be04b`** on all three. Astra's executed Unicode witness reproduces exactly on all three: `"é😀" (def x 1)` → def at bytes **[9,18)**, SHA **`f5c134bf6c2d0b925524242efe4dd5564dbec67f832b7e484f0a4bd1963c8939`**. |

Edge probes (`edge.clj`): out-of-range, reversed interval, non-string source, lone-surrogate
replacement all produce typed refusals; empty source yields `{:effective-count 0 :roots []
:gaps [{:start 0 :end 0}]}`; leading BOM parses as a token (effective-count 2), consistent
with both verbs continuing to refuse it. The one hole is M2.

---

## C. The fence

```
$ grep -nE '^\(def' libs/clj-splice/src/clj_splice/core.clj
8:(defn- fail! …          11:(defn- index-source …   35:(defn- coordinate …
42:(defn- digest …        47:(def ^:private trivia …
51:(defn spans …         101:(defn- splice-edits …  126:(defn splice …  133:(defn recount …

$ grep -rnEi 'slurp|spit|clojure\.java\.io|java\.io\.File|java\.nio\.file|ProcessBuilder|
   System/(getenv|getProperty|exit)|shell|refuse|receipt|policy|owner|edn/read' libs/clj-splice/src/
core.clj:2:  "…No edit policy."          <- docstring
core.clj:55: "…no owner classifications." <- docstring
```

**CLEAN.** One namespace, one file, exactly three public vars (`spans`, `splice`, `recount`);
everything else `defn-` / `^:private`. No I/O, no policy, no owner-name authority, no
formatting, no EDN request handling. `:require` is rewrite-clj parser + node only; `:import`
is `StandardCharsets` and `MessageDigest`. Errors are `ex-info` with a `:clj-splice/category`,
no custom class. Astra's §1 division is honoured, including the removal of authoritative
def-like owner kind/name from the library contract. No fence violation found.

---

## D. The port

| Check | Result |
|---|---|
| E4 through **both** verbs → `02332a74`, via the real bb CLI `:request-file` entrance | **PASS.** `bb -m clj-surgeon.core :rename-alias! :request-file …` and `:insert-forms! :request-file …` both returned `:state "committed"`; independent `sha256sum` on the two files on disk: `02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c` for both. Rename: `references_changed 2`, `other_forms_checked 32`. Insert: `bytes_added 94`, `other_forms_checked 34`. |
| parse-valid wrong-offset mutant | **LIVE.** `*splice-offset*` bound to `dec` in `insert-forms-candidate-structure-refuses` → `:candidate-structure-mismatch`, `:state "refused"`, file unchanged. Green in baseline (3 assertions). This is also the guard that backstops B3. |
| rename on a file with mixed CRLF/LF preserves every old byte | **N/A as posed — rename REFUSES mixed newlines** (`newline-style` with `mixed? false`) → `:unsupported-source`, remedy "…uniform LF or CRLF…". Only `insert_forms` passes `mixed? true`. Both admissible cases verified with an independent python hasher over the changed intervals: rename on uniform CRLF (176→160 B, 4 sites) **unchanged-bytes-identical=True**, all 8 CRLF pairs and the CR inside a string literal preserved; insert on mixed CRLF/LF (62→85 B) **unchanged-bytes-identical=True**. See L2 for the separator style. |
| rename plans with ONE parse of the original source | **CONFIRMED.** Counting seam over `clj-splice.core/spans` keyed by input SHA: a one-file rename plan calls `spans` **2×**, of which **1×** on the original source (the other is the candidate, legitimate verification). Insert plan: 4× total, **1×** on the original source (others: payload, reindented payload, candidate). The `annotate → addresses` duplicate parse is gone. |

---

## E. `refusals.edn` — `native_failure` honesty

All 46 rows carry the nine required keys and distinct `:type`s. Every `native_failure :none`
is confined to a non-semantic class:

- insert-forms `:none` — `invalid-guard` (protocol), `invalid-request` (protocol),
  `limit-exceeded` (resource), `unsupported-indentation` / `unsupported-owner-shape` /
  `unsupported-payload-syntax` (capability).
- rename-alias `:none` — `invalid-guard`, `invalid-request` (protocol), `limit-exceeded`
  (resource), `multiple-ns-forms`, `ns-not-found`, `unsupported-libspec`,
  `unsupported-namespace-mutation`, `unsupported-ns-shape` (capability).

**No refusal with `none` in the semantic class.** Every `:semantic` row names a concrete
native failure. Two issues, reported above: **M1** (a whole refusal type absent from the
registry, undetectable by the gate) and **L1** (`unsupported-source` conflates classes).

---

## F. Receipt-boolean class test still fails by name

Added a temporary unregistered boolean to the **real emitted receipt**
(`insert_forms_plan.clj:536`, `:redteam_fake_flag true`), ran the class test, reverted.

```
RESULT every-receipt-boolean-has-a-driven-false-witness     pass=25 fail=1 error=0
FAIL in (…) (receipt_booleans_test.clj:84)  insert_forms
insert_forms missing boolean fields
expected: (= [] (missing-fields committed seams))
  actual: (not (= [] [:redteam_fake_flag]))
RESULT unregistered-verbs-and-fields-fail-by-name           pass=2 fail=0 error=0
```

**PASS — it fails by name.** Worktree reverted clean (`git status --short` empty,
HEAD `4be489fe`).

---

## G. The gates the builder could not run (read, not re-run)

Both were executed by Fable under `run-bg` against **HEAD `4be489fee020a030255a3eb0073c4b5708972dca`** — the exact branch tip.

**`make test-fast` (normal multiprocess coordinator)** — `/var/tmp/forge/splice-fx/test-fast-coordinator.log`
```
clj-splice bb prerequisite: Ran 5 tests containing 57 assertions.  0 failures, 0 errors.
JVM fast lane:              Ran 755 tests containing 7931 assertions.  0 failures, 0 errors.
lanes (8), makespan 33651 ms — every lane exit 0
test-isolation: 0 violations across 61 namespace(s)
battery-parallel: makespan 33651 ms over 8 lane(s); serial-equivalent 50911 ms; skipped 0
```
**PASS.** Same 755/7,931 the builder obtained on its serial `SERIAL/NOT-A-GATE` diagnostic,
now through the real coordinator, with isolation proven.

**`~/bin/suite-run make landing-gate-prewarm`** — `/var/tmp/forge/run-bg/splice-prewarm.log`
```
:state :passed   :problems []   :prewarm? true   :landing? false
:git-head "4be489fee020a030255a3eb0073c4b5708972dca"   :wall-ms 183845
:stages [{:target "admit-transaction-recovery-battery" :exit 0 :wall-ms 12651}
         {:target "alias-migration-test"               :exit 0 :wall-ms 87392}
         {:target "mcp-test"                           :exit 0 :wall-ms 100149}
         {:target "test-bb"                            :exit 0 :wall-ms 85480}
         {:target "repository-hygiene"                 :exit 0 :wall-ms 2025}
         {:target "intent-audit"                       :exit 0 :wall-ms 2022}]
```
**PASS, 6/6 stages exit 0.** Note `:landing? false` — this is the prewarm, not the landing
gate itself; it does not by itself constitute landing authority.

---

## What I could not test

- **The landing gate proper** (`:landing? true`) was not run by anyone; only the prewarm.
- **Trunk-vs-branch parse-count comparison.** I measured the *new* count (1 parse of the
  original source per plan) and confirmed the duplicate-parse code is gone; I did not stand
  up a 04648059 worktree to measure the old count of 2. The build's before/after figures
  (123 → 101 rename planner calls) are unverified by me.
- **The wall-clock and assertion-count deltas** in `measure.md` — I re-ran the inventory and
  confirmed 24 retained verb deftests / 458 assertions and the library's 5/57, but made no
  timing measurement of my own and no native comparison.
- **Concurrency/transaction behaviour under real parallel writers** — the publication and
  recovery groups were run, not stressed.
- **The upstream issue** — drafted only; no issue filed, no maintainer response, as the
  build report states.
- **BOM / tab admission and branch-dependent editing** — deferred by the frozen scope; I
  confirmed BOM still parses as a token in the library and is still refused by both verbs,
  but did not review an admission path that does not exist.

### Evidence
Scripts and raw outputs under `/var/tmp/forge/splice-fx/redteam/`: `b1.clj` `b2.clj` `b6.clj`
`deadcode.clj` `edge.clj` `e4cli.clj` `dcrlf2.clj` `run8.clj` `runall.clj` `runF.clj`
`defects.py` `apply.py`, plus `a-before.bin`/`a-after.bin`, `c-before.bin`/`c-after.bin`,
`b1-src.bin`, `e4ws/`.
