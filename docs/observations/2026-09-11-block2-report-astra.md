# Block 2 — ship v3.10 staged packet

Staged for Fable's review and install. Nothing was installed into `/home/forge/bin`; no records checkout or product worktree was edited. Fixtures, isolated ledgers, retained model attempts and installation destinations are under `/var/tmp/forge/ship-v3.10-fx`. `MANIFEST.base.sha256` is unchanged (SHA256 `e2a721f59a3cbc56122c4b71ff58d9f0277e6189b692cfc12afcdcf7c9b90766`).

The deliverable is an executable build-to-review packet: `run build`, `run check`, `run build --delta`, `measure`, EDN schemas, packet-aware ship handoff, the five refusal/scheduling classes, and scoped Andon guards. `PACKET.md` explains the interface. This is execution evidence for Block 2, not independent product acceptance, fleet participation, or a performance-improvement claim.

## Coverage

- `run` / `lib/packet.py`: pre-launch schema, named skill paths, detached SHA, profile, ordered deliverables, environment, subscription authentication, command identity and resource admission. One protected context pack. Original CODEX_HOME/AGENTS/BABASHKA_PRELOADS/Java options retained; effective temp and heap settings recorded. Codex gets isolated CODEX_HOME with auth symlinked; Claude gets isolated subscription state and an empty explicit MCP configuration. No API-key fallback.
- `:standard-anvil`: rendered hard rules plus enforced owned filesystem roots through Landlock, including the worktree's own Git metadata/object store needed for commits; private temp/client state; read-only packet evidence and `~/bin`. Forbidden ports are checked in launch environment/config, as requested. JVM access uses the shared lease; check minute reservations and execution deadlines are executable. These environment controls are not a host-wide firewall or a cgroup proof against arbitrary hostile subprocesses.
- `report.edn`: required fields and row shapes validated after the builder exits; a stale prior report is archived before launch. Missing/malformed data produces `report-invalid`, even after client exit zero. Measurements cite existing receipts and retain their hashes. Authored obligations and ineligible checks become OWED; `board` folds discharge events and shows manifest IDs.
- `manifest.edn`: content-addressed policy, context/environment hashes, explicit builder/check argv and executable hashes, shipped one-shot closure, runtimes, and producer epoch. The exact post-resolution prompt hash is a separate launch-uptake event, avoiding a circular hash. Packet child writes to the context/manifest are denied. Packet-aware ship keeps the reviewer mandate and references the same pack, builder report and fix manifest; its observations carry the packet manifest ID. The installed `independent-review` helper also receives the identical complete pack; a 21 KB stub witness proves it is not truncated.
- Delta rounds require descendant lineage, identify changed and standing fields, supersede old claim IDs, and refuse reuse of old claim IDs on a changed subject. Verdict documents are excluded from patch manifests and ship's fix capture.
- `ship`: occupied-branch refusal, exact-tip prewarm admission for catalog/registry changes, named-holder JVM queue, packet admission and context handoff; FREEZE at admission and before publication, including direct and autofix pushes. `receipt-chain` checks out the sealed SHA detached. Existing landing-gate consumption remains in place.
- `fence-run`: an owned supervisor and retained exact process identities replace argv-wide discovery. Stop kills the owned session, waits/reaps, and archives partial verdict/log bytes. `records-push` takes its own lease before pull/push and queues another writer; the inherited code-publication lease wait remains.
- `land` and the staged copy of `round` check Andon at admission and mutation boundaries. `andon-pull` and `andon-lift` are copied from installed bytes; lift's owner/evidence contract is unchanged. Failed delivery leaves FREEZE standing. Decision 2a and the September 9 named owners are carried in policy: Astra steward, Opus runner one-shot recorder/acceptor, forge@anvil duty, mayor rung 2. Mayor-account issuance and exercised real delivery/ack supervision remain external.
- `install.sh`: source inventory and seal checks, START content hash over staged files and modes, drift rechecks through installation/witness completion, per-file atomic renames, installed digests and isolated installed-byte witnesses. Drift can leave a partial installation; the named backups remain the rollback. It does not print INSTALL OK after drift.

## Refusal → witness

| Class | Executed witness and result | Repair / retained evidence |
|---|---|---|
| Stale mandated skill | `test/packet.py`: missing path refuses before a model launch; repair points at an existing current SKILL.md | Correct the brief's skill path |
| Occupied branch | `test/refusals.py`: actual ship admission refuses the branch before fetch/review; receipt-chain then checks out a sealed SHA detached while that branch is occupied elsewhere | Pass the full SHA |
| One JVM conflict | Real ship entrance queues behind `fixture-one-jvm-holder`; ledger contains `queued` and holder; it resumes after lease release | Wait for the named holder / budget.wait |
| Stale catalog pin | Actual producer field shape (`git-head`, `git-tree`, `stages`, `prewarm?`, `landing?`): old tip refuses; exact-tip successful control admits | `git checkout --detach SHA && make landing-gate-prewarm`; supply exact receipt |
| Staged-set drift | Installer's source changes during copying; START hash detects it and refuses before INSTALL OK | Reseal and restart; inspect partial install/backups |
| Fence/records stop | Owned live reviewer stub killed; partial verdict archived and session reaped. A second records writer queues before pull, then publishes to a local bare fixture remote | Use the owned START pid; wait on records lease |
| Zero JVM budget | Required battery remains OWED with owner, argv, subject, unblock; later `run check` executes and discharges; duplicate is idempotent | Eligible executor with JVM capacity |
| Insufficient minutes | A 99-minute reservation under a one-minute packet remains OWED; insufficient check allocation refuses execution; sufficient explicit allocation discharges | RUN_CHECK_MINUTES on eligible executor |
| Self-referential verdict | Growing the verdict by three lines leaves the fix manifest digest unchanged; verdict path is excluded | Verdict prose remains outside the patch manifest |
| Freeze bypass / stale session | Ship, land and round refuse the named stop; a freeze created after admission is seen before mutation; all push sites have fresh guards | Isolated repair, then authorized lift |
| Failed delivery / invalid lift | Local delivery exits 17; FREEZE remains. Empty evidence lift refuses and retains the stop | Named owner plus repair evidence; missing delivery is not consent |
| Invalid report | Fixture and first real Claude attempt exit zero with invalid report data; terminal is `report-invalid` | Builder writes the required field shapes |
| Delta stale claims | Descendant round carries lineage and superseded IDs; current `99/141` present, old `141/141` absent from prompt | New claim IDs and current evidence |
| Context/handoff | Child context rewrite gets kernel PermissionError while report write succeeds; reviewer mandate preserved and same pack referenced | New packet for changed context/policy |

All inherited v3.9 witnesses remain in `test/run.sh`: ledger integrity/concurrency, lifecycle and UNKNOWN reconciliation, legacy RUN byte comparisons, real 60-second heartbeat, fixture filtering, exact-argv installation guards, and production-content isolation. Deliberate red content-oracle controls print `FAIL` for a scratch copy of the ledger; their enclosing witness must pass. The real production ledger is read-only to the fixtures, and unrelated live producer appends are allowed.

## Scoped circuits

### claude-sonnet-5

Packet `084178da-3122-4e8d-ba57-22353ca20575`; exit 0; observed launcher wall 26.307 seconds.

[launch](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/claude-13af58d4-bd64-437e-b22e-9ace3e297aaa/launch.stdout) | [rendered prompt](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/claude-13af58d4-bd64-437e-b22e-9ace3e297aaa/packets/084178da-3122-4e8d-ba57-22353ca20575/prompt.md) | [context pack](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/claude-13af58d4-bd64-437e-b22e-9ace3e297aaa/packets/084178da-3122-4e8d-ba57-22353ca20575/context-pack.md) | [manifest](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/claude-13af58d4-bd64-437e-b22e-9ace3e297aaa/packets/084178da-3122-4e8d-ba57-22353ca20575/manifest.edn) | [client transcript](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/claude-13af58d4-bd64-437e-b22e-9ace3e297aaa/packets/084178da-3122-4e8d-ba57-22353ca20575/output.log) | [report.edn](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/claude-13af58d4-bd64-437e-b22e-9ace3e297aaa/owner/report.edn) | [ledger](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/claude-13af58d4-bd64-437e-b22e-9ace3e297aaa/events.jsonl)

Report SHA256: `dfbf87fa13a820b581d18a0a838175a5bffd15c0e2b95215912522ddcac00b66`.

### gpt-6-astra

Packet `c408f9aa-b225-456b-9a7f-368ee48cd444`; exit 0; observed launcher wall 34.479 seconds.

[launch](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/codex-cb78695f-565b-4350-a972-04e56971468f/launch.stdout) | [rendered prompt](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/codex-cb78695f-565b-4350-a972-04e56971468f/packets/c408f9aa-b225-456b-9a7f-368ee48cd444/prompt.md) | [context pack](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/codex-cb78695f-565b-4350-a972-04e56971468f/packets/c408f9aa-b225-456b-9a7f-368ee48cd444/context-pack.md) | [manifest](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/codex-cb78695f-565b-4350-a972-04e56971468f/packets/c408f9aa-b225-456b-9a7f-368ee48cd444/manifest.edn) | [client transcript](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/codex-cb78695f-565b-4350-a972-04e56971468f/packets/c408f9aa-b225-456b-9a7f-368ee48cd444/output.log) | [report.edn](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/codex-cb78695f-565b-4350-a972-04e56971468f/owner/report.edn) | [ledger](/var/tmp/forge/ship-v3.10-fx/scoped-circuits/codex-cb78695f-565b-4350-a972-04e56971468f/events.jsonl)

Report SHA256: `46bcecd1b3e181d153b260ac44b6419132a663e27e889c435d91f1c2048f6eda`.

Seven subscription launches are retained: the initial Claude report-invalid attempt; a successful Claude/Codex pair before the final Andon publication-path correction; another successful pair before completing the independent-review pack handoff; and the final successful pair above. The initial circuit brief ambiguously asked for a least_sure string; it was corrected to require a vector containing a string. The failed result was not rewritten or counted green. The earlier successful pairs are historical, not proof for the final executable hashes. No real product review, prewarm/battery JVM gate, or remote product publication was performed.

`test/circuits.py` checks retained exact report/manifest hashes and successful terminal events. During fixture installation it verifies these scoped receipts; it does not spend more model launches. The staged directory and retained circuit artifacts must remain available for Fable's installation witnesses.

## Validation and DONE proof

Delivered in order, each full suite green before the next item: item1.log, item2.log, item3-producer.log, item4.log, item5.log, item6.log, item7.log, item8-final.log under the fixture root. Earlier failed attempts and intermediate green snapshots are retained there. `item9.log` records the installer/report suite before the independent-review handoff addition; `final.log` is the final full witness run.

The five stop classes either refuse cheaply or queue before expensive review: occupied branch, stale prewarm, occupied JVM lease, drifting staged set, and owned fence/records lifecycle. The delta control carries no stale claim text. These are executable fixture controls, not inferred behavioral proof from a write receipt. Fresh Claude and Codex additionally completed the actual packet round trip.

Python compilation and Bash syntax passed. `~/bin/clj-kondo` linted `lib/board.clj` and `lib/edn.clj` separately with zero errors and warnings (`lint-board.log`, `lint-edn.log`). A combined invocation initially warned about duplicate requires because these are separate scripts in the default user namespace; separate entrance checks resolve that tooling artifact without changing source or suppressing a diagnostic.

**Final full suite: exit 0, every witness green against staged and fixture-installed bytes.** [Transcript](/var/tmp/forge/ship-v3.10-fx/final.log); artifacts: `/var/tmp/forge/ship-v3.10-fx/witness.6BkVC3hg`. Production-content isolation checked 1,567 witness event IDs while nine unrelated live events arrived. Final transcript SHA256: `bbed38b39e013bc7cf502f0ec8519647587582cb76637b5e5e7c881757276cd9`. The installable payload seal and unchanged base manifest were checked after that exact suite process exited.

## Three least-sure choices

1. **Obligation discovery.** Catalog/registry and code-path classification is deliberately conservative and supplemented by explicit brief checks/diff-plan paths. It is not semantic whole-program discovery of every possible hidden registry mutation. The positive prewarm control uses the producer's actual field shape; no real repository JVM prewarm was run here.
2. **Resource and stop boundaries.** Landlock protects packet writes; leases serialize participating entrances; environment rules bound normal Java launches and deny forbidden configuration. This is not host-wide hostile-process containment. Andon checks are fresh at named boundaries, not an atomic global transaction with every possible external writer. Installation remains per-file, with explicit partial-state recovery.
3. **Custody and generalization.** Manifests and scoped receipts are self-issued execution evidence. They do not replace independently controlled assignment/acceptance, mayor-account issuance, real alarm delivery/ack exercise, cross-seat transfer, or a registered performance comparison. No broader Sublime completion is claimed.

## Compatibility authority and install

RUN, JSON handle, START/END RECEIPT, INDEPENDENT and SHIP status-line shapes were preserved. The installer still prints the exact `INSTALL OK v3.9 ...` compatibility literal because inherited waiters assert it. The staged package is v3.10 by its file set and digests. Renaming that literal to v3.10 is recorded as requiring Fable/Gene's waiter-contract authority; it is not necessary for this installation and was not changed.

After reviewing this report, Fable installs:

```sh
env -u INSTALL_PROC_ROOT SRC=/var/tmp/forge/ship-v3.10 DEST=/home/forge/bin \
  bash /var/tmp/forge/ship-v3.10/install.sh
```

The installer runs its witnesses with isolated ledgers and prints the installed digest manifest before its unchanged INSTALL OK line. Any REFUSED or INSTALL NOT PROVEN is not a successful install. `MANIFEST.staged.sha256` seals installable payload bytes; `MANIFEST.base.sha256` remains the supplied base. No binding file was touched.

Work on the staged set stops after this delivery. Fable owns the real installation and independent review.

## Fix round 1

**NO-GO for installation: final-byte scoped-circuit proof and the final full suite remain owed.** Source repairs are staged; the requested two-launch budget has been spent. An explicit extension is pending, not inferred.

This section supersedes the Block 2 policy and validation claims above where they conflict with the Opus NO-GO report or Fable’s ruling. The working-reviewer kill policy is withdrawn. This fix round does not install into ~/bin; Fable owns the second red-team and installation.

### Finding → change → witness

Each finding first failed against the original staged bytes in `fix1-red-P*-*.log` (including `fix1-red-P0-1.log` through `fix1-red-P3-5.log`). The earlier duplicate RED probes are retained, not counted as additional independent acceptance. Final finding checks run in `test/fix1.sh`, incorporated into `test/run.sh`; P0-4 is witnessed by the real inherited v3 fixture suite.

| Finding | Change | Witness |
|---|---|---|
| P0-1 | Restored the three-argument fence-run launch contract; removed all three stop calls. | Exact call-site invariant and real legacy v2/v3/v3.1/v3.4 suites. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P0-1.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P0-2 | Restored the v3.9 fast-lane-red late-verdict block verbatim. Ship retains reviewer identity/run; next admission records queued and names the live reviewer instead of replacing its work. | Exact v3.9 block comparison, live-reviewer admission refusal and post-exit admission; legacy fast-lane tests. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P0-2.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P0-3 | Installer creates a complete rollback inventory before mutation, writes incoming installed inventory plus INSTALL.PROVEN=false on first mutation, sets true only after required fixtures and content checks, and automatically rolls back failures including added files. --rollback accepts a stamp or backup directory. Missing fixture suites cannot be silently skipped. Packet sealing refuses inventory-missing/closure-incomplete, requires the complete shipped payload inventory, and also binds explicit interpreter script operands. | Missing/partial inventory refusals; real failing install, marker inspection before payload renames, old-byte restoration, added-run removal, idempotent explicit rollback; installed success marker in test/install.sh. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P0-3.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P0-4 | Excluded verdict basename with :(exclude,glob), retained git stderr, and typed git failures/empty patches. | Unmodified run-ship-v3 real SHIP-FIX-BLOCK auto-close and independent-review chains: 22 rows, zero mismatches. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P0-4.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-ship-v3.log) |
| P1-1 | Packet Git calls disable fsmonitor, hooks, SSH command and pager with GIT_CONFIG_NOSYSTEM=1; packet-capable shell entrances use the same flags. Hashes of Git config/worktree config/hooks are compared across the builder; changes refuse before post-builder work. | Builder plants core.fsmonitor payload; payload never executes and terminal is git-metadata-changed. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P1-1.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P1-2 | Profile says: writes are confined by Landlock; reads and network are NOT restricted. Removed private from profile text. | Exact rendered profile assertion; scoped circuits receive the profile. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P1-2.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P2-1 | Trusted Andon resolver validates both overrides and LEDGER_FIXTURE_ROOT beneath the permitted fixture boundary before executing any override. Pull/lift also validate overrides. | ANDON_ROOT outside fixtures and ANDON_ADMISSION=/bin/true refuse typed; confined positive paths and stale-session freeze tests pass. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P2-1.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P2-2 | Lift requires a nonempty existing evidence file or a ledger check/verdict event ID, and records the evidence digest in a ledger check before removing FREEZE. | Gene + x refuses and retains FREEZE; existing evidence file permits lift and records the event. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P2-2.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P2-3 | receipt-chain and records-push check FREEZE at admission and directly before publication; records-push checks before pull too. Publication-site witness includes both publishers. | Both entrances refuse the named freeze; legacy publication suites remain green. RED probe isolation incident below is not hidden. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P2-3.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P2-4 | Catalog discovery includes changed content, not only path strings. Exact-tip prewarm EDN must match a ledger check of its full byte hash, subject, successful exit and make landing-gate-prewarm argv; SHIP_PREWARM_EVENT can select the event. Packet prewarm checks emit this provenance. | Catalog hidden inside src/mcp_server.clj plus forged EDN refuses; matching check admits; changing receipt bytes refuses. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P2-4.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P2-5 | Closed top-level brief schema rejects unknown keys before launch. | dif-plan refuses as unknown-brief-key. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P2-5.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P2-6 | Every ordered deliverable must be a nonempty file at completion. Missing output produces deliverable-missing with the numbered step. | Valid report plus missing step 2 never completes green. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P2-6.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P3-1 | Budget-bounded JVM queue emits owed on deadline. Admission lock protects dead-identity lease reconciliation; stale inherited inode is retired with reconcile, and contenders recheck inode identity. Packet children no longer inherit the lease descriptor. | Live lock exceeds a subsecond budget and becomes owed; a dead recorded owner with an inherited locked descriptor is reconciled and its lease broken; live named-holder release resumes ship. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P3-1.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P3-2 | Owned fence supervisor is a Linux subreaper and retains exact descendant PID/start/pgid identities. It waits for reviewer exit, then reaps descendants including new sessions; there is no public stop verb. | Reviewer remains alive with paid verdict; setsid sleep descendant is reaped after normal reviewer exit and verdict archived. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P3-2.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P3-3 | Board coverage compares distinct ledger producers with executable entries in the installed/staged inventory; absent producers are unobserved. | Empty ledger and ledger containing ship/v3.9 produce different coverage sections. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P3-3.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P3-4 | Packet manifests retain claim evidence/input hashes. Delta refuses changed evidence for claims that stand (including identical claim values under new IDs); refreshed-claim deltas render changed-evidence hashes. | Same-subject standing claim with changed evidence refuses stale-content; descendant with refreshed claims passes inherited delta test. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P3-4.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |
| P3-5 | Ledger fixture confinement takes its root from LEDGER_FIXTURE_ROOT or the packet manifest, without a version path literal. | Declared fixture root admits isolated events; production path still refuses; literal-absence assertion. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P3-5.log) · [GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) |

### Legacy suites

Every inherited suite ran unchanged against the staged executable bytes with isolated fixture ledgers. No legacy assertion was weakened. Installer process/rollback mechanics additionally use synthetic confined fixture scripts in `test/install.sh`; those are not the real legacy acceptance results below.

| Suite | Result |
|---|---|
| [run-land-auto](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-land-auto.log) | land-auto fixtures: 19 rows, mismatches: 0 |
| [run-land-publication-truth](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-land-publication-truth.log) | land-publication-truth fixtures: 12 rows, mismatches: 0 |
| [run-ship-v2](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-ship-v2.log) | ship-v2 fixtures: 28 rows, mismatches: 0 |
| [run-ship-v3.1](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-ship-v3.1.log) | ship-v3.1 fixtures: 13 rows, mismatches: 0 |
| [run-ship-v3.2](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-ship-v3.2.log) | ship-v3.2 fixtures: 8 rows, mismatches: 0 |
| [run-ship-v3.3](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-ship-v3.3.log) | ship-v3.3 fixtures: 13 rows, mismatches: 0 |
| [run-ship-v3.4](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-ship-v3.4.log) | ship-v3.4 fixtures: 14 rows, mismatches: 0 |
| [run-ship-v3.5](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-ship-v3.5.log) | ship-v3.5 fixtures: 5 rows, mismatches: 0 |
| [run-ship-v3.6](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-ship-v3.6.log) | ship-v3.6 fixtures: 9 rows, mismatches: 0 |
| [run-ship-v3.7](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-ship-v3.7.log) | ship-v3.7 fixtures: 73 rows, mismatches: 0 |
| [run-ship-v3](/var/tmp/forge/ship-v3.10-fx/fix1-legacy-run-ship-v3.log) | ship-v3 fixtures: 22 rows, mismatches: 0 |

### Scoped-circuit evidence and pending refresh

The two originally authorized subscription launches completed with exit 0 and exact prompt/context/manifest/report checks. A later interpreter-closure witness found a remaining P0-3 omission; fixing it changed lib/packet.py. These two receipts are therefore historical and correctly refuse stale-session on the final bytes. Two additional final launches have been requested but are not authorized by elapsed time or silence. No API billing fallback or extra launch has been used.

- **claude-sonnet-5**: packet `3a77c120-0bfe-4e37-ae9b-7ce4780e35ff`, 31.243 seconds. [Launch](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/claude-83e5aa42-4c03-40a2-b374-fcbbdfb4546b/launch.stdout), [client output](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/claude-83e5aa42-4c03-40a2-b374-fcbbdfb4546b/packets/3a77c120-0bfe-4e37-ae9b-7ce4780e35ff/output.log), [prompt](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/claude-83e5aa42-4c03-40a2-b374-fcbbdfb4546b/packets/3a77c120-0bfe-4e37-ae9b-7ce4780e35ff/prompt.md), [context](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/claude-83e5aa42-4c03-40a2-b374-fcbbdfb4546b/packets/3a77c120-0bfe-4e37-ae9b-7ce4780e35ff/context-pack.md), [manifest](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/claude-83e5aa42-4c03-40a2-b374-fcbbdfb4546b/packets/3a77c120-0bfe-4e37-ae9b-7ce4780e35ff/manifest.edn), [report](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/claude-83e5aa42-4c03-40a2-b374-fcbbdfb4546b/owner/report.edn), [ledger](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/claude-83e5aa42-4c03-40a2-b374-fcbbdfb4546b/events.jsonl). Report SHA256 `cf122ee21b0fc12bd3d913599ada631e3ba872d080b500f11c683b3c0877b85e`.
- **gpt-6-astra**: packet `9965026c-08e7-460d-a017-edc754975570`, 41.022 seconds. [Launch](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/codex-657dd5fb-a033-4628-bcaa-c33fb4ffea5a/launch.stdout), [client output](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/codex-657dd5fb-a033-4628-bcaa-c33fb4ffea5a/packets/9965026c-08e7-460d-a017-edc754975570/output.log), [prompt](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/codex-657dd5fb-a033-4628-bcaa-c33fb4ffea5a/packets/9965026c-08e7-460d-a017-edc754975570/prompt.md), [context](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/codex-657dd5fb-a033-4628-bcaa-c33fb4ffea5a/packets/9965026c-08e7-460d-a017-edc754975570/context-pack.md), [manifest](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/codex-657dd5fb-a033-4628-bcaa-c33fb4ffea5a/packets/9965026c-08e7-460d-a017-edc754975570/manifest.edn), [report](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/codex-657dd5fb-a033-4628-bcaa-c33fb4ffea5a/owner/report.edn), [ledger](/var/tmp/forge/ship-v3.10-fx/fix1-scoped-circuits/codex-657dd5fb-a033-4628-bcaa-c33fb4ffea5a/events.jsonl). Report SHA256 `92e8ba27fbebf7c7f4d228ecfdd3ff2aec220c6b88a69deadf744b3329109d28`.

### Isolation incident — owner reconciliation required

I made an isolation error in the original P2-3 RED witness: `RECEIPT_WORKTREE` and `RECEIPT_LOG_ROOT` were not supplied. The first probe ran the unguarded original receipt-chain on the real `/home/forge/src/clj-surgeon-receipt`, fetched origin, and at 2026-09-11 19:41:55 UTC changed its checkout from `fable/cli-splice-classpath-r1` (HEAD `c74116ae`) to detached `origin/MCP/main` (`a15531ee`). The invalid tip `a` then failed to merge, before any battery, commit or publication. A second RED invocation repeated the failed merge at 19:42:45 UTC. `/var/tmp/forge/receipt-chain-b.log` was also written. The records-push probe was not reached because the receipt assertion failed first. Read-only inspection found the worktree clean; I did not change it again or claim isolation for these two probes. Fable should reconcile the receipt checkout with its current owner before its next use.

The witness now explicitly confines receipt worktree/log root and records repo/lease even when the admission guard is broken. A confined original-byte RED replay is retained in [fix1-red-confined-P2-3.log](/var/tmp/forge/ship-v3.10-fx/fix1-red-confined-P2-3.log). All later publisher checks stay inside fixtures. This was a worktree/temp ownership breach, not a fixture-ledger leak; the content-isolation oracles independently check ledger identity sets.

### Limits and handoff

Prewarm/lift provenance is local execution evidence, not cryptographic independent acceptance; a nonempty evidence path does not establish the truth of its prose. Catalog detection inspects names and changed text, not arbitrary dynamic semantics. Landlock permits reads and network. Subreaper custody is exercised on Linux, with exact process identities; working reviewers are never intentionally terminated. The legacy `INSTALL OK v3.9` literal remains unchanged under the existing compatibility rule. `install.sh --rollback <stamp>` also accepts the full backup directory. The installed `INSTALL.PROVEN` file is the explicit proven/unproven marker.


### Final closure witness and validation status

The original full `test/run.sh` passed, including both fixture installations and production-content checks with 10 unrelated live events: [fix1-final.log](/var/tmp/forge/ship-v3.10-fx/fix1-final.log). This predates the last P0-3 closure repair and is not labelled final-snapshot proof.

The final additional RED was [interpreter closure](/var/tmp/forge/ship-v3.10-fx/fix1-red-P0-3-interpreter-closure.log): `bash <destination>/records-push` sealed when records-push was absent from the destination inventory. The repair requires the complete payload list and binds explicit interpreter script operands. [Packet tests](/var/tmp/forge/ship-v3.10-fx/fix1-closure-packet.log) and [all finding checks](/var/tmp/forge/ship-v3.10-fx/fix1-closure-acceptance.log) pass after that repair. [Retained circuit validation](/var/tmp/forge/ship-v3.10-fx/fix1-circuits-stale.log) correctly refuses the old packet.py hash. Current-snapshot model/whole-suite proof remains outstanding pending the requested budget extension. Do not install on the strength of historical green logs.

The rename-failure injection also exposed an installer-only rollback edge: a failed atomic rename left its `.incoming.<stamp>` executable. [RED](/var/tmp/forge/ship-v3.10-fx/fix1-red-P0-3-rename.log). The rollback inventory now records incoming paths for removal, and the installer disables Python bytecode writes into the destination during verification. This changes the installer, not the packet payload manifest.

### Final payload and installer results

The final-payload fixture installation ran all 11 real inherited suites against the copied staged bytes: **216 rows, mismatches: 0**. [Full installed-byte log](/var/tmp/forge/ship-v3.10-fx/fix1-real-fixture-install.log), [machine-readable result](/var/tmp/forge/ship-v3.10-fx/fix1-final-legacy-results.json). The subsequent model-receipt check correctly refused stale-session, the installer printed that the destination had been replaced and named its backup directory, and automatic rollback completed. Its production-content oracle checked 2,202 witness event IDs while 19 unrelated live events arrived; no fixture event reached the production ledger.

The final installer-only incoming-file repair passed [P0-3 GREEN](/var/tmp/forge/ship-v3.10-fx/fix1-green-P0-3-final.log), including refusal rollback, removal of added files, explicit rollback replay, and injected rename failure without leftover incoming executables. [Syntax](/var/tmp/forge/ship-v3.10-fx/fix1-syntax.log) and [board lint](/var/tmp/forge/ship-v3.10-fx/fix1-lint-board.log) pass. The base manifest remains unchanged.

No install into `/home/forge/bin` was performed in this fix round; `run` remains absent there. The 24 present base payload entries match the supplied base digests; noninstalled installer/test entries are explicitly distinguished in [base comparison](/var/tmp/forge/ship-v3.10-fx/fix1-installed-base-check.json). The earlier receipt-worktree breach remains as described above and requires owner reconciliation.

The final full `test/run.sh` is **not claimed green**: the current-byte circuit check remains red until its receipts are refreshed. The two originally authorized launches are retained as historical successful execution, not relabelled as current proof. The requested additional pair has not been launched. After authorization, refresh the Claude/Codex circuit receipts, run the full suite, append the resulting proof, verify `MANIFEST.staged.sha256`, and return ownership to Fable for red-team/install. Until then this report is a NO-GO handoff, not installation approval.

Outstanding proof ledger event: `a5be0e8f-995c-4504-a428-7c7065f46a95` in [/var/tmp/forge/ship-v3.10-fx/fix1-proof/events.jsonl](/var/tmp/forge/ship-v3.10-fx/fix1-proof/events.jsonl).
