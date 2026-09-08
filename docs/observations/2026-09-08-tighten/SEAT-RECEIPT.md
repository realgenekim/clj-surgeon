# Daily seat receipt — field contract

Draft schema `tighten-seat-receipt/v1`, 2026-09-08. This specifies the builder/operator integration; it does not assert the jobs or fleet supervision are installed.
A receipt proves a named scope from retained events. An install stamp is configuration evidence; a fresh caller trace is behavioral evidence.
The independent recorder builds it; the subject does not issue its own authoritative grade. Keep raw facts separate from derived verdicts so the steward can replay interpretation.

## Binding and publication

A seat is **host + account + runner/profile + workspace context**. A model name alone is not a seat.
The operator publishes a seat binding outside the candidate's control before enabling the daily unit:

| Binding field | What the operator supplies |
|---|---|
| `seat_id`, `registry_ref` | Stable concrete seat ID and hashed roster including active, offline and explicitly dormant seats. |
| `package_ref`, `epoch`, `source_map` | Approved package/epoch and local resolutions for the parent skill, doctrine, memory and records sources linked by SKILL.md. |
| `commands` | Absolute resolutions and argv contracts for `seat-receipt`, `canary-cell`, `verb-sentinel`, `ledger-to-findings`, `round`, `land` and `block-ledger`. Verify through the actual launcher PATH. |
| `job_schedule` | Daily close and canary selection, hourly audit, weekly sentinel schedule, deadlines and missed-job policy; emit explicit skipped/no-work receipts. |
| `owners` | Named repo/image/deployment owners, builder, reviewer, landing owner, independent recorder/acceptor, instrument steward, external duty supervisor and rung-2 officer. |
| `alert_route` | Authorized primary/alternate cord channels, named recipients, delivery and acknowledgment proof, escalation deadlines and explicit resume authority. |
| `records` | Canonical records repository identity, approved ref, relative artifact root and destination-seat local resolution; independent publication/consumer check. |
| `assignment_manifest` | Prospectively assigned task set, acceptance contract, all attempts and independent clock source. |
| `round_profile` | Supported repository/task contract, immutable findings destination, fixture/request/gate bindings, explicit saved builder session and unique names. No generic-repo claim from today's Cell C round. |
| `block_values` | `TIGHTEN_BLOCK_SEAT`, `TIGHTEN_KIND`, `TIGHTEN_BLOCK_ID`, `TIGHTEN_DEADLINE`, `TIGHTEN_HYPOTHESIS`, `TIGHTEN_FALSIFIER`; defined before opening. |
| `batch_values` | `TIGHTEN_WORKTREE`, `TIGHTEN_FINDINGS`, `TIGHTEN_ROUND_NAME`, and later the reviewed `TIGHTEN_TIP`, `TIGHTEN_MERGE_TITLE`; emitted as structured values, not a second prose brief. |
| `close_values` | `TIGHTEN_OUTCOME`, `TIGHTEN_NATIVE_WALL`, `TIGHTEN_TOOL_WALL`, `TIGHTEN_CLOSE_RECEIPT`, `TIGHTEN_PREP_WALL`, `TIGHTEN_ATTEMPTS`; measured from retained events. |

The four new job names have the draft no-argument contract in SKILL.md: consume this registered binding and emit structured receipt/next action. The operator must reconcile the builder's actual argv before installation and test each real entrance. Never `eval` a returned command string; invoke validated argv with quoted data.
`round` and `land` use their documented positional signatures; their paths and scope vary by repository. Missing bindings refuse the affected operation with owner and next action, never silently choose another repository.
The current `~/bin/block-ledger` accepts `fable|astra` only. Preserve full seat identity separately; extending its seat parser is an isolated instrument candidate under CHANGE-RULE.md, not permission to masquerade as either seat.
Block-ledger `--attempts` describes refused/failed attempts in its help. The daily receipt separately counts **all** attempts; retain both definitions rather than conflating them. `none` means unmeasured wall, not zero.

**Durable destination:** records repository + approved ref + `docs/observations/tighten/YYYY-MM-DD/<seat-id>/<run-id>/` (proposed layout; binding fixes it before use).
On the source host, the records checkout is `/home/forge/src/clj-surgeon-records`; its spelling is not a fleet contract.
Store immutable receipt JSON, compact summary and hashed evidence manifest; large traces may live in retained storage referenced by hash.
Scratch under `/var/tmp` alone is not publication. Retain scratch evidence until durable publication and consumer visibility are proved.
Publication acknowledgment is a separate append-only envelope binding receipt hash → durable commit/object → destination readback, avoiding a receipt trying to hash its own publication.
Each boot checkpoint, daily close, correction and regrade has a new run/event ID. A latest-view index may advance; it must never replace history.
The dedicated records entrance must enforce its approved ref and prevent unrelated candidate commits riding a records push. Public `main` stays frozen for this program.

## Schema conventions

All fields below are required, using explicit `unknown` or `not-applicable` with a reason when unavailable; do not manufacture null success.
Times come from recorded clocks in UTC with precision/source retained; durations are seconds. IDs/hashes bind immutable artifacts.
Counts include failed and incomplete attempts. Empty lists assert observed absence only within declared complete coverage; otherwise name the gap.
`evidence_ref` means durable artifact identity, content hash, source/event range and causal binding to the subject.
Object rows below define their named subfields; arrays preserve an entry per task, attempt, gate, seat or event as applicable.

| Field | Meaning and authoritative source |
|---|---|
| `schema`, `receipt_id`, `event_kind` | Schema version, unique immutable ID; boot/resume/checkpoint/daily-close/regrade. Recorder generates them. |
| `day`, `window.start`, `window.end`, `window.clock` | Actual observed UTC interval and clock source. An open interval stays open. |
| `seat.id`, `seat.host`, `seat.account`, `seat.runner`, `seat.profile`, `seat.workspace_context` | Registry identity corroborated by launcher/runtime; no inheritance from a model label. |
| `seat.status`, `seat.reactivation_gate` | Active/offline/dormant, reason and owner; explicit gate for dormant-seat return. |
| `session.id`, `session.model`, `session.launcher_hash`, `session.resume_event` | Actual session/model and launcher bytes, plus boot/resume provenance from runtime logs. |
| `package.id`, `package.epoch`, `package.manifest_hash` | Approved package identity and retained manifest covering all instruments/dependencies. |
| `package.expected`, `package.observed`, `package.mismatches` | Per-component expected/actual hashes and paths: skill, block, client, one-shots, oracle, harness, observer, classifier and schema. Unknown coverage remains explicit. |
| `prompt.effective_hash`, `prompt.layers`, `prompt.uptake_evidence` | Effective global/repo/launcher/experiment text, each layer hash, doctrine revision and evidence this session consumed it. File parity alone is not uptake. |
| `commands[]` | Name, requested argv, actual resolved path/hash, launcher PATH binding, execution status and evidence. A package folder is not command-resolution proof. |
| `workspace.root`, `workspace.repo`, `workspace.base`, `workspace.candidate_hash` | Canonical root, repository identity, base revision and exact candidate snapshot; include uncommitted content hashes if present. |
| `runtime.client`, `runtime.server`, `runtime.jvm` | Actual client/server identity, PID/session/generation, `user.dir`, environment/classpath/properties fingerprint, resource limits and ownership/recovery owner. Mark non-JVM tasks inapplicable. |
| `runtime.remote_binding` | Originating seat → dispatched task → remote worker/JVM/root/evidence chain. An anvil success alone does not prove skiff delivery. |
| `assignment.manifest_ref`, `assignment.policy`, `assignment.assigned_ids`, `assignment.eligible_ids` | Prospectively frozen task set, eligibility meaning and all independent assignments; retain ineligible tasks with reasons. |
| `coverage.observer_hash`, `coverage.expected_sources`, `coverage.observed_sources`, `coverage.gaps` | Observer identity, expected/actual log and process/thread coverage; inaccessible, truncated and hidden execution gaps remain explicit. |
| `coverage.mode`, `coverage.sample_selection` | Observed-work/sampled-canary/configuration-only/no-eligible-work/unknown evidence and prospectively selected action windows. Canary rotation is not a census. |
| `tasks[]` | ID, original request ref, assigned time, worker, acceptance contract/hash, status, independent acceptor/evidence and completion time or continuing age. |
| `attempts[]` | Task/attempt ID, route, start/end, actual terminal exit/state, refusal class/text, fallback, recovery, result and trace refs. Failed, refused, timed-out, abandoned and unknown attempts stay. |
| `attempt_counts` | Total, first-attempt successes, refusals by type, fallbacks, failed, pending and unknown; retain definitions and task/attempt denominators. Never compute rates over accepted attempts alone. |
| `checks[]` | Task/candidate/environment, gate name and exact argv/configuration, kind (intended red/probe/oracle/cold/platform/review/acceptance), actual exit/result, test/assertion/failure counts where meaningful and evidence. |
| `proof.required`, `proof.completed`, `proof.pending`, `proof.reused` | Explicit required obligations, executed proof and outstanding items. Reuse binds exact candidate, environment/configuration and gate scope. Probe, commit and `/bin/true` cannot discharge substantive acceptance. |
| `clocks[]` | Task ID; request/assignment start, accepted completion or censor time; setup/probe/proof/recovery/wait boundaries and sources. Preserve off-path provisioning and gaps. |
| `tails.review`, `tails.landing`, `interventions[]` | Review/landing intervals and candidate bindings; each human/operator intervention's reason, duration and owner, including Gene. State overlap with primary clock; do not double-add concurrent intervals. |
| `completion.assigned`, `completion.accepted`, `completion.censored`, `completion.latencies` | Independently accepted/assigned totals; pending/failed/abandoned/unknown visibility and distribution with censoring, not successful median alone. |
| `apparatus.classifier_hash`, `apparatus.window`, `apparatus.counts`, `apparatus.denominator`, `apparatus.share` | Frozen action purpose classifier, sampled event IDs, apparatus/edit/verdict/orientation/unknown counts and denominator; retain mixed-command ambiguity and caller/operator windows separately. |
| `apparatus.program_cost`, `apparatus.audit_ref` | Harness-building cost stays apparatus at the program boundary even if locally editing code; independent raw-event sampling and limitations. Missing coverage bars a clean share claim. |
| `friction[]` | Stable class and occurrence IDs; original call, expectation, verbatim deviation, return-tax, context privilege, watcher evidence, owner, trigger, status and inbox ID. Preserve each occurrence after deduplication. |
| `findings.ref`, `findings.selection_hash` | Immutable generated round file and selection of inbox IDs, violated intent/acceptance, deferred reasons and short judgment. No second handwritten queue. |
| `ratchets[]` | Finding/intent ID → faithful red witness and original failing bytes → green candidate → enforcing mechanism → review/landing/delivery evidence. Do not invent a retrospective red execution. |
| `transfer[]` | Ratchet → prospectively assigned later task/seat/repo → independently accepted result → original obstruction absent/present → evidence; `untested` if no opportunity. Record maintenance cost/recurrence. |
| `canary[]`, `sentinel[]` | Scheduled slot/selection, current package, actual scope, terminal receipt, observed gates/coverage; sentinel also exact admitted verb, N/tool controls, order, complete clocks, actions and registered noise. |
| `heartbeat.jobs[]`, `heartbeat.auditor`, `heartbeat.supervisor` | Expected cadence, last start/terminal receipt, next due, missed audits, externally observed heartbeat and owner for each job, auditor and supervisor. A running PID is not terminal success. |
| `stops[]`, `alarms[]` | Incident/scope, reason/evidence, owner, delivery receipt, acknowledgment/deadline, escalation rung, pending obligations and explicit authorized resume evidence. Missing delivery never closes an alarm. |
| `owners`, `independence` | Actual role identities, evidence control/write boundaries, shared-account limitations and unverified dependencies. Subject cannot certify itself. |
| `records.repo`, `records.ref`, `records.path`, `records.evidence_manifest` | Stable destination and hashed retained artifacts; publication/consumer acknowledgment lives in the separate envelope. |
| `verdict.status`, `verdict.scope`, `verdict.reasons`, `verdict.next_action` | Derived evidence status, exact claims licensed, missing/failed obligations and owned executable recovery. Never a generic green hiding pending checks. |
| `supersedes`, `regrade` | Prior receipt ID if applicable; old/new epoch, steward matrix/ruling and retained original verdict/evidence. Null means no predecessor, not erased history. |

## Verdict semantics and hourly audit

- `observed`: required behavior was actually observed and accepted for the stated scope with complete relevant coverage; no fleet or perfection inference.
- `configuration-only`: package/delivery facts verified, eligible behavior not demonstrated. A self-written completion line cannot promote it.
- `no-eligible-work`: independent assignment/eligibility records establish no eligible task in the interval; not a successful behavioral canary or a day of eligible dogfood.
- `failed`: a known required obligation failed, even if other components are healthy. Preserve concurrent unknowns.
- `unknown`: required evidence, identity, binding or authority is missing/conflicting. Never recode it as zero failures.

Use a scoped row per claim if the day mixes statuses; the summary cannot mask any failed or unknown required obligation as observed.
The hourly supervisor compares the full registry to receipts, including offline/missing seats; actual package/prompt/command parity; records publication/readability; observer coverage; stop scopes; every scheduled job's terminal evidence; audit heartbeat and alarm acknowledgment.
It observes the auditor outside the audited job/process and records its own independent monitoring coverage. Two missed hourly audits summon duty/owner escalation; configuration drift is an immediate owned discrepancy.
Prove delivery at the consumer, including current-session instruction uptake after compaction. A summary that points only to scratch or a successful remote cell is incomplete.

Sources: Astra's daily design §§3–5 and assessment (2026-09-08); merged records entry at 12:55Z and scrutiny amendment at 13:03Z; house-rules delivery invariants 17–20 and prompt-parity rule; friction-ledger-to-ratchets memory (2026-09-03); inspected `~/bin/block-ledger`, `~/bin/round`, `~/bin/land` (2026-09-08). See SKILL.md for canonical source links and the seat binding for destination resolutions.
