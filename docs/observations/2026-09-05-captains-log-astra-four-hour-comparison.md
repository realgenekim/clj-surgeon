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
