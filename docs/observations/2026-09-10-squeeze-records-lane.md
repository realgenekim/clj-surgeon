# The last squeeze — records publication independent of the code ship lease (ship v3.8 staged)

finding `SQUEEZE-RECORDS-REF-LEASE-001` · builder/recorder: forge@anvil Opus one-shot ·
spec: Astra's consult `/home/forge/src/clj-surgeon-records/docs/observations/2026-09-10-astra-squeeze.md`
· workflow: tighten-the-loop (SKILL.md + CHANGE-RULE.md) · steward: Astra · supervisor: forge@anvil

**Block clock: 00:00 = 2026-09-10T03:33:10Z. Four-hour box closes 07:33:10Z. This file written 2026-09-10T07:05:49Z — parked.**

## Headline

| acceptance criterion (Astra) | required | measured | verdict |
|---|---|---:|---|
| zero **code-lease** wait | 0 s | 0 s in every treatment arm (`waited_lease=0`), and no arm read the code lease | **met** |
| request -> independently confirmed visibility on the registered LOCAL fixture origin | <= 30 s | 0.142 s (worst of 3) | **met** |
| improvement in the matched 660-s-lease comparison | median delta >= max(600 s, 2*s0) = 600.000 s | **660.229 s** | **met** |
| correctness | no regression; every negative still refused | 12/12 matrix rows, `run-ship-v3` 22/22, 0 code-ref violations | **met** |
| ship acceleration | **must not be claimed** | 0 s claimed, 0 s measured | **honoured** |

**660.2 seconds removed from a records publication that is blocked at the observed median, measured request-to-independently-confirmed-visibility on a local fixture origin. Old median 660.371 s -> new median 0.136 s. Control population n=6, median 660.417 s, SD 0.046 s.**

**This is a local-fixture win.** Real transfer is `transfer=untested`: this seat may not install, may not publish to the real remote, and does not merge. The network term is bounded separately at roughly 1.5-3 s. No ship acceleration is claimed and none was measured.

**Delivery is blocked by something else.** `bash /var/tmp/forge/ship-v3.8/install.sh` prints `INSTALL NOT PROVEN` -- with byte-identical mismatch counts to a v3.7 baseline control, i.e. inherited and not caused by this change. The same records change on the *installed* epoch produces `INSTALL OK v3.8` over 228 fixture rows with 0 mismatches.

## Block table (Astra's 00:00–04:00 rows)

| Block time | UTC | Deliverable | State |
|---|---|---|---|
| 00:00–00:20 | 03:33–03:53 | bind owners/scope, preserve installed bytes + triggering logs, seal cases and acceptance, execute the faithful red | **done 03:35:33Z.** Red retained verbatim (exit 4, `HELD reason=ship-lease-live`, blob absent remotely). Payload, frozen v3.7 helper and seal time sealed 03:34:26Z. Drift on all seven of Astra's frozen inputs: **zero**. |
| 00:20–01:30 | 03:53–05:03 | recorder runs six old controls (sequential, frozen executable and fixtures) while the builder repairs only the publication entrance and its witnesses | **done.** Controls 03:35:42Z→04:41:46Z, 6/6, median 660.417 s, SD 0.046 s. Candidate frozen 03:46:59Z. Matrix 12/12 and `run-ship-v3` 22/22 green by 03:44Z. Three installer runs executed beside the controls; no control's frozen executable or fixture was touched. |
| 01:30–02:15 | 05:03–05:48 | three matched pairs (old/new, new/old, old/new) + an idle-lease pair | **done 04:42:05Z→05:15:02Z**, chained to the controls' exact PID (`kill -0`, never a `pgrep -f` pattern). Δ = 660.298 / 660.229 / 660.207 s; idle-lease overhead +0.051 s. |
| 02:15–03:15 | 05:48–06:48 | independent acceptance the installer's way against a scratch DEST + FIXTURES; boundary and delivery-copy checks | **done.** Run A (candidate as staged) `INSTALL NOT PROVEN`; run B (v3.7 baseline) byte-identical mismatch counts; run C (records change on the installed epoch) **`INSTALL OK v3.8`**, 228 rows, 0 mismatches, real lease free. Delivered bytes then re-exercised at the consumer. Delivery-copy check: P1, P2. |
| 03:15–04:00 | 06:48–07:33 | result, manifest, block close, readback | **done.** Field-lease observer ran read-only 05:25:30Z→block close and saw no further real ship. Final report, terminal receipt and manifest written; installed bytes re-verified unchanged at close. |

## What was asked, and what the number to beat is

Astra's consult picked candidate **(e)**: make a records-only publication to `records/MCP-main`
independent of the **code** ship lease. The wait loop in `records-push` was written when records and
code advanced the *same* ref (a records push rejected a green landing at 21:2xZ on 2026-09-08).
Gene's 2026-09-09 01:34Z **decision 1a** separated the refs. The loop stayed.

**Number to beat: 660 s median reported lease wait** across the 21 retained terminal
`RECORDS-PUSH` rows in `/var/tmp/forge/run-bg/records-push-*.log` (range 0–1,500 s; 20 of 21
positive). Acceptance: zero code-lease wait; ≤30 s request→independently-confirmed visibility on a
registered LOCAL fixture origin; ≥600 s improvement in a matched 660-s-lease comparison.

**No ship acceleration is claimed. Expected reduction in the historical 594-s code ship: 0 s.**

## Drift check on Astra's frozen inputs — zero

Astra required the builder to recheck drift before using the consult's hashes as the old epoch.
All seven, re-verified this block (`/var/tmp/forge/squeeze/out/drift-check.txt`):

```
/home/forge/bin/records-push: OK      /home/forge/bin/ship: OK        /home/forge/bin/fence-run: OK
/home/forge/bin/seat-receipt: OK      /home/forge/bin/canary-cell: OK /home/forge/bin/verb-sentinel: OK
/home/forge/bin/tighten: OK
```

The old epoch is the installed epoch, unchanged since the consult.

## The faithful red — the installed bytes, a held lease, before any code was written

```
label=RED-v37-heldlease  helper=/var/tmp/forge/squeeze/seal/records-push.v3.7.frozen
helper_sha256=55e25d68b3300df8e0931b99d7dd7d992ee122c5df1c825f91d81075933d4907   (== /home/forge/bin/records-push)
lease_life_s=20 poll=1 cap=3
exit=4  visibility=unconfirmed  code_refs_moved=no
receipt=RECORDS-PUSH HELD reason=ship-lease-live waited=3s (records NOT pushed; rerun later)
utc=2026-09-10T03:35:33Z
```

Exactly Astra's predicted old failure: exit 4, `HELD reason=ship-lease-live`, the intended blob
**absent** from the remote records ref, and the two code refs unmoved. A real live helper process
owned a `pid=<PID>` lease record in the exact shape the installed script reads; no production lease
and no real remote were touched. The 3-second cap makes the behavioural red cheap — it is **not**
the performance measurement.

## The one change

`/var/tmp/forge/ship-v3.8/records-push`, sha256 `693239e0c4b55d3b234606b84bb3125660a59f987143fff9af46b36932ea39b6`,
frozen 2026-09-10T03:46:59Z. Staged only. **Nothing under `~/bin` was edited and the installer was
never run against the real DEST.**

| Old (v3.7) | New (v3.8) |
|---|---|
| waits while any file in the **code** ship lease dir names a live pid (median 660 s) | never reads the code lease; `waited_lease=0`, always |
| `RECORDS_DEST` is an unchecked override | destination **bound** to `records/MCP-main`; anything else → `REFUSED reason=forbidden-destination` (exit 7) |
| publishes from any branch | source **bound**: not on `records/MCP-main` → `REFUSED reason=wrong-branch` (exit 6) |
| — | pending publication touching `src/` → `REFUSED reason=src-changed-on-records-lane` (exit 8) |
| two publishers from one checkout race (inb-78d458) | `flock` on `<git-dir>/records-push.lock`, `waited_lock` measured (exit 9 on timeout) |
| one push; a rejection is terminal | rebase-and-retry up to 4 attempts; never a force push |
| `exit 0` from `git push` is treated as publication | **readback required**: the tip must be reachable from the remote records ref via `git ls-remote`/`git fetch`, else `UNVERIFIED reason=tip-not-visible` (exit 10) |
| `RECORDS-PUSH OK tip=<sha> waited=<s>` | `RECORDS-PUSH OK tip=<sha> waited_lock=<s> waited_lease=0 visible=<s>` |

### One deliberate deviation from the brief's wording, with its evidence

The brief says the entrance should refuse a checkout "whose `src/` differs from **trunk**". Measured
on the real records checkout, read-only, right now:

```
git diff --name-only origin/MCP/main HEAD -- src/            -> 6 files
git diff --name-only origin/records/MCP-main..HEAD -- src/   -> 0 files
```

An equality-against-trunk test **refuses every legitimate records publication**, because
`records/MCP-main` lawfully lags and diverges from `MCP/main`. Astra's contract also forbids
"reinterpret[ing] the historical records ancestry as a new patch" and requires validating "only the
pending publication". So the guard is implemented as *the pending publication may not touch `src/`*
— the ref-scoped reading — and row 08 of the matrix proves it refuses a sneaked `src/` change.

Second wording note: the brief calls these "the existing guards". They did **not** exist. The
installed v3.7 helper is 1,487 bytes and carries no branch check, no destination check and no `src/`
check; `RECORDS_DEST=MCP/main` published records onto the **code** ref with `RECORDS-PUSH OK` (row
07's old-arm comparison, recorded below). They are new guards in v3.8.

## The matrix — real git, private bare origin, 12/12

`/var/tmp/forge/ship-v3.8/fixtures/run-records-push-v3.8.sh`
(sha256 `3945cdf10ca0c840d2e4847d951bea10c68e2b111793b63f7d8b1c4c4f744b1e`).
Every row builds a fresh bare origin with three **distinct** refs (`main`, `MCP/main`,
`records/MCP-main`), a records checkout with one pending record commit, an **independent** consumer
clone, and a private lease dir. No fake `git`: the ref, race and readback rows are only decisive
against real git.

```
PASS  01-ordinary-publication-succeeds-and-the-receipt-carries-all-four-fields
PASS  02-a-LIVE-code-ship-lease-does-not-hold-the-records-lane(waited_lease=0)
PASS  02b-the-OLD-helper-still-HELDs-on-the-same-fixture(the-red-is-faithful)
PASS  03-a-stale-lease-dead-pid-does-not-block-the-records-lane
PASS  04-two-concurrent-publishers-from-one-checkout-serialize-both-land-no-REFUSED
PASS  05-a-remote-that-moved-is-rebased-onto-and-retried-both-records-survive
PASS  06-a-checkout-that-is-not-on-records/MCP-main-is-refused
PASS  07-RECORDS_DEST-cannot-silently-widen-the-boundary-to-a-code-ref
PASS  08-a-pending-publication-that-touches-src/-is-refused
PASS  09-a-rebase-conflict-is-a-typed-refusal-not-a-push
PASS  10-a-push-the-remote-did-not-keep-is-UNVERIFIED-never-OK
PASS  11-idle-lease-pair-the-candidate-adds-no-material-overhead
records-push v3.8 fixtures: 12 rows, mismatches: 0
```

Old/new on the **same** fixtures (the old arm is recorded, not asserted — it is the defect):

| case | old (v3.7, installed bytes) | new (v3.8) |
|---|---|---|
| live code lease | `RECORDS-PUSH HELD reason=ship-lease-live waited=6s`, 6 s, **not published** | `RECORDS-PUSH OK … waited_lease=0 visible=0.0s`, 0 s, published |
| two publishers, one checkout | `fatal: Cannot rebase onto multiple branches.` **on both** | both `OK`, same tip, `waited_lock=2.8s` / `2.7s`, no REFUSED |
| `RECORDS_DEST=MCP/main` | `RECORDS-PUSH OK` — **the code ref moved** | `REFUSED reason=forbidden-destination`, code ref unmoved |
| remote keeps nothing (post-receive rewinds) | `RECORDS-PUSH OK` — a false green | `UNVERIFIED reason=tip-not-visible` |
| idle lease | 74–126 ms | 136–166 ms |

The one-checkout race row is worth calling out: the old helper did not merely mis-report, it left
`git rebase` in `Cannot rebase onto multiple branches` — two concurrent `git pull --rebase` in one
worktree. inb-78d458's `REFUSED push-rejected` is one face of that; this is another.

**Row 10 is a class the brief did not ask for and the old entrance failed.** A remote that accepts a
push and does not keep the ref returned `RECORDS-PUSH OK` from v3.7. Astra's "no success on a
missing readback" is now enforced.

### The one changed verdict, with its intent anchor

`run-ship-v3.sh` row 20 read `20-records-push-WAITS-on-a-live-ship-lease-then-pushes` and asserted
`rpel >= 3`. It now reads `20-a-live-ship-lease-does-NOT-hold-the-records-lane(waited_lease=0)`.
**Intent anchor: Gene's 2026-09-09 01:34Z decision 1a** (the records lane has its own ref; `MCP/main`
is code only), recorded in the fixture's own header. Rows 21 (stale lease) and 22 (land-auto's own
records-readable lease) are unchanged and still green. Full run against the staged v3.8:
`ship-v3 fixtures: 22 rows, mismatches: 0`.

That fixture's records repo was also corrected: it published to `records/MCP-main` from a local
branch called `trunk`, which no longer models the real caller now that the source branch is bound.

The changed verdict is a **paired substitution**, not a weakened assertion. Verified by hash across
the three installer runs: the baseline ran the **old** fixture `5c822fb2…` against the **old** helper
`55e25d68…` (22/22); the candidate and minimal runs ran the **new** fixture `cfb100d1…` against the
**new** helper `693239e0…` (22/22 each). Neither epoch was run against the other's fixture, and
neither epoch was excused a row.

## A field observation that fell into the block: a real code lease, live, today

At 04:12:20Z the installer refused the acceptance run — `INSTALL REFUSED reason=ship-lease-live
pid=1844683`. A real ship (`run=20260910T040304Z-3585c7e59880`, base `3ea3803e`, tip `3585c7e5`) was
in flight. Read-only watch (`/var/tmp/forge/squeeze/out/lease-watch.log`):

```
FIELD-LEASE observed_at=2026-09-10T04:12:36Z  run=20260910T040304Z-3585c7e59880 pid=1844683
FIELD-LEASE released_at=2026-09-10T04:14:31Z observed_live_s=115
```

The lease's own run stamp is `20260910T040304Z`; it released at `04:14:31Z`. **That is ~687 s of
live code lease, on an ordinary ship, today** — the term a records publication requested at that
moment would have paid under the installed v3.7 entrance, and within noise of the 660-s retained
median. 115 s of it were directly observed by this watcher; the rest is inferred from the lease
record's own stamp, so the 687 s is stated as an inference and the 115 s as the direct observation.
Nothing was published and the real lease was never written.

A read-only observer then watched `/var/tmp/forge/ship/.lease` continuously from 05:25:30Z to the
end of the block (`/var/tmp/forge/squeeze/out/lease-observer.log`). **No further real ship lease
appeared in that window**, so the field population for this block is exactly one: the
`20260910T040304Z-3585c7e59880` ship. One observation is an observation, not a distribution — it is
reported as a single corroborating data point beside the 21 retained rows, not as a new median.

## Delivery: v3.8 as staged does NOT install — and the reason is not the records change

Running the installer the installer's way against a **scratch** DEST (never `~/bin`):

| SRC | run-land-auto | run-land-pub-truth | v2 | v3 | v3.1 | **v3.2** | v3.3 | v3.4 | **v3.5** | **v3.6** | **v3.7** | v3.8-records | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `/var/tmp/forge/ship-v3.7` (baseline) | 0 | 0 | 0 | 0 | 0 | **4** | 0 | 0 | **2** | **3** | **1** | n/a | `INSTALL NOT PROVEN` |
| `/var/tmp/forge/ship-v3.8` (as staged) | 0 | 0 | 0 | 0 | 0 | **4** | 0 | 0 | **2** | **3** | **1** | **0** (12 rows) | `INSTALL NOT PROVEN` |
| `min-src` = v3.8's records change on the **installed** epoch | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **`INSTALL OK v3.8`** |

**The mismatch set is byte-identical between the v3.7 baseline and v3.8-as-staged.** The records
change adds one fixture (12/12 green) and changes one verdict in `run-ship-v3` (22/22 green in both
runs, old fixture + old helper in the baseline, new fixture + new helper in the candidate). It
breaks nothing.

The cause is a **pre-existing, uninstalled v3.7 remainder**, found by comparing installed bytes with
the staged tree:

```
ship, land, fence-run, receipt-chain, land-auto, records-push, run-bg   installed == ship-v3.7  SAME
gate-envelope.clj  installed=034b5afed6023b29  staged=30ef0d5128c4700d  DIFFERS
gate-consume.clj   installed=6448222daab675d9  staged=a2a0b0a0799cbef7  DIFFERS
mutate.clj         installed=cb28ac1a416bdee4  staged=954e32acfba73a5a  DIFFERS
```

plus five fixture files (`envelope-green.edn`, `gen-field-mutants.clj`, `inventory-3ea3803e.edn`,
`run-ship-v3.7.sh`, `stub-gate.clj`) staged at 17:18–17:53Z on 2026-09-09, later than the last
install's canonical copies (16:11–16:30Z). **`ship-v3.7` was staged past its last successful
install, and its own installer refuses it.** v3.8 inherited that. The v3.5 rows fail on
`reason=candidate-mismatch` over a *tree* sha — a gate-consume/envelope epoch question with nothing
to do with records publication.

**The INSTALL OK line**, from the minimal composition (records change on the installed epoch), with
the real lease free:

```
INSTALL OK v3.8 stamp=20260910T041431Z files='ship land fence-run receipt-chain land-auto records-push ship-fix-block-spec.md run-bg sol-yolo gate-envelope.clj gate-consume.clj mutate.clj' — every fixture green against the installed bytes.
```

228 fixture rows across 12 fixture sets, 0 mismatches, against the bytes in the scratch DEST.

`/var/tmp/forge/ship-v3.8` was deliberately **left faithful** — v3.7's staged tree plus the records
change — rather than reverted to the installed epoch, because the uninstalled `.clj`/fixture
remainder is another builder's in-flight work and is not this brief's to discard. The proven
composition is retained, unmodified, at `/var/tmp/forge/squeeze/min-src/` for the owner.

## Delivery proved at the consumer, not at the writer

The bytes that produced `INSTALL OK` were then exercised **as installed** (from
`/var/tmp/forge/squeeze/min-dest/`, not from the staging tree), through the same recorder harness:

```
label=DELIVERED-heldlease helper=/var/tmp/forge/squeeze/min-dest/records-push helper_sha=693239e0c4b55d3b
lease_alive_at_end=yes  t0_to_t1_s=0.149  exit=0  visibility=confirmed  reported_lease_wait=0  code_refs_moved=no
receipt=RECORDS-PUSH OK tip=c47858d waited_lock=0.0s waited_lease=0 visible=0.0s

label=DELIVERED-race  mode=concurrent2 (two publishers, ONE checkout)
lease_alive_at_end=yes  t0_to_t1_s=0.214  exit=0 exit_b=0  visibility=confirmed  code_refs_moved=no
receipt  =RECORDS-PUSH OK tip=6b37a4d waited_lock=0.1s waited_lease=0 visible=0.0s
receipt_b=RECORDS-PUSH OK tip=6b37a4d waited_lock=0.0s waited_lease=0 visible=0.0s
```

`lease_alive_at_end=yes` is Astra's predeclared invariant, satisfied: **the record became visible on
the records ref while the other-ref lease was still live, and the two code refs did not move.**

The destination guard was then exercised against the **real** records checkout, with zero side
effects (the destination is validated before the helper even `cd`s into the repository):

```
$ RECORDS_REPO=/home/forge/src/clj-surgeon-records RECORDS_DEST=main       records-push
RECORDS-PUSH REFUSED reason=forbidden-destination dest=main approved=records/MCP-main        exit=7
$ RECORDS_REPO=/home/forge/src/clj-surgeon-records RECORDS_DEST=MCP/main   records-push
RECORDS-PUSH REFUSED reason=forbidden-destination dest=MCP/main approved=records/MCP-main    exit=7
```

The real checkout after both probes: `branch=records/MCP-main head=43337e51`, working tree carrying
only another worker's pre-existing untracked report, which was left alone. Nothing was committed,
nothing was pushed, no lock file was created (the refusal precedes the lock).

**Limitation, disclosed as Astra required:** the independent readback fixture is a different process
under the *same account*. It measures observable functionality; it does **not** establish
inaccessible observer custody. No v0→v1 seat-receipt upgrade and no resumption of evidence
consumption is claimed on this experiment's strength.

## The linked intent this fix registers

Per the seat's standing rule that a fix introducing a requirement registers a linked intent whose
witnesses fail first, the requirement is stated here and bound to the rows that already enforce it.
It is **not** written into `records-push` or the fixture as an `INTENT:` marker in this block,
because both are frozen artifacts of the measured candidate and the three acceptance runs; editing
either would invalidate the timing receipt. Registering the marker is the first task of whoever
installs.

```
INTENT SQUEEZE-RECORDS-REF-LEASE-001
  WHEN a records publication to refs/heads/records/MCP-main is requested
  WHILE a code ship lease names a live pid
  THE records entrance SHALL publish without waiting on that lease (waited_lease=0)
  AND SHALL NOT report success until the pushed tip is reachable from the remote records ref
  AND SHALL refuse any destination other than records/MCP-main,
      any source branch other than records/MCP-main,
      and any pending publication that touches src/.
```

| clause | witness that fails first | file |
|---|---|---|
| publishes under a live lease, `waited_lease=0` | row 02 (+ row 02b: the old helper still `HELD`s on the same fixture) | `run-records-push-v3.8.sh` |
| same, at the ship/land integration seam | row 20, the changed verdict | `run-ship-v3.sh` |
| readback before success | row 10 (post-receive rewinds the ref; old arm returned `OK`) | `run-records-push-v3.8.sh` |
| destination bound | row 07 (old arm moved a **code** ref) | `run-records-push-v3.8.sh` |
| source branch bound | row 06 | `run-records-push-v3.8.sh` |
| no `src/` in the pending publication | row 08 | `run-records-push-v3.8.sh` |
| one checkout, one publisher at a time | row 04 (external lock held 3 s; both arms must show `waited_lock>=2s`) | `run-records-push-v3.8.sh` |

Every one of these was observed **red on the old epoch first**, on the same fixture, before the new
epoch was asserted green — rows 02b, 04o, 07o and 10o carry the old arm's verbatim output.

## Measurement — before / after, both recorded

Same host (`anvil-server`, 16 cores), same filesystem (`/dev/sda1` ext4 under `/var/tmp/forge`),
`git version 2.53.0`, same sealed payload
(`a941e008a681059932a076130d07133335ec780f0b039a7a9e238d521ae82481`), same fixture generator
(`/var/tmp/forge/squeeze/harness.sh`, `bd9fec79da704da3a085ddc3b6a472ee58b91d7ee66fe9a4abe582fd068f02ba`).
Every arm gets a **fresh** fixture and its **own** live 660-second lease holder, production defaults
(poll 30 s, cap 1,500 s). `t0` is released **before** helper startup, lock acquisition, refresh or
rebase; `t1` is an **independent consumer clone** confirming the intended blob hash is reachable
from the remote records ref, with the two code refs unmoved. Helper terminal time is recorded
separately; `waited=0`, exit 0 and a git push message are never `t1`.

### Six old controls (sequential, fresh fixture each)

| control | t0→t1 (s) | helper terminal (s) | reported lease wait (s) | visibility | code refs moved |
|---|---:|---:|---:|---|---|
| CTL-old-1 | 660.403 | 660.363 | 660 | confirmed | no |
| CTL-old-2 | 660.376 | 660.343 | 660 | confirmed | no |
| CTL-old-3 | 660.451 | 660.422 | 660 | confirmed | no |
| CTL-old-4 | 660.336 | 660.306 | 660 | confirmed | no |
| CTL-old-5 | 660.430 | 660.394 | 660 | confirmed | no |
| CTL-old-6 | 660.451 | 660.389 | 660 | confirmed | no |

`control_n=6 control_median_s=660.417 control_range_s=660.336–660.451 control_sd_s=0.046`

### Three matched pairs (order old/new, new/old, old/new), candidate frozen first

| pair | arm | t0→t1 (s) | reported lease wait (s) | visibility | code refs moved | receipt |
|---|---|---:|---:|---|---|---|
| PAIR1 | old | 660.433 | 660 | confirmed | no | `RECORDS-PUSH OK tip=b26c62f waited=660s` |
| PAIR1 | new | 0.135 | 0 | confirmed | no | `RECORDS-PUSH OK tip=9adb523 waited_lock=0.0s waited_lease=0 visible=0.0s` |
| PAIR2 | new | 0.142 | 0 | confirmed | no | `RECORDS-PUSH OK tip=adcccb0 waited_lock=0.0s waited_lease=0 visible=0.0s` |
| PAIR2 | old | 660.371 | 660 | confirmed | no | `RECORDS-PUSH OK tip=92017ac waited=660s` |
| PAIR3 | old | 660.343 | 660 | confirmed | no | `RECORDS-PUSH OK tip=7541fa3 waited=660s` |
| PAIR3 | new | 0.136 | 0 | confirmed | no | `RECORDS-PUSH OK tip=7a7640a waited_lock=0.0s waited_lease=0 visible=0.0s` |
| IDLE | old | 0.091 | 0 | confirmed | no | `RECORDS-PUSH OK tip=636c04d waited=0s` |
| IDLE | new | 0.142 | 0 | confirmed | no | `RECORDS-PUSH OK tip=d17fefb waited_lock=0.0s waited_lease=0 visible=0.0s` |

`pair1: T_old=660.433s T_new=0.135s delta=660.298s`

`pair2: T_old=660.371s T_new=0.142s delta=660.229s`

`pair3: T_old=660.343s T_new=0.136s delta=660.207s`

`pairs_n=3 old_median_s=660.371 new_median_s=0.136 delta_median_s=660.229 delta_range_s=660.207-660.298`

`idle_lease_control_s=0.091 idle_lease_treatment_s=0.142 added_overhead_s=0.051`

### The real-network term, measured separately (read-only, nothing published)

Against the real `origin` of `/home/forge/src/clj-surgeon-records` (github.com/realgenekim/clj-surgeon),
2026-09-10T03:46Z: `git ls-remote` **0.512 / 0.517 / 0.458 s**, `git fetch` of the records ref
**0.489 s**. A real publication is three such round trips (rebase-pull, push, ls-remote readback), so
expect roughly **1.5–3 s of network** on top of the local-transport fixture numbers above. Fixture
performance is scoped to local git transport; the later actual remote run is the network/delivery
measurement and has **not** been performed in this block.

## Findings this block produced that were not in the brief

1. **`RECORDS_DEST=MCP/main` published records onto the CODE ref, with `RECORDS-PUSH OK`.** Measured,
   row 07's old arm. The v3.7 override is unchecked. This is the exact class decision 1a forbids.
2. **A remote that accepts a push and does not keep the ref returned `RECORDS-PUSH OK`.** Row 10's
   old arm. Exit 0 from `git push` was being treated as publication.
3. **Two concurrent v3.7 publishers in one checkout do not merely mis-report — they break `git
   rebase`** (`fatal: Cannot rebase onto multiple branches.` on *both* arms). inb-78d458's
   `REFUSED push-rejected` is one face of a wider concurrency defect in that entrance.
4. **The literal "src/ differs from trunk" guard would refuse every real records publication** (6
   files differ against `origin/MCP/main` on the real checkout right now; 0 in the pending delta).
5. **The brief's "the existing guards" did not exist.** v3.7 is 1,487 bytes with no branch,
   destination or `src/` check.

## Pending obligations — preserved, not closed

| # | Obligation | Owner | Why it is not closed here |
|---|---|---|---|
| P1 | `/home/forge/bin/seat-receipt:236-237`, `/home/forge/bin/canary-cell:133-134` and `/home/forge/bin/verb-sentinel:93-94` publish records-lane commits with `git push origin HEAD:MCP/main` — **straight onto the code ref**, bypassing this entrance entirely. `/home/forge/bin/tighten:150-151` advertises `:ref "records/MCP-main -> origin MCP/main" :push "HEAD:MCP/main"`. Installed hashes re-verified this block and **unchanged** from Astra's frozen table. | instrument owner (Astra / mayor) | `~/bin` is explicitly outside this brief's write set. Astra's instruction is to *preserve the finding for the instrument owner*. Named here with line numbers so the repair is a one-shot, not a re-investigation. |
| P2 | `records-push` is **not** in the tighten bundle (`/home/forge/src/claude-skills-nrepl/tighten-the-loop/bin/MANIFEST.txt` lists eight files; this is not one of them). If P1's callers are routed through this entrance they will depend on a helper the bundle does not deliver. | bundle owner | Editing a package copy is not installed uptake, and the bundle is not owned by this brief. |
| P3 | **`transfer=untested`.** Astra's step 6 — one already-useful records publication through the *installed* entrance while a real code ship is active, with an independent consumer recording request-to-visibility. | owner after qualification | This brief forbids installing, forbids the real GitHub remote in fixtures, and nothing merges from this seat. Manufacturing a ship for a favourable clock is explicitly forbidden. The real-network term is bounded separately above (≈1.5–3 s) and is **not** a substitute. |
| P4 | Independent qualification. The builder cannot issue its own authoritative qualification (SEAT-RECEIPT). | a verifier bound through the owner | The acceptance run below is the *installer's own* re-verification against a scratch DEST, which is evidence, not authority. |
| P5 | The per-checkout lock is per **checkout** (`<git-dir>/records-push.lock`), as briefed. Two *different* worktrees of the same records repository still do not serialize against each other. | — | Out of the briefed scope; recorded so nobody reads row 04 as a repository-wide guarantee. |
| P6 | **`/var/tmp/forge/ship-v3.7` was staged past its last successful install and its own installer refuses it** (`gate-envelope.clj`, `gate-consume.clj`, `mutate.clj` and five fixture files are newer than the installed/canonical copies; v3.2/v3.5/v3.6/v3.7 mismatch 4/2/3/1 against a scratch DEST — identically with or without the records change). Until that is repaired, `bash /var/tmp/forge/ship-v3.8/install.sh` will print `INSTALL NOT PROVEN`. | the v3.7 stager | Outside this brief's write set in substance: repairing the consumption/envelope epoch is a different change, and discarding it would throw away another builder's in-flight work. The proven minimal composition is retained at `/var/tmp/forge/squeeze/min-src/`. |

## Boundaries honoured

- Owned files: **only** `/var/tmp/forge/ship-v3.8/` (copied from `ship-v3.7/`). Nothing under
  `~/bin` was read-modified or written. The installer was **never** run against the real DEST.
- The repo, `test/gate_slot.py`, the Makefile and `/home/forge/src/clj-surgeon-skiff` were not
  touched. The real records checkout was read **read-only** (two `git diff --name-only`, one
  `ls-remote`, one `fetch`); nothing was committed or pushed there.
- Every fixture used an **isolated bare origin** under `/var/tmp/forge/squeeze/`. The real GitHub
  remote was only ever *read*. `/var/tmp/forge/ship/.lease` was never written; every lease in these
  runs lived in a private per-fixture directory with its own live holder process.
- **Close readback:** all seven installed inputs re-hashed at 07:05:22Z and still `OK` against
  Astra's frozen table — `/home/forge/bin/records-push` is byte-for-byte the v3.7 helper, dated
  `Sep 9 01:34`, untouched. Nothing this block installed, published, committed, pushed or merged.
- No `pkill`/`pgrep -f`. All temp under `/var/tmp/forge`. No ports touched (none opened at all;
  7888 / 7890 / 7894 / 7895 / 8300-8339 untouched).

## Independent acceptance — the installer's own way, against a SCRATCH DEST

**Run A -- v3.8 exactly as staged** (`SRC=/var/tmp/forge/ship-v3.8`):

```
ACCEPTANCE START 2026-09-10T05:14:56Z
   run-land-auto: land-auto fixtures: 19 rows, mismatches: 0
   run-land-publication-truth: land-publication-truth fixtures: 12 rows, mismatches: 0
   run-ship-v2: ship-v2 fixtures: 28 rows, mismatches: 0
   run-ship-v3: ship-v3 fixtures: 22 rows, mismatches: 0
   run-ship-v3.1: ship-v3.1 fixtures: 13 rows, mismatches: 0
   run-ship-v3.2: ship-v3.2 fixtures: 8 rows, mismatches: 4
   run-ship-v3.3: ship-v3.3 fixtures: 13 rows, mismatches: 0
   run-ship-v3.4: ship-v3.4 fixtures: 14 rows, mismatches: 0
   run-ship-v3.5: ship-v3.5 fixtures: 5 rows, mismatches: 2
   run-ship-v3.6: ship-v3.6 fixtures: 9 rows, mismatches: 3
   run-ship-v3.7: ship-v3.7 fixtures: 73 rows, mismatches: 1
   run-records-push-v3.8: records-push v3.8 fixtures: 12 rows, mismatches: 0
INSTALL NOT PROVEN — a fixture mismatched against the installed files. The backups above are
ACCEPTANCE EXIT=3 2026-09-10T05:24:18Z
```

**Run B -- baseline control, v3.7 exactly as staged** (proves the mismatches pre-date the candidate):

```
   run-land-auto: land-auto fixtures: 19 rows, mismatches: 0
   run-land-publication-truth: land-publication-truth fixtures: 12 rows, mismatches: 0
   run-ship-v2: ship-v2 fixtures: 28 rows, mismatches: 0
   run-ship-v3: ship-v3 fixtures: 22 rows, mismatches: 0
   run-ship-v3.1: ship-v3.1 fixtures: 13 rows, mismatches: 0
   run-ship-v3.2: ship-v3.2 fixtures: 8 rows, mismatches: 4
   run-ship-v3.3: ship-v3.3 fixtures: 13 rows, mismatches: 0
   run-ship-v3.4: ship-v3.4 fixtures: 14 rows, mismatches: 0
   run-ship-v3.5: ship-v3.5 fixtures: 5 rows, mismatches: 2
   run-ship-v3.6: ship-v3.6 fixtures: 9 rows, mismatches: 3
   run-ship-v3.7: ship-v3.7 fixtures: 73 rows, mismatches: 1
INSTALL NOT PROVEN — a fixture mismatched against the installed files. The backups above are
BASELINE37 EXIT=3 2026-09-10T04:10:50Z
```

**Run C -- the records change on the INSTALLED epoch** (`SRC=/var/tmp/forge/squeeze/min-src`). Its first attempt was correctly REFUSED by the installer's own lease gate while a real ship held the lease; it was retried unmodified once the real lease released, and is the only run in which the `INSTALL OK` line can be earned:

```
INSTALL REFUSED reason=ship-lease-live pid=1844683 file=/var/tmp/forge/ship/.lease/owner
MINRUN EXIT=2 2026-09-10T04:12:08Z
  ...the real lease then released; the run was retried, unmodified:
FIELD-LEASE observed_at=2026-09-10T04:12:36Z run=20260910T040304Z-3585c7e59880 pid=1844683
FIELD-LEASE released_at=2026-09-10T04:14:31Z observed_live_s=115 run=20260910T040304Z-3585c7e59880
   run-land-auto: land-auto fixtures: 19 rows, mismatches: 0
   run-land-publication-truth: land-publication-truth fixtures: 12 rows, mismatches: 0
   run-ship-v2: ship-v2 fixtures: 28 rows, mismatches: 0
   run-ship-v3: ship-v3 fixtures: 22 rows, mismatches: 0
   run-ship-v3.1: ship-v3.1 fixtures: 13 rows, mismatches: 0
   run-ship-v3.2: ship-v3.2 fixtures: 8 rows, mismatches: 0
   run-ship-v3.3: ship-v3.3 fixtures: 13 rows, mismatches: 0
   run-ship-v3.4: ship-v3.4 fixtures: 14 rows, mismatches: 0
   run-ship-v3.5: ship-v3.5 fixtures: 5 rows, mismatches: 0
   run-ship-v3.6: ship-v3.6 fixtures: 9 rows, mismatches: 0
   run-ship-v3.7: ship-v3.7 fixtures: 73 rows, mismatches: 0
   run-records-push-v3.8: records-push v3.8 fixtures: 12 rows, mismatches: 0
INSTALL OK v3.8 stamp=20260910T041431Z files='ship land fence-run receipt-chain land-auto records-push ship-fix-block-spec.md run-bg sol-yolo gate-envelope.clj gate-consume.clj mutate.clj' — every fixture green against the installed bytes.
MINRUN EXIT=0 2026-09-10T04:21:27Z
```

Exact commands (this is what was run; **every DEST is a scratch directory, never `~/bin`**):

```sh
# Run A -- the candidate exactly as staged
SRC=/var/tmp/forge/ship-v3.8       DEST=/var/tmp/forge/squeeze/scratch-dest FIXTURES=/var/tmp/forge/squeeze/scratch-fixtures bash /var/tmp/forge/ship-v3.8/install.sh
# Run B -- the baseline control
SRC=/var/tmp/forge/ship-v3.7       DEST=/var/tmp/forge/squeeze/base37-dest  FIXTURES=/var/tmp/forge/squeeze/base37-fixtures  bash /var/tmp/forge/ship-v3.7/install.sh
# Run C -- the records change on the installed epoch  (this is the run that earned the INSTALL OK line)
SRC=/var/tmp/forge/squeeze/min-src DEST=/var/tmp/forge/squeeze/min-dest     FIXTURES=/var/tmp/forge/squeeze/min-fixtures     bash /var/tmp/forge/squeeze/min-src/install.sh
```

`min-src` is `ship-v3.8` with exactly eight files reverted to the **installed** epoch:
`gate-envelope.clj`, `gate-consume.clj`, `mutate.clj` (from `/home/forge/bin`) and
`fixtures/{envelope-green.edn,gen-field-mutants.clj,inventory-3ea3803e.edn,run-ship-v3.7.sh,stub-gate.clj}`
(from `/var/tmp/forge/tighten/fixtures`). Nothing about the records change differs between the two:
`min-dest/records-push` and `scratch-dest/records-push` are both `693239e0c4b55d3b...`, and both
fixture directories carry the same new `run-ship-v3.sh` `cfb100d1c4288d22...` and the new
`run-records-push-v3.8.sh`.

The scratch `FIXTURES` directory is seeded from the canonical
`/var/tmp/forge/tighten/fixtures` first, so the four fixtures that do **not** travel in
`ship-v3.8/fixtures` (`run-land-auto`, `run-land-publication-truth`, `run-ship-v2`,
`run-ship-v3.1`) are exercised as well; the installer then overwrites the ones v3.8 ships.
`SHIP_LEASE` was left at its default so the **real** lease gate was exercised, with the real lease
free.

**To actually install (NOT run in this block, and not this seat's call):**

```sh
bash /var/tmp/forge/ship-v3.8/install.sh          # SRC defaults to /var/tmp/forge/ship-v3.8, DEST to /home/forge/bin
```

## Immutable manifest

| artifact | sha256 |
|---|---|
| `/home/forge/bin/records-push` | `55e25d68b3300df8e0931b99d7dd7d992ee122c5df1c825f91d81075933d4907` |
| `/var/tmp/forge/ship-v3.8/records-push` | `693239e0c4b55d3b234606b84bb3125660a59f987143fff9af46b36932ea39b6` |
| `/var/tmp/forge/ship-v3.8/install.sh` | `ee639c1ffd6383dcfdb9c1f3d052e3e638514cfe563fb3f9fd66f17f8b78f76b` |
| `/var/tmp/forge/ship-v3.8/fixtures/run-records-push-v3.8.sh` | `3945cdf10ca0c840d2e4847d951bea10c68e2b111793b63f7d8b1c4c4f744b1e` |
| `/var/tmp/forge/ship-v3.8/fixtures/run-ship-v3.sh` | `cfb100d1c4288d22c0802747b185799e037528f7a59f41f16cf8ab872e7b8442` |
| `/var/tmp/forge/ship-v3.8/fixtures/records-push-v3.7-frozen.sh` | `55e25d68b3300df8e0931b99d7dd7d992ee122c5df1c825f91d81075933d4907` |
| `/var/tmp/forge/squeeze/harness.sh` | `bd9fec79da704da3a085ddc3b6a472ee58b91d7ee66fe9a4abe582fd068f02ba` |
| `/var/tmp/forge/squeeze/seal/payload.md` | `a941e008a681059932a076130d07133335ec780f0b039a7a9e238d521ae82481` |

Retained evidence (append-only, nothing rewritten): `/var/tmp/forge/squeeze/out/*.record.txt` (one
per arm), `*.helper.txt` (verbatim helper output), `controls.log`, `pairs.log`,
`acceptance.log`, `fx-v3.log`; sealed inputs in `/var/tmp/forge/squeeze/seal/`.

## Terminal receipt

```text
finding=SQUEEZE-RECORDS-REF-LEASE-001
reference=55e25d68b3300df8e0931b99d7dd7d992ee122c5df1c825f91d81075933d4907 candidate=693239e0c4b55d3b234606b84bb3125660a59f987143fff9af46b36932ea39b6 scope=records-publication
red="RECORDS-PUSH HELD reason=ship-lease-live waited=3s (records NOT pushed; rerun later)" exit=4 blob-absent-remotely
matrix=12/12 real-git rows green (old arm recorded on every decisive row); run-ship-v3=22/22 with ONE changed verdict (row 20, anchor=decision-1a)
control_n=6 control_median_s=660.417 control_sd_s=0.046
pairs_n=3 old_median_s=660.371 new_median_s=0.136 delta_median_s=660.229
accepted=3/3 failures=0 censored=0 code_ref_violations=0
lease_wait_old_s=660.417(reported 660 in every control) lease_wait_new_s=0
field_request_to_visibility_s=unknown-with-reason (no install and no real-remote publication is permitted from this seat in this block)
ship_seconds_saved=0-claimed transfer=untested
build_wall_s=829 (00:00 block open -> 03:46:59Z candidate frozen) measurement_wall_s=5578 (6 controls 03:35:42-04:41:46Z + 3 pairs + idle pair 04:42:05-05:15:02Z) block_wall_s=12759 outcome=parked
pending=P1 wrong-destination adapters in ~/bin (seat-receipt/canary-cell/verb-sentinel/tighten) · P2 records-push absent from the tighten bundle · P3 real-remote second encounter untested · P4 independent qualification · P5 the lock is per-checkout, not per-repository · P6 ship-v3.7 was staged past its last install, so install.sh from ship-v3.8 prints INSTALL NOT PROVEN for inherited reasons (the proven minimal composition is retained at /var/tmp/forge/squeeze/min-src)
evidence=/var/tmp/forge/squeeze/out/*.record.txt + *.helper.txt (verbatim) + seal/ + this report
```
