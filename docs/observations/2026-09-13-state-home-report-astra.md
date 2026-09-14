| Measurement | Result |
|---|---|
| Wall, first command → last code commit | 1039 s (2026-09-13T16:41:29Z → 2026-09-13T16:58:48Z; 1-second clock resolution) |
| Hand-carries | 8; all listed below |
| Refusals, typed / false | 2 Surgeon refusals / 0 false; also 7 intent-audit violations and 1 typed helper parser error |
| Observed writes outside envelope | 1 file outside task envelope; 0 observed outside runtime envelope; no exhaustive syscall trace |
| Tests red → green | state_home_warm.py: checkout dirty → clean; affected namespaces 110 tests / 1,630 assertions; final startup namespaces 30 / 869, all green |
| Required diff-impact | NOT RUN: launcher-path authorization pending |

Verdict: implementation committed; required verification incomplete. No push. Branch tip is the code tip. .gitignore unchanged.

Commits:

- `d17a8481f397c00b8d482a591634a190d0bfeda2 test: reproduce warm identity dirtying a fresh checkout`
- `d0ae186aadd42c8728df29aa30584bf6fc8cb831 fix: keep warm image identity in per-workspace seat state`

The affected run covers clj-surgeon.probe-state-test, mcp-http-server-test, mcp-hot-verify-test, help-test and splice-envelope-test. The final startup run repeats probe-state-test and mcp-http-server-test after the publication-failure cleanup change. Counts overlap and must not be summed. The final warm witness executes explicit state home, XDG fallback, home fallback and EACCES; successful modes run a real Babashka probe and compare its descriptor path to an independently computed SHA-256 path. Ports are selected in 19000–19999; children are terminated by exact PID. All launched test JVMs use -Xmx1024m, serial suites, with fixtures under statehome-fx.

Lint: ~/bin/clj-kondo, 0 errors / 0 warnings / 1 pre-existing informational redundant-str note. Linked-intent audit: {:ok true :violations []}. Raw logs: red.log, focused.log, focused2.log, focused3.log, focused4.log, focused5.log, affected.log, startup-final.log, lint-final.log, intent-audit.log and intent-final.log.

Hand-carries (broad operator definition, including my preparation mistakes):

1. No Surgeon MCP operations exposed in ALL_TOOLS. Used installed CLI; did not recover through forbidden shared ports.
2. :batch-form-selection-failed for nonexistent owner probe. Used returned cli! owner; next structural read succeeded.
3. EDN reader rejected source regex; ensuing request file was absent (:invalid-spec-source). Rebuilt the request using rewrite-clj, then executed the same guarded edit successfully.
4. Successful change! ignored the requested fixture receipt-out and reported a home-state receipt. Kept the committed mutation; used local rewrite-clj structural scripts for remaining changes. Did not delete the outside receipt or replay the change.
5. New test file malformed; formatter reported success; one structural recovery failed. Rebuilt the new test file as a complete parser-validated candidate. Retained original failed runs.
6. Real Babashka client could not import java.nio.file.AccessDeniedException. Classified native exceptions by class name without importing unavailable classes; reran the real client boundary.
7. Intent audit returned seven missing-witness violations for comma-separated IDs. Split each ID into its own structural comment node; audit passed.
8. Final mode-edit helper emitted :edamame/error after its mutation had already executed. Checked current mutation status (unwritable mode was present); did not replay the edit. Subsequent boundary tests passed.

Refusals and findings (exact unrepaired diagnostics retained):

- Registered second encounter only; no matched predecessor/native control was run and no reduced-carrying claim is supported.

- The original Make recipe wrote checkout/.clj-surgeon/probe.edn; the red witness observed ?? .clj-surgeon/ after exact-PID shutdown. This was inside the allowed fixture envelope but violated checkout cleanliness.

- One observed outside-task file: /home/forge/.local/state/clj-surgeon/artifacts/change-receipts/9b54e7cd589b6adad1232e84e6a13e2ba1c218dd3ff9a5124fb861da0c2/086378f3-f5f4-44a1-a9d7-e8eb823d3ada.edn. The successful Surgeon receipt explicitly reported this path despite :receipt-out pointing under statehome-fx. The runtime envelope admitted it; the user's narrower task envelope did not.

- Untyped preparation failure exact text: No dispatch macro for: ". Caused by using clojure.edn on Clojure regex source; replaced by rewrite-clj.

- Initial focused load: Syntax error reading source at (clj_surgeon/probe_state_test.clj:83:1). EOF while reading, starting at line 44. The second attempt also failed; both logs are retained.

- The single failed structural recovery's exact text: Unexpected EOF. [at line 83, column 2]. It wrote nothing; the malformed newly authored file was rebuilt through a parser-validated whole-file candidate.

- Formatter false negative: standard-clj reported success on the malformed newly authored test file. This is not counted as a false refusal.

- Live client failure exact text: Unable to resolve classname: java.nio.file.AccessDeniedException. JVM-only focused checks missed it; actual Babashka round-trip caught it.

- A helper load reported Unmatched delimiter: ) after a preceding top-level form had already written its change. Mutation status was inspected before continuing; it was not applied twice.

- Operator output waste retained in cost: several broad reads were truncated, and a mistaken cat of the native clj-kondo executable emitted binary output (tool reported 13,503,015 original tokens). This was a read, not an outside write or product refusal.

- No process-wide filesystem syscall trace was collected. Outside-write count is observed evidence, not an exhaustive all-writers attestation.

- Required diff-impact launcher hardcodes /home/forge/tmp/suite-1.lock, /var/tmp/forge/jvm-lease-datacode-*, and /var/tmp/forge/clj-surgeon-suite-* outside the allowed fixture root. An explicit ruling was requested; no response yet, so the command has not been run or silently adapted.

Typed refusal detail is in report.edn. Deliberately exercised product refusals are separate from operator-route refusals: :probe-image-absent, :stale-probe-image, :probe-state-not-writable/EACCES and :write-outside-envelope. No false refusal was observed. Formatter acceptance of malformed source was a false negative.

Least sure: no fixed-point impact result; no independent cost observer or all-writers trace; errno names are derived from native JVM exceptions, with unknowns explicitly unavailable. Fresh Maven resolution is not tested by the warm launcher adapter. No reduced-cost claim follows from this single incomplete encounter.

Owed: the exact required diff-impact command and every namespace it selects. Its hardcoded external lock/lease/temp paths need the pending user ruling. No bypass or adapted launcher has been run. This report will be updated after that gate.

## Round 2

Encounter `a40169948469d546f59ad8cd5c5623c01e8305ba3f9a5148d2b05fdd1cf091fb` continues. This section supersedes round 1's pending-verification verdict and owed authorization.

| Measurement | Round 2 | Encounter cumulative |
|---|---|---|
| Wall, first command → last code commit | 816.008 s (2026-09-13T17:20:28.992Z → 2026-09-13T17:34:05Z) | 1855.008 s active-round wall; inter-round gap excluded |
| Hand-carries | 5 (#9–13 below) | 13 |
| Refusals, typed / false | 0 Surgeon / 0 false; 1 untyped helper guard , 1 report-script parse error and 1 report-helper process error | 2 Surgeon / 0 false; prior 7 audit violations and typed helper parser error retained, plus this guard, report-script parse error and report-helper process error |
| Observed writes outside envelope | 0 unauthorized observed; oracle lock/lease/temp paths explicitly authorized | 1 prior outside-task file retained; no exhaustive syscall trace |
| Tests red → green | Seat's 54 selected / 53 green / 1 red → 54 selected / 54 green / 0 red | Full fixed-point gate now complete |

Committed `ed7e41caf28a8049b0813157f82c64a62ce1f972` on `fable/state-home-identity`. No push; no evidence commit. Both membership directions remain unchanged; only the frozen set and pinned count admit `probe-image-absent`. The existing registry entry now sits beside `probe-connection-failed`, whose missing-descriptor description is corrected.

Exact oracle: `bash test/diff-impact 53b9f158979eb1c71cd8afa1d5397f7254c6d55a /var/tmp/forge/statehome-fx/impact-r2 fixed-point`. **Selected 54; green 54; red 0.** Logs and per-namespace receipts: `impact-r2/`; launcher output: `impact-r2.log`. Its inventory records the pre-commit HEAD, but the tested Clojure bytes are those committed. During the run, the registry received a formatting-only change guarded by EDN value equality; the final audit and lint ran afterward.

The formerly red namespace passed 158 tests / 3,503 assertions. Lint through `~/bin/clj-kondo` on all eight changed Clojure/EDN files since the base: 0 errors, 0 warnings, 3 existing informational notes. Linked-intent audit: `{:ok true :violations []}`. Logs: `lint-final-r2.log`, `intent-final-r2.log`.

The oracle found mcp-alias-migration-test's frozen refusal pin: round 1 omitted the reverse require chain probe -> mcp-hot-verify -> mcp-change-buffer -> mcp-alias-migration-test; the test also reads reachable source forms to derive refusal kinds.

9. Seat's fixed-point oracle caught the dependent omitted from the round-1 hand-selected run. Pinned the deliberate refusal in that dependent witness and ran the full oracle.

10. Structural helper guard: Assert failed: Expected one form, got 3; (= 1 (count hits)). No writes had occurred; narrowed owner selection to def/deftest heads and repaired once successfully.

11. Formatter accepted adjacent EDN maps with excessive indentation. Inserted structural newline/space nodes and normalized the two state-home entries; asserted EDN value equality and reformatted.

12. Reporting script parse-only validation found Unmatched delimiter: ) at line 24, column 60. No script forms were executed; reconstructed the scratch script from its parsed forms, discarding the one invalid delimiter, then validated the full candidate.

13. Reporting helper passed a vector to process/shell, which tried to execute [git and failed with error 2. No report writes occurred; changed the helper to pass argv as separate arguments and reran.

First command time comes from this session's tool-call timestamp; last commit time comes from Git (one-second resolution). Cost is self-observed, not independently counted. Read/search misses and truncated output are included in wall; they are not typed product refusals. The historical outside receipt remains counted. No reduced-cost claim follows from this encounter.


## Round 3

Encounter `a40169948469d546f59ad8cd5c5623c01e8305ba3f9a5148d2b05fdd1cf091fb` continues from seat commit `606cb9c2ace038e50f51cb291b67692a9ef5c620`. Committed `a2d797c2223eb276c690b0a3a0d0c6f508b1950f` on `fable/state-home-identity`. No push or evidence commit.

**Recurrence = true for the envelope-writer class, caught by the gate, not by the build round.** The round-1 brief supplied the hardcoded root, but the older repository TMPDIR policy governed the fixture. The fix's own witness repeated the encounter's class. This is a recurrence, not a new unrelated defect.

| Measurement | Round 3 | Encounter cumulative |
|---|---|---|
| Wall, first tool call → code commit | 2149.675 s (2026-09-13T18:16:04.325Z → 2026-09-13T18:51:54Z) | 4004.683 s active-round wall; inter-round gaps excluded |
| Hand-carries | 3 (#14–16 below; includes the intervening seat registration) | 16 |
| Refusals, typed / false | 0 Surgeon / 0 false; 3 known gate refusals including the seat's namespace census, the supplied EACCES gate result, and this run's TEST-ISO-007 budget refusal | Prior 2 Surgeon / 0 false and prior helper/audit failures retained; these 3 gate refusals added |
| Recurrence | **true — envelope-writer class; caught by the gate, not by the build round** | This recurrence is retained |
| Observed writes outside envelope | 0 new successful unauthorized writes observed; the incoming gate denied the warm fixture outside its envelope | 1 prior outside-task receipt retained; denied attempts are not successful writes |
| Tests red → green | Incoming gate: warm witness red in all four modes → wrapper: 6 tests / 28 assertions, 0 failures/errors | Required fixed-point run remains complete: 54 selected / 54 green / 0 red |
| Class oracle cost | One prewarm: 965.1190664651804 s, exit 2 (budget); unreached-stage supplement: 249.85124962124974 s, exit 0 | 1214.9703160864301 s wrapper wall added; no speedup claim |
| Outside-session cost | Incoming EACCES prewarm approximately 3 min per brief; seat registration/control wall not fully metered here | Excluded from active-round total rather than invented |

14. The seat manually registered the previously omitted test namespace, produced bb-load, bb-test and JVM-test portability controls, and regenerated the census in 606cb9c2. The JVM control passed 6/28 in 57.8 s; the bb-hosted child could not load clojure.main. This was additional encounter work between rounds, not work performed by this driver.

15. The gate caught the repeated hardcoded fixture writer at `/var/tmp/forge/statehome-fx/warm-jtonkc60` (representative exact denied path supplied in the brief; all four modes failed). Replaced the hardcoded base with `tempfile.TemporaryDirectory(prefix='warm-')`, resolved the result, and asserted containment under a configured TMPDIR while printing both paths. The rest of the Python fixture writes below that directory. In probe_state_test.clj, real writes already inherit java.io.tmpdir; `/seat`, `/xdg`, `/house`, `/checkout/a`, `/checkout/b`, and `/another-checkout` are calculation/stale-descriptor inputs, not writers. No Clojure edit was needed.

16. The class run reached a previously masked namespace-budget refusal. All probe-state-test assertions passed, but its 126088 ms runtime exceeded the existing 20000 ms per-namespace budget. The gate drained its runtime pool and refused before three trailing stages. Ran those exact unreached stages under the unchanged wrapper to complete the surface coverage. No budget, membership, skip, or gate predicate was changed; no second prewarm was run.

### Class oracle receipts and denied paths

Exact prewarm invocation (one invocation):

```sh
taskset -c 0 python3 docs/observations/2026-09-12-bbtower-block-b/attempt18/restricted-gate.py -- make landing-gate-prewarm
```

CPU affinity makes the unchanged gate derive 1 worker lane. The unchanged wrapper supplies `-Xmx1g` (1024 MiB) and a packet-shaped TMPDIR; repository workers retain their own tighter 512 MiB limits, and warm-image children use `-Xmx1024m`. No independent test JVM workload overlapped either wrapper run or diff-impact; the existing warm witness necessarily launches its child from its test-host JVM.

The three writable roots were the checkout, `/var/tmp/forge/bbtower-fx/packets/a58c256b-0194-406a-a172-d809a55fa511/tmp`, and `/home/forge/.local/state/clj-surgeon`; the only extra writable file was `/dev/null` (WRITE_FILE only). The wrapper was not modified. Summary line, verbatim:

```json
{"exit": 2, "wall_seconds": 965.1190664651804}
```

The sole recorded isolation violation was `TEST-ISO-007`, `clj-surgeon.probe-state-test`, `time budget`, 126088 ms > 20000 ms. Alias passed 192 tests / 3820 assertions; MCP passed 1437 / 17264 behavior assertions with 0 test failures/errors but **1 budget isolation failure**; bb passed 903 / 8061 with 0 test failures/errors. The prewarm is **not green** and has no successful landing/prewarm claim.

Unreached-stage coverage supplement:

```sh
taskset -c 0 python3 docs/observations/2026-09-12-bbtower-block-b/attempt18/restricted-gate.py -- make -k test-bb-diagnostic repository-hygiene intent-audit
```

Its packet TMPDIR was `/var/tmp/forge/bbtower-fx/packets/34f49fa1-e2aa-4eba-b513-5160c0a74bac/tmp`; the other roots and /dev/null allowance were identical. All three stages passed; the diagnostic ran 834 tests / 7360 assertions, 0 failures/errors. Summary line, verbatim:

```json
{"exit": 0, "wall_seconds": 249.85124962124974}
```

**Unexpected denied paths after the fix: `[]`.** The complete retained output also contains one deliberately denied path (twice because console and lane output duplicate it):

```text
/var/tmp/forge/bbtower-fx/packets/a58c256b-0194-406a-a172-d809a55fa511/tmp/clj-surgeon-tmpleak-witness.NKk0Ld/nowrite/clj-surgeon-suite-1329946-168aaa3a
```

This is the existing tmp-leak ratchet's `chmod 500` / `run_probe unwritable` negative control (`test/tmp_leak_ratchet_test.sh:350–353`), inside TMPDIR, reporting AccessDeniedException as intended. It is preserved, not repaired into a writable directory or silently dropped. The state-home unwritable mode likewise intentionally proves EACCES beneath its own state root. The denied-path scan covers retained console and raw lane stdout/stderr; it is not an exhaustive syscall trace.

Evidence: `envelope-r3.log`, `envelope-r3-run/` (including suite and per-lane receipts), `envelope-unreached-r3.log`, and `denial-scan-r3.json`. Both completed wrapper packet roots were removed after evidence retention; `cleanup-r3.json` records exact owned paths and timestamps.

### Required verification and intent

Exact command, exit 0:

```sh
bash test/diff-impact 53b9f158979eb1c71cd8afa1d5397f7254c6d55a /var/tmp/forge/statehome-fx/impact-r3 fixed-point
```

**54 selected; 54 green; 0 red.** Logs and per-namespace receipts: `impact-r3/`, launcher output `impact-r3.log`. They cover the Python bytes committed above, with the pre-commit HEAD in the inventory. No source changed during verification.

Paved `~/bin/clj-kondo` on all 21 changed Clojure/EDN files since the base: 0 errors, 0 warnings, 3 existing informational notes (`lint-r3.log`, `lint-files-r3.txt`). Python AST parsing and `git diff --check` passed. `make intent-audit`: `{:ok true :violations []}` (`intent-r3.log`); the same audit also passed under the wrapper.

Linked-intent review: existing state-home design → STATE-HOME-001/007/008 → the existing four-mode warm witness and containment assertion → TMPDIR-derived fixture allocation. The older repository temp policy and the user's explicit round-3 repair govern this fixture change; no new product promise or leaf-segment crossing. Read the working-tree Surgeon skill and `/home/forge/src/claude-skills-nrepl/linked-intent-testing/SKILL.md`; used the native route, no Surgeon calls. The brief names TEST-ISO-007 with the TMPDIR policy; in this checkout TEST-ISO-007 specifically defines time budgets, while CLAUDE.md states the temp-root policy. Both findings are reported under their actual contracts.

**Which oracle found the defect: the gate. Which should have: the wrapper, had round 1 run it.**

Remaining limitation: a green prewarm is still blocked by the existing 20-second namespace budget for the real four-startup witness (already 57.8 s in the seat's unconstrained JVM control). This round does not relax that budget or claim a green prewarm. All requested unexpected envelope-writer repairs and fixed-point/lint/audit checks are complete. Cost is session-log/self-observed, not independently judged; no reduced-carrying or performance claim follows. Read/search misses and truncated diagnostics are included in wall, not classified as product refusals. The pre-existing untracked observation directory was left untouched.


## Round 4

Encounter `a40169948469d546f59ad8cd5c5623c01e8305ba3f9a5148d2b05fdd1cf091fb` continues from `73f8e38d` on `fable/state-home-identity`. Red committed as `f72895ec44c5ac29f92241ed2c2b2df4bc901fdc`; fix committed as `4d06221bbed71e4db06ae299642e492e046dda3f`. No push. Only `test/clj_surgeon/mission_fallback_test.clj` changed this round. The pre-existing untracked observation directory is untouched.

**Oracle that found it: the battery. Round-3 fixed-point runs namespaces one per process; the battery packs a lane into one JVM, carrying earlier failed-append history into mission-fallback.** The round-3 prewarm stopped on the probe-state namespace budget; its trailing-stage supplement did not exercise this battery lane. The lane reshuffle exposed an existing contract-equality defect; state-home runtime code did not create it.

| Measurement | Round 4 | Encounter cumulative |
|---|---|---|
| Wall, earliest captured clock → fix commit | 1318 s (2026-09-13T19:15:00Z → 2026-09-13T19:36:58Z); lower bound, initial instruction/report reads precede the first captured clock | At least 5322.683 s active-round wall; inter-round gaps excluded |
| Model returns | Not independently metered; no inferred count | Unknown; no reduced-carrying claim |
| Hand-carries | 1 (#17 below) | 17 under the earlier report's broad operator definition |
| Refusals, typed / false | 0 Surgeon / 0 false; expected red assertion is not a route refusal | Prior refusal/helper/audit history retained |
| Observed writes outside envelope | 0 new unauthorized writes observed; no all-writers syscall trace | 1 historical outside-task receipt retained |
| Red witness | 1 test / 7 assertions: 6 pass, 1 fail, 0 errors; 5.23 s | Committed red before normalization fix |
| Ordered JVM gate | 21 tests / 166 assertions; 0 failures/errors; 465.69 s | Shared-process regression covered |
| Fixed-point gate | 54 selected / 54 green / 0 red; 687.60 s | Complete required impact set green |
| Lint / intent audit | 0 errors / 0 warnings, 0.03 s; audit ok / violations [], 2.16 s | Green over final changed bytes |
| Measured test/check subprocess wall | 1160.71 s (red + ordered + fixed-point + lint + audit); overlapping wall is not summed into active wall | No performance claim |

The first clock was printed by a tool, and the end is Git's commit timestamp (one-second resolution). The explicit `r4-start.txt` was written later at 19:15:21Z and is not substituted for the earlier observed clock. Initial read latency is unmetered; this round and its cumulative wall are lower bounds. Read/search misses, truncated diagnostics and polling remain in elapsed wall, not product refusal counts. No independent cost observer was used.

17. The witness previously redefined `default-events-file`, which an inherited `CLJ_SURGEON_EVENTS_FILE` overrides. Bound the public resolved `events-file` seam directly to its owned JVM ledger so the test works under the fixed-point harness and cannot redirect this fallback append to the inherited ledger. No runtime path policy changed.

### Red, fix, and schema classification

The existing named `fallback-bb-and-jvm-share-the-event-contract` regression now substitutes `events/dropped` with `(atom 16)` before `cli/fallback!`. `with-redefs` restores the original atom and its existing count even on an exception. It asserts the actual JVM event carries 16 drops and the fresh bb event omits the counter. Distinct JVM/bb seat values additionally witness process-context independence. Red log shows the contract-equality assertion failed while all six other assertions passed. This is not a zero-reset fixture fix: the equality is exercised with positive carried history on every run.

`stable-report` now excludes `:telemetry_dropped` and `:seat` in addition to `:ts :pid :wall_ms`. No production counter, ledger behavior, lane metadata or budget changed.

| Event keys | Meaning and comparison policy |
|---|---|
| input `:dropped` → output `:telemetry_dropped` | Prior failed appends in this process; exclude from cross-runtime contract equality. `:dropped` itself is not emitted. |
| `:pid`, `:seat` | Process identity/context; seat derives from SURGEON_SEAT, USER, or unknown. Exclude. |
| `:ts`, `:wall_ms` | Invocation clock/timing, already excluded. |
| `:kind :tool :ok :error_type :mission_id` | Event contract; retain. |
| Admitted `:mission_state :mission_verb :fallback_kind :report_basis :fallback_reason :executor :cost_source :provider :model :upstream :refused_rung :candidate_count` | Mission/route facts; retain. |
| `:prompt_tokens :completion_tokens :reasoning_tokens :cost_usd` | Event measurements, not process-global counters; retain, including unknown nil values. |
| `:error_type_truncated`, renderer-added `:over_limit` | Content/serialization diagnostics, not process history; retain. `:over_limit` belongs to rendered JSONL rather than record!'s returned line-map. |

### Class oracle, once

Ran the two requested searches once, before editing, plus a supplemental producer/projection search. One affected member: mission-fallback's cross-runtime stable-report equality. The other whole event equality at its original line 96 compares the returned event to the JSONL of the **same invocation**; it intentionally keeps exact equality, including process fields. Telemetry drop matches at telemetry_events_test.clj:150/152/163 assert the counter contract directly. Other event matches compare scalar event names or mission transition literals. Related mission event suites stub record! to capture raw event inputs, before process metadata is attached. No additional affected member found. Full classification and search commands: `class-oracle-r4.md`.

### Verification and intent

Exact requested order, one JVM, exit 0 (`ordered-r4.log`):

```sh
clojure -J-Xmx1024m -M:clj-surgeon/test-deps -e "(require 'clojure.test 'clj-surgeon.reader-eval-fence-test 'clj-surgeon.mission-fallback-test) (let [r (clojure.test/run-tests 'clj-surgeon.reader-eval-fence-test 'clj-surgeon.mission-fallback-test)] (prn r) (shutdown-agents) (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))"
```

TMPDIR and java.io.tmpdir for the red and ordered runs were `/var/tmp/forge/statehome-fx/r4-tmp`; the empty directory was removed after completion. Cold red and ordered executions are milestone gates; nREPL discovery found no server in this worktree and no other worktree's server was used.

Exact fixed-point command, exit 0 (`impact-r4.log`, `impact-r4/`):

```sh
bash test/diff-impact 53b9f158979eb1c71cd8afa1d5397f7254c6d55a /var/tmp/forge/statehome-fx/impact-r4 fixed-point
```

All 54 per-namespace receipts have exit 0, including mission-fallback. Inventory HEAD is the red commit; the verified working-tree test bytes include the fix and are identical to the final commit. No source changed during either runtime gate. The formatter ran during the ordered gate but made no additional byte change. The unchanged launcher owned and removed its lease and suite temp root. Test JVM gates were sequential and bounded by the requested 1024 MiB setting; the fixed-point launcher supplies its existing environment policy.

`standard-clj fix` passed (`format-r4.log`); paved `~/bin/clj-kondo --lint test/clj_surgeon/mission_fallback_test.clj` passed (`lint-r4.log`); `make intent-audit` returned ok with no violations (`intent-r4.log`); `git diff --check` passed.

Intent review: HLD function-boundary event evidence → telemetry-events design → TELEMETRY-EVENTS-001 → existing append/count witnesses → record!. This repair only corrects cross-runtime test projection and adds the witnessed process-history precondition. No new product promise, scoped LID source change, or leaf-segment crossing. The user's explicit red/fix/verify sequence authorizes this repair. Working-tree Surgeon skill read; native route used; no Surgeon operations or delegated agents.

All requested Round 4 deliverables complete. No full battery rerun was requested or claimed; the supplied failing order and deterministic positive-history witness are green, as is the complete fixed-point gate. Report appended at 2026-09-13T19:38:08.827802+00:00.


## Round 5

Status: COMPLETE — all requested verification is green at `df0c9e1c5f21e5a80027cf35ae68d63e1625f403`. Starting branch `fable/state-home-identity`, tip `4d06221b`; Sol's `sol-verdict-1.md` was read verbatim before repository inspection. Red oracles committed as `d1050101`. No push. The pre-existing untracked observations directory is untouched. Raw logs and this report stay external.

**recurrence = 2**, in Sol's words: “CLASS: SECOND RECURRENCE — envelope writers whose configured state root resolves into the checkout or widens the pre-existing destination envelope.” The round-3 Landlock scan cannot detect this member because the checkout is deliberately writable. The Cartesian Make state-root matrix can.

| Class | Oracle that found it | Oracle that now enumerates it |
|---|---|---|
| Envelope writer / configured state root | Sol's adversarial resolver/writer probe: direct and symlink roots recreated `?? workspaces/` | `probe-state-test/configured-root-cartesian-matrix`: 3 sources × 4 path shapes × 2 envelopes × 3 destinations = 72 real Make cells in fresh committed copies; exact refusal kind/canonical path, absent refused descriptor, clean Git status. Additional redirected-descendant/override witness. :battery |
| Descriptor publication failure | Sol's child `ulimit -f 1` probe destroyed the prior descriptor and left 1,024 bytes | `publication-fault-matrix`: create/write/sync/publish × EFBIG/ENOSPC/EDQUOT/EACCES/EINTR/unknown, using one function-var seam; `native-efbig-preserves-descriptor` uses a real child limit; `short-write-cannot-publish` silently returns partial bytes. Every refusal preserves prior bytes and leaves no temporary file. :battery |
| Pre-network filesystem failure | Sol's long-path, malformed EDN and EACCES probes: uncaught exception or transport misclassification | `local-filesystem-failures-never-connect` and `invalid-path-receipt-is-bounded-at-the-crossing`: resolution, absent, EACCES, directory, malformed, oversize, path-too-long/NUL and identity-source reads; counted transport seam stays zero, with a positive control proving it reaches one. Bounded EDN diagnostics include actual paths or explicitly marked prefixes. :battery |
| Generated population drift | Sol compared the Markdown 159 header with 160 controls entries | `lane-manifest-test/generated-portability-census-agrees-with-all-inventories`: Markdown header/table/summary, controls map, runtime/lane inventory, exact derived deftest census. Generator consumes `portability-summary/population-line`. :fast |

No new test namespace was introduced. The additions reuse `clj-surgeon.probe-state-test` (:battery/:jvm) and `clj-surgeon.lane-manifest-test` (:fast/:bb), which already have all five registrations. Runtime population stays 160; the regenerated deftest census adds eight names and removes zero (2,617 total). The affected packed battery namespace order is `[clj-surgeon.probe-state-test]`; the permanent class witnesses run together in that JVM, with intentional child processes at the Make and EFBIG boundaries.

The frozen refusal enumeration changes deliberately **169 → 176**: `probe-image-malformed`, `probe-image-path-invalid`, `probe-image-too-large`, `probe-image-unreadable`, `state-root-inside-workspace`, `state-root-outside-envelope`, and the previously existing `probe-state-not-writable` now included through the explicit shared state namespace dependency. Membership was observed red at 176 before updating the pin.

Red evidence: `red-unit-r5.log` reproduces the partial overwrite, wrong errno, local read classifications/escaping boundary, and 159/160/160 header mismatch. `red-matrix-r5.log` reproduces checkout writes and incorrect narrow-envelope refusal kinds; this initial run was stopped after class failures were demonstrated, not claimed as a complete red matrix. `red-supplement-r5.log` and `red-green-identity-r5.log` run added witnesses against the original implementations extracted from committed `d1050101`, then restore current implementations.

The first full candidate matrix (`green-matrix-r5.log`, despite its provisional filename) was RED: 43/48 cells passed, the dangling-home-symlink positive failed on telemetry startup, and four empty-home expectations failed. The complete oracle found these before completion. Empty home now explicitly denotes the current directory and is refused inside the checkout; materializing the already admitted canonical state root before startup fixes the dangling-link case. Their focused green receipts are `matrix-symlink-green-r5.log` and `matrix-empty-green-r5.log`. Matrix coverage was then expanded to 72 cells to directly refuse widening of the default envelope as well as a narrow envelope; the direct default-envelope refusal is retained in `matrix-default-envelope-r5.log`.

Cost meter starts at the recorded `2026-09-13T20:04:21Z` (`r5-meter-start.txt`); initial verdict/instruction/source reads precede that clock and remain unmetered. Final cost table and verification receipts follow below.

Progress entry written at 2026-09-13T20:23:22.223630+00:00.

The first packed receipt (`packed-r5.edn`) has 13 tests / 201 assertions, no test failures/errors, and an explicit TEST-ISO-007 violation: 481,025 ms versus the old 300,000 ms namespace budget. Its zero child exit was NOT accepted as a green gate: the child intentionally leaves violation folding to its coordinator. TEST-ISO-007 permits a reasoned namespace override. Commit `112c960d` declares 1,000,000 ms for this measured cold-start oracle; the battery lane ceiling stays 1,800,000 ms. The subsequent in-flight run was interrupted and retained as `packed-final-r5.*` (215,944 ms; intentional exit-130 matrix failure) so the final packed run could load the new allowance. `packed-verified-r5.*` is the final run; its wrapper explicitly rejects any violation or leak before starting fixed-point verification.

The budget-contract witnesses pass 27 tests / 167 assertions (`budget-tests-r5.log`). The assigned Babashka lane-manifest namespace passes 34 tests / 1,902 assertions in 1,711 ms (`census-bb-r5.log`), including exact derived deftest membership. Lint is 0 errors / 0 warnings, with two pre-existing informational notes in the alias-migration test file. Intent audit is ok with no violations.

Documented manual repairs/extra-run episodes (self-counted; the earlier broad counter ended at 17):

18. Fixed an EOF delimiter error in the new filesystem witness.
19. Fixed the new census witness's binding-vector delimiter.
20. Repaired an incorrectly quoted nREPL var expression.
21. Separated the matrix process log from the server log and included startup output on a failed positive cell; the initial diagnostic hid the telemetry cause.
22. Made empty-home selection explicit after the matrix exposed the Java empty-parent behavior and the oracle's prior assumption.
23. Expanded the matrix after noticing default-envelope widening needed its own destination control; a previously started 48-cell packed run therefore remained preliminary.
24. Inspected the packed receipt beyond exit status, found the old per-namespace budget, registered its measured replacement through the documented contract, and restarted the in-flight run.
25. Reworked filesystem refusal construction and the shared-state dependency after enumeration exposed missing literal branch kinds; all seven additions are now pinned.

These are at least nine documented carrying episodes this round, 26 on the prior report's cumulative counter (including item 26 below). Product corrections found by the class oracles, read/search misses and truncated diagnostic output also consume wall; they are not invented model-return counts.

The final packed receipt is green: `packed-verified-r5.edn` records 13 tests / 201 assertions, 657,856 ms namespace wall, `:violations []` and `:leak-fail 0`. Its wrapper validates those fields explicitly. This run executes the complete 72-cell matrix at `112c960d`; the subsequent source change is solely the forwarding comment below, with no runtime expression change. The final fixed-point run also executes the complete probe-state namespace in one JVM.

26. The first fixed-point run exposed `no-reachable-namespace-spells-a-refusal-kind-dynamically`: the `(:error-type data)` reader-error relay lacked its required forwarding marker. The 176-kind membership pin passed. Commit `df0c9e1c` adds the marker at the actual lookup; the two refusal guards then pass 2 tests / 5 assertions (`forwarding-green-r5.log`). Lint and intent audit pass again. The known-red gate was stopped after 36 completed namespaces (35 green, one red), and its owned process group exited before cleanup and the repair.

First-impact evidence relocation: the original `/var/tmp/forge/statehome-fx/impact-r5` directory is retained byte-for-byte as `/var/tmp/forge/statehome-fx/impact-r5-first`; raw rows retain their original path prefix, so resolve that prefix to the retained directory when reading this failed attempt. Its launcher log is `impact-launch-first-r5.log`. The complete final rerun uses the user-requested `impact-r5` path. No old run file was overwritten.

Round-5 branch commits: `d1050101` (committed red oracles), `be6c27e6` (four class fixes; deliberate 169 → 176 pin), `112c960d` (declared namespace cost), `df0c9e1c` (verified refusal-forwarding marker). No raw control receipts, execution logs or this report were committed. The existing generator and generated census header were updated as requested.

Final verification:

- Exact requested command: `bash test/diff-impact 53b9f158979eb1c71cd8afa1d5397f7254c6d55a /var/tmp/forge/statehome-fx/impact-r5 fixed-point` — exit 0, **55/55 namespaces**, **1,321 tests / 23,177 assertions**, zero failures/errors. Inventory records final HEAD `df0c9e1c5f21e5a80027cf35ae68d63e1625f403`.
- The final fixed-point probe-state result is **13 tests / 201 assertions**, zero failures/errors; its child wall is 658,362 ms. The 72-cell matrix completes again on the final commit.
- Packed namespace order: `[clj-surgeon.probe-state-test]`, one JVM. `packed-verified-r5.edn`: 13 tests / 201 assertions, 657,856 ms namespace wall, no isolation violations, no leaks.
- Assigned bb census namespace: 34 tests / 1,902 assertions, zero failures/errors, 1,711 ms. Budget-contract namespace: 27 tests / 167 assertions, zero failures/errors. These overlap other verification and are not added to the fixed-point totals.
- Paved `~/bin/clj-kondo`: zero errors and warnings; two existing informational notes in the alias-migration test. Standard formatter, Python AST parse, and `git diff --check` pass. Final intent audit: `{:ok true :violations []}`; no new intent debt or exclusions.
- Final Git status contains only the pre-existing untracked `docs/observations/2026-09-13-state-home/` directory. No push.

| Measurement | Round 5 | Encounter cumulative / interpretation |
|---|---|---|
| Envelope-writer recurrence | **recurrence = 2 — “SECOND RECURRENCE”**, in Sol's words | The configured-root member repeats the envelope-writer class; Landlock cannot see it because the checkout is deliberately writable. The Cartesian Make oracle can. |
| Meter → report completion | At least **4,294 s** (2026-09-13T20:04:21+00:00 → 2026-09-13T21:15:55+00:00) | At least 9,616.683 s active-round wall including prior rounds; inter-round gaps excluded. Initial Round-5 reads precede the meter. |
| Meter → final gate completion | At least 4,133 s | Complete requested verification, not just code-write time. |
| Meter → last code commit | At least 2,826 s | Final code commit `df0c9e1c`; later wall is verification/reporting. |
| Model returns | Not independently metered | Unknown; no inferred count or reduced-carrying claim. |
| Documented hand-carries | At least 9, items 18–26 above | 26 on the earlier report's broad cumulative counter. |
| Route refusals / false refusals | 0 Surgeon / 0 false; no Surgeon route was invoked | Expected oracle refusals are test outcomes. One actual namespace-budget violation and one forwarding-guard failure were retained and repaired. |
| New unauthorized writes observed | 0 observed; no all-writers syscall trace | The historical outside-task receipt remains counted. Red checkout writes occurred only in owned adversarial fixtures. |
| Final packed check | 13 tests / 201 assertions; 661 s launcher wall, 657.856 s namespace wall | One JVM; 72 Make cells; no violations or leaks. Namespace allowance 1,000 s; battery lane ceiling remains 1,800 s. |
| Final fixed-point check | 55/55 green; 1,321 tests / 23,177 assertions; 1,290 s launcher wall | Sum of namespace child walls: 1289.849 s. |
| Retained incomplete/negative work | Original red oracles; five failures in the first 48-cell candidate matrix; 481.025 s packed budget violation; 215.944 s deliberately interrupted packed run; first impact attempt 35 green / 1 red before interruption | All retained, never relabeled green. No aggregate check-wall sum is invented from unmetered or overlapping runs. |
| Lint / intent / census | Zero lint errors/warnings; audit ok; 160 runtime assignments; 2,617 deftests | Eight deftest names added, zero removed; no new test namespace or missing registration. |

This is correctness and coverage evidence, with no native-control speed claim. The four class/oracle rows above name both the finding oracle and the permanent enumerating oracle. Round 5 is complete; report finalized at 2026-09-13T21:15:55+00:00.


## Round 6

### Explanation first — oracle finding (filed; not fixed here)

At df0c9e1c the fixed-point inventory selected 55 namespaces and omitted `clj-surgeon.splice-envelope-test`; its 55/55 green result is true for that incomplete selection. The gate ran this witness in MCP lane 2 and found three failed assertions covering six missing registrations. This is selection incompleteness, not a test that passed alone because of a different process environment.

Exact missing dependency edges (reader → read path): `clj-surgeon.splice-envelope-test/probe-vocabulary` → `src/clj_surgeon/probe.clj`, `src/clj_surgeon/probe_state.clj`, `src/clj_surgeon/mcp_hot_verify.clj`, and `src/clj_surgeon/mcp_http_server.clj`; and `bounded-input-path-encoding` → `docs/intent/probe/refusals.edn`. `probe-owners` supplies source filenames and `slurp` reads their forms as data. The witness has no require edge to those owners. `test/diff_impact.clj/dependencies` reads only ns :require/:require-macros/:use declarations, `impact` closes only those reverse edges, and changed seeds come only from `git diff --name-only BASE -- src`. Thus the changed probe/probe-state/server sources cannot reach this witness through their path-read edges; an EDN-only registration change is also outside the seed model. A fixed point closes the modeled graph, not omitted filesystem dependencies. Oracle defect: undeclared source-as-data/resource-read impact edges, plus non-source resource seeds excluded. Filed here for the oracle owner; no oracle code or artificial require edge will be added in this round.

Evidence: `impact-r5/impact-fixed-point.json` records df0c9e1c and 55 entries with no splice-envelope-test; `results-fixed-point.edn` has no receipt for it. The supplied check log lines 213–239 run the witness and report the missing vocabulary.

Confirmed: the `probe-fixture.check-test` and `sol-round5.probe-test` stale-check FAIL output is expected inside green `mcp-hot-verify-test`, whose supplied gate lane 16 exits 0.

Oracle that found it: the gate. Oracle that should have selected and found it: fixed-point impact, with the filesystem-read edges above.

The frozen 176-kind enumeration and the probe vocabulary are two independent ledgers. The vocabulary/remedy registry read at splice_envelope_test.clj:73 is itself `docs/intent/probe/refusals.edn`, not a third separate table. Six kinds are absent there; probe-state-not-writable already has a row. Implementation follows this explanation.

Explanation recorded at 2026-09-13T21:22:45.775613+00:00.

### Registration class checklist — what the census refusal should print

1. Emitting source owner and every literal/forwarded kind: `rg -n 'probe-image-|state-root-' src/` is retained in `registration-src-r6.txt`; supplemental search includes `probe-state-not-writable`. Four new image kinds originate in probe.clj; both state-root kinds and publication failure originate in probe_state.clj. Forwarded lookups remain explicitly marked.
2. Frozen enumeration: `test/clj_surgeon/mcp_alias_migration_test.clj/frozen-refusal-kinds`, exact count 176, and both directions in `the-refusal-enumeration-is-pinned-in-count-and-in-membership`. All seven were already present; unchanged. The dynamic-spelling/forwarding witness remains required.
3. Vocabulary/remedy registry: `docs/intent/probe/refusals.edn/:refusals`, with the union of `:owner-refusals` for shared non-probe owners. This is the SAME EDN ledger the splice witness reads, not another independent registration. Add complete rows for all six missing kinds, with native failure, concrete repair in `native_method`, promise, class, owner, reproducer, check, witness and retirement condition. Existing probe-state-not-writable receives explicit permissions/space/quota repair text. Do not add these probe kinds to the shared-owner exception sets or relax either equality.
4. Help/repair reference and owning intent: all seven rows appear in the state-home specs' new repair table, linked to the EDN ledger and existing STATE-HOME/PROBE-RECEIPT intent. The CLI's core :probe help entry describes invocation and response semantics; it has no separate refusal-kind dispatch table. `probe/refusal` is a generic constructor with no kind registry. We do not invent or claim an extra runtime ledger. The existing hot-verification specs already name the four descriptor kinds; state-home specs already name both root kinds and publication failure.
5. Behavioral witnesses: image failures → `local-filesystem-failures-never-connect` and bounded-path witness; state-root failures → `configured-root-cartesian-matrix`; publication → native-permission and publication fault witnesses. These existing tests remain in probe-state-test. Refusal census witnesses must be selected even when they read owner source or EDN by path.

Machine-checked matrix: `check-registrations-r6.log` records all seven kinds present in the 176-kind frozen set, exactly one registry row per kind with nonempty failure/repair/check/witness fields, and a row in the intent repair table. `registration-surfaces-r6.txt` lists all matching source, tests, resources and intent files. No new test namespace, metadata lane or portability registration was introduced.

The six additions were a FORM edit: rewrite-clj selected the :refusals vector and appended six complete map forms; the helper asserted the final EDN value equals the previous registry plus precisely those rows. Formatting later preserved EDN value equality. Existing registry schema and both directions of vocabulary equality are unchanged. Only the registry and its owning intent reference are repository changes; source code and the frozen pin are unchanged.

### Verification

Same gate lane membership and order, using the gate's `mcp-test-runner` child entrance and `--emit-edn` (which supplies the same isolation/metadata/budget measurements):

```sh
TMPDIR=/var/tmp/forge/statehome-fx JAVA_TOOL_OPTIONS='-Xmx1024m -Djava.io.tmpdir=/var/tmp/forge/statehome-fx' clojure -J-Xms64m -J-Xmx1024m -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --emit-edn /var/tmp/forge/statehome-fx/lane2-r6.edn --ns clj-surgeon.ns-isolation-test clj-surgeon.splice-envelope-test clj-surgeon.mcp-change-buffer-test clj-surgeon.mcp-prepared-confirmation-test clj-surgeon.mcp-formatter-test
```

`lane2-r6.edn`: 83 tests / 651 assertions, fail 0 / error 0, leak-fail 0 and every run's violations empty; 12.02 s launcher wall. The receipt's violations and leak fields were explicitly checked, not inferred from the child exit. Splice witness: 2 tests / 107 assertions, 853 ms. Home isolation is true. This replays the gate's lane, not the whole landing coordinator; no whole-prewarm PASS is claimed. Final registry formatting after that run changes no EDN value (guarded equality).

`standard-clj fix` and `git diff --check` pass. Final paved clj-kondo: 0 errors / 0 warnings. Final intent audit: ok true / violations []. No new behavior or scoped LID code edit; the repair table documents existing approved intent and witnesses.

Exact fixed-point rerun in progress:

```sh
bash test/diff-impact 53b9f158979eb1c71cd8afa1d5397f7254c6d55a /var/tmp/forge/statehome-fx/impact-r6 fixed-point
```

The fixed-point oracle is unchanged, so its selection still omits splice-envelope-test. The explicit lane replay supplies that missing proof.

Cost notes retained: the first formatter left appended map forms on one long line; a formatting helper mistakenly used z/edit (sexpr) with a node function and refused with `unsupported operation` before any write. Repaired once to z/edit*, asserted EDN equality, then formatted successfully. Initial appended rows also named `admitted-state-root!`; corrected to the actual `admit-state-root!` before testing. Native search misses and truncated reads consume wall, not product refusals. No Surgeon operation, external agent, push or new runtime service was invoked.

### Final result and Round-6 cost table

**COMPLETE.** Commit `097a7708ef9accb61d7ac192daf1b8ac80d9b22c` on `fable/state-home-identity`. No push. The two committed files are `docs/intent/probe/refusals.edn` and `docs/intent/state-home/state-home-specs.md`; the pre-existing untracked observations directory remains untouched.

The exact fixed-point command exited 0: **55/55 green**, **1,321 tests / 23,177 assertions**, no failures/errors. Launcher wall: 1,286.76 s; sum of child walls: 1286.462 s. Probe-state passed 13 tests / 201 assertions with 658.552 s child wall, including the full 72-cell matrix. `impact-r6/` retains inventory and every namespace log/receipt; `impact-r6.log` and `impact-r6.seconds` retain launcher output and timing. Inventory HEAD is df0c9e1c because verification preceded this commit; the final working-tree registry and docs were present throughout fixed-point verification. Verified file hashes are in `verified-files-r6.sha256`.

The gate's lane 2 replay is independently green (83 tests / 651 assertions; every violation vector empty, leak-fail zero). Its splice namespace is absent from the unchanged fixed-point set: do not claim 55/55 proves that missing witness. The two runs overlap and their test totals are not summed.

| Measurement | Round 6 | Encounter cumulative / interpretation |
|---|---|---|
| Meter → report completion | At least 1563.720 s (2026-09-13T21:21:56+00:00 → 2026-09-13T21:47:59.719854+00:00) | At least 11180.403 s active-round wall; inter-round gaps excluded; initial instruction/report reads precede this round's meter |
| Meter → commit | 1522.000 s | Commit follows all requested verification; Git timestamps have one-second resolution |
| Model returns | Unknown; no independent observer | No inferred count or reduced-carrying claim |
| Documented hand-carries | 3: incoming gate's missing second-ledger registration (#27), correction of the new rows' owner-function spelling before tests (#28), formatting-helper operation repair (#29) | 29 on the prior broad cumulative counter; one gate-ledger omission, one preparation correction, one helper repair |
| Refusals / false refusals | 0 Surgeon / 0 false; no Surgeon route invoked; one untyped formatting-helper error before write | Incoming gate: three failed assertions for six missing rows. Expected fixture stale-check failures remain green parent-test evidence |
| New unauthorized writes observed | 0 observed; no all-writers trace | Prior outside-task receipt remains counted; existing authorized launcher lock/lease/temp paths used unchanged |
| Same gate lane 2 | 83 tests / 651 assertions; 12.02 s launcher wall; no violations or leaks | Splice witness 2 / 107, 853 ms; red incoming gate → green same lane |
| Fixed-point rerun | 55/55 green; 1,321 tests / 23,177 assertions; 1,286.76 s launcher wall | Still omits the path-reading splice witness; no oracle repair or coverage-completeness claim |
| Final lint / intent audit | 0 errors / 0 warnings, 0.00 s at timer resolution; audit ok / violations [], 2.16 s | Earlier lint/audit also green; rerun after formatting to cover final bytes |
| Registration census | All seven in frozen 176-kind set, vocabulary EDN rows, and intent repair table | Vocabulary/remedy registry and probe/refusals.edn are the same ledger; CLI has no separate kind table |

**Found by the gate; should have been found by fixed-point impact through the missing source-as-data and registry-path read edges filed at the start of Round 6.** The oracle finding is filed here as requested, not fixed in this commit. No performance improvement claim follows from this round.


## Round 7

Incoming oracle: packet 68bacdec, `make test-battery` at 097a7708.

- **Teardown race — found by the battery.** One of 72 cells (`user.home`, symlink, narrow, inside) failed during deletion of `.git/objects`, after its assertions. Earlier successful matrix/fixed-point runs did not force the scheduling window between signalling the warm PID and the final descendant exit; waiting for Make alone was not a process-tree lifetime proof. The permanent boundary helper now owns a new session, signals the exact warm PID, waits for Make, polls `kill -0` on its process group with a bound, and reaps adopted descendants on Linux. A bounded forced-stop path repeats the wait. Cleanup retries once after 250 ms, records the original OSError in `cleanup_retry`, and propagates a second failure. A real delayed-descendant regression and injected first/second cleanup failures exercise this class; the original failing cell remains in the five-cell boundary set.
- **Total cost — found by the battery.** The incoming serial-equivalent 1,851,865 ms exceeds TEST-ISO-007's 1,800,000 ms budget by 51,865 ms; makespan 710 s and probe-state alone 709,979 ms. Earlier fixed-point selection and the packed namespace run could prove behavior and its declared 1,000,000 ms exception, but neither sums the complete battery's namespace costs. Raising that exception cannot repair this gate. The full battery run below is the cost authority.

The admission class remains 3 sources × 4 shapes × 2 envelopes × 3 destinations = 72 cells. `state-home-admission-test/configured-root-cartesian-matrix` (:fast, :bb) calls the existing selection, canonicalization and admission functions, with real temporary directories and dangling symlinks and no subprocess. Relative roots are interpreted from the fixture checkout, equivalent to the Make child's cwd; default authority is the fixture runtime subtree and narrow authority is runtime/allowed. Every cell asserts its exact refusal kind (or admission) and canonical destination; the cardinality/uniqueness assertion gives 145 assertions.

The :battery helper runs FIVE real fresh committed Make copies: user.home/symlink/narrow/inside; explicit/relative/default/outside-envelope; and admitted explicit/direct, XDG/symlink, user.home/relative external destinations. It asserts descriptor/refusal behavior and clean Git status in each, prints cleanup retry facts, and fails above 120 s total. `warm-leaves-fresh-checkout-clean` and its four-mode Python witness are unchanged.

Five registrations for the new namespace: namespace :fast metadata; lane manifest entry; runtime assignment with actual bb-load/bb-test/JVM-test controls and generated portability inventory; corpus membership pin; deliberately regenerated named deftest census. The census changes move configured-root-cartesian-matrix to the fast namespace and add configured-root-warm-cells, net +1 test. The runtime population pin is 161. Both refusal registry witness paths now point to the fast enumerating oracle.

Candidate commit: `2c6df947`, on fable/state-home-identity; no push. The reduced packed control is 116,659 ms, 13 tests / 201 assertions, no failures/errors/isolation violations/leaks. The measured-baseline allowance is now 240,000 ms (~2×); the total battery budget remains 1,800,000 ms.

Preparation negatives retained: the first fast control exposed Java relativize returning an empty string for the same path (repaired to literal `.` to match the original Python matrix); first registration audit exposed the 160→161 population pin; one malformed report-helper delimiter after its intended write; one runner option refusal (`--results-file`, corrected to `--emit-edn`); one synthetic descendant's quoted Python expression was corrected before the permanent regression passed. None is reported as a product regression or green verification.

Verification results and cost table follow after the required gates finish.

Round 7 entry opened at 2026-09-13T22:18:51.676565+00:00.

### Complete battery gate

Executed `/var/tmp/forge/statehome-fx/r7-lease-run make test-battery MCP_JAVA_OPTS='-J-Xms64m -J-Xmx1024m'`: one exclusive JVM-suite lease, default eight internal lanes, no overlapping suite. Exit **0**. Subject `2c6df947d71e02d14ef71c37012575c99317adc6`. Complete stdout/stderr: `battery-r7.log`; frozen lane outputs/EDN: `battery-parallel-r7/`.

The command's literal last two lines (this Make target emits battery-ledger, not a landing-gate line):

```text
battery-parallel: makespan 198150 ms over 8 lane(s); serial-equivalent 1282675 ms; skipped 0
battery-ledger: appended {:sha "2c6df947d71e02d14ef71c37012575c99317adc6", :started "2026-09-13T22:18:09Z", :wall_s 200, :verdict :pass, :host "anvil-server", :lanes 8, :skipped 0}
```

Suite result:

```text
Ran 1191 tests containing 18928 assertions.
0 failures, 0 errors.
test-isolation: 0 violations across 54 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

Total margin: **517,325 ms (28.74%)** below 1,800,000 ms. This is the complete battery's observed sum, not a namespace-budget argument. The five Make cells took **71,384 ms**; all five row statuses are empty and all five cleanup_retry values are null. Their teardown-regression line passes. Probe-state namespace: **139,833 ms**, 13 tests / 201 assertions, no violations; its allowance is 240,000 ms. The unchanged four-mode witness is included in that namespace measurement.

### Final verification and cost

- Exact requested `bash test/diff-impact 53b9f158979eb1c71cd8afa1d5397f7254c6d55a /var/tmp/forge/statehome-fx/impact-r7 fixed-point`: exit **0**, **56/56 namespaces**, **1,322 tests / 23,328 assertions**, zero failures/errors. The new fast admission namespace is selected. `impact-r7/results-fixed-point.edn` sums to **753,974 ms** child wall. This does not claim to repair the path-reading impact blind spot filed in Round 6.
- Fixed-point probe-state: **117,969 ms** child wall; its five warm cells are **56,630 ms**, all clean and no cleanup retry. These are additional behavior receipts, not the complete battery-cost authority.
- Final admission/isolation runner: **28 tests / 312 assertions**, zero failures/errors/violations/leaks. New fast matrix **349 ms** in the runner; ns-isolation budget contracts **1,362 ms**. Retained portability controls: bb **90 ms**, JVM **328 ms** including load/test; each is 1 test / 145 assertions. No new namespace remains unregistered.
- Paved `~/bin/clj-kondo`: zero errors/warnings, **0.14 s** launcher wall. `make intent-audit`: `{:ok true :violations []}`, **2.15 s**. Standard Clojure formatter, Python AST parse and `git diff --check` pass. Receipt: `summary-r7.edn`; final implementation/document/control hashes: `verified-files-r7.sha256`.
- Final branch tip **6e77580fa1c98c9c7ae2a57861b113a3327334ae** (`6e77580f`) records only the generated battery ledger and measured namespace-wall receipts after the verified implementation commit `2c6df947`. The battery and fixed-point subjects are explicitly `2c6df947`; no code/test/intent bytes changed afterward. No push. Git status contains only the pre-existing untracked `docs/observations/2026-09-13-state-home/` directory. Owned runtime temp roots and the suite lease have been removed; retained report evidence remains under statehome-fx.

| Measurement | Round 7 | Encounter cumulative / interpretation |
|---|---|---|
| Meter → report completion | At least **1596.855 s** (2026-09-13T22:10:54+00:00 → 2026-09-13T22:37:30.854728+00:00) | At least **12777.258 s** active-round wall including prior 11,180.403 s; initial Round-7 reads precede the meter; inter-round gaps excluded |
| Model returns | Unknown; no independent observer | No inferred count or agent-speed claim |
| Documented hand-carries | Two incoming battery findings; five preparation corrections listed above | These are documented facts, not an independently measured complete action count |
| Refusals / false refusals | 0 Surgeon / 0 Surgeon false; no Surgeon route invoked | One runner option refusal retained; expected root refusals are oracle outcomes |
| Unauthorized writes | 0 newly observed; no all-writers trace | Earlier encounter finding remains counted; writable Git copies are owned adversarial fixtures |
| 72-cell admission class | **145 assertions; 90 ms bb control / 328 ms JVM control / 349 ms final isolated runner** | No JVM child inside this test; fixture canonicalization includes dangling links and absent destinations |
| Five real Make cells | **71,384 ms in the battery**, **56,630 ms in fixed-point** | 5/5 clean Git statuses; zero cleanup retries in both runs; hard pass condition <120,000 ms |
| Probe-state namespace | **116,659 ms packed control; 139,833 ms battery** | 13 tests / 201 assertions; current allowance **240,000 ms**, replacing 1,000,000 ms; four-mode witness unchanged |
| Complete battery | **1,282,675 ms serial-equivalent; 198,150 ms makespan; 200 s Make launcher wall** | **517,325 ms / 28.74% margin** below 1,800,000 ms; 1,191 tests / 18,928 assertions; zero failures/errors/skips/isolation violations |
| Fixed-point | **56/56; 1,322 tests / 23,328 assertions; 753,974 ms summed child wall** | Green affected-set behavior; cannot substitute for the battery total-cost gate |
| Final fast/budget check | **28 tests / 312 assertions; 1,711 ms namespace wall** | No isolation violations/leaks; overlaps other verification, so totals are not added |
| Registration census | **161 runtime assignments; 2,618 named deftests; 34 tests / 1,908 assertions** | All five registrations checked; no dropped class or artificial dependency edge |
| Lint / intent audit | **0 errors / warnings; audit ok / violations []; 0.14 s / 2.15 s** | Final implementation bytes; no new intent debt or exclusions |

The **battery** found both the teardown race and the total-cost violation. Earlier green process runs could have hit the race by chance but could not prove descendant quiescence; earlier packed/fixed-point checks could not establish the complete battery's serial-equivalent budget. The deterministic teardown regression and split admission/boundary oracles preserve those distinctions. This is coverage and cost-gate evidence, not a native-control speed claim.

Round 7 finalized at 2026-09-13T22:37:30.854728+00:00.


## Round 8

Sol verdict read verbatim first: sol-verdict-2.md, NO-GO, SH-ROUND2-01.
Incoming branch fable/state-home-identity at 6e77580f; pre-existing untracked
docs/observations/2026-09-13-state-home/ left untouched.

Red oracle commit **db23914d** extends four real publication stages × six
registered errno cases × before/after timing (48 cells). The nREPL receipt
`oracle-red-r8.log` has **1 test / 235 pass / 6 fail / 0 errors**, with a distinct
.probe-* file for each after-create errno. Each cell has its own temporary
directory, so one failure cannot contaminate the next. Production code was
unchanged in that commit.

Fix commit **7bf44cfe** removes return-based creation tracking and attempts
deleteIfExists on failure. Cleanup errors supplement the original typed refusal
with `:cleanup-failure {:path temporary-path :errno ... :native-class ...
:native-message ...}`. The original descriptor path, errno and kind are retained.
No new refusal kind; the existing vocabulary/remedy row and intent repair table
now describe cleanup recovery. The frozen kind enumeration is unchanged.

The final oracle adds 72 cells: primary errno × cleanup errno × before/after
real removal. All 120 cells pass **648 assertions** (`oracle-green-r8.log`).
Successful cleanup requires zero residue; failed removal must name the residue,
and fixture teardown removes it. Prior descriptor bytes survive every precommit
failure. A real atomic move is the commit boundary: injecting a throw AFTER
`:publish` leaves the new descriptor already published. This boundary is explicit
in tests and intent; no racy restoration protocol was added. This interpretation
was presented as an optional clarification; no reply arrived, so the stated
atomic-commit interpretation was used.

**Why round 5 missed it:** its seam threw instead of running create, so it never
produced an existing temporary file followed by a create-stage exception.

Verification uses the actual manifest at this tip: state-home-admission-test is
:fast; probe-state-test is :battery and occupied lane 7 alone in the retained
Round-7 packing. The current helper has five Make cells plus the teardown
regression, and the unchanged four-mode warm witness; the whole namespace is
replayed without dropping any of them. No lane reclassification or new namespace.

Preparation cost: one malformed nREPL var literal refused before evaluation;
corrected to `(var ...)`. Native discovery included a missing tests.edn and
test/lane_manifest.edn; those were search misses, not product refusals. No
Surgeon route or sub-agent was used. Model-return count remains unknown without
an independent observer. Full requested verification and cost table follow.

Round 8 entry opened at 2026-09-13T23:05:39.826987+00:00.

### Verified candidate gates

Subject **7bf44cfe**, file hashes frozen in `verified-files-r8.sha256`.
Fast admission: **1 test / 145 assertions**, zero failures/errors, violations [],
leak-fail 0, 304 ms namespace wall; 3.51 s launcher wall (`fast-r8.edn`).
Packed probe-state: **13 tests / 752 assertions**, zero failures/errors,
violations [], leak-fail 0; **138,911 ms** namespace wall (`packed-r8.edn`).
All 13 expected test vars executed. Configured-root Make cells: **58,516 ms**,
5/5 clean statuses and zero cleanup retries; teardown regression PASS.
The four-mode warm witness and native EFBIG witness also passed.
This is the actual retained lane-7 namespace packing, not a full-battery cost
claim. Final fixed-point and explicit path-reading results follow below.

### Final verification and Round-8 cost table

**COMPLETE**, branch `fable/state-home-identity`, tip **7bf44cfe3e9df3903d573fe9cac20e961ea35902**.
Red test commit `db23914d` preceded the implementation commit `7bf44cfe`.
No push. No code, test or intent bytes changed after the implementation commit;
all seven final hashes match. Git status contains only the pre-existing untracked
observations directory. The owned nREPL exited; its empty temp root and the empty
control temp root were removed. No suite lease remains from this round.

The exact requested command exited **0**:

```text
bash test/diff-impact 53b9f158979eb1c71cd8afa1d5397f7254c6d55a /var/tmp/forge/statehome-fx/impact-r8 fixed-point
```

**56/56 namespaces, 1,322 tests / 23,879 assertions**, zero failures/errors.
Summed child wall **899,147 ms**; launcher **925.81 s**, including
the short wait behind the packed-run suite lock. Fixed-point probe-state:
**13 tests / 752 assertions**, **136,896 ms**. Its full before/after matrix and
all registered warm witnesses passed again. This is affected-set verification,
not a new complete-battery budget claim. `impact-r8/` retains inventory and
per-namespace logs/receipts; `summary-r8.edn` records the aggregate.

Explicit `splice-envelope-test` and `receipt-artifacts-boundary-test` replay:
**36 tests / 424 assertions**, zero failures/errors/isolation violations/leaks;
**4,249 ms** namespace wall, **12.51 s** launcher wall.
The first namespace checks the second refusal ledger through file content.
The fixed-point blind spot is not treated as repaired; these are independent
requested checks. Overlapping test totals are not added together.

Final paved `~/bin/clj-kondo`: **0 errors / 0 warnings**, **0.04 s**
launcher wall. Final `make intent-audit`: **{:ok true :violations []}**,
**2.16 s**. Standard Clojure formatting and
`git diff --check` pass. No new refusal kind or frozen-enumeration change.

| Measurement | Round 8 | Encounter cumulative / interpretation |
|---|---|---|
| Meter → final report | At least **1339.701 s** (2026-09-13T23:01:37+00:00 → 2026-09-13T23:23:56.701387+00:00) | At least **14116.959 s** active-round wall; initial reads precede meter, inter-round gaps excluded |
| Model returns / monetary cost | Unknown; no independent observer or billing meter | No inferred action count, dollar cost or speed claim |
| Documented hand-carries | One incoming gate finding; one nREPL literal repair; native path-search misses retained above | Not an independently measured complete action count |
| Refusals / false refusals | 0 Surgeon / 0 Surgeon false; no Surgeon route invoked | Expected injected native failures are oracle outcomes; one nREPL reader refusal before evaluation |
| Unauthorized writes | 0 newly observed; no all-writers trace | Existing untracked observations untouched; owned fixture/lease/temp paths only observed |
| Red oracle | 48 cells; **235 pass / 6 fail / 0 errors** | All six after-create cells reproduce residue at db23914d |
| Final fault oracle | **120 cells / 648 assertions**, green | 48 forward-stage cells + 72 primary/cleanup errno and removal-timing cells |
| Fast admission namespace | **1 test / 145 assertions; 304 ms** namespace, **3.51 s** launcher | Existing 72-cell admission class preserved |
| Packed probe-state | **13 tests / 752 assertions; 138,911 ms** namespace, **142.14 s** launcher | Same retained lane-7 packing; below unchanged 240,000 ms allowance |
| Configured-root Make cells | **5/5 green; 58,516 ms**, plus teardown regression PASS | Actual branch has five configured-root rows; unchanged four-mode warm witness also passed |
| Fixed-point | **56/56; 1,322 tests / 23,879 assertions; 899,147 ms** summed child wall | **925.81 s** launcher includes queued lock wait; zero nonzero exits |
| Explicit boundary replay | **36 tests / 424 assertions; 4,249 ms** namespace wall | **12.51 s** launcher; no violations or leaks |
| Lint / intent audit | **0 errors/warnings; ok/violations []; 0.04 s / 2.16 s** | Covers final committed bytes |

**Round-5 blind spot:** its seam threw instead of running create, so no cell
could leave a real temporary file and then report a create-stage failure.

Round 8 finalized at 2026-09-13T23:23:56.701387+00:00.
