# Block 1 — staged v3.9 report

**Staged and witnessed; not installed into ~/bin.** `test/run.sh` passed against the staged scripts and again against a fixture installation of their bytes. Fable owns the real install and subsequent use. No authority stop was needed for RUN, SHIP, or INSTALL OK grammar changes: their existing field shapes remain intact. RUN gains a separate JSON handle line, as requested.

Read: the September 11 system plan of record, Astra vision §3 and §7, and the driver's-seat answer §3–§4. This delivery is Block 1, not the remaining work-protocol or publication-admission blocks.

## Files and coverage

- `lib/ledger.sh` and `lib/ledger.py`: shared writer and reader. Default file `/var/tmp/forge/ledger/events.jsonl`; `RUN_LEDGER` overrides it for fixtures. `flock` on `events.jsonl.lock`, complete JSON lines, contiguous `seq`, SHA-256 `checksum`, flush and fsync. The checksum covers canonical compact, sorted-key JSON excluding `checksum`. `seq` and `checksum` are framing fields in addition to the requested envelope. UTC comes from `date -u`; elapsed milliseconds use the owning process's Linux monotonic start. Subject has exactly repo/base/tip/tree, with nulls for facts not known yet.
- `run-bg`: durable requested before spawn, started after successful exec admission, terminal with child exit and actual pid/pgid, 60-second heartbeats. UUID attempt directories retain their own log/config/handle/exit artifacts. Legacy name.log/name.pid/name.rc are compatibility symlinks; the JSON handle points directly to its unique log. The reported pid remains the job's pid; pgid names the detached supervisor/job group.
- `await`: reconnects by ledger run ID for run-bg handles; emits UNKNOWN reconciliation when its recorded supervisor identity is gone without terminal evidence. Identity includes boot ID and process start ticks. Reconciliation includes unknown exit status, observed identity and the digest of the last 4096 log bytes. It never reruns the command. Exit 0 means a terminal event was found (inspect payload.exit_code for the child result); exit 3 means UNKNOWN.
- `ship`: requested at entry and argument resolution, then a candidate-bound requested observation when sealing has actually established the candidate/tree; the brief is digested when available. Queued/admitted observations, its existing journal transitions, check verdicts, first reviewer line, every SHIP-STOP line and following indented repair text, confirmed landed mutation/publication, cleanup, and terminal fields are mirrored into the ledger. Existing events.log text and the SHIP status-line construction remain intact. A refusal before sealing has null candidate identity; the ledger does not invent a sealed subject. The heartbeat reuses the existing lease heartbeat loop.
- `fence-run`, `receipt-chain`, and `land-auto` are covered **through ship's observations of their steps**. They are not independent new ledger producers.
- `board` / `lib/board.clj`: Babashka fold, ordered RUNNING / TERMINAL since cursor / UNKNOWN / OWED, followed by timestamp, cursor and producer coverage. The fold neither writes a cursor nor starts services. Elapsed is the last recorded process elapsed value, not a fabricated live counter. Terminal rows retain task ID, outcome, refusal reason, landed SHA and terminal payload. A later heartbeat with an unknown subject does not erase an already observed sealed tip.
- `board --resume PATH` (or `BOARD_RESUME_NOTE` with `--resume`) adds only the note header before its first second-level heading, explicitly imported and timestamped by file mtime. No note body becomes current state.
- `python3 lib/ledger.py import /path/to/events.log historical-run-id` imports legacy journal lines as timestamped observations with `imported=true`, a digest, and deterministic per-line import keys. Reimport skips already seen keys. Imported observations are not promoted to live execution state.

**Uncovered as standalone producers:** fence-run, receipt-chain, land-auto, land, records-push, independent-review, sol-yolo, suite-run, worktree-add, maven-r, build, measure, install.sh, gate-envelope.clj, gate-consume.clj, and mutate.clj. Board is a reader, not a lifecycle producer. Await writes reconciliation only. Ledger imports are administrative observations, not proof that an uncovered verb was instrumented. This is not fleet-wide coverage.

OWED exposes explicit refusal owners and the read-only inbox observation. It does not infer an owner or discharge an obligation automatically. The installed `maven-r` explicitly forces a JVM-backed fallback, so this slice now reports that connector as skipped. Successful read-only inbox import was witnessed using a local command stub; live inbox contents are not claimed.

## Witnesses

Executable tests live under `test/`; run:

```sh
/var/tmp/forge/ship-v3.9/test/run.sh
```

The final run also installed under its unique fixture directory, checked installed digests, and ran the witnesses against installed bytes. Installer tests use an alternate process-table fixture only for destinations and process-table paths under `/var/tmp/forge/ship-v3.9-fx/`; a real ~/bin install cannot use this bypass. A real-host preflight during development refused on live Ship/Astra processes, without replacing files.

```
PASS ledger envelope/checksum, duplicate id, torn tail
PASS concurrent writers: 6 x 200, contiguous sequence, verified checksums
PASS fast exit (10 ms): requested / started / terminal
PASS spawn failure: terminal with error, no started
PASS duplicate attach: identical terminal event
PASS crash-before-terminal: exact child pgid killed, UNKNOWN reconciliation
PASS immutable attempt logs
PASS ship refused launch: requested, refusal reason, cleanup, exact terminal fields
PASS ship transition adapter and marked legacy import
PASS restart reconciliation: both fresh board processes show crash UNKNOWN, never RUNNING
PASS stale heartbeat, owed owner, cursor, resume header as timestamped imported observation
PASS v3.7 RUN byte diff: missing exact; fast/live/126/127 with only OS pid substitution
PASS real 60-second heartbeat with exact child pid/pgid
PASS staged install: installed digests, installed-byte witnesses, INSTALL OK shape, live Astra and lease refusals
PASS all witnesses (artifacts: /var/tmp/forge/ship-v3.9-fx/witness.0DMA3H4t)
```

The RUN diff uses the frozen v3.7 launcher, SHA-256 `45c3d1dd5ed5f12037948d1e316ad993a62122b6da6ac44f9b0e6447297a802d`. Missing-command output is compared literally. Successful outputs substitute only the OS-assigned old pid with the new pid, then run `diff -u` over all remaining bytes, including the same log path and wait expression. Different invocations cannot literally receive the same live OS pid. The added 126/127 cases preserve v3.7's exec-failed classification.

Additional checks: `bash -n` on changed shell entrances; `~/bin/clj-kondo --lint lib/board.clj --cache false` => 0 errors, 0 warnings. The first cache-enabled lint attempt hit a native analyzer cache exception; disabling its cache resolved the tool failure. No Clojure semantic tooling or JVM test suite was needed.

The ship witness exercises a real refused launch and the journal adapter's lease/check/verdict/publication classifications. It does **not** claim an end-to-end green review, battery, gate suite, or remote publication. No real review was launched. The inherited v3.7 repository fixtures were not run in this build; the installer retains their existing optional execution when present in FIXTURES.

## Install — Fable's act

The three helpers absent from the initial staged inventory (`gate-envelope.clj`, `gate-consume.clj`, `mutate.clj`) were copied read-only from ~/bin into this staged set so the inherited install file list is complete. `MANIFEST.base.sha256` remains unchanged. `MANIFEST.staged.sha256` seals installable source bytes; installer preflight refuses drift or missing inputs, and post-install verification compares the installed bytes with that seal. `MANIFEST.installed.sha256` is written in the destination and named immediately before INSTALL OK.

After reading this report, when live Ship/Astra runs have finished:

```sh
env -u INSTALL_PROC_ROOT SRC=/var/tmp/forge/ship-v3.9 DEST=/home/forge/bin bash /var/tmp/forge/ship-v3.9/install.sh
```

Require the existing line shape:

```text
INSTALL OK v3.9 stamp=<UTC> files='<installed file list>' — every fixture green against the installed bytes.
```

Then use the installed verbs. Any refusal or INSTALL NOT PROVEN is not INSTALL OK. Backups default to `/var/tmp/forge/ship-v3.9-fx/backups/<stamp>/`. The existing per-file atomic-rename installer remains a per-file installer, not a set-wide transactional swap. The staged directory and its tests must remain available while installing.

## Three least-sure choices

1. **Scale and retention:** the small Python writer rereads the ledger under its append lock to find sequence state, and fsyncs each event. Six writers × 200 events is witnessed; unbounded ledger growth, archival, and sustained production latency are not. This favors a simple auditable first slice over an unqualified throughput claim.
2. **Observation boundaries:** a stale heartbeat is UNKNOWN after three minutes, never completion. Await observes the exact supervisor before declaring missing terminal, allowing it to flush a child exit. That is child-lifecycle evidence, not proof that arbitrary daemonized descendants, deliverables, or obligations are complete. Resume timestamps are file-mtime observations, not an assertion that note prose is current.
3. **Coverage and admission:** ship owns the orchestration observations; standalone subverbs remain uncovered. Adapter witnesses cannot establish review/gate/publication correctness, and a pre-install process scan cannot eliminate every concurrent-start race during a multi-file installation. Full resource admission and the larger work protocol remain Block 2 work.

## Resource accounting exception

Early board witnesses attempted the optional real `maven-r inbox list` before I inspected its wrapper. Those bounded subprocess calls timed out. The wrapper was subsequently found to force its Clojure fallback; I did not verify `-J-Xmx1g` for those initial attempts, so I cannot claim that the entire build used no JVMs or that those attempts met the requested JVM flag. Their direct subprocesses were reaped by the deadline mechanism; descendant lifetime was not recorded by that first implementation. Further live probes were stopped, the known fallback is now explicitly skipped, and the final suite uses a local read-only stub. The current generic connector deadline owns and reaps its exact process group. No server or real reviewer was intentionally launched, and no installed script or records file was edited.

## Done proof: ledger-only reconstruction in a fresh shell

Actual refused launch, using the default global event ledger, with all run logs and the legacy ship ledger under the allowed fixture root:

```sh
RUN_BG_DIR=/var/tmp/forge/ship-v3.9-fx/done/runs \
SHIP_LEDGER=/var/tmp/forge/ship-v3.9-fx/done/ship.legacy \
SHIP_ROOT=/var/tmp/forge/ship-v3.9-fx/done/ship \
GIT_REPO=/var/tmp/forge/ship-v3.9-fx/done/no-repository \
SHIP_FENCE_DIR=/var/tmp/forge/ship-v3.9-fx/done/no-fence \
/var/tmp/forge/ship-v3.9/run-bg v39-dry-proof /var/tmp/forge/ship-v3.9/ship --help
```

```text
RUN name=v39-dry-proof pid=1474519 log=/var/tmp/forge/ship-v3.9-fx/done/runs/v39-dry-proof.log wait='while kill -0 1474519 2>/dev/null; do sleep 30; done'
{"run_id": "d42c1182-443d-41c3-a5ee-7ea64e171a45", "pid": 1474519, "pgid": 1474495, "log": "/var/tmp/forge/ship-v3.9-fx/done/runs/d42c1182-443d-41c3-a5ee-7ea64e171a45/output.log", "ledger": "/var/tmp/forge/ledger/events.jsonl"}
```

`await d42c1182-443d-41c3-a5ee-7ea64e171a45` found terminal exit 2. The outer run and ship have distinct run IDs and the same task ID. No candidate was sealed, no review ran, and nothing was landed.

Fresh-shell command (no RUN_LEDGER override, no resume note, no inherited task context):

```sh
env -i HOME=/home/forge PATH=/usr/local/bin:/home/forge/bin:/usr/bin:/bin \
  bash --noprofile --norc -c '/var/tmp/forge/ship-v3.9/board'
```

Unedited stdout:

```text
RUNNING
(none)
TERMINAL since 0
{"elapsed_ms":303,"producer":"ship/v3.9","run_id":"7b2ca406-0f1c-42e8-a928-e01487611f69","refusal_reason":"usage","tip":null,"outcome":"refused","state":"TERMINAL","landed_sha":"-","reason":"-","line":"SHIP status=UNVERIFIED run=- candidate=- base=- landed=- ship_elapsed=0s review_elapsed=unknown battery_elapsed=unknown gates_elapsed=unknown fastlane=unknown fastlane_elapsed=unknown fastlane_candidate=- gates=- attempts=0 fix_attempts=0 auto_closes=0 initial_candidate=- final_candidate=- fix_commits=- patch_producer=- independent_review=not-required reason=- events_hash=unknown ledger_ref=/var/tmp/forge/ship-v3.9-fx/done/ship.legacy request_to_landed=unknown","result":{"review_elapsed":"unknown","gates_elapsed":"unknown","fix_attempts":"0","independent_review":"not-required","run":"-","landed":"-","candidate":"-","patch_producer":"-","fastlane":"unknown","events_hash":"unknown","initial_candidate":"-","gates":"-","attempts":"0","final_candidate":"-","fastlane_elapsed":"unknown","ship_elapsed":"0s","auto_closes":"0","reason":"-","line":"SHIP status=UNVERIFIED run=- candidate=- base=- landed=- ship_elapsed=0s review_elapsed=unknown battery_elapsed=unknown gates_elapsed=unknown fastlane=unknown fastlane_elapsed=unknown fastlane_candidate=- gates=- attempts=0 fix_attempts=0 auto_closes=0 initial_candidate=- final_candidate=- fix_commits=- patch_producer=- independent_review=not-required reason=- events_hash=unknown ledger_ref=/var/tmp/forge/ship-v3.9-fx/done/ship.legacy request_to_landed=unknown","status":"UNVERIFIED","fix_commits":"-","battery_elapsed":"unknown","request_to_landed":"unknown","base":"-","ledger_ref":"/var/tmp/forge/ship-v3.9-fx/done/ship.legacy","fastlane_candidate":"-","exit_code":2},"last_kind":"terminal","task_id":"d42c1182-443d-41c3-a5ee-7ea64e171a45","seq":8}
{"elapsed_ms":359,"producer":"run-bg/v3.9","run_id":"d42c1182-443d-41c3-a5ee-7ea64e171a45","refusal_reason":null,"tip":null,"outcome":"red","state":"TERMINAL","landed_sha":null,"reason":null,"line":null,"result":{"error":"child-exited-nonzero","exit_code":2,"log":"/var/tmp/forge/ship-v3.9-fx/done/runs/d42c1182-443d-41c3-a5ee-7ea64e171a45/output.log","pgid":1474495,"pid":1474519},"last_kind":"terminal","task_id":"d42c1182-443d-41c3-a5ee-7ea64e171a45","seq":9}
UNKNOWN
(none)
OWED
(none)
INBOX imported read-only observation {"state":"skipped","reason":"installed maven-r forces JVM fallback; no-JVM status slice","observation_utc":"2026-09-11T13:04:46Z"}
as_of 2026-09-11T13:04:46Z cursor 9
COVERAGE BY PRODUCER
run-bg lifecycle/v3.9
ship orchestrated-path/v3.9 (fence-run, receipt-chain, land-auto observations owned by ship)
await reconciliation/v3.9
fence-run uncovered
receipt-chain uncovered
land-auto uncovered
land uncovered
records-push uncovered
independent-review uncovered
sol-yolo uncovered
suite-run uncovered
worktree-add uncovered
maven-r uncovered
build uncovered
measure uncovered
install.sh uncovered
OBSERVED PRODUCERS ["run-bg/v3.9","ship/v3.9"]
```

The board read the ledger, not the run log or resume note. The log is only an artifact pointer/digest in the already recorded terminal event. Raw proof artifacts and the executable proof script are under `/var/tmp/forge/ship-v3.9-fx/done/`.
