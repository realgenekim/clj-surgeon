# Block 3 build — candidate, BLOCKED on required gates

Subject: `4ab97c9614624ae1b12de7925671eafd1d35e440` (owner worktree remains detached).

The seven implementation deliverables are present. This is local, self-issued build evidence, not independent acceptance or an installation approval. No product checkout was changed, no publication or server was started, and no additional model was launched. The source tree has no `INSTALL.PROVEN` claim. The inherited RUN line, JSON run handle, SHIP status line, and `INSTALL OK v3.9` literal are unchanged.

## Delivered, in assignment order

1. **Bet registry and fold.** `lib/bets/registry.edn` retains the complete original registration texts and their hashes, plus per-cell scoring rules for Fable, Astra's Claude amendment, and Astra's Codex forecasts. `measure fold <registry> <report-or-jsonl-ledger> [--out bets.edn]` parses measured Markdown tables or retained cohort/bet observations. Each analysis keeps its input bytes, source hashes, scorer hash, registry revision, predecessor IDs, and unknown cells. Revisions append candidate analyses; they do not qualify a new production scorer.
2. **Second encounters.** `measure encounter register <finding-id> <witness-file> <delivery-file> <trigger>` returns an encounter ID and writes OWED. `score <id> <later-run-id>` requires a later matching requested event and a subject-matched acceptance check with explicit accepted=true, recurrence=false, exit=0, and readable hash-matched evidence. An exit-zero terminal alone cannot prove transfer. Local proven status does not discharge independent acceptance. `reopen <id> <witness-file>` retains history and requires a new later run. `seed` imports both unregistered, retrospectively proven prewarm encounters and the registered receipt-boolean obligation with trigger “the next verb build.” It also imports the two receipt findings and the committed refusal/witness maps from `a15531ee`.
3. **Four meters.** `board --meters` folds inner edit→probe endpoints, routine findings→final-batch endpoints with continuing ages/status/tails, accepted recurrence-free later assignments, and a roster retaining forge@anvil/skiff and missingness. Independent recorder absence stays explicit. Unregistered retrospective successes never enter the prospective numerator. Offline rows remain in the active denominator unless explicitly deactivated.
4. **Cohort replay.** `measure cohort <plan.edn> [--out cohort-report.md]` binds task/arm/artifact digests, seed or explicit historical seed gap, callers, deadline, registration, exact reader, runner/oracle bindings, original TSV schedule order and task/arm/caller assignments, and packet manifest. Plan bytes are retained by digest before ingestion. It imports each retained run, including the capacity failure, then feeds ledger rows to the pinned original report fold. `mode=execute` is refused: this packet authorized replay, not another cohort. Imported real-run observations carry fixture=false outside fixture mode; tests cannot override fixture=true. Existing calibration/smoke rows stay outside the assigned scored population.
5. **Morning facts.** `board --report` emits `vs_native`, `wins_losses`, `learnings_ratchets`, and `next` as JSON facts. The default ship window is the current UTC day. `--from <UTC> --to <UTC>` supplies an explicit historical window; OWED is still folded over the whole ledger. Failed attempts remain in launches-per-landing. Native comparisons and complete routine clocks remain unknown unless their own evidence is recorded.
6. **Receipt view.** `board --receipt YYYY-MM-DD` renders the v1 field structure with missing nested paths named unknown, plus ledger package/prompt/assignment/attempt/check facts. Participation is self-issued. Any requested observed verdict is refused because this implementation has no independently controlled issuer/replay verifier. Merely claiming an independent issuer in a payload does not promote it.
7. **Witnesses and packaging.** Eight new behavioral tests and a bidirectional linked-intent contract are wired into `test/run.sh`. The prior staged installer is restored and its payload inventory extended; packet sealing requires every new dependency. `WITNESS_ROOT` relocates test storage without changing production paths or assertions. Source-tree tests use the existing fixture-only destination witness mode without creating an install stamp. The exact-default-root N1 test is retained unchanged.

## Reproduced measurements

| Measurement | Result | Retained evidence |
|---|---|---|
| Claude published held labels | Astra 11/12; both Fable H adoption bets missed | [bets.edn](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/bets.edn) |
| Claude exact point forecasts | 4/12, separately reported | [combined scores](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/bets.json) |
| Codex main M−L bets | +10 / +4; acceptance loss 0 / 0; both held | [Codex scores](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/codex-bets.edn) |
| Historical cohorts | 120 + 156 + 130 = 406 retained scored runs; zero new cohort launches | [replay directory](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/cohort) |
| Claude and Codex tables | Eight tables each byte-identical to their retained reports | [table comparison](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/table-comparison.json) |
| Morning window, Sep 10 22:00–Sep 11 08:00 UTC | 3 landings; ship launches 3/5/1; landed ship median 823 s | [morning facts](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/report-morning.json) |
| Sep 11 UTC day | 3 landings; ship launches 5/1/1; landed ship median 823 s | [day facts](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/report-today.json) |
| Local meters and receipt | Missing protected clocks/recorder/participation remain unknown | [meters](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/meters.json), [receipt](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/receipt.json) |

The Claude 11/12 labels are a published judgment, not exact point accuracy. The registration does not grant a universal ±1 margin for those point forecasts. Both outcomes are retained rather than silently changing the scorer. Approximately-AC bets without a numeric margin remain unqualified/unknown. Codex N, M/N cost, and conditional repair without pre-write exposure remain unknown. Ship launch counts group attempts by base; they are not builder/reviewer-launch counts, and ship elapsed is not the protected completion metric.

## Validation and blockers

[New tests](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/new-tests-final.log): eight tests pass, including duplicate ingestion, appended scorer revision, missing evidence, later acceptance, reopen, cohort table equality/drift, missing recorder, and false observed receipt. New Clojure files pass `~/bin/clj-kondo`; Python AST and shell syntax checks pass. [Remaining inherited tests](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/remaining-tests.log) and [continuation](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/remaining-tests-2.log) cover refusal checks N2–N11, five publication sites with frozen zero-push replay, ledger concurrency, lifecycle, compatibility, production-content isolation, the real 60-second heartbeat, and fixture filtering. The intentional content-oracle RED lines in those logs are negative fixtures, followed by their PASS result; they are not production-ledger writes.

**The complete required suite is not green.** [Latest full-suite attempt](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/full-witness-candidate.log) reaches `test/fix2.py` N1, which creates an actual default packet under `/var/tmp/forge/packets`. Landlock denies that write outside this packet's owner/client roots. The witness is not skipped or rewritten to manufacture green. The inherited scoped-circuit check validates retained historical packet paths, not a fresh launch of this candidate; no fresh-circuit claim is made.

**The declared make gates cannot run in this repository.** [Prewarm](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/prewarm.log): `No rule to make target 'landing-gate-prewarm'`. [Battery](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/battery.log): `No rule to make target 'test-battery'`. There is no Makefile at the supplied subject. No replacement `/bin/true` target or altered gate argv was introduced.

A scratch installer attempt reached the same N1 failure, printed `INSTALL NOT PROVEN`, and automatically rolled back all added files. No `INSTALL OK` was emitted. The retained log is `/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/install-attempt.log`.

Installation remains unproven until the complete suite runs in an eligible execution envelope. Fable owns installation; no write to `~/bin` was attempted. Independent acceptance, independent-account receipt issuance, and the next real prospective encounter remain external.

## Reproduction

The complete local fact ledger is [/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/verified-events.jsonl](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/verified-events.jsonl). After installation, read the slice without changing it (the source tree correctly remains destination-unproven):

```sh
./board --report --ledger /var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/verified-events.jsonl
./board --meters --ledger /var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/verified-events.jsonl
./board --receipt 2026-09-11 --ledger /var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/verified-events.jsonl
```

Before installation, the read-only library entrance is `bb lib/views.clj` with the same view arguments; the witness suite exercises `board` using the existing confined fixture mode.

Every new measure verb accepts `--ledger <local-path>` before its subcommand; omitting it uses the existing ledger default. Do not import the same historical experiment into a production ledger merely to run these examples. Frozen replay plans are in [/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/cohort](/var/tmp/forge/packets/207c15bb-99cc-4c99-a970-b35c18a4f4b4/codex-home/work-temp/build-evidence/cohort). Their historical artifacts remain read-only. Input snapshots, model traces referenced by those plans, and event hashes are local evidence, not proof of independent custody.

Recording contracts for future producers: inner measurements use `payload.type=inner`, `edit_utc`, and `probe_verdict_utc`, with optional setup/recovery/cold-baseline seconds. Routine events use `findings-selected` and `final-batch` with the same run ID. Encounter requests carry `encounter_id`; acceptance checks carry `name=acceptance`, `encounter_id`, `finding_id`, explicit booleans/exit, the same subject, and artifact digests. Seat facts use `type=seat-receipt` and the v1 nested field names. These facts do not grant authority.

## Least sure and owed

- The apparatus readers contain historical classification/default choices. Their exact hashes and tables are preserved; replay equality is not new instrument qualification. Some historical ordering seeds are not retained in the supplied run plans and are named unknown, not invented.
- Same-account evidence remains self-issued even when a different process/model authored it. The four protected meters cannot certify independence or fleet participation.
- Launch grouping by base is useful for this log's three landings, but it cannot reconstruct missing original findings, builder clocks, human intervention, or independent acceptance.
- Fable/Astra need an eligible envelope for N1 and the complete installed-byte suite. Gene or the registered gate owner must supply applicable make targets or ratify a corrected execution manifest. Mayor owns independent-account issuance; Astra owns scheduling the next prospectively eligible receipt-boolean encounter. No silent gate substitution is authorized by this report.

No commit was created; `report.edn` carries the machine-readable blocked verdict and obligations. Traceability proves these promises have witnesses, not that their intent is right; independent review remains owed.
