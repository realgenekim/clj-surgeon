# CurtainCall LID batch 5 — system HLD

Worktree: `/home/forge/src/cc-lid6`  
Branch: `astra/lid-batch-5-hld`  
Base: `f474df96` (batch 4)  
Commit: `31d71c31e40f9ef9630ea7614bfcefe4919d5975`  
Completed: 2026-09-07 18:52 UTC  
Author from environment: `forge-anvil <forge-anvil@anvil>`  
Trailers: `Co-Authored-By: Gene Kim <genek@itrevolution.com>` and
`Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`.

Committed six documentation files, 220 added lines and zero deleted lines.
Post-commit status contains only the two incoming `.codex` changes named below;
the staging index is clear and neither configuration path was staged. No push.

## Result and section map

All source-worktree changes are documentation additions. The pre-existing HLD
prose, migration checklist, registry IDs, EARS, statuses, misreadings, boundaries,
and witnesses remain unchanged. The HLD already contained short engineering,
authorization, and delivery sections from earlier batches; this batch extends
them rather than duplicating or rewriting them. Each authorization/delivery
extension adds five sentences; each new system-boundary section has five or six.

| HLD section | Enforcing IDs / explicit boundary |
| --- | --- |
| Engineering practice | `LID-SPEC-001`, `LID-TEST-TAG-001`; registry + all topic spec files + intent contract + Prolog wiring; existing debt is the only spec-linkage exception and is shrink-only |
| Authorization model | `AUTHZ-001`, `AUTHZ-003`, `REV-STEWARDSHIP-001`, `API-SCOPE-001` through `API-SCOPE-010` |
| Delivery and notification evidence | `CFP-001`, `LETTER-001`, `LETTER-002`, `MAIL-DELIVERY-001` through `MAIL-DELIVERY-008` |
| Exports and receipts | `EXPORT-RECEIPT-001`, `CSV-SAFE-001`; receipts beside settings; generated-response evidence and the two review CSV renderers only |
| Storage durability | `BLOB-DURABILITY-001`; `CANONICAL-LINK-001` identified as the separate emission promise, not a stored-path witness |
| Public surface boundary | `EMB-001`, `EMB-002`, `EMB-004`, `EMB-005`, `SCHED-002`, `AGENT-TRUST-001`; archival and fragment promises explicitly unlinked |
| External lanes | Sheet + separate `cc-ledger` + cache; no core-store `bp.*` facts; positional and header-ownership fences, one-conference clamp, idle polls, frozen rating order. No active lane IDs exist; `LID-SPEC-001` is named only to explain the coverage limit |
| Tenets | Added “A refusal preserves the operator's work and is loud” (`TASK-REFUSAL-001`, `BLOB-DURABILITY-001`) and “A persisted value must be readable from any host” (distinguished from `CANONICAL-LINK-001`) |
| Fact and view flow | Added replay-from-empty invariant, explicitly including historical evidence such as recusal history; no new witness claimed |
| Success Metrics → Migration checklist (closed) | All original checklist bullets retained verbatim under the new subheading |
| Success Metrics → System health | Recurring traceability/oracle, authorization, delivery-evidence, receipt/CSV/blob, public-boundary and asynchronous-telemetry checks; `TELEM-ASYNC-001` adds the request-latency boundary |
| References | All six original topics: canonical agenda, append-only correction, Rough Blocking drag, Rough Blocking command, speaker-profile edit, canonical letters; both new topics: authorization and delivery; all eight spec files, registry, debt, traceability, and the two standalone intent notes |

## Cross-links and scope proof

- Added 38 optional `:doc` fields naming repository-relative HLD section anchors.
  The existing registry schema accepts additive metadata; its required fields and
  all behavioral data are unchanged. A ledger comment defines the pointer as
  navigation, not a behavioral witness.
- Added HLD links to authorization, delivery, and canonical-letters spec files,
  and to the spec-traceability contract. No spec checklist row changed.
- Manual automated check: all 40 Markdown links in the touched Markdown files
  resolve; all 38 registry HLD pointers resolve; all eight topic designs are
  referenced; every HLD requirement ID exists in the registry.
- Removing the added `:doc` lines and their convention comment reproduces the
  base registry byte-for-byte. Every documentation diff has zero deleted lines.
  The debt ledger is byte-unchanged.
- No application, test, Makefile, runtime, or fleet file changed. No service was
  started or contacted directly. No push was performed.
- Initial `.codex/config.toml` deletion and untracked
  `.codex/config.toml.disabled-by-sol-yolo` belong to the incoming worktree state;
  neither is part of this batch or its staging set.

## Gates

All gate logs and temporary fixtures are under `/var/tmp/forge/lid-batch/`.
Commands set `TMPDIR=/var/tmp/forge/lid-batch/tmp`; JVM gates also set
`JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/lid-batch/tmp`, and `PATH` puts
`/home/forge/bin` first so analyzer subprocesses use the required kondo entrance.

`bin/kaocha unit` — exit 0. Log: `cc-b5-unit.log`.

```text
1036 tests, 13320 assertions, 0 failures.
```

`make runtests-once` — exit 0. Log: `cc-b5-runtests-once.log`.
All five Prolog oracle files (34 tests), JavaScript (18 tests), the temporary
mission-worktree regression, and the final Clojure gate passed:

```text
admin_event_slack_contract: All 9 tests passed
review_board_contract: All 6 tests passed
rough_drag_projection: All 5 tests passed
speaker_profile_edit_contract: All 8 tests passed
talk_agenda_contract: All 6 tests passed
# tests 18
# pass 18
# fail 0
1036 tests, 13320 assertions, 0 failures.
```

`~/bin/clj-kondo --lint docs/intent/registry.edn` — exit 0.
Log: `cc-b5-kondo.log`.

```text
linting took 11ms, errors: 0, warnings: 0
```

`git diff --check` — exit 0.

## Doubts and retained limits

1. Historical named API keys with an absent scope still resolve to organizer;
   removing a narrow key's scope can promote it. This is preserved compatibility,
   not a newly strengthened authority rule.
2. Dev-log still returns sent and records `email.sent`, including Cloud Run with
   no configured provider. Production missing-provider refusal remains absent.
   Provider acceptance is not inbox arrival; the HLD keeps both distinctions.
3. No dedicated linked rows currently cover archival/fragment publication,
   persisted-resource portability, recusal replay, or the applicant subsystem.
   Existing tests cover some of these; applicant idle polls and rating-session
   order remain unwitnessed at that boundary. No audit-draft ID was invented or
   made active to make the HLD look complete.
4. `EXPORT-RECEIPT-001` witnesses the visible receipt matching returned bytes.
   The existing settings-survival regression is separate, and neither the row
   nor this batch claims receipts for every export format. CSV safety is scoped
   to the two review renderers, not every spreadsheet import mode.
5. The debt ledger's historical no-growth rule is review-enforced; the contract
   checks current missing, stale, and unknown links. Optional `:doc` pointers
   were checked in this batch but have no new permanent anchor-validity gate.
6. `bd prime` worked, but creating the batch task failed with “database not
   initialized: issue_prefix config is missing.” No bootstrap, sync, or tracker
   configuration change was attempted within this source-only batch.
7. The specifically named `linked-intent-dev` skill was absent from the available
   skill locations. The related shared skill at
   `/home/forge/opt/claude-skills/linked-intent-testing/SKILL.md` was found and read;
   its traceability-versus-correctness distinction informed the final review.
   No code change or new behavior required a downstream LID implementation phase.
