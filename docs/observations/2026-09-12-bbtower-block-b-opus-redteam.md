# Red-team review — clj-surgeon `bb-rewrite-tower` block B

**Subject:** tip `6e3e5535b4d5607f909ba8a26a74b2a71d40dfb7`, base `eae1e43280635be9b4e317e176d29476fd358702`
**Reviewer worktree:** `/var/tmp/forge/bbtower-fx/redteam` (detached, removed after the run)
**Every JVM/bb:** `-Xmx1g`, `JAVA_TOOL_OPTIONS="-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx"`, one at a time.

---

## VERDICT: GO-WITH-FIX

**Single reason:** every gating witness I attacked held — planted defects go red *by name*, the new ceiling
is enforced at the exact boundary, and `make test-fast` is green on a cold worktree at **37,321 ms / 60,000 ms**
fast-lane sum with **0 isolation violations** — but the probe refusal-completeness witness has a **proven blind
spot** at one of its three declared owner files (I changed `probe-servlet` to emit an unregistered refusal kind
and the witness stayed green), and the **`bb test/run_all.clj` diagnostic entrance is broken** by this block
(exit 96). Neither is a gate failure; both are unguarded and small.

---

## Claim table

| # | Claim | Verdict | Deciding command | Output excerpt |
|---|---|---|---|---|
| 1 | TEST-ISO-016 fails a `:bb` entry with ratio > 2.0 **by name** | **CONFIRMED** | edit `lane_manifest.clj` `namespace-runtimes` → `(assoc … 'clj-surgeon.splice-envelope-test :bb)`; `bb -Xmx1g --classpath src:test -e '(require …lane-manifest-test)(t/run-tests …)'` | `FAIL in (every-manifest-entry-exists-on-disk) (…lane_manifest_test.clj:158)` / `TEST-ISO-016: #{clj-surgeon.splice-envelope-test}` / `1 failures, 0 errors` — baseline before/after restore: `Ran 27 tests containing 421 assertions. 0 failures` |
| 2 | `:bb` ceiling 399,155 ms enforced; at ceiling passes, +1 ms refuses naming lane/sum/budget | **CONFIRMED** | `ns_isolation.clj` `:bb 399155` → `399156`; `clojure -J-Xmx1g -M:clj-surgeon/test-deps -e '(require …ns-isolation-test)(t/run-tests …)'` | baseline `Ran 25 tests containing 160 assertions. 0 failures, 0 errors.`; tampered → `4 failures`, incl. `expected: (= 399155 (:bb iso/lane-budget-ms))  actual: (not (= 399155 399156))`. Live message: `TEST-ISO-007 VIOLATION in :bb -- lane time budget: the bb lane took 399156 ms, over its 399155 ms budget`; `(lane-budget-violation :bb 399155)` → `nil` |
| 3a | Deleting a probe refusal row turns the completeness witness red | **CONFIRMED** | delete `:probe-namespace-not-found` row from `docs/intent/probe/refusals.edn`; run `clj-surgeon.splice-envelope-test` | `FAIL … (splice_envelope_test.clj:67)` / `probe refusal vocabulary must be complete` / `actual: (not (= #{… :probe-namespace-not-found …} #{…}))` |
| 3b | No row exists for a kind the code never emits | **CONFIRMED** | `remedy-vocabulary "probe"` printed live | `(:invalid-probe-port :invalid-probe-request :probe-connection-failed :probe-message-too-large :probe-namespace-not-found :probe-source-too-large :stale-probe-image)` = exactly the 7 registry rows; all 7 are reachable literals in `probe.clj` / `mcp_hot_verify.clj` |
| 3c | *(my own probe)* the witness covers all three declared owner files | **REFUTED** | change `mcp_http_server.clj:219` `:invalid-probe-request` → `:probe-servlet-unregistered-kind`; run the witness | `Ran 2 tests containing 50 assertions. 0 failures, 0 errors.` — **stayed green** while the server emits an unregistered kind |
| 4 | receipt_booleans probe false seam **flips** `verification_complete` through the production path | **REFUTED** | `clojure … -e '(map :verification_complete [(probe/verdict …)×3 (probe/refusal …)])'` | `:vc-values #{false}` for every input, including a failing summary and an empty one. `probe.clj:44` is a literal `:verification_complete false`; `receipt_booleans_test.clj:86` maps `:behavior-not-run` to the *same* `committed` object. Suite is green (`Ran 2 tests containing 34 assertions. 0 failures`) but the "false witness" is a constant, not a driven seam, and no probe request is driven. |
| 5 | Budgets unchanged; only `:bb` added; no test deleted or skipped | **CONFIRMED** | `git diff eae1e432..6e3e5535 -- test/clj_surgeon/ns_isolation.clj`; per-file `(deftest` name diff base↔tip; `git grep '^:skip\|kaocha'` | ns_isolation diff is exactly `+   :bb 399155` inside `lane-budget-ms`, `:fast 60000` / `:integration 240000` untouched. Deftest name sets: **2603 == 2603, `diff` empty**. No `^:skip` / `^:kaocha` / `:kaocha/skip` in either revision. `deftest_census.edn` unchanged. |
| 6 | Fast lane green | **CONFIRMED** | `make test-fast` once, cold worktree, 08:04:16Z→08:06:53Z (157 s), `EXIT=0` | `bb-runtime: serial-equivalent 240810 ms; budget 399155 ms; makespan 153511 ms` · `namespace walls (109, slowest first, serial-equivalent total 272915 ms)` · `test-isolation: 0 violations across 109 namespace(s) (TEST-ISO-002/003/004/005/007/010)` · `battery-parallel: makespan 153565 ms over 8 lane(s); serial-equivalent 272915 ms; skipped 0`. Receipt: `:state :passed :suite fast :runtime :hybrid :wall 153565 :procs 25 :isofail 0 :result {:test 1631, :pass 16011, :fail 0, :error 0}`. **Fast-lane sum 37,321 ms vs 60,000 ms — 37.8 % headroom remaining.** |
| 7a | Reassignment walls match the attempt-7 receipts | **CONFIRMED (all 35, not 3)** | programmatic compare of every `runtime-measurements` row against `attempt7/a-base/receipt.edn` and `attempt7/c-subject/receipt.edn` | `attempt7-sourced rows: 35` · `MISMATCHES: []`. Spot values: splice-envelope `834/28591`, rename-alias `3265/6478`, insert-forms `2017/5222`, rename-alias-receipt `414/1553`, namespace-split `696/744`, helper-extraction `1439/788` — all exact. |
| 7b | Makefile changes to what `make test` runs | **CONFIRMED, material** | `git diff eae1e432..6e3e5535 -- Makefile`; `git log --oneline … -- Makefile` | `test-fast`, `test-bb` and `landing-gate-prewarm` switched from `clojure -J-Xmx512m -M:clj-surgeon/test-battery-parallel` to `bb -Xmx1g -m clj-surgeon.battery-parallel-runner`; `suite-namespaces "fast"` is now `bb-namespaces ∪ fast` → **`make test-fast` runs 109 namespaces, not 61**. Attributable to `54b790cf` (block A), inherited by block B; `make test` itself (`landing-gate`, stages `mcp-test` + `test-bb`) is unchanged in structure. |
| 7c | Dead / broken code | **FOUND** | `bb -Xmx1g test/run_all.clj` (no args) | `bb-lane-refused: invalid or missing namespace selection`, **exit 96** — see F2 |

Also verified as true: `reassignments.md`'s "the original 35 bb-assigned fast members are all paired" —
61 fast namespaces, 35 bb-portable, `:unpaired []`. Lint of all twelve changed `src/`+`test/` files through
`~/bin/clj-kondo`: `errors: 0, warnings: 0`. `dev/experiments` added to the bb classpath (`bb.edn`, block A)
shadows nothing: 41 files, zero basename collisions with `src`/`test`/`libs/clj-splice/src`.

---

## Findings, ranked by severity

### F1 — MEDIUM. The probe refusal-completeness witness is blind at one of its three declared owners
`test/clj_surgeon/splice_envelope_test.clj:24-37`

`remedy-vocabulary` declares three owner files, including `{"mcp_http_server.clj" #{'probe-servlet}}` (line 26),
but extracts a kind only when a form's **head is a symbol named `refusal`** (line 35). `probe-servlet`'s call is
`((requiring-resolve 'clj-surgeon.probe/refusal) :invalid-probe-request …)` (`src/clj_surgeon/mcp_http_server.clj:218-219`)
— the head is a *list*, so that owner contributes **zero** kinds. The file's presence in `owners` is decoration.

Proven, not inferred: I changed line 219 to `:probe-servlet-unregistered-kind` and the witness reported
`Ran 2 tests containing 50 assertions. 0 failures, 0 errors.` A refusal kind reachable on the server path with no
registry row, no `native_failure`, and no `retirement_condition` passes the completeness gate. This is the
`scanner-brief-names-vs-spellings` class: a source scan derived from a spelling cannot see a call that spells
nothing. Fix: match on the *resolved* target (`'clj-surgeon.probe/refusal` appearing anywhere in the form head),
or assert non-empty extraction per owner file so a silent-zero owner fails.

### F2 — MEDIUM. `bb test/run_all.clj` (no args) is broken by this block
`test/run_all.clj:51` (inventory) vs `test/run_all.clj:74` (new runtime guard)

The new guard requires `(every? #(= runtime (get lm/namespace-runtimes %)) selected)`. With no `--emit-edn`,
`selected` is the hardcoded `namespaces` vector, which still contains `clj-surgeon.intent-transaction-test` —
now `:jvm` by the contract escape. Measured: `bb -Xmx1g test/run_all.clj` → `bb-lane-refused: invalid or missing
namespace selection`, **exit 96**, zero tests run.

That entrance is still documented and pinned by `docs/intent/temp-dir-hygiene/temp-dir-hygiene-design.md:25,44`,
`test/clj_surgeon/reader_eval_fence_test.clj:37`, `test/clj_surgeon/runner_membership.clj:33,166` and
`test/clj_surgeon/ns_isolation_test.clj:61,71,182`. No Makefile target invokes it, so **no gate can see this**.
Fix: filter the no-arg default (`(filterv #(= :bb (lm/namespace-runtimes %)) namespaces)`), or drop the entry.

### F3 — MEDIUM. The 399,155 ms ceiling is derived from a classification the tree no longer ships
`docs/intent/test-isolation/test-isolation-specs.md:257-262`, `attempt8/verify_report.clj:11`

The spec states "the step-2 run records a serial bb-runtime namespace sum of **245,773 ms** … Evidence:
attempt8/test-fast.log and attempt8/test-fast/receipt.edn". Two problems:

1. `attempt8/test-fast.log` contains **zero** `bb-runtime:` lines (`grep -c` → `0`; the prewarm logs have 3 each).
   The figure is not in the cited log.
2. Recomputing from the cited receipt with the **shipped** `lm/namespace-runtimes` gives **240,785 ms**, not
   245,773. The builder's own verifier reproduces 245,773 only by patching the map first:
   `step2-runtimes (assoc lm/namespace-runtimes 'clj-surgeon.intent-transaction-test :bb)` — i.e. the pre-escape
   classification. (240,785 + intent-transaction-test's 4,988 ms = 245,773 exactly.)

Direction of the error is **safe** — a shipped-consistent derivation would be `ceil(240785*60000/36944)` =
**391,053 ms**, so the registered ceiling is 8,102 ms (2.1 %) *looser*. But the spec and REPORT.md assert a
derivation the tree does not reproduce, and Fable is being asked to ratify that number. Fix: one sentence in the
spec saying the input is the step-2 (pre-escape) classification, and cite the receipt, not the log.

My independent cold run corroborates the shipped-classification figure: **240,810 ms** (25 ms from 240,785).

### F4 — LOW. Probe's "false witness" is a compile-time constant, not a seam
`src/clj_surgeon/probe.clj:27,44`; `test/clj_surgeon/receipt_booleans_test.clj:67,72,86`

`verdict` and `refusal` both write `:verification_complete false` literally; `(:verification_complete)` is `false`
for every input I drove (pass, fail, empty, refusal). The scenario `(fn [_] (probe/verdict …))` ignores its seam
argument, and `:behavior-not-run` resolves to the *same* `committed` map at line 86, so the assertion at line 94
reduces to `(false? false)`. No probe request, HTTP route or CLI is exercised.

This is consistent with how `insert_forms`/`rename_alias` already treat `:behavior-not-run`, and the registry
rule text discloses it ("Probe's verification_complete remains false because warm success is not cold proof"), so
it is not dishonest — and REPORT.md does not claim a flip. But RECEIPT-BOOL-001's own `:misreadings` list warns
against "an always-true computed expression"; an always-**false** literal is the same non-evidence. The only
falsifiable content added for probe is the negative test at line 82 (`missing-registrations` names `"probe"`).
Treat the probe row as a *declaration*, not a witness, in any later "third verb is covered" claim.

### F5 — LOW. TEST-ISO-016's only implementation-side link is a Makefile comment
`Makefile:1122`

`audit-current-repository` (`src/clj_surgeon/mcp_intent_contract.clj:219-221`) takes `implementation-sources` as
`src/**/*.clj{,c,s}` **plus `Makefile`**. `grep -rn TEST-ISO-016 src/ Makefile` returns exactly one hit:
`Makefile:1122: @# @spec TEST-ISO-016 -- coordinator consumes measured lane-manifest runtimes.` The actual rule
(`measured-runtime`, `namespace-runtimes`) lives in `test/clj_surgeon/lane_manifest.clj:213,267`, which the audit
classifies as *test* source. So the `[x] implemented` status is satisfied by a recipe comment: delete the
implementation and keep the comment and the audit stays green. Not wrong, but the ratchet is weaker than it reads.

### F6 — LOW / informational. 47 of the 109 namespaces `make test-fast` now runs carry no per-namespace budget
Measured on my run: `lane-of = nil` group = **47 namespaces, 231,133 ms** of the 272,915 ms serial-equivalent
(85 %). `iso/lane-default-budget-ms` is keyed by lane (`:fast/:integration/:battery`), so a `nil`-lane namespace
is subject to **no** per-namespace ceiling. Two of them dominate the run: `clj-surgeon.install-test` **80,613 ms**
and `clj-surgeon.parser-admission-test` **71,227 ms** — each alone larger than the entire fast-lane budget. They
are governed only by the new aggregate 399,155 ms bb ceiling. Makespan of the "every-run inner loop" entrance is
now 153,565 ms cold here (83,361 ms in the builder's warm-cache run) against the 27,638 ms of the attempt-7
JVM-only base arm. This is inherited from `54b790cf`, not created by block B, but it is what the new ceiling is
protecting and it deserves a named owner before "bb-first" is called a win.

### F7 — informational. `report!` charges the bb sum by *declared* runtime, not executed runtime
`test/clj_surgeon/battery_parallel_runner.clj:895-903`. `bb-runs` filters on `lm/namespace-runtimes`, while the
plan forces `:jvm` for the `battery`/`gate`/`alias` suites (`:1022`). On those suites the bb ceiling is charged
against namespaces that ran on the JVM. Amounts are small today; the predicate is still not measuring its subject.

---

## What I could not verify, and why

- **`attempt8/gate.md`'s archived RED log does not match the shipped assertion text.**
  `bb-budget-red.log` shows all four failures at `ns_isolation_test.clj:463`; the shipped assertions sit at
  **465 / 468 / 469 / 470** (my tampered run). I cannot reconstruct the builder's intermediate working tree, so I
  cannot say whether the archived red-before run used the same assertion forms that shipped. The claim
  "red before the budget entry, green afterward" is therefore *plausible but not reproduced* — what I did
  reproduce is that the shipped witness goes red on a one-ms change to the shipped constant.
- **Warm-cache timings.** My worktree had no wall cache (`109 namespace(s) have no measured wall and are
  scheduled first at the 300000 ms fallback`), so my 153,565 ms makespan is not comparable to the builder's
  83,361 ms. Only the *sums* and the pass/fail verdict are comparable; makespans are not.
- **The CLI/`owed.md` workspace-status claims** (26,581 vs 2,876 characters, empty normalized diffs). Reproducing
  them needs a private `GIT_INDEX_FILE` replay against a dirty tree, which I judged out of scope for a
  read-only red team and would have perturbed the one measured `make test-fast` lease.
- **Prewarm receipts, integration lane, feature-thread 43,280/728,265 ms.** Single-observation builder numbers on
  the builder's box; I ran neither (one JVM at a time, and the brief's required run was `make test-fast`). The
  feature-thread and intent-transaction rows are the two `runtime-measurements` entries **not** cross-checkable
  against attempt-7 receipts — I confirmed only that their cited logs exist.
- **Encounter scoring / Fable ratification / performance certification** — external by the builder's own report;
  nothing in the tree claims them.

---

## Housekeeping

Every tamper was restored and verified: `git status --porcelain` is empty at the tip; `ns_isolation.clj:67` reads
`:bb 399155`; `clj-kondo` on all changed files is `errors: 0, warnings: 0`. Nothing pushed, tagged, committed or
installed. No forbidden port contacted. Worktree removed on exit.
