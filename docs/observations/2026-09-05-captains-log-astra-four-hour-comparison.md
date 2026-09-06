# Captain’s log — Astra four-hour comparison block

## 2026-09-05T14:54Z — Gene directive

Gene confirmed the telemetry blind spot: ten public `helper_extraction` calls had produced zero service events. He assigned Astra four hours to repair that route and pursue a fair, replicated comparison including orientation and proof costs.

## 2026-09-05T15:04Z — Astra starts

The cause is in the public handler: `handle-helper-extraction` routed directly to `helper-extraction/execute!` and never used the shared `record-call!` seam. Added content-free `helper_extraction` request/outcome shaping and public `tool.call` emission for success and routing refusal. The public wire witness now observes one service event for a real helper call. Focused helper lane: 48 tests, 1,159 assertions, zero failures. Commits: `68516776` implementation; `7f3605fa` public telemetry witness.

The comparison contract is frozen in `2026-09-05-astra-fair-comparison-prereg.md`: six accepted native controls per model, mirrored serial order, attested subjects, startup-inclusive primary wall, symmetric orientation/proof burden, independent acceptance, blind judges, and retained failures. `make anvil-arms-self-test` passed 389/389; this is apparatus evidence only. No timed comparison arm has been accepted yet.

## Astra 2026-09-06T03:58:11.332837+00:00 — paper-cut integration checkpoint

Strict comment preservation merged **98c3a1c3** after five independent probes
and a combined **93 tests / 529 assertions**, zero failures/errors. Same
comment at the same ordinal is insufficient: it must retain expression
identity. The fixed comparator ignores whitespace nodes while preserving
literal bytes and nested comments. Original swap false acceptance now refuses;
reindentation and insertion before the guarded expression succeed. No broad
comment-rewrite opt-in is exposed. This proves a syntactic boundary, not the
truth of prose after behavior changes.

Saved-mission Git receipts and BB fallback integrated **fd76badc**; combined
Git/fallback/telemetry/manifest gate **80 tests / 564 assertions**, zero failures.
The Git command does not stage for the caller, and source commit remains
distinct from Git publication. Actual separate-author CLI review exercised nine
cases; report is 2026-09-06-mission-commit-cli-executed-review.md. Its reviewer
authored the kernel, so independent kernel review remains open. Sol returned a
service error, not a verdict; Fable has been asked for Opus review. No real
user landing through this new command yet.

Native fallback is now an event-only BB command, avoiding JVM startup for
bookkeeping. Its witnesses compare BB/JVM receipts, poison the clojure entrance,
and verify unchanged source/ledger and permission preservation. One wall
snapshot is retained, not a replicated speedup. Usage/history gate also passed
52 tests / 263 assertions; saved summaries expose unknown usage rather than
fabricating zero. R6 offline runner integration **632713d8** preserves limited
fixture/openat witnesses and does not claim network or billing containment.

Routing b2594098 policy is GO, installation is held only for removing synthetic
admission figures from the example and running parity before installation.
All five table copies match and 14 marker tests / 130 assertions pass.
Production chooses complete verified task cost; executor-first is mandated
experiment behavior.

Learning: the next return saved often comes from a trustworthy receipt or a
runnable recovery, not a more elaborate edit grammar. These cuts reduce
bookkeeping; they do not yet earn a speed claim. The JSON cohort is still a
reliability loss (3/4 versus native Sol 4/4), and the raw comparison still needs
a fresh preregistered run after the native phase-transition failure. No new
provider calls occurred during this integration. Deadline remains 15:31Z.

## Astra 2026-09-06T04:10:17.493927+00:00 — review changes the next action

Fable's independent executed Opus review at b3dbd9e4 found two blockers hidden
by our green functional tests. Git subprocess sanitization removed explicit seat
author identity and fell back to the repository's Gene identity. Also, mission
undo could revert source after Git publication while leaving the published ref
standing without explanation. Neither has been exercised on a real user landing
through the new command. Fixes are assigned with faithful witnesses; publication
needs an intent record before the Git boundary, not merely a happy-path ledger
write afterward. Existing Git lane enrollment is already fixed on our current
branch. Review report: /var/tmp/forge/review-b3dbd9e4-fx/verdict.md.

Two smaller cuts closed meanwhile: actual help commit/fallback now omit the
unrelated helper-extraction/no-op-proof example (4cdfd031; builder31tests260
assertions; root help+manifest36tests185assertions). Spark now refuses during
executor planning rather than claiming readiness without an adapter (9a450631;
builder17tests264assertions, root admission+manifest27tests122assertions).
Routing final mirror b2824ee1 is GO, with actual installation receipts pending.

New raw cohort apparatus is only prepared code,60ba2ec1: six fixed native
attempts, failures retained, explicit end to orientation-only mode, floor saved
before paired dispatch. No paid run or score yet. Parent review adds actual
CLI-header attestation and pre-dispatch frozen fixture checks before launch.

This is the slowification lesson in concrete form: an executable witness must
include the real environment and the next operation in the lifecycle, not only
a successful isolated call. Otherwise fast success can hand the caller a costly
identity or undo surprise. No performance claim changes from these corrections.

### Astra 04:40Z — trunk integrated; executed review earned two safety ratchets

Trunk 0970f4e5 (records atop landed 3dda2a61) merged into astra/typist-route as f92f04be; merge gate 93 tests / 1438 assertions passed. Independent actual Opus review at 9a450631 found staged gitlinks could be hidden by repo submodule configuration while write-tree still included them. Commit 189e0086 pins --ignore-submodules=none; two faithful repo-config/.gitmodules witnesses failed before and pass afterward. Applied the decided literal fix through installed Surgeon :change!, retained receipt /var/tmp/forge/astra-git-submodule-fx/receipt.edn; approximately 0.164 s command wall, no native comparator or complete-task speed claim. Review and gate costs dominate this safety task.

Publication/undo repair e50c4403 writes a forced recovery intent before Git publication, preserves true or uncertain Git outcomes if ledger persistence fails, and blocks silent source undo/resume after publication. Combined current Git, identity, submodule, publication, mission, display, fallback and lane gate passes 101 tests / 827 assertions. Seven new publication tests enrolled: total 1344 tests, 86 namespaces. Combined fix awaits independent re-review; no real publication or paid cohort admitted yet. Additional Opus report retained in docs/observations/2026-09-06-opus-mission-git-review.md; smaller failure-taxonomy findings remain visible.

### Astra 05:01Z — actual proposal dogfood and clearer output

The real lexical rename is now proposed twice without provider execution or source mutation. M-1 at04:50:53Z demonstrated a60118-byte full stdout response; M-2 uses the newly bounded public proposal view while retaining full saved authority. Both admit the explicitly transferred measured prior as k3. M-1 remains a historical ready proposal, not falsely recorded as superseded; M-2 is the candidate for actual apply. Projection improves output size; no timing comparison follows from these loaded development calls.

Caller help now provides the nested request schema with honest unknown placeholders, exact proposal semantics and publication-aware recovery advice. A fresh read-only caller is checking the public surface without implementation or previous context. Independent raw apparatus re-review GO15tests, root15tests pass; cohort still unprepared. Full battery had682tests13209assertions green but overallFAIL on Node compile-cache temp hygiene. Reproduced npm cache leak exactly; same formatter with NODE_DISABLE_COMPILE_CACHE=1 has identical output and leaves no temp entries. Retain failed receipt, rerun quality gate with that command-only variable, no ignored leak names or benchmark environment change.

### Astra 05:30Z — independent3.29x prepared-task result

Fixed raw-v2 loop complete onba683ee1: all6controls and4N/4T pass; Nmedian25.193s,T7.655s, ratio3.2909x, gap17.538s>2SD3.571s. Independent reviewer verified all14 output files byte-identical and replayed28/28 gate/witness commands on copied authority. No unknown Tusage:18336prompt,21894completion(includes14618reasoning),12requests,$0.0228381. Prior orientation10.618s/shared seed0.109s/task and proof preparation excluded; actual per-arm setup/startup/proof/write included. This is a narrow prepared five-owner rename result, not arbitrary problem-to-done or Astra-vs-native. Native model Sol medium; tool OSS120B/Cerebras raw/k3. Every attempt recorded and followed by ethno/events.

Current productive usage includes actual lexical keeper6022ad35 and stale-authority refusal; no fixture replay counted as a productive edit. Next landing cuts are spawn-ledger recording, explicit artifact destination/recoverable refusal and correct projection error exit, then fresh battery/cacheoff plus full gate. Raw outcome report and independent audit retained under docs/observations.

### Astra 05:46Z — usage and landing follow-through

Independent retained-timing review localizes about 5.0–5.5 seconds outside measured setup/planning/apply, while winner proofs take only tens of milliseconds. Startup remains a hypothesis, not an attributed cause; report: [latency localization](2026-09-06-astra-latency-localization.md). The 06:00 checkpoint separates the 3.29x prepared-task result, actual M-1 keeper, and outstanding adoption/preparation evidence.

Final-tip broad battery exposed phase-event and token-accounting fixtures that omitted the new explicit receipt destination. Repair on isolated ebd3b165 uses real unique artifact parents and finally cleanup, preserves intended assertions, and passes 9 tests / 48 assertions. This useful two-owner maintenance edit dogfooded deterministic Surgeon successfully on its first guarded change call; 198 seconds includes worktree preparation and focused gate, with no native speed claim. The broader failed receipt remains retained; root will integrate only after that run closes.

Astra 05:49Z: final-tip battery at 1f47c694 terminated with 691 tests / 13289 assertions, 26 failures, zero errors or skipped/failed preconditions, zero isolation violations; 788s failed ledger receipt retained. All 26 assertion failures are in the two omitted-destination fixtures repaired in ebd3b165. No node-cache temp leak occurred with the witnessed quality-gate environment amendment. This is a failed gate, not a partial pass.

### Astra 06:03Z — final-tip battery passed

Battery at 6c864e10: 691 tests / 13289 assertions, zero failures/errors/skipped or failed preconditions, zero isolation violations across 32 namespaces, 788 seconds. The runner appended the passing receipt; both prior failed receipts remain. The only timing-environment amendment was NODE_DISABLE_COMPILE_CACHE=1 for quality gates. This does not alter the earlier performance cohort. Normal make test and Fable delta review remain required before landing.

Independent refusal taxonomy audit found the cited five collapsed events were four disclosed witness events and one real stale refusal predating the exact-code fix. No new telemetry patch is warranted on those rows; outer CLI exit must not be guessed inside service events. A genuine warning-only structural-listing refusal was reproduced, repaired separately, and independently reviewed; installation remains pending.

### Astra 06:12Z — correct the actual skill packaging failure

Normal make test at c1ef2284 passed the fast/integration suite (662 tests / 8009 assertions; no failures/errors/skipped preconditions/isolation violations), then failed two existing Babashka install assertions (862 tests / 7352 assertions overall): root skill length 72 exceeded70, and canonical/Claude mirrors retained the old invalid change! example while root had source-string and receipt-out corrections. Failed log retained at /var/tmp/forge/astra-telemetry-fx/final-make-test.log.

All three skill entrances now carry the correct invocation and source-string/receipt requirements; root-relative references remain correct. Tightened wording brings the root to70lines without relaxing the test. Skill validator passes; focused existing install suite passes11tests389assertions. No source or test changes, no installed account mutation. Normal landing gate reruns; prior final battery remains the source-code receipt.

### Astra 06:41Z — landing handoff and remaining condition

Full make test at2cc32d3a passed via the quiet slot wrapper: JVM662tests8009assertions and BB862tests7352assertions, zero failures/errors, repository hygiene passed. However slot suppressed stderr; its exit status remained trustworthy. A focused repeat with stderr restored and seat temp guard explicit passed all assertions but again exceeded the fast namespace time ceiling: outline-differential8.312s versus8.000s. The earlier8.030s failure and both logs remain. No retry-until-green or silent override.

Independent diagnosis found the whole-repository differential corpus is31.8%larger in bytes than at lane enrollment, and its frozen legacy double parse is intentional. The actual change is a cost-class correction: one full-corpus test moves to integration, four bounded tests remain fast, all bodies and the full source enumeration stay intact. Testtotal remains1353 (920original+433adopted);87namespaces49fast/6integration/32battery. Focused30tests133assertions/isolationgreen; independent exact-owner and manifest reviewGO, followed only by map-equivalent editorial cleanup. No product code, Makefile, exclusion, or timing-budget edits.

Split fullmcp run executed662tests8011assertions with zero assertion failures/errors but failed a workingtree snapshot because the author edited a plan while it ran. An unnecessary duplicate was interrupted after lead coordination; neither is a passing gate. Fable owns the final frozen merged-tree make test, still REQUIRED before landing. The passing691test/13289assertion battery receipt remains committed on the unchanged battery code.

Fable independently repaired the local slot stderr bug after pure-shell red/green; root made no shared wrapper change. Explicit per-child cohort capture files are unaffected. The separate warning-only structural read fix at92795197 has independentGO and both owning analyzer8/46 and normalgate662JVM+862BB pass; its required Solfence and installation remain pending. Main remains frozen.

### Astra 2026-09-06T08:15:56.611443+00:00 — landing completed; stronger-model comparison verified

Fable landed 9e83d4b9 plus fresh receipt cd1eb5cc on working MCP/main as 39a4e858 at07:21Z. Spawn-site ledger, artifact-destination opt-in and fresh battery close the earlier Sol r2 HOLD. Final gates passed: battery691/13289, JVM662/8011, BB862/7352, recovery3/3, oracle, hygiene and audit. One skipped precondition in the receipt battery remains disclosed; the following recovery gate passed. Public main remains frozen.

Completed new real Maven three-owner task with actual application JVM closure, independent behavioral/candidate witnesses and outbound-network denial. Paired complete prepared medians: Sol29.713s vs executor15.038s=1.976x; separate actual-Astra extension27.508s vs15.088s=1.823x. Fable replayed all10outcomes/20commands successfully. Shared preparation1035s reduces first-epoch two-attempt ratio to1.028x when charged. Small n2pairs/epoch, no free-choice or general superiority claim. Twelve provider requests cost known$0.01981695, no unknowns; reasoning tokens retained as subsets. Full report: [Maven native comparison](2026-09-06-astra-maven-native-comparison.md).

Gene's adopted cadence is small usable slice → use immediately → ledger row. Two actual test leaks are next: Node compile-cache suppression belongs at the guarded test-child boundary; the root receipt leak comes from a global io/file test mock. A separately reviewed warm-profile prototype uses the existing trusted profile argv, owned immutable runtime generation and verification-only lock. Changed candidates initially refuse/restart: no premature per-edit-speed claim. Groq live boundary and Spark adapter remain unfinished.

### Astra 2026-09-06T08:26:57.350919+00:00 — two real leak cuts and live fallback boundary

Node-cache test-child enforcement016685f0 passed owningratchet and fullnormalgate JVM662/8011 +BB863/7358, zero failures/errors; independentreview verified all7testedfilehashes againstcommit. No productionformatter/Make/JVMmanifest change. Fullnormalwall retained as399.79–420.65s interval, not invented precision.

Receipt test globalio/filemock faithfully reproduced rootreceipt leak while old7assertionspassed; correctedtest14assertions and externalrootinventorypass. MCPdogfood firstrefused two-form require replacement, correctedcompleteform committed2edits696ms; formatted and explicittelemetryrequire resolvedlintwarning. IndependentstaticGO and focusedJVMRED/GREEN complete; combinednormalgatepending. [Receipt report](2026-09-06-astra-receipt-fixture-leak.md).

Singlepreregistered induced429→actualGroqtransport passed: onepaidrequest,84prompt/175completion/159reasoningsubset, unknowncost;0.515sexternalwall, no retry. No realoutage/missionfailover/speedclaim. [Groq boundary report](2026-09-06-astra-groq-live-boundary.md). Warmprototype now owns next window; exact runtime authority and parity still required.

### Astra — proof cost and actual structural discovery (2026-09-06T09:22:44.623611+00:00)

The immutable warm-profile slice earned exact proof parity, not per-edit utility: four cold and eight warm commands passed the same counters; six negative cases passed. External warm calls took 667–722 ms, with 3.28/3.43 s startup and about 0.415 s teardown. The under-100 ms target failed in all eight warm calls. My own invocation passed four tests / 24 assertions at 657 ms profile-reported wall, after a retained slot-marker refusal. All owned runtimes stopped. Reusing a still-valid receipt dominates rerunning an identical proof; the next slice must earn a real source transition. Evidence: `/var/tmp/forge/astra-warm-profile-fx/RESULT.md`, `independent-outcome.md`, `ASTRA-USE.md`, and `FINAL-CLEANUP.json`.

Actual public MCP preparation resolved 59 direct call spellings in 54 named owners across all 20 selected Maven files in one 290 ms server request, returning 767 source characters. A separate 29 ms pilot outside the selected set also counts. The original owner list was correct; cell-prep mistakenly resolved all owners against the first file. Fable fixed the parser instead of shrinking the task. This establishes useful structural discovery, not an A/B speed ratio or semantic alias resolution. Evidence: `/var/tmp/forge/astra-fanout-owner-fx/owners.json`.

Prepared real-repo execution remains promising, but shared preparation nearly erased the small-cell advantage. Two drafted witnesses were inadmissible; a marker or Var-existence test cannot stand in for forwarding behavior. The next deterministic multi-file comparison therefore requires baseline/reference-candidate proof admission, six native controls, identical actual Astra actors, native scripts allowed, complete tool/startup/proof cost, and optional adoption measured separately. This is a different class from the single-file fast typist.

The two fixture leaks landed on working trunk 181c365c after the battery and all landing gates passed, per Fable's terminal report. Freshness fix 9475cbd7 plus enrollment 846c8631 now excludes only proven ordinary content edits to three exact journal paths. Independent 14 tests / 73 assertions and 15 public cases pass; the normal combined gate is still pending. The actual failed history changes from 55 raw to 22 counted, excluding 33 archival commits; age and commit limits are unchanged. Other documentation can be executable input, so no blanket docs exclusion is justified.

### Astra — actual transition and Spark (2026-09-06T09:30:10.545202+00:00)

Personally executed both prepared harnesses after explicit timing handoff. Actual closed-closure transition passed in866ms; current two-JVM cold obligations4386ms, one observation only. Startup3428ms plus teardown467ms prevents a one-shot win. Five negative refusals passed; independent review found poisoned=false on a poisoned refusal, now assigned a successor receipt correction. Spark one permitted availability/accounting attempt passed in3.906s, actualSpark/low,5987input/38output including27reasoning, costunknown, no observed tool calls. Both owned cleanups empty. See [full report](2026-09-06-astra-warm-transition-and-spark.md). No production or replicated speed claim.

### Astra — stronger cold alternative and actual landing (2026-09-06T09:58:04.633110+00:00)

Personally executed single-JVM cold comparator: complete2.611796s, same gate2/5+witness4/24, hashes/modes/cleanup independently verified. Earlierwarm0.865773s now has3.02x component ratio, unreplicated;5.07x remains only against two-JVM cold. Warmstartup+transition4.294s still loses isolated use. See [comparison](2026-09-06-astra-single-jvm-cold-comparison.md). Freshness455a8dd9 landed as working trunk38a2cec4 at09:51 per Fable terminal: mcp-test664/8031, run_all863/7358, other gates green; freshness accepted without another receipt-branch battery rerun. Root did not push public main or change shared services.

The bounded paved usage collection found5catalog Surgeon calls plus a separately correlated manual-HTTP admission probe. Two collector bugs emerged: edit_clojure missing from route classification, and raw source text confused with typed refusal authority. These are under repair before the same window is rerun once; initial receipt is superseded for interpretation, not combined into later counts. No token or adoption claim inferred from an incomplete classifier.

Astra correction, 2026-09-06 10:22Z: Fable disclosed possible builder JVM overlap with the 09:52 cold comparator. The 2.612s receipt is retained, but the 3.02x warm/cold component ratio is potentially contaminated, not a clean speed estimate. Proof correctness remains verified; exact contention impact is unknown. No replacement run. See the timing disclosure in [the comparison](2026-09-06-astra-single-jvm-cold-comparison.md).

### Astra control usage, 2026-09-06T10:26:37.619179+00:00

Six native controls passed; complete verified wall median105.364s and sampleSD17.025s. My20–60s prediction missed allsix, unchanged in preregistration. Read-only bound-rollout study of completedC1–C4 shows native already batches59sites/54owners/20files into one patch. Three actors built and repaired extra proof checks after completing the source edit; C1/C4 also hit payload/patch framing failures. Each received the92.6KB dossier once; cumulative input reflects repeated inference, not unique prompt size. These are observations, not causal attribution or an interim tool win. [Trace study](2026-09-06-astra-native-control-usage.md). Frozen DN cohort continues without intervention.

Separate task-selection negative: inspected caller defects are already repaired; no honest ready non-rename raw-forms mission found. Do not invent a cosmetic task or transfer rename calibration to a semantic fix. Existing fanout and bounded Spark utility are next evidence; no additional provider call was spent on the selection.

### Astra completed D1 findings, 2026-09-06T10:36:19.680146+00:00

Frozen D1 correct=false/279.015scomplete retained. Independent completed-rollout review:9MCPattempts,7refused,2commits; actor temporarily removed18existingrequires then restored them around the migration. Only3.561s was inside measured MCP calls, versus274.054sactorwall; do not attribute all gaps to reasoning. Gate and independent scope audit passed;2witnessfailures reflect require-layout mismatch (closingdelimiters moved to new libspec), not mass source deletion or a demonstrated semantic failure. [Bound review](2026-09-06-astra-d1-refusal-and-witness.md). No acceptance rewrite, retry, active-actor feedback or overallpairedverdict.

The observed alias-reuse workaround is a concrete next API investigation after the frozen cohort, unlike inventing another cosmetic dogfood task. Any fix must preserve both existing aliases and remaining jdbc uses, with an independent source/proof witness; no shared server mutation during timing. Native can also reuse proof and smaller dossiers, so those improvements must be given equally to both arms.

### Astra route diagnosis, 2026-09-06T10:43:10.992242+00:00

The18require workaround did not establish missing editing capability. Source review found an existing batched within/from/to route preserving bindings; compact add-only refusal is intentional EDIT-022. ActualD1 outputs include the detailed structured errors, so diagnostic invisibility is not supported for this actor. Prefer explicit preconditions and recovery to the existing route before extending compact semantics; any API extension needs a separate measured justification. [Static request and anchors](2026-09-06-astra-existing-binding-reuse.md). No execution or mid-cohort change.

### Astra completed frontier, 2026-09-06T10:57:11.050749+00:00

Frozen fanout actor route loses: complete Nmedian84.682s vsD237.575s (D2.81times aslong), N4/4 vsD1/4 under a witness with a disclosed require-layout assumption. Rootowns admissionmiss; no semanticfailureclaim from those3checks, no rerun. Separate rootexistingroute capabilityprobe succeeded59/54/20 withoriginalproofs,23.012sattempt/2.413stool, excludesprep/notABratio. [Complete report](2026-09-06-astra-fanout-final-and-existing-route.md).

Sparkunpaidpreflight: actual reference kernel+proofs pass2/5and4/24, outerreceipt refuses descriptive-scope field overwritten by auditmap.9.858s retained; no paidgeneration. Successor fixes field identity withactualretainedreceipt regression; originalfailure stays. Rootwindowreleased10:55Z, Fablelandingnext.

### Astra class contract clarification, 2026-09-06T11:23:03.190063+00:00

Source audit confirms mission classes are rate labels over one existing-definition replacement compiler. General extraction, file creation and top-level insertion/deletion are not supported by owner_forms; ready confirms frozen authority, not prose feasibility. JSON may replace a subset; raw Clojure requires exact coverage in one file. Updated request documentation and current plan without changing admission or code. [Source audit](2026-09-06-astra-class-capability-audit.md). No runtime during Fable's fence.

### Astra single Spark coding result, 2026-09-06 11:28Z

The sole paid attempt verified on the original three-owner Maven task: generation 4.152 s, complete parent 13.020 s, proofs 2/5 and 4/24, scope preserved. Input 7089, output 804 including reasoning 256; subscription dollar cost unknown. New unpaid preflight independently GO; old failed preflight retained. No retry or production route change. Timing window explicitly released to Fable after terminal cleanup. [Full boundaries](2026-09-06-astra-spark-utility.md). Collector fence sole hold fixed separately in99d5d5fc by removing a root-only study’s live fleet-cutoff marker.

### Astra 12:00Z checkpoint, recorded 2026-09-06T11:42:12.863714+00:00

[Full Gene report](2026-09-06-astra-checkpoint-1200z.md) records the prepared wins, the new assigned-route loss, the independently verified single Spark candidate, the known witness defect and next small fresh-caller pilot. Meter observed11:37: 50% weekly remaining, reset September12 15:48:08UTC; no token-budget estimate. Next scheduled checkpoint15:00, extended end16:20.

### Astra fresh-caller usability result, 2026-09-06T12:31:10.559486+00:00

Both new callers verified (N67.262s, D87.680s), but ordinary-batch adoption failed: D authored a native patch, tried admit_clojure_patch(commit,verify=none), got the correct verification-incomplete refusal, then applied the same patch natively. Zero tool-committed sites. Both reused and ran supplied proofs, then parent repeated them. Preparation/review/overlapping wait was38.2minutes, separately visible. [Full result, fairness limits and accounting](2026-09-06-astra-recovery-pilot-result.md). Collector/docs landed b912f714 after the recorded freshness refusal and fresh battery; no policy waiver.

### Astra 12:47Z — preserve mixed native routes; make proof integration explicit

The fresh two-caller pilot exposed a real collector omission: one exec that
reads and applies a native patch kept its patch counter but lost its patch route.
Fixed the route classification without changing outer-action counts or clocks.
Paved self-test passes; identical-bounds recollection reports native-read5,
native-patch1, skill-load1. D's opaque stored-patch action remains language-unknown
to the collector. Exact source trace remains the authority for native completion.
Service refusal event has no timing field: missing samples remain missing.

Added development MCP guidance for commit/focused verification and existing
profile configuration. Independent design review caught that candidate snapshots
are partial and the runner starts at the live project root; candidate-first
loading is now explicit. No production API/code change or new provider run.
Details: docs/observations/2026-09-06-astra-recovery-pilot-result.md and
 docs/plans/2026-09-06-astra-patch-proof-usage.md. These are usability corrections,
not speedup claims. Fable retains the quiet window for the corrected fence.

### Astra 12:55Z — receipt volume is a concrete next hypothesis

Read-only analysis of the same D1 refusal: canonical compact UTF-8 JSON result
43,264 bytes, structured portion 15,054 bytes, text content 28,013 bytes.
Recorded operation elapsed 577.461 ms; the next model input grew from 42,348
to 58,520 tokens. Canonical representation size is not observed transport bytes,
and the input delta includes all intervening context. This does not attribute
latency to receipt size or establish a speedup. Owners/hashes/files account for
much of the structured detail; this was not simply an echoed giant patch.
Keep detailed evidence, but investigate reducing what a caller must ingest to
decide the next action. No new API or run admitted from this observation.

The reserved non-rename diagnostic contract now has an explicit behavior matrix
at /var/tmp/forge/astra-two-form-refusal-design-review.md. No accepted syntax
changes; parsed cardinality only, detached comments and malformed input distinct.
The repo-required linked-intent-dev skill is absent from searched local roots;
Fable has been asked for its canonical location before scoped implementation.
His actual r5 fence ended HOLD with matching end SHA; later work stays his lane.

Receipt-size evidence: /var/tmp/forge/astra-recovery-pilot-usage-fx/receipt-size-analysis.json.

### Astra 13:21Z — real diagnostic fixed through installed CLI

Fable supplied the canonical linked-intent skill and approved design/test phases.
Recovered the actual 08:18 two-vector request rather than the mismatching saved
draft. RED 2/72: 21 diagnostic failures, zero errors. Installed CLI preview left source
unchanged; stdin change! committed one change in one file with guarded receipt 69ed8beb.
GREEN: same 2/72 all pass; paved lint zero warnings/errors. Independent result review GO:
exact approved replacement, unrelated bytes and acceptance predicate unchanged,
no timeout or process-group survivors. Full normal gates and landing remain pending.
This is useful CLI dogfood, not a paid typist attempt or speed claim.
Report: docs/observations/2026-09-06-astra-cardinality-dogfood.md.

Admission-prose reconciliation also prepared: old unverified-commit/whole-map
precedence statements corrected, historical receipts preserved and labeled.
A restricted real Maven profile adapter is static GO but UNEXECUTED; its 5m26s
preparation is separate from any future proof/usage time. No public admission win
claimed. Quiet window explicitly released after focused diagnostic run.

### Astra 2026-09-06T14:25:10.000524+00:00 — real proof binding passes; first public integration refuses

Cardinality keeper landed ee911577 after exact-tip LAND YES and merged gates. Real Maven profile polarity cases each2.006s: good candidate over broken live passes; broken candidate over good live fails3 actual timestamp assertions, zeroerrors. The first actual public clarity-refactor call refused before tests/write because my adapter rejected the gate-created .clj-surgeon/.gitignore and write.lock. Source/test/dependency bytes unchanged, lintclean, first-call complete0/1, toolcommits0. Exacttwo-path repair staticGO; runtime retry pending. Full report: 2026-09-06-astra-real-profile-utility.md. No speed/adoption claim; preparation and queueing remain visible.

### Astra 2026-09-06T14:33:19.384566+00:00 — verified public keeper after one retained refusal

All five exact bookkeeping-boundary preflights refused as expected. The same native patch then committed through admit_clojure_patch with verify=focused in 2144.939 ms: one file, one owner, clean lint, actual recording-query suite 2 tests / 0 failures/errors, complete verification, zero outside-hunk drift, next_call null. Receipt post-image hash matches the prepared candidate; no source reread or repeated suite needed. Independent saved-evidence review GO. First-call success remains 0/1, total successful calls 1/2. Preparation and shared-box waits prevent treating the 2.145-second successful call as complete-task speedup. Current-source audit confirms producer timing is still missing; both usage windows retain caller clocks separately. See 2026-09-06-astra-real-profile-utility.md.

### Astra 2026-09-06T14:49:38.734851+00:00 — second keeper and description dogfood

Second task reused the unchanged v3 profile and passed on its first public call in 2068.714 ms: private timing-points helper added, two consumers updated, one file, actual suite2tests0fail0errors, cleanlint, complete verification, zero outside-hunk drift. This is behavior-preserving duplicate conversion extraction; Fable’s earlier clamping-policy experiment was declined and remains unapplied. Aggregate first-call-complete tasks1/2, successful public calls2/3; no free-choice or native-speedup claim.

The existing usage plan’s description guidance was applied through installed CLI stdin :change! after preview: exactly one owner/file, guarded receipt5849276f, post-image19f43825. Only the description string changes, not schema/validation/verification. Paved lint before/after:0errors, same2existingwarnings+1info (rows shift by10); no introduced findings. Focused registration and normalgate results pending at this entry. Public catalog remains old until deliberate refresh; no exposure claim. Artifacts astra-description-dogfood-fx and astra-admission-use-2-fx.

Astra 2026-09-06T14:52:34.263681+00:00: description validation update — one-Var attempt hit three whole-suite refusal-census fixture failures (retained; not a green partial suite). A separate direct public catalog probe passed3checks: one entry, description carried unchanged from its source Var, expected outcome classes. No live catalog refresh. Normal gate follows on the committed tip.
