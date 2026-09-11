# bb-rewrite-tower block A — Astra report

Branch: bb-rewrite-tower-local. Base eae1e43280635be9b4e317e176d29476fd358702,
the commit behind annotated stable/2026-09-11.2. Final tip
978a0c48 (full hash recorded at close below). No push; stable tag unchanged.

**Not landing-ready.** Implementation and assertion witnesses are present,
but the unchanged TEST-ISO-007 fast-lane time budget failed. No test was removed,
runtime misclassified for speed, or budget relaxed to obtain a green result.
The one prewarm attempt ran on e9caa663 and exposed a refusal-census defect.
That defect is repaired on the final tip; another final-tip prewarm is OWED
under the one-attempt limit, rather than silently re-running the gate.

## Commits

| Commit | Contents |
|---|---|
| dbcff6a0 | Initial inventory and block contract; the regex metadata discovery defect in that draft is explicitly corrected by the final inventory |
| 54b790cf | bb coordinator/worker routing, runtime manifest, warm MCP probe, CLI/help, intent and existing-group witnesses |
| e9caa663 | Corrected runtime inventory, measured inner/coordinator walls, usage and tech-tree evidence |
| 978a0c48 | Repair the source-complete refusal census: explicit probe dependency, forwarding markers, seven literal probe kinds pinned; focused RED-to-GREEN evidence |

## Inventory and implementation

Final source inventory: **104 bb-portable, 20 JVM-only** (124 total).
Final test inventory: **107 bb-portable, 52 JVM-only** (159 total).
All JVM-only namespaces and their unavailable library/class are listed in
docs/plans/bb-first-inventory.md. Fresh bb loads use babashka v1.13.219 with
src, test, libs/clj-splice/src and dev/experiments. Support/runner files under
test are not test namespaces. The original bb lane remains 50 namespaces.

The initial regex misread metadata before ns names; parser-based discovery
corrected it before the runtime manifest was populated. Executing the hybrid
then overturned two successful-load classifications: ns-isolation-test needs
JVM Var.getRawRoot reflection, and mcp-namespace-split-test dynamically reaches
nREPL during a test. Both are now JVM-only. Battery cadence remains separate
from load eligibility; no battery is promoted into the inner loop just because
it loads under bb.

test-fast now uses a bb coordinator and the union of the previous bb/fast
inventories (109 namespaces). Workers split by runtime and home-isolation
requirement. Portable bb workers use 1 GiB and explicit startup tmpdir; JVM-only
workers retain the existing JVM runner. Existing kernel slot admission remains
intact. The explicit permission for Make entrances was used for their existing
helper, after correcting an initially overbroad reading of “no Python.” No
Python or Go implementation was added.

`make warm PORT=9107` started the one owned MCP JVM with -J-Xmx1g, nREPL disabled,
and a loopback /probe endpoint. The bb client checks a root/generation/server-
classpath fingerprint descriptor; the server checks it again before serial
local dependency reload and namespace test execution in mcp-hot-verify.
Every probe verdict retains verification_complete false and pending landing-gate.
The existing MCP inspect endpoint returned read_complete=true on this same image.

A JVM-only probe was also executed: insert-forms-receipt-test passed 2 tests /
68 assertions after reloading 57 local namespaces, in 2,383.263903 server ms,
with verification_complete false. The ordinary MCP inspect endpoint passed
again after that reload. See probe-jvm-only.edn and mcp-inspect.log.

After the final refusal-census repair, a fresh owned image again passed this
JVM-only probe: 58 namespaces reloaded, 2 tests / 68 assertions, 2,428.894374 ms,
verification_complete false. The ordinary MCP inspect endpoint also passed
afterward. See probe-jvm-final.edn and mcp-inspect.log.

## Meter

Same forms-test namespace, 24 tests / 94 assertions, n=5 each. Each sample is a
reversible native comment edit; date +%s.%N runs after patch completion and after
verdict/process exit. This excludes authoring/patch time and is a reload-cost
meter, not semantic-edit performance admission. All 15 exits were zero.

| Run | Cold JVM s | Cold bb s | Warm probe s |
|---|---:|---:|---:|
| 1 | 3.135338176 | 0.045910513 | 0.359213035 |
| 2 | 3.103918246 | 0.052564329 | 0.348543651 |
| 3 | 3.071775631 | 0.047694287 | 0.355682673 |
| 4 | 3.140376179 | 0.044548521 | 0.352632251 |
| 5 | 3.185531059 | 0.042671989 | 0.353943146 |
| Median | 3.135338176 | 0.045910513 | 0.353943146 |

The portable cold bb run is fastest. Retained raw clocks and commands:
meter.tsv, meter.sh, meter-*.log, plus docs/plans/edit-to-probe.md.

Complete Make/coordinator before: **35.050757448 s**, 755 tests / 7,931 assertions,
61 namespaces, plus the 5-test/59-assertion library prerequisite. After:
**86.983589821 s**, 1,631 tests / 15,788 assertions, 109 namespaces, plus the same
library. Coverage differs because the new entrance includes the old bb lane;
this is not a matched speed claim. The after run's assertions all pass, but its
68,210 ms summed fast-lane test time exceeds the unchanged 60,000 ms cap.
A 512 MiB candidate took 161.427512594 s and failed the cap at 70,937 ms.

Live negative: a deliberately wrong forms-test expectation produced probe-failed,
1 failure and exit 1 (86.376872 server ms); restoring it produced probe-passed,
0 failures (89.375258 ms). A changed fingerprint refused at the client; a forged
generation reached the server and refused as stale-probe-image (0.757867 ms).
All reversible specimen edits are restored.

## Gates

| Gate | Result |
|---|---|
| bb library | PASS, 5 tests / 59 assertions, projection hash eb7f6099906935cd68d29b6a7c99b96a4e129fee2ef9b4640f4b70e76260b622 |
| Twenty verb witness groups | PASS in hybrid run: 20 tests / 420 assertions across insert-forms, insert-forms-receipt, rename-alias, rename-alias-receipt and splice-envelope |
| New make test-bb | PASS, 898 tests / 7,981 assertions; test-bb.log |
| New make test-fast | Assertions PASS, 1,631 tests / 15,788 assertions; gate FAILED solely on TEST-ISO-007 time budget; fast-1g.log |
| Named portable-load failure | WITNESSED: bb-portable-load-failed named mcp-schema-test when dev/experiments was missing from worker classpath; repaired classpath without deleting the namespace |
| Changed source lint | PASS, 0 errors / 0 warnings through ~/bin/clj-kondo; lint-changed.log |
| Broad src/test lint | At e9caa663: 29 errors / 66 warnings, byte-identical diagnostics to eae1e432 including fixture diagnostics; only timing differs. Final repair files separately lint at 0/0. |
| Census diff | PASS: 1,690 derived = 1,690 ledger, diff nil; no census file edit; census.edn |
| landing-gate-prewarm | One attempt on e9caa663 FAILED: alias 181 tests / 3,580 assertions with 3 refusal-census failures; MCP 923 tests / 11,736 assertions all passing but time budgets failed (fast 65,434 > 60,000 ms; integration 758,486 > 240,000 ms); bb 898 / 7,981 passed. The feature-thread bb group alone took 727,579 ms. prewarm.log |
| Refusal-census repair | PASS on final source: 5 focused witnesses / 1,149 assertions; alias-contract-static.log. New probe constructor is statically reachable and all seven literal kinds are included in the 160-kind census. |
| Final pure probe contract | PASS, 1 existing witness / 19 assertions; probe-pure-final.edn |

## Every source edit — dogfood mechanisms

Seven successful insert_forms transactions inserted thirteen top-level forms
and three test-body forms. Requests and receipts are insert-*.edn and
insert-*.edn.receipt; dogfood-receipts.edn indexes them. No existing alias was
renamed, so rename_alias had no applicable call. New require entries are not
alias renames. All native replacements below are structures for which neither
insert_forms nor rename_alias fits; no edit_clojure MCP entrance was available.

| Source file | Intent / edits | Mechanism |
|---|---|---|
| src/clj_surgeon/probe.clj | Bootstrap new ns; nine contract/client forms; HTTP client and lint fixes | apply_patch for new-file bootstrap (insert_forms cannot create files), then insert_forms request-file; native replacement for client fixes |
| src/clj_surgeon/mcp_hot_verify.clj | Local reload ordering and serial probe execution; repair constructor reachability and forwarding | insert_forms request-file, two top-level forms; native patch for explicit require, qualified call and comments |
| src/clj_surgeon/mcp_http_server.clj | Probe servlet, conditional route and descriptor publication | insert_forms request-file for servlet; apply_patch for existing function bindings/body |
| src/clj_surgeon/core.clj | Probe registry entry, shorthand dispatch, failed-verdict exit | apply_patch; map entries and replacements, not top-level form insertion |
| test/clj_surgeon/lane_manifest.clj | Per-namespace runtime map; two execution-grounded reclassifications | insert_forms request-file for map; generated native apply_patch for replacements |
| test/clj_surgeon/battery_parallel_runner.clj | Hybrid union, runtime/home worker partition, runtime receipt, 1 GiB bb child | apply_patch; existing-function replacements |
| test/run_all.clj | Runtime admission, named load refusal, home isolation and 1 GiB reexec | apply_patch; existing ns/body replacements |
| test/clj_surgeon/runner_membership.clj | Recognize bb coordinator Make recipes | apply_patch; regex replacements |
| test/clj_surgeon/help_test.clj | Pure probe identity/verdict witnesses and op completeness | insert_forms body request-file; apply_patch for expected registry set |
| test/clj_surgeon/lane_manifest_test.clj | Runtime witness, hybrid membership expectation, shifted sleep pin | insert_forms body request-file; apply_patch for existing expectations and pin; one delimiter correction during editing |
| test/clj_surgeon/mcp_hot_verify_test.clj | Dependency order and absent namespace witness | insert_forms body request-file |
| test/clj_surgeon/mcp_alias_migration_test.clj | Pin seven newly reachable literal probe refusal kinds and 160-kind count | apply_patch; existing set and assertion replacement, no test/form addition |
| test/clj_surgeon/forms_test.clj | Meter comments (17 patches), red/fixed assertion (2 patches), all restored | generated native apply_patch; replacements, no form insertion or alias change |
| Makefile | bb coordinator entrances and warm target | apply_patch; non-Clojure build rules |
| bb.edn | Add worker experiment classpath root | apply_patch; configuration map replacement |
| Scratch inventory.clj and summarize.clj | Isolated load discovery, parser repair, corrected inventory/runtime payload | native preparation; inventory data is not repo source mutation |
| Scratch insert.clj | Guarded request-file caller, receipt/error capture, worktree fallback, body-anchor support | native preparation |
| Scratch replace.clj | Exact-one-line patch generator for replacements | native preparation; executes apply_patch |
| Scratch *-payload.clj and *-test-body.clj | Insertion request payloads and delimiter repair | native payload preparation; publication through insert_forms |
| Scratch meter.sh | Timestamped edit-to-verdict orchestration | native shell preparation; executes apply_patch |
| Scratch mcp-smoke.clj | Real MCP initialization/inspect and schema repair | native scratch preparation |
| Scratch alias-contract-check.clj | Focused final repair verification using five existing witness Vars | native scratch preparation |

All scratch files are under /var/tmp/forge/bbtower-fx. Markdown inventory,
plan, requirements, README, HLD and tech-tree edits used apply_patch or the
recorded bb report generator; no Clojure editing verb applies to prose.

## Refusals and repair quality

| Refusal / failure | Mutation status and repair |
|---|---|
| Installed ~/bin/clj-surgeon startup: missing clj_splice/core | Three startup attempts, no operation receipt. The first harness lost stderr; the next identified the missing classpath. One process-local preload repair was ineffective. Switched to the worktree bb -m clj-surgeon.core request-file entrance. No installed/shared file was edited. Subsequent guarded insertion began from the bootstrap hash. No typed remedy existed. |
| insert_forms :payload-parse-error | mutation_attempted=false, source_unchanged=true; missing delimiter at reported line/column. Remedy was sufficient; one repair committed. |
| inspect_clojure :invalid-mcp-request / unknown-fields | read_complete=false, source_unchanged=true; include_source was not accepted on that outline request. Unknown-field path was sufficient; removing it made the real MCP read complete. |
| Probe :probe-connection-failed | Missing descriptor witness, then URI openConnection and bb reflection failures during client development. The latter two were implementation errors; repaired by using bb HTTP client. No source mutation by probe. |
| Probe :stale-probe-image | Expected fingerprint/generation witnesses; restart remedy was actionable. No reload/test work preceded server refusal. |
| Coordinator suite failure | Initial run named runtime errors, missing experiment classpath and stale expectation/spec pins; repaired. Final runs retained the genuine time-budget failure. |
| Prewarm refusal-census failures | Three assertions exposed missing reachability/forwarding and unpinned literal kinds. A forwarding comment alone was insufficient: the dynamically bound function named refusal could not be resolved by the constructor scanner. An explicit namespace dependency fixed the mechanism, and the complete seven-kind addition is now pinned; focused checks pass. |
| Native/harness errors | A docs patch used a nonexistent HLD header (atomic refusal, corrected anchor); inventory p/shell initially threw on expected nonzero loads; regex metadata discovery, quoted-map sorting, a shell literal and receipt-index parsing required repairs. These are harness/editing errors, not fabricated Surgeon typed refusals. |

## OWED and three least-sure choices

**OWED: fast-lane performance acceptance.** The check was executed and failed,
not skipped. Under the mandated bb-first routing and 1 GiB cap, observed summed
time did not clear TEST-ISO-007. The resource-constrained round continues with
that failure visible; no stable or landing approval is claimed.

**OWED: a complete gate on the repaired final tip**, including resolution of
the integration time budget. The one allowed prewarm attempt found a real
contract defect and two time-budget failures. The contract defect is fixed and
focused checks pass; the broad gate is not falsely marked passed and was not
re-run after the permitted attempt. Shared installation was not changed; the
existing installed launcher defect remains outside this worktree and its repair
is not claimed.

1. Load success does not establish full runtime eligibility, especially for
   battery namespaces not executed here. Two counterexamples already refined
   the manifest; unseen dynamic paths remain the least certain inventory rows.
2. The warm reload walks flat local .clj requires and uses require :reload.
   It is not tools.namespace refresh, class redefinition proof or a clean
   image; removed Vars and unsupported require shapes are reasons cold proof
   remains mandatory. The fingerprint binds configuration/server files, not
   arbitrary mutable contents of external dependency jars.
3. The new fast entrance includes more coverage and its process allocation is
   partitioned by runtime/home. Its before/after wall is not a matched speed
   estimate, and preserved budgets reject the current result. The prewarm
   feature-thread integration group took 727.6 seconds under bb: load
   portability is especially weak evidence for that scheduling choice.

The owned warm image on 9107 is stopped and its descriptor retained as
final-image.edn. The owned baseline checkout was removed after archiving its
coordinator receipt as fast-before-receipt.edn. Measurement scripts/logs and
all dogfood requests remain under this report directory. The working tree is
clean; no shared installation, protected port, AGENTS.md, stable tag or main
branch was changed.

Close receipt (UTC): 2026-09-11T14:30:37Z
Final commit: 978a0c485ea4d23504da4105f7572614268b4335
