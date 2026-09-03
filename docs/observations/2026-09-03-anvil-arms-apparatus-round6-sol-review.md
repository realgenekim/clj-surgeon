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

---

# Round 8 — Sol re-check at 54f3b50 (2026-09-03T11:40Z)

Verdict: **NO-GO for 54f3b50** (the meta-ratchet has a false-green: a tally whose file operand points at another case's .out passes both case-45 checks); cohort-ready GO stays at **23a7643**. Round 9 launched on case 45.

## Sol, verbatim

## NO-GO

Case 45 catches the requested missing-tally and orphan mutations, but a wrong-file tally produces a genuine false-green.

Baseline summary:

> `anvil-arms self-test: 386 passed, 0 failed  (workdir /home/forge/tmp/arms/solreview8/selftest-baseline)`

Exact suffix-collision patterns:

```bash
grep -c '^FAIL case35' "$WORK/case35.out"
grep -qF "grep -c '^FAIL ${base}'" "$HERE/self-test.sh"
grep -c "^FAIL ${base}" "$f"
grep -c '^FAIL case' "$f"
```

`^FAIL case35` does match `FAIL case35d ...`. It does not count the same file line twice in case 45, but it masks the case-ID mismatch by attributing the suffixed line to `case35`.

1. [self-test.sh:1821](/home/forge/tmp/sol/arms8-wt/bench/anvil-arms/self-test.sh:1821): deleting both case35d tally lines produced `FAIL case35d.out...`, summary `383 passed, 1 failed`, rc 1. [Witness](/home/forge/tmp/arms/solreview8/missing-case35d-tally/run.log:445)

2. [self-test.sh:1822](/home/forge/tmp/sol/arms8-wt/bench/anvil-arms/self-test.sh:1822): injected `FAIL case35d injected` was counted by the ordinary tally; summary `386 passed, 1 failed`, rc 1, while case 45 remained balanced. [Witness](/home/forge/tmp/arms/solreview8/counted-case35d-fail/run.log:390)

3. [self-test.sh:2382](/home/forge/tmp/sol/arms8-wt/bench/anvil-arms/self-test.sh:2382): orphan `FAIL case99 orphan` passed (a), but (b) caught global 1 versus tallied 0; summary `385 passed, 1 failed`, rc 1. [Witness](/home/forge/tmp/arms/solreview8/orphan-case99/run.log:447)

4. [self-test.sh:2375](/home/forge/tmp/sol/arms8-wt/bench/anvil-arms/self-test.sh:2375): the suffix probe counted `FAIL case35d` using `^FAIL case35`; both case-45 assertions passed with tallied/global 1. [Witness](/home/forge/tmp/arms/solreview8/suffix-case35-case35d/run.log:444)

5. [self-test.sh:2372](/home/forge/tmp/sol/arms8-wt/bench/anvil-arms/self-test.sh:2372): decisive defect—the presence test ignores the tally’s file operand, then line 2375 reconstructs an idealized command against `$f`; pointing the actual case35d tally at `case35c.out` yielded a visible FAIL, both case-45 checks green, summary `386 passed, 0 failed`, rc 0. [Witness](/home/forge/tmp/arms/solreview8/wrong-file-case35d/run.log:390)

Fix case 45 to validate the exact tally pattern and matching output-file operand, with a case-ID boundary such as `( |$)`.

Checkout restored clean at `54f3b50`; no review test processes remain; port 7909 is free.

---

# Round 9 — Sol re-check at 59ed16a (2026-09-03T12:19Z)

Verdict: **NO-GO for 59ed16a**; cohort-ready GO stays at **23a7643**. Round-eight mutations all red now (items 2–6), but a static regex over the script source cannot distinguish inert text from executed code: an exact tally line inside a quoted heredoc is accepted (false green, "386 passed, 0 failed" over a visible FAIL) and a semantically equivalent double-quoted/spaced tally is rejected (false failure). Round 10 moves the tally to a runtime function.

## Sol, verbatim

## NO-GO

The round-eight defects are fixed, but the new heredoc attack produces a genuine false-green: the raw-source regex treats an inert tally string as executable tally logic.

1. [self-test.sh:2441](/home/forge/tmp/sol/arms9-wt/bench/anvil-arms/self-test.sh:2441) — Baseline, rc 0: `anvil-arms self-test: 386 passed, 0 failed  (workdir /home/forge/tmp/arms/solreview9/baseline)` ([witness](/home/forge/tmp/arms/solreview9/baseline.log:454)).

2. [self-test.sh:1821](/home/forge/tmp/sol/arms9-wt/bench/anvil-arms/self-test.sh:1821) — Missing case35d tallies, rc 1: `anvil-arms self-test: 383 passed, 1 failed  (workdir /home/forge/tmp/arms/solreview9/missing-tally)` ([witness](/home/forge/tmp/arms/solreview9/missing-tally.log:454)).

3. [self-test.sh:1822](/home/forge/tmp/sol/arms9-wt/bench/anvil-arms/self-test.sh:1822) — Injected counted FAIL, rc 1: `anvil-arms self-test: 386 passed, 1 failed  (workdir /home/forge/tmp/arms/solreview9/injected-counted-fail)` ([witness](/home/forge/tmp/arms/solreview9/injected-counted-fail.log:455)).

4. [self-test.sh:2418](/home/forge/tmp/sol/arms9-wt/bench/anvil-arms/self-test.sh:2418) — Orphan case99, rc 1: `anvil-arms self-test: 384 passed, 2 failed  (workdir /home/forge/tmp/arms/solreview9/orphan-case99)` ([witness](/home/forge/tmp/arms/solreview9/orphan-case99.log:454)).

5. [self-test.sh:2411](/home/forge/tmp/sol/arms9-wt/bench/anvil-arms/self-test.sh:2411) — `^FAIL case35` over `FAIL case35d x`, rc 1: `anvil-arms self-test: 384 passed, 3 failed  (workdir /home/forge/tmp/arms/solreview9/suffix-case35-case35d)` ([witness](/home/forge/tmp/arms/solreview9/suffix-case35-case35d.log:455)).

6. [self-test.sh:2398](/home/forge/tmp/sol/arms9-wt/bench/anvil-arms/self-test.sh:2398) — Wrong case35d FAIL-tally operand, rc 1: `anvil-arms self-test: 384 passed, 2 failed  (workdir /home/forge/tmp/arms/solreview9/wrong-file-case35d)`; it names `file operand "$WORK/case35d.out" exactly` ([witness](/home/forge/tmp/arms/solreview9/wrong-file-case35d.log:452)).

7. [self-test.sh:2381](/home/forge/tmp/sol/arms9-wt/bench/anvil-arms/self-test.sh:2381) — Semantically equivalent double-quoted/spaced tally is rejected: false failure, rc 1, `anvil-arms self-test: 385 passed, 1 failed  (workdir /home/forge/tmp/arms/solreview9/noncanonical-valid-tally)` ([witness](/home/forge/tmp/arms/solreview9/noncanonical-valid-tally.log:454)).

8. [self-test.sh:2380](/home/forge/tmp/sol/arms9-wt/bench/anvil-arms/self-test.sh:2380) — Blocking: an exact tally line inside an inert quoted heredoc is accepted; visible `FAIL case35d heredoc-probe` still yields rc 0 and `anvil-arms self-test: 386 passed, 0 failed  (workdir /home/forge/tmp/arms/solreview9/heredoc-commented-tally)` ([FAIL witness](/home/forge/tmp/arms/solreview9/heredoc-commented-tally.log:395), [false-green summary](/home/forge/tmp/arms/solreview9/heredoc-commented-tally.log:455)).

Every mutation was restored with `git checkout`. The checkout is clean at `59ed16a`, no review test process remains, and port 7909 is free.