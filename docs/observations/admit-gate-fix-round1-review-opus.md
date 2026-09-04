# Adversarial review — clj-surgeon `bridge/admit-gate` @ 7985b986 (patch admission gate: analyzer-truncation fix)

## GO-WITH-FIX

Reviewer: Opus (Anvil seat), taking Sol's brief verbatim after Sol's content filter refused it.
Clone: `/home/forge/tmp/sol/gate-wt`, `git rev-parse HEAD` = `7985b986c8926c660b07d92ee614d7c06aebcd6e`,
detached, `git status --porcelain | grep -v '^?? \.cpcache'` empty (0 tracked modifications) at start and at end.
Baseline for every "pre-existing" claim: a second clone at `17125fe6b2406df3a7470a39e66bf0ee421b6970`
(`/tmp/gate-review-fx/base`, `/tmp/gate-review-fx/base2`). All fixtures under `/tmp/gate-review-fx`.
The 14 frozen arms were read only through `cp -a` copies. No server was started (the verb is a library fn);
no port was bound by me. Nothing was committed, pushed, stashed, or `git add`-ed anywhere.

**One-line summary.** The fix does what it says: the field defect is real, reproduced red at the parent on
four independent arms, and green at the tip with the shipped defaults and no var rebinding. The 16 MiB
ceiling is a genuine memory bound and refuses honestly one byte past it. But the rung-3 ratchet itself has a
hole: `detectors_not_run` uses the wrong predicate for the focused-test half, so **three reproducible states
publish `verification_status: partial`, `hazards 0`, `detectors_not_run []` and a text block containing no
detector name and no "not a clean bill of health" sentence — while the focused-test detector produced no
usable reading at all.** That is the brief's stated blocking shape, in the very field this branch adds. It is
a two-line fix and it must land before the tip does.

---

## BLOCKING

### F1 — `detectors_not_run` asks "did the process run?" when it means "did the detector produce a reading?"

`src/clj_surgeon/mcp_admit_tool.clj:767-788`

```clojure
      (not (true? (:ran tests)))
      (conj {:detector "focused-tests"
             :reason (reason tests :no-test-evidence)})
```

`default-test-runner` sets `:ran true` whenever the child process *finished*, whatever it produced. The
predicate that means "produced a usable reading" is the one `verification-status` already computes two forms
below — `(:ok evidence)` from `test-evidence`. For the analyzer the two coincide (`default-lint-runner` sets
`:ran false` on every failure), so the field was never exercised on the test half. Its own docstring states
the contract it breaks: *"Name every requested detector that produced no reading at all."*

Command (fixtures and fake runners under `/tmp/gate-review-fx/bin`, probe at
`/tmp/gate-review-fx/probe/probe_partial.clj`):

```
cd /home/forge/tmp/sol/gate-wt && clojure -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.1"}}}' \
  -M /tmp/gate-review-fx/probe/probe_partial.clj
```

Verbatim output, three of the four cases (each: a tree that ships a `.clj-surgeon/focused-test.edn`, one
real `clj-kondo` run, one focused runner that produces no usable evidence):

```
######## R1 report names the wrong namespaces
  status= :partial  complete= false  reasons= [:report-namespaces-do-not-match]
  detectors_not_run= []
  tests= {:ran true, :passed 3, :failed 0, :reason :report-namespaces-do-not-match, :profile_absent false}
  TEXT MENTIONS 'did not run'?  false  | 'clean bill'?  false
   | admit_clojure_patch
   |   admit-patch-preview · 2 file(s) · owners +0 ~2 -0 · drift 0 bytes · hazards 0 · 1.00 ms
   | verification_complete=false verification_status=partial

######## R2 report says zero tests
  status= :partial  complete= false  reasons= [:no-test-evidence]
  detectors_not_run= []
  tests= {:ran true, :passed 0, :failed 0, :reason :no-test-evidence, :profile_absent false}
  TEXT MENTIONS 'did not run'?  false  | 'clean bill'?  false
   | admit_clojure_patch
   |   admit-patch-preview · 2 file(s) · owners +0 ~2 -0 · drift 0 bytes · hazards 0 · 1.00 ms
   | verification_complete=false verification_status=partial

######## R3 clean report but runner exits 3
  status= :partial  complete= false  reasons= [:runner-exit-nonzero]
  detectors_not_run= []
  tests= {:ran true, :passed 6, :failed 0, :reason :runner-exit-nonzero, :runner_exit 3, :profile_absent false}
  TEXT MENTIONS 'did not run'?  false  | 'clean bill'?  false
   | admit_clojure_patch
   |   admit-patch-preview · 2 file(s) · owners +0 ~2 -0 · drift 0 bytes · hazards 0 · 1.00 ms
   | verification_complete=false verification_status=partial

######## R4 runner exits 3 writing nothing
  status= :unverified  complete= false  reasons= [:verification-runner-failed]
  detectors_not_run= []
  tests= {:ran true, :passed 0, :failed 0, :reason :verification-runner-failed, :runner_exit 3, :profile_absent false}
  TEXT MENTIONS 'did not run'?  false  | 'clean bill'?  false
```

Why each is blocking, in the branch's own terms:

* **R3 is the sharpest.** The runner wrote a report claiming six passing tests and exited 3. The gate's own
  `test-evidence` docstring says of exactly this: *"A report that says everything passed, from a command that
  exited three, describes a run that did not finish the way it meant to. The report is not evidence of a clean
  suite."* The receipt then publishes `detectors_not_run []` — an affirmative structured claim that every
  requested detector produced a reading — beside `ok true`, `hazards 0` and a text block that says nothing at
  all about the suite.
* **R4** is `:verification-runner-failed`, the reason whose own docstring reads *"A runner that was launched,
  exited non-zero and wrote nothing produced no result at all."* `detectors_not_run` is empty for it.
* **R1/R2** are `partial` — the literal word the brief names as blocking — with `hazards 0` and no note.

This is the same asymmetry MCP-OP-ADMIT-124 was written to close, mirrored: 124 gave the analyzer the
"could not run" rule the tests already had; `detectors-not-run` gives the tests a "did not run" rule the
analyzer already had, and gets the test half's predicate wrong. Contrast the honest case, same probe suite
(`/tmp/gate-review-fx/out/matrix2.log`, T6 — a runner that cannot launch, so `:ran false`):

```
######## T6 analyzer live + runner cannot launch
  detectors_not_run= [{:detector "focused-tests", :reason :verification-runner-failed}]
   | detectors that did not run: focused-tests (verification-runner-failed)
   | this receipt reports what was inspected; with a detector silent it is not a clean bill of health
```

Whether the note appears turns on whether the broken runner managed to exit, which is not a fact about
verification.

**Required fix (F1).** In `detectors-not-run`, drive the test entry from the evidence verdict, not from
`:ran` — i.e. pass `evidence` in and emit the entry when `(not (:ok evidence))` and the reason is not
`:tests-failed` (a suite that ran and failed is a reading, and is already blocking). Ship it as its own
RED→GREEN rung with a witness over the R1/R3/R4 runner shapes above (fake runners are in
`/tmp/gate-review-fx/bin/runner-wrong-ns`, `runner-good-but-exit3`, `fake-test-runner-crash`), and reuse the
existing text-superset loop so the text block goes red with the structure. Also fold the analyzer half onto
the same predicate so the two halves cannot drift again.

**Corollary, same predicate, lower severity.** `empty-receipt` seeds `:detectors_not_run []`
(`mcp_admit_tool.clj:171-174`), and the `no-op?` and `(seq blocking)` refusal branches
(`mcp_admit_tool.clj:1467-1494`) merge that empty vector on receipts where **no detector ran at all**. Those
are refusals (`ok false`), so nothing reads clean — but `[]` there means "we never asked", indistinguishable
from "everything ran". Reproduced incidentally at `/tmp/gate-review-fx/probe/probe_edn.clj` first run:
`ok= false status= :unverified ... detectors_not_run= []` on a `patch-does-not-apply` refusal. Distinguish
`[]` from `nil`, or seed the refusal branches from `detectors-not-run` with the verify mode.

---

## Attacks, one by one (each reproduced)

### 1. Rung 2's 16 MiB — is it truly a memory bound?

**Yes, and it refuses honestly one byte past it.** Built a synthetic analyzer whose stdout is exactly
16,777,217 and exactly 16,777,216 bytes of valid findings EDN
(`/tmp/gate-review-fx/probe/gen_edn2.clj`, served by `/tmp/gate-review-fx/bin/fake-kondo-{over,under}`),
driven through the production `default-lint-runner` with the shipped default ceiling:

```
cd /home/forge/tmp/sol/gate-wt && clojure -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.1"}}}' \
  -M /tmp/gate-review-fx/probe/probe_ceiling.clj
```

```
max-heap-MiB 7832
analyzer-findings-visible-bytes 16777216
analyzer-read-ceiling default 16777216

=== OVER  (16MiB+1) wall-ms 129 heap-used-MiB 165
{:ran false, :ok false, :error-type :analyzer-output-truncated, :cap 16777216, :observed-bytes 16777217}
remedy: clj-kondo answered with 16777217 bytes of findings and this gate reads at most 16777216; raise the analyzer read ceiling (:admit-analyzer-visible-bytes) or narrow the patch to fewer files
error: clj-kondo findings were cut at 16777216 bytes of 16777217; the analyzer ran and the gate could not read its answer

=== UNDER (16MiB) wall-ms 2129 heap-used-MiB 169
{:ran true, :ok true, :baseline-count 143394, :future-count 143394, :introduced-count 0}
```

Typed refusal by rung 1, naming both the ceiling and the observed size, in 129 ms. No OOM, no hang. The
mechanism is sound by construction: `run-bounded!` redirects the child to a temp FILE and `file-evidence`
(`mcp_process.clj:345-360`) reads back at most `visible-byte-limit`, so the JVM's exposure is bounded by the
ceiling regardless of how much the analyzer emits — `:truncated` is `(> byte-count visible-count)`, a
comparison, not a read.

**At the shipped server heap.** `MCP_JAVA_OPTS ?= -J-Xms64m -J-Xmx512m` (`Makefile:35`), so I re-ran the
at-ceiling case at 512 MiB with 129,726 **distinct** findings (identical findings would be collapsed by the
delta's sets and would flatter the result):

```
cd /home/forge/tmp/sol/gate-wt && clojure -J-Xms64m -J-Xmx512m -Sdeps '{...nrepl...}' \
  -M /tmp/gate-review-fx/probe/probe_heap.clj
→ max-heap-MiB 512 / wall-ms 2361 / {:ran true, :ok true, :baseline-count 129726}
```

Survives, both images parsed, ~2.4 s. So the number is defensible as a memory bound at the heap the server
actually runs with.

**Should the branch run the memory battery? Yes — once, as a receipt, not as a gate.** Two reasons this
measurement is not the same as the battery's: (a) the pre and post images are read sequentially and both
parsed structures are live at the same time, so the worst case is shaped by finding *diversity*, which the
battery's shapes vary and my one fixture does not; (b) `OutOfMemoryError` is an `Error`, and every catch on
this path is `(catch Exception ...)` — `kondo-findings` (`:269`), `execute-request!` (`:1743`),
`handle-admit-clojure-patch` (`:1826`). An OOM below the ceiling therefore escapes the handler with no
receipt at all, rather than becoming a typed refusal. That is not a defect I could provoke at 512 MiB, but it
is the reason the bound deserves one battery run at 100/1k/10k-file shapes with an explicit `-Xmx` and a
numeric pass line, linked by LID id, before this ceiling is treated as proven. Cheap, and it converts a good
argument into a receipt.

### 2. Rung 3 — every field a consumer could read as clean, with detectors forced silent

Enumerated the consumer-visible verdict surface: `ok`, `operation`, `committed`, `mutation_attempted`,
`hazards`, `byte_drift_outside_hunks`, `verification_complete`, `verification_status`,
`verification_reasons`, `detectors_not_run`, `lint_delta`, `tests`, and the text block (which is exactly
`summary`, published verbatim by `mcp-operation/invoke!` — `[summary]` is the content vector, `result` the
structuredContent, and nothing truncates the text).

Nine states driven through `execute-request!` on real trees, analyzer forced absent via a production
`:admit-analyzer-command` that cannot launch and forced truncated via a real ceiling
(`/tmp/gate-review-fx/out/matrix2.log`):

| state | status | detectors_not_run | text names the detector | "not a clean bill" |
|---|---|---|---|---|
| T0 both live | complete | `[]` | n/a | n/a |
| T1 analyzer truncated + tests live | unverified | clj-kondo (analyzer-output-truncated) | yes | yes |
| T2 analyzer truncated + no profile | unverified | both | yes | yes |
| T3 analyzer absent + tests live | unverified | clj-kondo (clj-kondo-unavailable) | yes | yes |
| T4 analyzer absent + no profile | unverified | both | yes | yes |
| T5 analyzer live + runner exits 3 | unverified | **`[]`** | **no** | **no** |
| T6 analyzer live + runner cannot launch | unverified | focused-tests | yes | yes |
| T7 analyzer absent + runner exits 3 | unverified | **clj-kondo only** | partial | yes |
| T8 analyzer truncated + runner cannot launch | unverified | both | yes | yes |

T1–T4, T6, T8 are correct and the analyzer half is honest in every combination I could produce, including
both-absent. T5/T7 are F1 above; R1–R3 (same defect, reached through a runner that writes a *report*) are the
`partial` cases and are the blocking ones. The deliberately-excluded `:no-clojure-files` case is honest too
(`probe_edn.clj`): `detectors_not_run [{:detector "clj-kondo", :reason :no-clojure-files} ...]` with the full
note in the text.

**Sabotage of the text-superset witness — it holds.** I copied the tip's `src`/`test`/`test-fixtures`/
`resources` to `/tmp/gate-review-fx/sabotage` and dropped **only the reason names** from `detector-note`
(keeping the detector names and the bill-of-health sentence — a sharper sabotage than the builder's, which
dropped everything). `clj-surgeon.admit-patch-test` goes red on exactly three assertions and nothing else:

```
FAIL in (a-receipt-whose-detector-did-not-run-names-it-in-text-and-structure) (admit_patch_test.clj:3691)
the text block never names the reason clj-kondo-unavailable
expected: (str/includes? text (name reason))
  actual: (not (str/includes? "... detectors that did not run: clj-kondo\nthis receipt reports what was inspected; with a detector silent it is not a clean bill of health" "clj-kondo-unavailable"))
FAIL in (a-receipt-whose-detector-did-not-run-names-it-in-text-and-structure) (admit_patch_test.clj:3717)
... "verification-not-requested" ... (×2, the verify=none case)
Ran 105 tests containing 1194 assertions.
3 failures, 0 errors.
```

The witness genuinely iterates the structured entries and demands each detector *and* each reason in the
text. It is a real ratchet — for whatever `detectors_not_run` contains. Its blind spot is F1: it cannot see a
detector the structure never listed.

### 3. `partial` vs `unverified` — is `partial` with a missing suite false comfort?

**Argued both ways, then a verdict.**

*For the builder's split.* `partial` means "one of the two requested checks produced a usable result", and a
repository that ships no focused-test profile has not hidden a failure — it has nothing to run. Collapsing
that to `unverified` would make the two states that need different reactions look identical, which is the
exact complaint MCP-OP-ADMIT-082 exists to answer. And on this branch the receipt is not silent about it:
S3/T-shape output shows `detectors_not_run [{:detector "focused-tests", :reason :no-focused-test-profile}]`
plus the full text note, on a tree that has `test/app/*_test.clj` files and simply lacks the profile.

*Against.* `partial` is not merely a word here — it is a **write permission**. `incomplete-commit-refusal-
reason` (`mcp_admit_tool.clj:1302-1309`) waives the commit block on `(and allow-partial? profile-absent?)`
and never consults `verification_status`. So on a tree with tests and no profile — which describes
clj-surgeon itself: `ls /home/forge/tmp/sol/gate-wt/.clj-surgeon` → *No such file or directory* — a caller
passing `allow_partial: true` writes with the suite never run. Reproduced (`probe_waiver.clj`, W4):
`ok=true committed=true WROTE=true status=:partial reasons=[:no-focused-test-profile]`.

*Verdict.* The status word is fine; the **waiver** is the false-comfort surface, and it is pre-existing (see
Finding 2 below). Keep `partial`; fix the waiver's predicate. The one change I would make to the word itself
is small: `partial` should require that the half which *did* run is the analyzer, which is already true today
only by accident of which detectors exist.

**"A profile exists but the suite fails to start"** — `T6`, verbatim above: `tests {:ran false, :reason
:verification-runner-failed, :runner_exit nil, :profile_absent false}`, `verification_status unverified`,
detector named in structure and text, and `allow_partial` cannot waive it because `profile_absent` is false.
That case is handled correctly and is exactly right. The neighbouring case where the runner *does* start and
then produces nothing (R4) is F1.

### 4. The 14-arm replay claim — reproduced on four arms

Four `-pre` trees `cp -a`'d out of `/home/forge/tmp/arms/egater/trees/`, each with its frozen
`diff.patch` from `/home/forge/tmp/arms/ereg/<arm>/`, driven through `execute-request!` in preview with the
**shipped defaults and no `alter-var-root`** (the probe prints the two constants to prove it):

```
cd /home/forge/tmp/sol/gate-wt && clojure -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.1"}}}' \
  -M /tmp/gate-review-fx/probe/probe_replay.clj
```

```
NO VAR REBINDING. exact-verification-visible-bytes = 12000
analyzer-findings-visible-bytes = 16777216
ereg-k1-N-1  files=21  ok=true  | lint ran=true  ok=true  base=44    future=44    intro=0 blocking-intro=0 err= | status=:partial | 452 ms
ereg-k2-N-1  files=21  ok=true  | lint ran=true  ok=true  base=54    future=54    intro=0 blocking-intro=0 err= | status=:partial | 387 ms
ereg-k3-N-1  files=21  ok=true  | lint ran=true  ok=true  base=65    future=65    intro=0 blocking-intro=0 err= | status=:partial | 331 ms
ereg-k6-N-1  files=21  ok=true  | lint ran=true  ok=true  base=79    future=79    intro=0 blocking-intro=0 err= | status=:partial | 314 ms
```

Baselines **44 / 54 / 65 / 79**, exactly the E-GATE-R post-hoc cap-lifted figures, `introduced 0` on all four,
`ran true` 4/4. The same probe on the parent `17125fe`:

```
ereg-k1-N-1  ... lint ran=false ok=false ... err=:clj-kondo-unavailable | status=:unverified detectors_not_run=nil
ereg-k2-N-1  ... lint ran=false ok=false ... err=:clj-kondo-unavailable | status=:unverified detectors_not_run=nil
ereg-k3-N-1  ... lint ran=false ok=false ... err=:clj-kondo-unavailable | status=:unverified detectors_not_run=nil
ereg-k6-N-1  ... lint ran=false ok=false ... err=:clj-kondo-unavailable | status=:unverified detectors_not_run=nil
```

The defect is real, it is exactly as described, and it is fixed. This is the strongest evidence in the branch.

### 5. Are the vendored fixtures a ratchet or decoration? — a ratchet.

`test/clj_surgeon/admit_patch_test.clj:3620-3640` (`a-real-fan-out-patch-gets-a-lint-delta-that-ran`)
materializes `test-fixtures/field-diffs/egater-k{1,6}-pre-image`, applies the vendored `egater-k{1,6}.diff`
through `execute-request!` with no config overrides, and asserts `(is (true? (:ran lint)) ...)`. It fails
loudly when the analyzer half does not run — I watched it do so at `6f3d5649` with the field's own byte
counts, and again (for an environmental reason) in my sabotage tree before I copied `resources/`. A second
test (`a-denser-fan-out-image-yields-a-larger-baseline`) pins `k6 > k1`, so a stubbed or constant reading
cannot satisfy both.

Caveat worth one line in the branch's notes: the ratchet is **environment-coupled**. It needs `clj-kondo` on
the box *and* an admission wrapper resolvable from the JVM's CWD (see Finding 3), and when either is missing
it fails for a reason that has nothing to do with the code under test. It fails loud, not silent, which is the
right direction — but it cannot distinguish "the fix regressed" from "this box is not set up", so it should
carry a named PRECONDITION rather than be read as a pure regression signal.

### 6. The two disclosed pre-existing failures — both confirmed, neither masks anything here

| gate | @ 7985b986 | @ 17125fe | delta |
|---|---|---|---|
| `~/bin/suite-run bb test/run_all.clj` | 702 tests / 5912 assertions / **5 failures**, 0 errors | 702 / 5912 / **5 failures**, 0 errors | identical |
| `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | 482 / 5148 / **1 failure**, 0 errors | 477 / 5083 / **1 failure**, 0 errors | +5 tests, +65 assertions, same single failure |

* `mcp_change_buffer_test.clj:686` — `expected: ["/opt/homebrew/bin/clj-kondo" ...] actual: ["/usr/local/bin/clj-kondo" ...]`. A hard-coded macOS Homebrew path asserted on a Linux box. The file is untouched by this branch (`git diff 17125fe..7985b98 --stat` lists only `test/clj_surgeon/admit_patch_test.clj` among tests). It is one `is` inside `exact-profile-compilation-is-project-owned-and-snapshot-bound`; the assertions after it still execute, and the deftest is about `compile-exact-profile`, not the admit gate. **Cannot mask a regression in this branch's gates.**
* `agent_routing_test.clj:106` — all 5 failures are one deftest, `terminal-response-routing-is-conditional-on-complete-user-work`, asserting that the routing plate contains `terminal_response` wording the current plate no longer carries (the 2026-09-02 Surgeon-routing doctrine change). Byte-identical failure set at both shas. Unrelated surface. **Cannot mask a regression in this branch's gates.**

Neither is a *silencer*: both are hard failures with visible counts, so a new failure would raise the count.
The real masking risk is the opposite one and it is worth naming: **the mcp-test suite already runs non-green,
so "1 failure" is the pass condition, and a second failure is a number a tired reader can miss.** Both should
be fixed or explicitly quarantined with an expected-failure list before this suite is used as a merge gate.

---

## Other findings (not blocking this branch)

### Finding 2 (HIGH, pre-existing, must be filed) — `allow_partial` waives the commit block without consulting the status word, so a dead analyzer can still write

`mcp_admit_tool.clj:1302-1309`. The waiver is `(when-not (and allow-partial? profile-absent?) reason)` —
`verification_status` never enters it. Consequences, all reproduced (`probe_waiver.clj`), and **byte-identical
at the tip and at `17125fe`**:

```
--- TIP 7985b98 ---
W1 commit allow_partial verify=focused dead-analyzer ok=true  committed=true  WROTE=true  status=:unverified  reasons=[:clj-kondo-unavailable :no-focused-test-profile]
W2 commit allow_partial verify=none                  ok=true  committed=true  WROTE=true  status=:unverified  reasons=[:verification-not-requested]
W3 commit             verify=none                    ok=false committed=false WROTE=false status=:unverified  reasons=[:verification-not-requested]
W4 commit allow_partial verify=focused live-analyzer ok=true  committed=true  WROTE=true  status=:partial     reasons=[:no-focused-test-profile]
--- BASE 17125fe ---
(the same four lines, unchanged)
```

`WROTE=true` is a re-read of `src/app/core.clj` off disk, not a receipt field.

* **W1**: on a tree with no focused-test profile, `allow_partial: true` writes with **zero detectors having
  run** — the analyzer dead and the suite absent. MCP-OP-ADMIT-124's whole purpose ("an analyzer that could
  not run publishes `unverified` rather than `partial`") buys nothing at the commit gate, because the gate
  reads `profile_absent`, not the status.
* **W2**: `verify: "none"` plus `allow_partial: true` writes with nothing run at all. The docstring directly
  above the code says *"A gate a caller can turn off is a caller's gate. Verification may still be declined —
  in `preview`."* Rung L closed the `verify: "none"` ladder rung; `allow_partial` re-opens it one rung over,
  on any repository without `.clj-surgeon/focused-test.edn` — **including clj-surgeon itself**.

This is **not a regression of this branch** and the branch strictly improves the receipt in that state (the
parent published `detectors_not_run` nil; the tip names both silent detectors in structure and text). But it
is the single largest false-authorisation surface in the gate, and MCP-OP-ADMIT-124's commit message can be
read as claiming protection it does not deliver. File it as its own defect with a rung: the waiver should
require the *analyzer* to have produced a reading, i.e. `(and allow-partial? profile-absent? (true? (:ran
lint)))`, and `verify: "none"` should never be waivable in commit mode.

### Finding 3 (MEDIUM) — `kondo-findings` throws away the typed admission error, re-creating rung 1's defect one layer down

`kondo-findings` (`mcp_admit_tool.clj:269-315`) branches on `output-truncated` and otherwise falls to
`:clj-kondo-unavailable`. But `run-process!` (`mcp_change_buffer.clj:1284-1291`) already preserves
`:admission-error (ex-data error)`, and `mcp_change_buffer.clj:1293+` even ships an `admission-unverified?`
helper for exactly these types. `kondo-findings` ignores it. Two reproductions
(`/tmp/gate-review-fx/probe/probe_admission.clj`):

```
--- B: forced pressure deferral (CLJ_SURGEON_CLJ_KONDO_MAX_NORMALIZED_LOAD=0.0001) ---
run-process! -> {:finished? true, :exit 75, :output-truncated false}
receipt lint_delta -> {:ran false, :ok false, :error-type :clj-kondo-unavailable, :error "clj-kondo did not produce readable findings", :detector "clj-kondo"}

--- C: admission wrapper missing (server CWD not a clj-surgeon checkout) ---
admission wrapper resolves to: "/tmp/gate-review-fx/no-such-wrapper"
run-process! -> {:finished? false, :launch-error true, :exit nil, :admission-error {:error-type :clj-kondo-admission-unavailable, :gate "/tmp/gate-review-fx/no-such-wrapper"}}
receipt lint_delta -> {:ran false, :ok false, :error-type :clj-kondo-unavailable, ...}
```

So "the box was too loaded and the analyzer was deferred" (transient, retryable, exit 75) and "this server
cannot find its own admission wrapper" (a deployment fault, and the ex-data names the exact path) both publish
the type reserved for "the analyzer did not answer". Note the consequence for the new set:
`unverifiable-lint-error-types` (`:748-764`) lists `:clj-kondo-executable-unavailable`,
`:clj-kondo-admission-unavailable`, `:clj-kondo-admission-timeout`, `:clj-kondo-pressure-deferred` and
`:process-interrupted` — **four of those seven members are unreachable from this code path**, because the
collapse happens before the set is consulted. That is dead code masquerading as coverage. Rung 1's own thesis
applies verbatim: *"Collapsing the second into the first is how a gate reports a missing tool it is in fact
holding."*

Severity is medium, not blocking, because every one of these still lands on `:clj-kondo-unavailable`, which
*is* in the unverifiable set, so the status is correctly `unverified` and no commit is authorised. It is an
honesty and diagnosability defect, not an authorisation one. Fix: propagate `(:error-type (:admission-error
raw))` when present, and carry `:gate` / `:remedy` the way rung 1 carries `:cap` / `:observed-bytes`.

Related deployment note surfaced by the same probe: `clj-kondo-admission-path` (`mcp_process.clj:121-133`)
falls back to `resources/clj-kondo-admission.py` **relative to the JVM's current working directory**, and
`~/bin/clj-kondo-admission` is not installed on this box. A workspace-routed server started outside a
clj-surgeon checkout therefore reports `clj-kondo-unavailable` for every admit call on every workspace.
Pre-existing, untouched here, worth a bead.

---

## Gates, verbatim, with exit codes

All JVM suites through `~/bin/suite-run`; `clojure -M:clj-surgeon/mcp-test` invoked directly, never
`make mcp-test`.

| gate | command | result | exit |
|---|---|---|---|
| babashka suite | `~/bin/suite-run bb test/run_all.clj` | `Ran 702 tests containing 5912 assertions.` / `5 failures, 0 errors.` — all 5 pre-existing (`agent_routing_test.clj:106`), identical at `17125fe` | n/a (log) |
| MCP JVM suite | `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 482 tests containing 5148 assertions.` / `1 failures, 0 errors.` — pre-existing (`mcp_change_buffer_test.clj:686`), same single failure at `17125fe` (477/5083/1) | n/a (log) |
| Prolog oracle | `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | **0** |
| intent audit | `clojure -M -e "(clj-surgeon.mcp-intent-contract/audit-current-repository \".\")"` | `:ok true :violations []`; `MCP-OP-ADMIT-121/122/123/124` each `:implemented :impl true :test true` | **0** |
| admit gate alone | `~/bin/suite-run /tmp/gate-review-fx/run-admit-test.sh /home/forge/tmp/sol/gate-wt` | `Ran 105 tests containing 1194 assertions.` / `0 failures, 0 errors.` | **0** |

Caveat on the intent audit, which the branch leans on: it is a **marker-presence** audit. It checks that a
`@spec MCP-OP-ADMIT-123` comment exists in some src file and some test file. It cannot see that
`detectors_not_run` is empty in three states the intent's own sentence covers — F1 sails through it green.
An audit that verifies markers is not a ratchet for the behaviour the markers name.

### RED→GREEN ladder — independently re-run, every rung

`/tmp/gate-review-fx/ladder.sh`, checking out each sha in a separate clone and running
`clj-surgeon.admit-patch-test` alone:

| sha | claim | observed |
|---|---|---|
| `454a3c42` | RED rung 1 | `6 failures, 1 errors` — `expected: (= :analyzer-output-truncated (:error-type result)) actual: (not (= :analyzer-output-truncated :clj-kondo-unavailable))`, `:cap nil`, `:remedy nil`. Red for the defect's reason. |
| `be442ce9` | GREEN rung 1 | `Ran 101 tests containing 1140 assertions. 0 failures, 0 errors.` |
| `6f3d5649` | RED rung 2 | `6 failures, 10 errors` — `the analyzer half never ran in the field: {:error-type :analyzer-output-truncated, :cap 12000, :observed-bytes 12043}` and `... :observed-bytes 21883`. Both figures match the field replay. |
| `6614e901` | GREEN rung 2 | `Ran 104 tests containing 1176 assertions. 0 failures, 0 errors.` |
| `5c0da977` | RED rung 3 | `7 failures` — including `expected: (= :unverified (:verification_status result)) actual: (not (= :unverified :partial))` (the MCP-OP-ADMIT-124 asymmetry, disclosed and real), `detectors_not_run` nil, and a text block containing neither the status word, the detector, the reason, nor the bill-of-health sentence. |
| `7985b986` | GREEN rung 3 | `Ran 105 tests containing 1194 assertions. 0 failures, 0 errors.` |

Every rung is red at its RED sha for the reason its commit message states, and green at its GREEN sha. The
commit messages' quoted numbers (12,043 / 21,883 / 44 / 79) all reproduce. **No claim in the six commit
messages was found false.**

---

## What is genuinely good here (so the fix is not read as a rejection)

1. **The decision in rung 2 is right and the reasoning is the reusable part.** "A cap on the detector's input
   is a cap on truth; a cap on the receipt is a cap on noise" is a distinction worth promoting past this
   repository. The alternative (a bigger constant) would have moved the cliff to a new `k` with no new signal,
   as the commit argues and the 11,999–21,883 straddle demonstrates.
2. **The witnesses assert at the ceiling, not at a constant** — both ceilings derived from the image's own
   measured size — so they survive a change to the number. That is the right shape for a bound.
3. **The red run found something worse than it was written for** (the `partial` asymmetry, MCP-OP-ADMIT-124)
   and the builder folded it in rather than shipping around it.
4. **The sabotage was run and disclosed, and it holds** under a sharper sabotage than the one disclosed.
5. **The `no-clojure-files` exclusion from `unverifiable-lint-error-types` is correct and correctly argued.**

---

## Verdict

## GO-WITH-FIX

**This exact tip may NOT land on MCP/main.** F1 is blocking by the brief's own criterion — three reproducible
states publish `verification_status: partial`, `hazards 0` and `detectors_not_run []` with a text block that
names no silent detector, while the focused-test half produced no usable reading — and it is a defect *inside*
the ratchet this branch adds, which is the worst place for it: a consumer who learns to trust
`detectors_not_run` is worse off than one who never had the field. Everything else on the branch is sound and
independently reproduced, including the field defect, its fix on four arms, the ceiling's behaviour at
16 MiB ± 1 byte and at the shipped 512 MiB heap, and the whole RED→GREEN ladder.

**The tip plus F1 may land**, on these conditions: (a) F1 ships as its own RED→GREEN rung with a witness over
the R1/R3/R4 runner shapes, driven through the production path, and with the existing text-superset loop
extended to it; (b) that witness is run and green **at the new tip on current `MCP/main`**, not on this
branch's base — a GO is a claim about a base, and `MCP/main` has moved; (c) Finding 2 (the `allow_partial`
waiver) and Finding 3 (the discarded admission error type) are filed as beads before the merge, with Finding 2
treated as high — it is pre-existing, so it does not block this branch, but it is the gate's largest
false-authorisation surface and this branch's commit messages can be read as implying it is closed.
Recommended additionally, not blocking: one memory-battery run at explicit `-Xmx` to convert the 16 MiB
argument into a receipt, and a `(catch Throwable ...)` on the admit handler so an OOM below the ceiling
becomes a typed refusal rather than an escaped `Error`.

Clone left clean at `7985b986c8926c660b07d92ee614d7c06aebcd6e`; no server started; nothing committed, pushed,
or stashed on any tree.
