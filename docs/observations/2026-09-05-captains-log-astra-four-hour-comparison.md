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
