# Raw Clojure cohort v2 — prospective review draft

Status: **not admitted or launched**. Parent owns the final preregistration,
subject freeze, resource allocation and launch. This draft changes no prior
outcome. The aborted cohort and its runner remain at
`/var/tmp/forge/astra-raw-cohort-fx`; its six controls were 5 verified / 1 failed,
and one already-dispatched paired native verified before the stop. No fast-model
arm ran. See [the retained failure report](2026-09-06-astra-raw-cohort-control-failure.md)
and [the original preregistration](2026-09-06-astra-raw-cohort-prereg.md).

## Observable correction

A fresh continuation receives this phase transition, without a solution:

> Orientation is over. The earlier read-only instruction to reply READY applied
> only to orientation and no longer applies. The workspace has been reset to
> the original file. Complete the actual edit now exactly as specified in the
> task. Edit the file in place; any script or batched patch is allowed. Run the
> required verification and KEEP the verified diff in the workspace. Do not
> revert the edit to return to orientation. Reply DONE when finished.

The new [runner](../../bench/raw-cohort-v2/run.py) loads the pinned native launcher
in a fresh Python process, changes only `WARM_TRIAL_PROMPT`, then invokes its
existing native main. Its source hash and exact prompt bytes join the freeze.
The model, effort, orientation, task, proof and judging logic are unchanged:
native gpt-5.6-sol/medium, independent forks of orientation
01a07465-7a8c-7423-b598-cbac0681c1f5, fresh real1 preimage. This is a new prompt epoch;
old rows cannot be pooled as unchanged controls. The added wrapper startup is
included in native command wall, not subtracted.

## Fixed sequence and decisions

Exactly six native control attempts C1–C6, then four pairs in order N,T / T,N /
N,T / T,N: 14 command attempts, with at most 12 fast-provider requests from k3.
A genuine wrong answer, reverted/no diff result, or failed candidate acceptance
is retained as incorrect with its terminal wall. There are no replacement controls
and no retries to obtain six successes. Even zero verified controls would not by
itself convert those completed observations into apparatus faults; any such result
requires explicit limitations in the report, not retrospective replacement.

Immediately after C6 and **before N1 or any T**, persist the six-control floor:
all six terminal walls, median, sample SD, correctness numerator/denominator, and
whether median minus the preregistered 10-second tool prediction exceeds 2 SD.
That last flag limits the strength of latency claims; it does not change trial
count or skip the already specified functional comparison. Do not use only
successful controls to calculate the admission floor.

Stop dispatch for identity/fixture/quiet-ownership faults, missing or malformed
terminal evidence, typed provider transport/service failure requiring review, or an external
watchdog with no completed receipt. Retain the fault row and every prior row.
A nonzero command exit with an intact, correctly bound terminal outcome is not
by itself a reason to discard a real model failure. In particular, the retained
old C6 has a correctly bound NW/real1/Sol receipt, no transport error/refusal,
`first_verified_s=nil`, and a six-character READY response with no diff; that is
an incorrect native result, not missing capture.

## Scope, predictions and clocks retained

This remains the supplied-decision real1 prepared-owner comparison of combined
raw representation plus k3 against context-warm/process-cold native. It does not
identify representation's causal effect alone, discovery, or general adoption.
The transferred JSON 3/4 prior fixes k3 before outcomes. The earlier JSON cohort
remains a reliability loss. Prediction remains raw 4/4 verified, median complete
command wall below 10 seconds, nominal ratio at least 1.5. Any raw failure defeats
the correctness prediction; terminal latency alone is not an equal-reliability
win. Report both all-terminal latency and successful-task latency with explicit
populations, and all correctness denominators. Do not convert failures to free
latency or cancelled unknown usage to zero cost.

Both public-command clocks include process startup through command exit. Tool
includes proposal, saved ledger, generation, formatting, proof and commit; native
includes wrapper/launcher, model/self-check and independent judge. Materialization,
intent preparation and review are excluded on both sides. The original 10.618s
orientation is separately disclosed; this continuation does not charge it again.
Retain every candidate/proof/token/cost record. Any replay occurs after timing and
never rewrites the winner. No new measured number or speed claim is supplied by
this draft or its offline tests.

## Parent-owned prelaunch work

The new default fixture root is `/var/tmp/forge/astra-raw-cohort-v2-fx` and must not
exist before preparation. `prepare` may materialize only fresh fixtures and
freeze the current subject; it has not been run in this task. Root must approve
and bind final engine/transport/source/spec/proof/model/CLI/session identities,
verify fixture bytes/modes and actual warm-workspace ownership/quiescence, clear
live processes and allocate the quiet window. The old runner's hardcoded dead PID
848070 is not carried forward as evidence of current quiescence. The prospective
runner checks owned quiet marker, clean tracked tree/HEAD and frozen hashes, but
these checks do not replace the parent's full allocation/identity review. Its
watchdog retains the inherited own-process-group cleanup; detached-child cleanup
needs parent review before launch.
Native identity is now checked against the actual opening CLI stderr header,
not solely the inherited receipt's declared model. The frozen expected fields
are Codex 0.153.3, Sol, medium effort, OpenAI provider, approved sandbox settings,
and the fixed warm workspace. The fork UUID must differ from the orientation UUID
and must not repeat in this cohort. Save the resolved header values, fork UUID,
header hash and whole-capture hash. Parsing requires one exact stderr delimiter
and the opening header; later model text cannot repair a mismatched header.
This is CLI-reported identity, not cryptographic proof of a remote backend.

The final parent-held `--frozen-sha` binds the materialized manifest. Preparation
and run entry points are deliberately separate. No fixture was prepared, no model
or provider called, and no timing window allocated for this apparatus repair.

## Offline witnesses

Eleven tests in [test_run.py](../../bench/raw-cohort-v2/test_run.py) pass: the old
sixth-control failure shape remains incorrect rather than a capture fault; six
fixed attempts retain failures; identity faults save then stop; floor persistence
happens before any paired dispatch; failure to save it prevents the first pair;
and a fake native entry receives the new phase prompt and unchanged native args.
New prelaunch witnesses parse the actual old C6 capture and EDN receipt: its
opening header is valid and its edit remains incorrect. The actual retained raw
handdrive's completed transport and proof receipt passes the evidence validator.
Negative witnesses cover wrong version/model/effort/workspace, duplicate fields,
missing/ambiguous header delimiter, later quotation, file bytes/modes/additions
and symlinks, and the fault-policy distinctions below. Small throwaway test
fixtures under /var/tmp/forge are removed; no cohort fixture is materialized.
These tests make bounded BB EDN-parser subprocess calls over retained artifacts,
not proof executions. They are not synthetic performance evidence.

```sh
CLJ_SURGEON_EVENTS_FILE=/var/tmp/forge/raw-cohort-v2-prep/events.jsonl \
PYTHONDONTWRITEBYTECODE=1 nice -n 10 python3 bench/raw-cohort-v2/test_run.py
```


## Frozen fixture and terminal-evidence policy

Before each T command starts, inventory the actual workspace and match the frozen
seed exactly: file set, SHA256 and POSIX mode of every regular file, directory set
and modes. No symlinks or special files are admitted. Prepared files use0644 and
directories0755, recorded in the manifest. A changed preimage stops before dispatch.
Post-command protected-byte checks remain part of correctness; a generic failed
gate does not prove the apparatus is broken.

| Observed evidence | Prospective classification |
|---|---|
| Native correctly bound receipt, no transport error, no diff/wrong edit/failed acceptance | Incorrect attempt; retain and continue fixed sequence |
| Tool owner/compiler refusal with valid completed transport | Incorrect candidate/attempt; no replacement |
| Attested model content refusal, length limit, empty content or nonterminal model output | Incorrect candidate/attempt; no provider infrastructure claim |
| Completed proof command nonzero, or bounded candidate proof timeout with terminal result fields | Candidate proof failure; do not infer broken infrastructure |
| Typed API/service/network/key/response error, identity mismatch, unrecognized transport error | Fault requiring review; retain row then pause, without guessing root cause |
| Accepted cancelled loser also present in completed | Retain cancellation/unknown usage; not a successful or free request |
| Missing/malformed candidate or proof results, unconfirmed transport cleanup | Apparatus/evidence fault; retain then pause |

The tool validator reads the actual owned `transport-close.edn`, confirms bounded
candidate indices and provider identity, and checks terminal proof record shapes.
It does not infer infrastructure failure from `:ok false`, a nonzero proof exit,
or `:finished? false` alone. Compiled candidates require their gate results, and
acceptance results when the gate passed. A compiled=false candidate does not
require a proof that was never run. Evidence-path confinement and read-size
bounds apply before parsing. All raw receipts and capture paths are retained.
