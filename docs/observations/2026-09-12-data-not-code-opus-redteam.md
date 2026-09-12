# Independent red-team review — clj-surgeon `fable/data-not-code`

**VERDICT: GO-WITH-FIX** — the envelope, sandbox-classification and sleep-identity ratchets all
survive adversarial plants, but `DATACODE-ROWS-001`'s "receipts are required" never opens a
receipt: an internally consistent forged evidence row that contradicts its own cited receipts is
accepted and changes which runtime a namespace actually runs on.

Subject: tip `4658dc124ee243300d19861402b2696ae1046321`, base `3b6d53577be3173cce209934e37bac904b3d38d7`.
Worktrees `/var/tmp/forge/datacode-fx/redteam` (tip) and `.../redteam-base` (base), both removed after the run.
All JVMs `-Xmx1024m`, `TMPDIR=/var/tmp/forge/datacode-fx`, one at a time, `make test-fast` once at the end.
Every defect plant was made with a native form edit and reverted with `git checkout`; the tip tree was
verified clean (`git status --porcelain` empty) before the final suite and at the end.

---

## Verdicts

| # | Claim | Verdict | Deciding command | Excerpt |
|---|---|---|---|---|
| 1 | Envelope admission refuses outside-envelope publication with `:write-outside-envelope` + `:path`/`:resolved-path`/`:envelope-id`, creates nothing, changes no outside bytes | **CONFIRMED** (one limit, F3) | `clojure -M:clj-surgeon/test-deps -e '(load-file ".../probe/p1.clj")'` | 5/5 escapes refused; `dirs-created-check: outside/absent exists? false  allowed/ok exists? false  kept.edn= "sentinel"  sibling= "sibling-sentinel"` |
| 1a | absent parent | CONFIRMED | p1 | `{:error-type :write-outside-envelope, :path ".../outside/absent/tail/undo.edn", :resolved-path ".../outside/absent/tail/undo.edn", :envelope-id "29a108ba…", :effect :receipt-publish}` |
| 1b | symlink ancestor | CONFIRMED | p1 | `:path ".../allowed/anc/absent/undo.edn", :resolved-path ".../outside/absent/undo.edn"` |
| 1c | symlink FINAL file | CONFIRMED | p1 | `:path ".../allowed/final.edn", :resolved-path ".../outside/kept.edn"` |
| 1d | telemetry ledger path | CONFIRMED | p1 | `ledger-outside-root: {:error-type :write-outside-envelope, :effect :telemetry-append, …}` / `outside/alias-migration-receipts created? false` |
| 1e | request map carrying destination authority refused as unknown field | CONFIRMED | p1 + p5 | `request-carried-envelope: {:error-type :unknown-arguments, :unknown [:destination-envelope]}`; `alias_migration → {:error_type "invalid-mcp-request", :error "Unknown alias_migration fields: destination_envelope"}` |
| 1f | launcher supplies none → bounded policy default, never unbounded | CONFIRMED | p1 | `policy-default: {:source :policy-default} ["/var/tmp/forge/datacode-fx" "/home/forge/.local/state/clj-surgeon" "/var/tmp/forge/datacode-fx/redteam"]` / `valid? true` / `unbounded-root? false` |
| 1g | **plant**: remove admission from `require-change-io/save!` | **RED by name** | `rt.sh plant1 receipt-artifacts-boundary-test` | `FAIL in (destination-envelope-final-filename-consumer-matrix) … clj-surgeon.require-change-io/save!` — `4 failures` vs baseline `0` |
| 1h | **plant**: remove ledger admission from `append-telemetry!` | **RED by name** | `rt.sh plant2 receipt-artifacts-boundary-test` | `FAIL in (destination-envelope-admits-ledger-before-creation) … expected :write-outside-envelope, actual nil` — `2 failures` |
| 2 | Refusal classification is a value, not prose | **CONFIRMED** | see below | no classification regression on a 22-expression battery |
| 2a | change the English denial wording → classification unchanged | CONFIRMED | wording replaced with `"Zzz qqq wibble 42 -- totally different wording"`, then `rt.sh xray-test edit-dsl-test` | `Ran 56 tests containing 926 assertions. 0 failures, 0 errors.` |
| 2b | untyped exception whose message *looks* like a denial is NOT classified as one | CONFIRMED | `xray_test.clj:188-190` under a clean tip | in-suite: `"spit is not allowed!"` → `:evaluation-failed`; whole ns green `28 tests / 507 assertions` |
| 2c | **plant**: classifier reads the message again | **RED by name** | `rt.sh plant3 xray-test` | `FAIL in (sandbox-refusal-classification-is-data-not-prose)` ×10 — both directions: wording-changed denials → `:evaluation-failed`, and the English impersonation → `:disallowed-symbol` |
| 2d | no hidden classification regression vs base | CONFIRMED | `diff p2-base.txt p2-tip.txt` over 22 expressions | only difference is the added `:symbol` column (base `nil` → tip `spit`/`eval`/`deref`/…); every `:reason` identical |
| 3 | Manifest consumes EDN rows; missing receipts refuse; statistic tampering is caught | **REFUTED (partially)** | `.../probe/p3.clj` | `A1 nonexistent-receipt-paths -> :ACCEPTED`; `A4 forged-consistent-row (walls 6ms -> 9000ms, runtime bb -> jvm, receipts UNCHANGED) -> :ACCEPTED` |
| 3a | delete one row's receipt path → refusal by name | CONFIRMED | p3 | dropped `:logs` → `:missing-evidence-receipts` with `:namespace clj-surgeon.mcp-recovery-test`; dropped one log element → `:invalid-runtime-evidence` with `:namespace` |
| 3b | alter a *single* statistic → caught | CONFIRMED | p3 | `A3 mean-only-altered -> :invalid-runtime-evidence` |
| 3c | alter statistics *consistently* without touching the receipt → caught? | **NO — finding F1** | p3 A4 | accepted; and the shipped data is honest: `B cross-check rows-vs-receipts: rows= 38 receipts-checked= 456 disagreements= 0` |
| 4 | Sleep identities are owner + ordinal + declared purpose; lines are diagnostics | **CONFIRMED** | `.../probe/p4.clj` on `test/clj_surgeon/mcp_hot_verify_test.clj` | see 4a–4d |
| 4a | unrelated line above a pinned sleep | CONFIRMED green | p4 | `unrelated line inserted at top  []` (line observation moves 289→290, pin unaffected) |
| 4b | purpose keyword changed / metadata removed | CONFIRMED red, names owner + ordinal | p4 | `[{:owner clj-surgeon.mcp-hot-verify-test/hot-verification-deadline-is-not-reset-by-non-terminal-responses, :ordinal 1, :reason :purpose-changed}]` |
| 4c | second sleep added to the same test (cardinality) | CONFIRMED red | p4 | `[{… :ordinal 2, :reason :undeclared}]` |
| 4d | sleep argument changed 50→51 | CONFIRMED red | p4 | `[{… :ordinal 1, :reason :call-changed}]` |
| 5 | Preregistration fixes membership, order, n, both arms' commands, complete wall, both bettors' rows | **CONFIRMED with named ambiguities (F7)** | read of `round3/probe-measure-preregistration.md`; `grep -n ':probe' src/clj_surgeon/core.clj`; `grep -n '^warm:' -A3 Makefile` | membership/order/n/commands/wall-definition all present and unambiguous; both commands exist in-tree (`core.clj:2163 :probe`, `Makefile:371 warm`). Five reinterpretable points listed in F7. |
| 6 | Nothing else regressed | **CONFIRMED** | see below | `ns_isolation.clj` diff 0 lines; deftest census 2,620 → 2,628 with **0 removed**; `make test-fast` green; block-B witnesses green |
| 7 | Coverage is "the shared artifact boundary and its inventoried consumers, not every writer" — exact? | **CONFIRMED, and I name two uncovered writers** | `comm` of writers vs `admit-target!`/`admitted-file` callers | `src/clj_surgeon/mcp_telemetry.clj:70-78`, `src/clj_surgeon/mcp_http_server.clj:234-240` (F6) |

### Claim 6 detail — every number from a command

| Check | Command | Result |
|---|---|---|
| `ns_isolation.clj` unchanged | `git diff 3b6d5357..4658dc12 -- test/clj_surgeon/ns_isolation.clj \| wc -l` | `0` |
| deftest census (real source, not the declaration) | `grep -rn '^(deftest ' test …` base vs tip, `comm` under `LC_ALL=C` | `base=2620 tip=2628  REMOVED=0  ADDED=8` |
| declared census file | `diff` of `test/clj_surgeon/deftest_census.edn` | `removed-lines=0  added-lines=8` (agrees with source) |
| baseline boundary witness (tip, clean) | `rt.sh baseline-boundary receipt-artifacts-boundary-test` | `Ran 29 tests containing 259 assertions. 0 failures, 0 errors.` |
| xray + lane-manifest + census-pool + operation-algebra | `rt.sh baseline-others …` | `Ran 73 tests containing 2001 assertions. 0 failures, 0 errors.` |
| receipt booleans | per-ns `run-tests` | `tests=2 assertions=46 fail=0 error=0` |
| runtime rule / isolation | per-ns `run-tests` `ns-isolation-test` | `tests=27 assertions=167 fail=0 error=0` |
| refusal completeness | per-ns `run-tests` `mcp-write-refusal-test` | `tests=6 assertions=54 fail=0 error=0` |
| ceiling + refusal census (alias lane) | `rt.sh alias-migration mcp-alias-migration-test` | `Ran 158 tests containing 3468 assertions. 0 failures, 0 errors.` |
| scope-stream / hot-verify / mcp-tool | per-ns `run-tests` | `15/78`, `14/81`, `123/1359` — all `fail=0 error=0` |
| `make test-fast`, once | `make test-fast` | `Ran 1246 tests containing 12632 assertions. 0 failures, 0 errors.` · `test-isolation: 0 violations across 94 namespace(s)` · `battery-parallel: makespan 29267 ms … skipped 0` · `real 0m33.188s`, exit 0 |
| lint differential | `~/bin/clj-kondo --lint src` and on the 6 changed test files, base **and** tip | identical on both commits: src `errors: 2, warnings: 34`; changed test files `errors: 0, warnings: 4`. **Delta = 0.** |
| data move is behaviour-preserving | `namespace-runtimes` + lane sizes printed at base and tip, `diff` | `IDENTICAL runtime assignment + lane sizes` · `:measured 38` · `:lanes {:battery 53, :fast 94, :integration 7}` |
| assertion source counts per touched test file | `grep -c '(is ' base vs tip` | no file lost an assertion: `mcp_tool 536/536`, `hot_verify 70/70`, `census_pool 8/8`, `scope_stream 78/78`, `alias_migration 948/949`, `operation_algebra 65/66`, `lane_manifest 123/142`, `xray 197/204`, `boundary 52/86` |

**Report figure corroborated independently:** the report's alias lane "187 tests / 3,727 assertions"
decomposes exactly as `158 + 29 = 187` and `3,468 + 259 = 3,727` from my two separate runs. The report's
`make test-fast` figures (1,246 / 12,632 / 0 violations / skipped 0) reproduce exactly.

---

## Findings, ranked by severity

### F1 — MEDIUM. `DATACODE-ROWS-001` receipts are a string shape check; nothing ever opens a receipt.
`test/clj_surgeon/lane_manifest.clj:266-297` — `validate-runtime-evidence!`'s own docstring says
"*without reading files*". It requires `:logs` to be a non-empty vector of distinct non-blank strings of
the right cardinality, and it recomputes mean/sd/ratio from `:walls-ms`. It never checks that a cited
receipt exists or that its `:elapsed-ms` matches the row.

Measured (`/var/tmp/forge/datacode-fx/probe/p3.clj`):

```
A1 nonexistent-receipt-paths -> :ACCEPTED
A4 forged-consistent-row (walls 6ms -> 9000ms, runtime bb -> jvm, receipts UNCHANGED) -> :ACCEPTED  new-ratio= 58.372637553359816
```

This is load-bearing, not cosmetic: `test/clj_surgeon/lane_manifest.clj:359-363` reads
`(get-in runtime-measurements [n :runtime] …)` straight out of the row, so a forged row changes which
runtime a namespace actually runs on. The paired witness
`lane_manifest_test.clj:248-278` only exercises the cases the validator already catches, so it cannot see
this. **The shipped data is honest** — I cross-checked all 456 cited receipts:
`rows= 38 receipts-checked= 456 disagreements= 0`, every `:elapsed-ms`, `:namespace` and `:runtime`
matching. The defect is the missing ratchet, not the numbers.

**Fix:** in `validate-runtime-evidence!` (or a new witness), read each `:logs` entry and assert
`(= (:elapsed-ms receipt) (nth walls-ms i))`, `(= (:namespace receipt) n)`, `(= (:runtime receipt) runtime)`;
refuse `:missing-evidence-receipts` when the file is absent. Then plant a one-wall edit and watch it go red.

### F2 — MEDIUM. A soft witness: renaming `policy-envelope-roots` leaves the suite green.
`test/clj_surgeon/receipt_artifacts_boundary_test.clj:174` wraps the two `DATACODE-ENV-002` literal-root
assertions in `(when-let [policy (ns-resolve 'clj-surgeon.receipt-artifacts 'policy-envelope-roots)] …)`
with **no** `(is (some? policy))` guard — unlike its neighbours at lines 65-66 and 249-252, which do guard.

Plant: renamed `policy-envelope-roots` → `policy-envelope-roots-v2` (both the `defn` and its one call site
in `default-envelope`) with native form edits.

```
EXIT=0  Ran 29 tests containing 257 assertions.  0 failures, 0 errors.
```

Baseline is `29 tests containing 259 assertions`. The suite stays green while two assertions silently
evaporate; the only signal is an assertion count nobody compares. Those two assertions are the sole place
the policy roots (`/var/tmp` fallback, `$home/.local/state/clj-surgeon`, workspace) are pinned as literals.

**Fix:** add `(is (some? policy) "DATACODE-ENV-002: the policy-root function exists")`, matching the
`with-envelope` and evidence-row helpers in the same file.

### F3 — LOW-MEDIUM. A hard-linked final file is admitted; a write-in-place through it escapes the envelope.
`src/clj_surgeon/receipt_artifacts.clj:98-131` — `resolved-target` follows symlinks and normalises absent
tails; a hard link has no link target to follow, so `.toRealPath` returns the in-envelope path.

```
hardlink-final-file -> {:ADMITTED "/var/tmp/forge/datacode-fx/probe/p6/allowed/hard.edn"}
outside/kept.edn now = "OVERWRITTEN-VIA-HARDLINK"
```

Exposure is narrow: most consumers use `file-ops/atomic-write!` (temp + move), which *replaces* the link
rather than writing through it — I observed exactly that during the F1g plant, where the outside sentinel
survived. But `append-telemetry!` (`src/clj_surgeon/mcp_alias_migration.clj:2829-2840`) opens the ledger with
`FileChannel/APPEND` + `NOFOLLOW_LINKS`, which appends through a hard link.
`NOFOLLOW_LINKS` does not see hard links.

This is **not** a "filesystem race" and **not** a "writer outside the boundary", so it is not covered by the
report's disclaimer. The design doc's promise is precisely "missing parents and symlinks", which is what was
delivered; the intent text `DATACODE-ENV-001` ("a resolved destination outside the trusted envelope") reads
broader than that. **Fix:** either carve hard links out of `DATACODE-ENV-001` explicitly, or add a link-count
check (`Files/getAttribute path "unix:nlink"`) for final files on the append path.

### F4 — LOW. The consumer matrix is order-dependent on one shared symlink fixture.
`test/clj_surgeon/receipt_artifacts_boundary_test.clj:196-214` builds one `allowed/detail.edn` symlink and
drives four consumers through it in sequence. My single-line plant in the *first* consumer produced **four**
failures: `require-change-io/save!` replaced the symlink with a regular file (atomic move), so the remaining
three consumers then saw an in-envelope regular file and correctly did not refuse. Failures do not localise
to the defect. It cannot *mask* a defect (a later-consumer defect still fails alone), so this is a diagnostic-
quality finding only. **Fix:** recreate the symlink per consumer inside the `doseq`.

### F5 — LOW. The sleep ratchet forces shared helpers to be inlined, and the report does not mention the cost.
`sleep-pin-violations` refuses `:not-a-test-owner`, so a `Thread/sleep` may only live inside a `deftest` body.
That forced two shared top-level helpers into their single caller in this change:
`census_pool_test/eventually-dead?` (removed at `test/clj_surgeon/census_pool_test.clj:11-18`, inlined into
the deftest) and `scope_stream_test/await-cleared` (same pattern). Assertions are preserved verbatim, but a
helper shared by two tests can no longer hold a declared sleep without duplication. Worth a line in the design
note so the next author does not read the refusal as a bug.

### F6 — LOW (this is the claim-7 answer). Two real writers outside the covered boundary.
1. `src/clj_surgeon/mcp_telemetry.clj:70-78` — `start!` does `.mkdirs` on a caller-supplied `:directory`
   (default `default-directory`, line 17-20) and writes `<session-id>.jsonl` there. No `admit-target!`.
   It derives its default from the **`user.home` system property**, whereas the policy envelope's home root
   comes from **passwd** (`receipt_artifacts.clj:62-71`). A `-Duser.home` override — which the fast lane
   deliberately sets, per the `:fast` lane rule in `lane_manifest.clj` — moves the telemetry tree outside the
   envelope with no refusal. Both MCP launchers call `telemetry/start!` immediately *after*
   `initialize-envelope!` (`mcp_server.clj:345-348`, `mcp_http_server.clj:250-259`).
2. `src/clj_surgeon/mcp_http_server.clj:234-240` — `write-ready-file!` does `.mkdirs` on the parent of an
   arbitrary caller-supplied `:ready-file` and `spit`s into it. No `admit-target!`.

Also uncovered, same class, lower traffic: `src/clj_surgeon/mcp_recovery.clj:228-229`,
`src/clj_surgeon/telemetry_events.clj:340`.

The report's sentence is therefore **exact**: coverage is the shared artifact boundary and its inventoried
consumers, and these writers are correctly excluded rather than silently missed. Note that `workspace_lock.clj`
*is* covered — it publishes through `artifacts/target` (`workspace_lock.clj:51`), which admits.

Related, by design rather than defect: a launcher may supply `["/"]` —
`root-envelope-valid? true`. The envelope bounds *requests*, never the launcher. The claim is only about
missing launcher authority, so this is a limit to state, not a violation.

### F7 — LOW. Preregistration points a later scorer could reinterpret.
The document does fix task membership (six named namespaces + owners), order (T1–T6 / T3–T6,T1–T2 /
T5–T6,T1–T4, with per-parity arm orders), n (3 per cell, 36 cells, plus 6+6 native variance controls), both
arms' exact commands (both of which exist in-tree: `core.clj:2163`, `Makefile:371`), the wall definition
(T0 → accepted verdict) and falsification rules. Both bettors' rows are present. Reinterpretable:

1. **"A wall claim must clear twice that control standard deviation."** The control SD is in milliseconds;
   the primary statistic is a dimensionless median ratio. No conversion is stated — a scorer may band around
   1.0 or around the observed ratio and get different verdicts.
2. **"First-attempt success: Equal to native"** has no decision rule at n=3; the same document later says
   "n=3 cannot establish population equivalence" and asks only that a difference be *reported*. Both bet rows
   are therefore unscoreable as written.
3. **Fable's "Probe refusal rate ≥ native" is unfalsifiable.** The document states that "equal zero is
   compatible with Fable's greater-than-or-equal prediction" — `0 ≥ 0` always holds.
4. **The subject is "the final data-not-code branch commit", not a SHA**, and the six patches are described in
   prose, not frozen as bytes or hashes. The document *requires* freezing them before running, but the
   preregistration itself does not contain them, so per-task difficulty is not yet fixed.
5. **T0 and "verdict understood" are observer-recorded with no named instrument**, while only command wall gets
   a monotonic wrapper. Given the round-4 host already produced malformed `date +%s%3N` values, the primary
   measure's endpoints are the least instrumented part of the design.
6. **Both bettors agree on bb-portable (≤ 0.50)**, so only the JVM stratum (0.30 vs 0.45) can discriminate
   them; a bb-only result settles nothing between the two authors.

None of this contradicts the report's claim, which asserts presence, not precision.

### F8 — INFORMATIONAL. User-facing remedy text was traded for a byte ceiling.
`src/clj_surgeon/mcp_tool.clj:1760-1765` (`max-rendered-next-call-characters` = 1024) forced the X-ray
disallowed-symbol remedy to shrink from
*"Return a path, or end with analyze; add expect-count for exact cardinality. Do not execute X; quote it when
it is Clojure data, or use a terminating pure collection operation for computation."* to
*"Quote X as data; use pure functions with analyze for computation."* The report discloses this
("the remedy was shortened"), but not that the lost half is the part that told a caller *why* and offered the
"terminating pure collection operation" alternative. Worth a second look before this reaches callers.

---

## What I could not verify, and why

- **The report's absolute lint figure ("0 errors; 7 inherited warnings").** The tree has no `make lint`
  target and the retained receipts lint a copied tree at `/var/tmp/forge/datacode-fx/lint-baseline/…` with an
  invocation that is not recorded in a runnable form. My uncached `~/bin/clj-kondo --lint src` gives
  `errors: 2, warnings: 34` on **both** base and tip, so the *differential* is zero and nothing was silenced;
  the absolute figure is **UNVERIFIABLE** from the tree.
- **The bb arm of the sandbox classifier.** `xray-test` is a `:bb` namespace and is not in the
  `make test-fast` lane; I exercised it on the JVM only (28 tests / 507 assertions green, plus the JVM
  classification differential). The report's bb receipts (26 / 480) were not reproduced.
- **darwin behaviour** — `passwd-home`, `/var/tmp → /private/var/tmp` canonicalisation, `toRealPath` semantics.
  No Mac reachable; the report already names this as its least-sure area.
- **The prewarm receipt** (422,819 ms gate wall, source digest `56fc87b0…`, the 7-stage pass). I did not rerun
  the prewarm; I reproduced its component suites independently instead (see the corroboration note above).
- **Any probe-vs-native performance number.** None was run and none is claimed; item 5 is preregistration only
  and I deliberately did not execute an arm.
- **TOCTOU between admission and publication.** Explicitly out of scope per the design doc; not attempted.
- **Whether the `:probe` arm command actually works end to end.** `clj-surgeon :probe :ns` and `make warm`
  both exist in-tree, but the preregistration states no arm was ever run, so the PROBE arm's first-attempt-
  success denominator starts unvalidated.

## Recommended fixes before landing

1. **F1** — make `validate-runtime-evidence!` read each cited receipt and bind `:elapsed-ms`/`:namespace`/
   `:runtime` to the row; add a witness that forges a consistent row and watches it refuse.
2. **F2** — guard the `when-let` at `receipt_artifacts_boundary_test.clj:174` with `(is (some? policy) …)`.

Both are small and confined to test/validator code. Everything else in this review is a limit to state or a
note to the next author; nothing in the production path failed an adversarial plant.
