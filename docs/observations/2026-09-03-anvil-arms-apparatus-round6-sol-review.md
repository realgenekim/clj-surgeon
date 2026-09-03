# Apparatus round 6 — Sol cohort-readiness review of bridge/anvil-arms-apparatus at 8017789 (2026-09-03T11:14Z)

Verdict: **GO-WITH-FIX**. Items 1–7 CLOSED with replays; item 8 (two independent schema literals) closed by the seat in round 7 (23a7643): score.py imports WATCH_SCHEMA_VERSION, self-test case35d asserts equality and that score.py carries no literal of its own; red 383/1 (literal restored), green 384/0. Sol round-7 confirm-only re-check launched.

## Sol verdict, verbatim

Round six is functionally cohort-ready, with one non-blocking contract fix: the schema version still has two independent literals. All four round-five defects replay correctly.

Self-test summary, verbatim:

```text
anvil-arms self-test: 382 passed, 0 failed  (workdir /home/forge/tmp/arms/solreview6/selftest)
```

The checkout remains clean, port 7909 is free, and the replayed aborts left no surviving children.

Cohort-readiness decision: GO-WITH-FIX

1. CLOSED — [watch.py:217](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/watch.py:217): `MAKEFLAGS=CMD=/bin/echo make verify` produced `make-runtime-override:env:MAKEFLAGS`, watcher rc 6; scorer rc 3, `incomplete-run`, no receipt.

2. CLOSED — [watch.py:114](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/watch.py:114): `make -- verify` and `make verify --` both scored rc 0 as one test action; `make -- -j4 verify` refused with `make-runtime-override:-j4`, scorer rc 3, no receipt.

3. CLOSED — [score.py:260](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/score.py:260): schemas 1 and 3 returned `watch-schema-unsupported`, rc 3, and removed the stale receipts; schema 2 scored rc 0 and wrote a receipt.

4. CLOSED — [watch.py:1110](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/watch.py:1110): the silent-driver replay aborted in 0.662 s with two scans; cases [14](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/self-test.sh:443), [19](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/self-test.sh:674), and [26](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/self-test.sh:1088) retained their reap/orphan guarantees.

5. CLOSED — [watch.py:261](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/watch.py:261): `FOO=bar bin/kaocha --focus x` scored rc 0 with one test action and zero non-test actions; `env MAKEFLAGS=x make verify` refused as the same typed Make override.

6. CLOSED — [watch.py:1113](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/watch.py:1113): a partial unterminated line was retained byte-for-byte and aborted in 0.690 s with two scans; a single burst delivered exactly 10,000 rollout lines, 10,000 watch returns, and 10,000 `run.json` returns.

7. CLOSED — [watch.py:1191](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/watch.py:1191): an infinite TERM-resistant child observed SIGTERM, remained through the grace, then was gone after 2.675 s total—approximately the 0.5 s wall cap plus the 2 s KILL ceiling—with `orphans_after_reap=0`.

8. OPEN — [watch.py:66](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/watch.py:66), [score.py:234](/home/forge/tmp/sol/arms6-wt/bench/anvil-arms/score.py:234): emitter and scorer still define separate literal `2` constants, although `score.py` already imports from `watch.py`; importing the schema constant would remove this drift path.

---

# Round 7 — Sol confirm-only re-check at 23a7643 (2026-09-03T11:20Z)

Verdict: **GO** for cohort readiness. Item 8 CLOSED.

## Sol, verbatim

1. Clean self-test, rc 0: `anvil-arms self-test: 384 passed, 0 failed  (workdir /home/forge/tmp/arms/solreview7/selftest)`

2. Mutation ratchet: restoring `WATCH_SCHEMA_SUPPORTED = 2` made case 35d report `FAIL case35d the scorer does not carry its own schema literal`; tally was `383 passed, 1 failed`, suite rc 1. The file was restored via `git checkout`.

3. Exact-schema replay: schemas 1 and 3 were refused with `watch-schema-unsupported`, rc 3, and no receipt; schema 2 scored rc 0 and wrote a receipt.

4. Item 8: CLOSED — [score.py:51](/home/forge/tmp/sol/arms7-wt/bench/anvil-arms/score.py:51) imports `WATCH_SCHEMA_VERSION` from [watch.py:66](/home/forge/tmp/sol/arms7-wt/bench/anvil-arms/watch.py:66), and [score.py:236](/home/forge/tmp/sol/arms7-wt/bench/anvil-arms/score.py:236) assigns `WATCH_SCHEMA_SUPPORTED = WATCH_SCHEMA_VERSION`.

5. Cohort readiness: **GO**. Checkout remains clean at `23a7643`; port 7909 is free.