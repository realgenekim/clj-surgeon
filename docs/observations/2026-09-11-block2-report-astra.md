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
