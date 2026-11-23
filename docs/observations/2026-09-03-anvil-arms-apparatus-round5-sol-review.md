# anvil-arms-apparatus e9a40dc — Sol executed round-5 review: GO-WITH-FIX (header + meter cost CLOSED; MAKEFLAGS env bypass; `--` false positive; exact schema version; capture-mode blocking readline) — round 6 launched

Round five is GO-WITH-FIX. Two fail-closed gaps remain, and the new scan counter exposed a pre-existing Claude/capture-mode polling defect.

### Round-four fixes

| Fix | Status | My replay |
|---|---|---|
| Runtime Make override | PARTIAL | `make CMD=bin/kaocha verify` produced `make-runtime-override:CMD=bin/kaocha`, watcher rc 6/incomplete-run, scorer rc 3, no receipt. Plain `make verify` resolved with watcher rc 0. Environment `MAKEFLAGS` still bypasses detection. |
| Exactly one header | CLOSED | Contradictory second header produced `SCORE-ABORT malformed-watch duplicate-header`, rc 3, no receipt. |
| Watcher cost receipt | CLOSED | Fake-driver receipt contained computed `watcher_cpu_s=0.052`, `scans=3`, `scan_interval_ms=250`. Removing them yielded all three as `unverified` plus `watcher-cost-unverified`. |

### Override matrix

| Invocation | Watcher result | GNU Make replay | Judgment |
|---|---|---|---|
| `make -- verify` | Refused as `make-runtime-override:--` | Ran `DEFAULT-RUNNER`, rc 0 | Wrong; `--` is inert and should resolve. |
| `make verify --` | Same refusal | Ran `DEFAULT-RUNNER`, rc 0 | Wrong; same false positive. |
| `make "V=1 verify"` | Refused as `make-runtime-override:V=1 verify` | Treated as one assignment and ran the default goal | Correct refusal. |
| `MAKEFLAGS=CMD=/bin/echo make verify` | Resolved, no unresolved targets | Replaced the mapped recipe; output changed from `DEFAULT-RUNNER` to blank | Wrong; must refuse environment-carried Make semantics. |
| `make verify` | Resolved | Ran `DEFAULT-RUNNER` | Correct control. |

A record-zero header with `schema_version: 3` scored rc 0 and produced a receipt. That is the wrong default for a fail-closed evidence format: the scorer should accept the exact schema it understands, not every unknown future schema.

`RUSAGE_SELF` is correct. A non-capture fake driver consumed 2.01 CPU seconds while `watcher_cpu_s` was only 0.134 seconds, with nine scans. Driver/child CPU was not included.

The fixture audit covered all 18 embedded Python fixture blocks plus the apparatus Python and shell scripts. No other `.get(..., 0) + arithmetic` occurrence remains; case 39a now uses `(header.get("rollout_ino") or 0) + 1`.

The mandated self-test passed:

```text
anvil-arms self-test: 368 passed, 0 failed  (workdir /home/forge/tmp/arms/selftest.BN91WO)
```

The checkout remains clean at `e9a40dc`; port 7909 is free and no review-owned process remains.

### Verdict: GO-WITH-FIX

1. [watch.py:152](bench/anvil-arms/watch.py:152) — PARTIAL: preserve and reject Make-affecting leading environment assignments; witness: `MAKEFLAGS=CMD=/bin/echo make verify` changed GNU Make’s recipe but resolved with `[]`.

2. [watch.py:101](bench/anvil-arms/watch.py:101) — fix the `--` false positive; witness: both placements ran the same recipe as plain `make verify` but were refused.

3. [score.py:255](bench/anvil-arms/score.py:255) — require the exact supported schema version; witness: unknown schema 3 scored rc 0 and wrote a receipt.

4. [watch.py:1043](bench/anvil-arms/watch.py:1043) — fix capture-mode’s blocking `readline()` before any Claude cohort; witness: `--max-wall 0.5` took 4.13 seconds and recorded one scan while a silent two-second driver ran.

5. [score.py:183](bench/anvil-arms/score.py:183) — CLOSED: my contradictory two-header stream returned rc 3, `malformed-watch duplicate-header`, and no receipt.

6. [watch.py:1177](bench/anvil-arms/watch.py:1177) — CLOSED: a 2.01-second CPU-burning child contributed none of its CPU to the watcher’s 0.134-second `RUSAGE_SELF` delta.

7. [self-test.sh:1968](bench/anvil-arms/self-test.sh:1968) — CLOSED: the explicit-`None` fixture is repaired, and the repository-wide sibling-fixture audit found no duplicate idiom.