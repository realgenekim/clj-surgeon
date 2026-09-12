# Independent verification — clj-surgeon `bb-rewrite-tower` block B, attempt 10

**Subject:** tip `b2049db5148cc1020c79ecf1cbaa01dfe160514b` (code tip `7da1b44320de66d583ebf0a3b467db297a4c6088`), base `eae1e43280635be9b4e317e176d29476fd358702`
**Prior review:** `/var/tmp/forge/bbtower-fx/opus-redteam-blockB.md` (GO-WITH-FIX at `6e3e5535`, five findings)
**Verifier worktree:** `/var/tmp/forge/bbtower-fx/verify` (detached, removed at exit)
**Every JVM/bb:** `-Xmx1g`, `JAVA_TOOL_OPTIONS="-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx"`, one at a time. Nothing pushed, tagged, committed or installed. No forbidden port contacted. Every plant restored; `git status --porcelain` empty at exit.

---

## VERDICT: NO-GO

**Single reason:** the **374,149 ms `:bb` ceiling that Fable is being asked to ratify does not reproduce from the shipped tree.** Under the shipped `lane_manifest.clj` at this tip, the same calibration receipt gives `ceil(240,989 × 60,000 / 42,143) = **343,102 ms**`, not 374,149 — because the derivation's denominator is a *frozen* cadence map (`ceiling-inputs.edn`, 107 entries) that omits 47 measured namespaces, 33 of which are `:fast` at this tip and sum 3,497 ms. The numerator was repaired to the shipped runtime map; the denominator now carries the identical defect the red-team raised as F3, moved one term over. Fourteen lines below the new number, `docs/intent/test-isolation/test-isolation-specs.md:286` still reads *"At 399,155 ms accept; at 399,156 ms refuse"* — the retired ceiling, which the shipped code now **refuses**.

Everything else I attacked held. Four of five checks are FIXED, the fast entrance is green and fast (26,960 ms makespan, 0 isolation violations), and nothing here is a correctness failure — the ceiling error's direction is *safe* (31,047 ms / 9.05 % **looser**, so it can never manufacture a refusal). This is a two-line repair: recompute to 343,102 under the shipped manifest, **or** state the frozen-denominator rule as the ceiling's definition, and correct `specs.md:286`. After that repair I expect GO.

---

## The five checks

| # | Check | Verdict | Deciding command | Output excerpt |
|---|---|---|---|---|
| 1 | Refusal-completeness blind spot (red-team F1) | **FIXED** | `mcp_http_server.clj:219` → `:probe-servlet-unregistered-kind`; then a complete valid dead row in `docs/intent/probe/refusals.edn`; `clojure -J-Xmx1g -M:clj-surgeon/test-deps -e "(require …splice-envelope-test)(t/run-tests …)"` | Baseline `Ran 2 tests containing 63 assertions. 0 failures, 0 errors.` · Servlet plant → `2 failures`: `FAIL … (splice_envelope_test.clj:89)` / **`mcp_http_server.clj emits unregistered refusal kinds`** / `actual: (not (= #{} #{:probe-servlet-unregistered-kind}))`, plus the vocabulary set failure at `:85`. · Reverse plant → `1 failures`: `probe refusal vocabulary must be complete` / `actual: (not (= #{…7 kinds…} #{…7 kinds… :probe-registered-but-never-emitted}))`. Both restored. |
| 2 | Probe verification boolean (red-team F4) | **FIXED** | `grep -rn verification_complete src/` ; `clojure … receipt-booleans-test` ; two plants | `src/clj_surgeon/probe.clj` has **no** `verification_complete` — `refusal` (`:24-28`) and `verdict` (`:42-48`) publish only `:proof_pending [cold-gate]`. Registry records why: `registry.edn:13 :probe-policy "Option (a) … verification_complete false was a constant, not evidence that a seam could flip"`, and `:verbs {"probe" {}}`. Baseline `Ran 2 tests containing 46 assertions. 0 failures`. Plant `:verification_complete false` back into `probe/verdict` → **`7 failures`** incl. `probe missing boolean fields` / `actual: (not (= [] [:verification_complete]))` and three ×2 `(not (contains? receipt :verification_complete))`. Plant removing `"probe" {}` from the registry → **`2 failures`**: `Missing boolean seam registry by verb` / `actual: (not (= [] ["probe"]))`. Unregistered-verb-by-name witness exists and is executed: `receipt_booleans_test.clj:107-111` injects `{:name "third_verb" … "version"}` and asserts `["third_verb"]`. |
| 3 | Ceiling derivation reproduces (red-team F3) | **NOT FIXED** | `bb -Xmx1g --classpath src:test:libs/clj-splice/src:dev/experiments` folding `attempt10/ceiling-run/receipt.edn` through shipped `lm/namespace-runtimes` and `lm/lane-of` | `declared-bb count 79 sum 240989` ✅ reproduces the numerator exactly · `fast-cadence count 94 sum 42143` ❌ (spec claims 38,646) · `ceil(bb*60000/fast) = 343102` vs registered **374149**. The 38,646 comes from `ceiling-inputs.edn`'s frozen `:cadence-map` (107 entries); 47 measured namespaces are absent from it, of which **33 are `:fast` at this tip summing 3,497 ms** (38,646 + 3,497 = 42,143) and 14 are `:battery` summing 228,306 ms. Corroborated live by my own run at this tip: `cadence :fast: serial-equivalent 42141 ms`. **Enforcement itself is exact and real**: `:bb 374149`→`374150` in `ns_isolation.clj` → `Ran 25 tests containing 160 assertions. 4 failures`, incl. `expected: (= 374149 (:bb iso/lane-budget-ms))  actual: (not (= 374149 374150))` and the boundary pair at `:466-470`. |
| 4 | bb no-argument diagnostic (red-team F2) | **FIXED** | `bb -Xmx1g test/run_all.clj` | **`EXIT=0`**, `Ran 829 tests containing 7281 assertions.` / `0 failures, 0 errors.`, 3 m 35 s. Gate stage: **`Makefile:1211-1214 test-bb-diagnostic`** (`bb -Xmx1g -Djava.io.tmpdir="$(TMPDIR)" test/run_all.clj`, `@spec TEST-ISO-015`), wired into the landing gate and prewarm by `test/clj_surgeon/battery_parallel_runner.clj:739` → `{:target "test-bb-diagnostic" :kind :after}` in `gate-stage-manifest`, and pinned by `test/clj_surgeon/battery_parallel_test.clj:699` which asserts the exact stage vector. |
| 5 | Every fast-entrance namespace budgeted (red-team F6) | **FIXED** (with a scope caveat) | `bb … -e '(let [inv (r/suite-namespaces "fast")] … (r/unbudgeted-members inv))'` ; then `make test-fast` once | `fast inventory count: 94` · **`lane-of nil: []`** · **`unbudgeted-members: []`** · `count nil-lane: 0  count unbudgeted: 0`. The red-team's 47 are gone. `make test-fast` **`EXIT=0`** in 30.6 s wall: `makespan: 26960 ms` · `cadence :fast: serial-equivalent 42141 ms; budget 60000 ms` · `bb-runtime: serial-equivalent 14181 ms; budget 374149 ms; makespan 8291 ms` · **`test-isolation: 0 violations across 94 namespace(s) (TEST-ISO-002/003/004/005/007/010)`** · `battery-parallel: makespan 26960 ms over 8 lane(s); serial-equivalent 42141 ms; skipped 0`. Receipt: `{:state :passed, :suite "fast", :runtime :hybrid, :wall-ms 26960, :result {:test 1238, :pass 11707, :fail 0, :error 0}, :isolation-failures 0, :lane-count 8, :process-count 16}`. Cold worktree (`94 namespace(s) have no measured wall`). Makespan is **55 % under** the 60 s fast-lane budget. |

### Also verified (the red-team's own method, at this tip)

- `git diff eae1e432 b2049db5 -- test/clj_surgeon/ns_isolation.clj` is **exactly one line**: `+   :bb 374149` inside `lane-budget-ms`. `:fast 60000` and `:integration 240000` are byte-untouched.
- `deftest` name sets, base vs tip: **2,603 total / 2,602 unique on both revisions, `diff` empty — IDENTICAL NAME SETS.** (One name legitimately duplicated across two files on both sides; the red-team's 2,603 is the total count.)
- Red-team **F7** (`report!` charged the bb sum by *declared* runtime) is **fixed in effect**: `battery_parallel_runner.clj:905-907` now derives `bb-members` from `(filter #(= :bb (:runtime %)) lanes)` — the executed lane runtime. Folding the calibration receipt both ways gives the identical 240,989 ms, so the predicate now measures its subject.

---

## Findings, ranked by severity

### V1 — MEDIUM. The ceiling awaiting ratification cannot be evaluated from the shipped tree
`docs/intent/test-isolation/test-isolation-specs.md:269-272`, `docs/observations/2026-09-12-bbtower-block-b/attempt10/ceiling-inputs.edn`, `docs/observations/2026-09-12-bbtower-block-b/attempt10/derive-ceiling.clj:10`

The spec states the equation in shipped terms — *"The shipped-map run records a serial bb-runtime namespace sum of 240,989 ms and a fast-cadence sum of 38,646 ms in the same run"* — but only the **numerator** is shipped-derived. `derive-ceiling.clj:9` folds `lm/namespace-runtimes` (shipped, and I reproduce 240,989 exactly); `derive-ceiling.clj:10` folds `(:cadence-map inputs)` (frozen, 107 entries). The script's own assertion only checks the *runtime* map against shipped; there is no equivalent assertion for cadence, and its final `doseq` silently prints the 47 namespaces the frozen map cannot classify rather than refusing.

Measured at this tip, over the identical receipt:

| term | shipped value | value used | delta |
|---|---:|---:|---:|
| bb-runtime sum | 240,989 ms | 240,989 ms | 0 |
| fast-cadence sum | **42,143 ms** | 38,646 ms | −3,497 ms |
| ceiling | **343,102 ms** | **374,149 ms** | **+31,047 ms (9.05 % looser)** |

Live corroboration from my own `make test-fast` at this tip: `cadence :fast: serial-equivalent 42141 ms` (2 ms from the receipt-derived 42,143 — different run, same tree).

This is honest — `specs.md:277-279` *does* disclose *"These maps precede the subsequent cadence repair; replay uses the frozen maps so later lane moves cannot rewrite the inputs"* — and the direction is safe: a looser ceiling can never manufacture a refusal. But a ratifier cannot check the number, and mixing a shipped numerator with a frozen denominator is inconsistent on its face: freeze both, or ship both. **Fix (either):** (a) recompute and register **343,102 ms**, which the tree reproduces and which the current bb sums clear by a factor of 24 (`bb-runtime: 14,181 ms` in my fast run; 235,673 ms in the builder's bb pool); or (b) make the frozen denominator the *definition* — state that the ratio is fixed at its calibration cadence and never re-derived — and add the missing cadence assertion to `derive-ceiling.clj` so a future lane move refuses instead of printing.

### V2 — MEDIUM. The spec's boundary sentence still declares the RETIRED ceiling, which the code now refuses
`docs/intent/test-isolation/test-isolation-specs.md:286`

Verbatim, fourteen lines below the new 374,149 declaration:

> `At 399,155 ms accept; at 399,156 ms refuse naming bb and both numbers.`

At this tip `iso/lane-budget-ms` is `{:bb 374149}`, so a 399,155 ms bb sum **refuses**. The sentence is not merely stale, it asserts the opposite of the shipped behaviour, by 25,006 ms, in the exact paragraph a ratifier reads to decide. The *tests* are correct and current (`ns_isolation_test.clj:465-470`, `battery_parallel_test.clj:173-185` both pin 374,149/374,150) — only the intent spec disagrees, and no gate compares the two. `grep -rn "399155\|399,155"` returns this line and nothing else in `src/`, `test/` or `docs/intent/`. One-line fix.

### V3 — MEDIUM. Residual completeness blind spot *inside* a declared owner file
`test/clj_surgeon/splice_envelope_test.clj:22-25`; reachable via `src/clj_surgeon/mcp_hot_verify.clj:265`

F1's file-level hole is closed — `mcp_http_server.clj`'s `requiring-resolve` call is now seen, and `(is (seq kinds))` (`:87`) fails any owner file that contributes zero. But `probe-owners` still narrows `mcp_hot_verify.clj` to `#{'probe! 'probe-reload-order}`, and `probe!` relays **any** kind out of `ex-data`:

```
(if-let [kind (:error-type (ex-data e))]
  (assoc (probe/refusal kind (.getMessage e)) :elapsed_ms (elapsed))
```

So a kind minted in *any other top-level form of that same declared owner file* reaches the caller unseen. Proven, not inferred: I added `(defn- probe-guard [request] … (ex-info "guarded" {:error-type :hot-verify-helper-unregistered}))` to `mcp_hot_verify.clj` and one call to it inside `probe!`, and the witness reported `Ran 2 tests containing 63 assertions. **0 failures, 0 errors.**` The `(is (seq kinds))` guard cannot catch it because the two named forms still contribute their own kinds. Same `scanner-brief-names-vs-spellings` class, one level down. **Fix:** drop the name filter for `mcp_hot_verify.clj` as was already done for `probe.clj` (`probe-owners` maps it to `nil` = whole file), or assert that no `:error-type` literal anywhere in an owner file is unregistered.

### V4 — LOW / scope note. The 47 were budgeted partly by *removing* the expensive ones from the fast entrance
`test/clj_surgeon/lane_manifest.clj` (manifest), `battery_parallel_runner.clj:663-670`

Of the red-team's 47 unbudgeted coordinator members (231,803 ms measured), **33 became `:fast` (3,497 ms) and 14 became `:battery` (228,306 ms)** — 98.5 % of the mass left the fast entrance rather than acquiring a fast-lane ceiling. The 14, slowest first: `install-test` **81,558 ms**, `parser-admission-test` **71,055 ms**, `tmp-leak-support-test` 15,212, `cli-dispatch-test` 13,950, `show-form-test` 13,462, `help-test` 11,635, `edit-test` 6,029, `intent-transaction-test` 5,511, `extract-test` 4,823, `lens-query-test` 2,354, `xray-test` 1,503, `edn-config-integration-test` 516, `partition-all-test` 375, `platform-selector-test` 323. This is legitimate lane triage and is exactly why the makespan fell from the red-team's 153,565 ms to my 26,960 ms — and they remain covered by `test-bb` / the battery lane in the landing gate — but the `make test-fast` inventory went 109 → 94, so "the fast entrance is now 27 s" and "the fast entrance covers what it covered last week" are different claims. `suite-namespaces "fast"` is now plain `lm/namespaces-for :fast` with the historical bb union removed (`:666`), which is the right shape.

### V5 — LOW, carried forward unfixed. TEST-ISO-016's only implementation-side link is still a Makefile comment
`Makefile:1122`

Re-ran the red-team's F5 probe at this tip: `grep -rn "TEST-ISO-016" src/ Makefile` returns exactly one hit, `Makefile:1122: @# @spec TEST-ISO-016 -- coordinator consumes measured lane-manifest runtimes.` The rule still lives in `test/clj_surgeon/lane_manifest.clj`, which the intent audit classifies as test source. Unchanged by this block; noting only that it was not addressed.

---

## What I could not verify, and why

- **Warm-cache comparability.** My worktree had no wall cache, so my 26,960 ms makespan is not comparable to the builder's 23,939 ms except in sign; the *sums* (42,141 vs 42,653 ms fast cadence) and the pass/fail verdict are.
- **The prewarm, integration lane and bb-pool receipts.** Single-observation builder numbers on the builder's box; the brief's required runs were `make test-fast` and the bb diagnostic, and the one-suite-at-a-time rule forbids more. I confirmed the cited logs exist and that `prewarm-2`'s stage vector contains `{:target "test-bb-diagnostic", :exit 0, :wall-ms 218927}` at `git-head 7da1b443`, the code tip.
- **Whether `ceiling-inputs.edn`'s frozen cadence map faithfully equals `lm/lane-of` at commit `22bb9296`.** I did not build that intermediate revision. What I *did* establish is stronger for the verdict either way: **zero** of the 107 frozen entries disagrees with the shipped manifest (`measured ns where frozen cadence != shipped lane-of: 0`) — the entire 3,497 ms gap is absence, not contradiction.
- **Encounter scoring, Fable ratification, performance certification, physical acceptance** — external by the builder's own report; nothing in the tree claims them.

## Housekeeping

Five plants, all restored and verified (`git status --porcelain` empty after each): `mcp_http_server.clj` servlet kind; a valid dead row in `docs/intent/probe/refusals.edn`; `:verification_complete false` in `probe/verdict`; the `"probe" {}` registry entry; `:bb 374149`→`374150`; and the `probe-guard` helper in `mcp_hot_verify.clj`. All Clojure/EDN edits were made as form edits with a native patch, never string surgery. One JVM or bb at a time throughout; box load 1.38 at start. Worktree removed at exit.
