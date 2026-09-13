# Probe versus native — preregistered measure: STOPPED before any measured cell

Subject: `759974c718889c52a05a1d0746c9c21d0f00dcdb` (tag `stable/2026-09-13.1`, `origin/MCP/main`).
Recorded 2026-09-13 (UTC) on forge@anvil. Nothing was pushed, tagged, committed or installed.
No frozen patch and no target file was edited.

## Headline — no cell was measured

| Stratum | NATIVE median ms | PROBE median ms | P/N | n per arm | Unknown cells |
|---|---:|---:|---:|---:|---:|
| bb-portable (T1,T2,T3) | — | — | — | 0 of 9 | 18 of 18 |
| JVM-only (T4,T5,T6) | — | — | — | 0 of 9 | 18 of 18 |
| NATIVE T1 controls | — | n/a | n/a | 0 of 6 | 6 of 6 |
| NATIVE T4 controls | — | n/a | n/a | 0 of 6 | 6 of 6 |
| Planted non-test (N1,N2) | — | — | n/a | 0 of 2 per arm | 4 of 4 |

**One line of learning.** The frozen instrument contradicts the frozen protocol: `measure.py`
demands a clean checkout *and* demands the warm image's identity file inside that same checkout,
and `.clj-surgeon/` is not ignored in this repository — so every PROBE cell aborts at line 56
before T0, and the comparison cannot be run as written.

**One caveat.** The probe verb itself is not implicated. It answered all six target namespaces
correctly in the untimed admission pass (6/6 `:probe-passed`, 0 failures, exit 0), and the NATIVE
arm of the runner completes end to end. The blocker is one assertion in the apparatus, not the
subject under test.

## Bets — all UNSETTLED

| Bettor | bb-portable P/N | JVM-only P/N | First-attempt success | Probe refusal rate |
|---|---|---|---|---|
| Fable (≤0.50 / ≤0.30 / equal 6-6 / 2-2 typed, NATIVE 0-2) | UNSETTLED | UNSETTLED | UNSETTLED | UNSETTLED |
| Astra (≤0.50 / ≤0.45 / equal on valid targets / higher with non-test) | UNSETTLED | UNSETTLED | UNSETTLED | UNSETTLED |

The preregistration's own rule governs: *"Missing/terminated/unattested runs remain named unknown
cells; no replacement, imputation, outlier deletion or claim from incomplete cells is allowed"* and
*"unknown telemetry leaves the bet unsettled."* Every cell is unknown, so neither row is scored
HELD or MISSED on any column. Neither bettor's row was touched.

## Blocker B1 — STOP: the exact contradictory line

**`round5/measure.py` line 56** (byte-identical in `instrument/measure.py`,
sha256 `1aff7c05688125126b0bee21192a4a756457747134d8a2099f98f53923a142be`):

```python
assert not git(checkout, 'status', '--porcelain'), 'checkout must start clean'
```

It contradicts, in the same document set:

- the preregistration, *Arms and exact commands*: *"In a separate owned terminal in the PROBE
  checkout: `make warm PORT=19066`"*; and
- **`measure.py` line 88**: `identity = checkout / '.clj-surgeon/probe.edn'` /
  `assert identity.is_file(), 'attested warm setup required before T0'`.

Mechanism, verified on the subject: the `warm` target at `Makefile:373-374` runs
`mkdir -p .clj-surgeon` and starts the image with `:probe-image-file ".clj-surgeon/probe.edn"`,
relative to the checkout root. `.clj-surgeon/` is **not** in the subject's `.gitignore` at
759974c7 — only `.clj-surgeon-receipts/` is — and it has never been ignored on any branch
(`git log -S'.clj-surgeon/' --all -- .gitignore` is empty). So after the required warm start,
`git status --porcelain` in `subject-probe` returns exactly `?? .clj-surgeon/`.

Executed receipt, not a source reading (`results/apparatus/probe-cell-attempt.err`):

```
Traceback (most recent call last):
  File "/var/tmp/forge/probe-measure-fx/instrument/measure.py", line 153, in <module>
    main()
  File "/var/tmp/forge/probe-measure-fx/instrument/measure.py", line 56, in main
    assert not git(checkout, 'status', '--porcelain'), 'checkout must start clean'
AssertionError: checkout must start clean
```

The assertion fires before `cell.mkdir()`, so no cell directory, `request.json`, `verdict.json`
or `timing.json` is produced: the run is an unknown cell with no partial receipt. Consequence:
0 of 18 PROBE cells and 0 of 2 PROBE non-test controls are runnable, so P/N is undefined in both
strata and no bet column can be scored.

I did not work around it. Making `git status` clean requires either editing the subject
(forbidden), editing the instrument (forbidden — it is frozen and hashed into `subject.json`), or
adding an exclude outside the tree, which is a substitute for a frozen line and is exactly what
this brief forbids without a decision.

## Blocker B2 — T4 and T5 are UNKNOWN by the preregistration's own rule

`measure.py` line 85 asserts the target file's sha256 equals the manifest's `before_sha256` before
the patch is applied. Measured on the subject (`results/patch-applicability.json`):

| Task | Stratum | File | Expected before_sha256 | Trunk sha256 | Admissible |
|---|---|---|---|---|---|
| T1 | bb-portable | test/clj_surgeon/forms_test.clj | 95403c2e08c77a24… | 95403c2e08c77a24… | yes |
| T2 | bb-portable | test/clj_surgeon/analyze_test.clj | 1db7cab655040779… | 1db7cab655040779… | yes |
| T3 | bb-portable | test/clj_surgeon/edit_dsl_test.clj | b86dbb4c6288b1da… | b86dbb4c6288b1da… | yes |
| **T4** | JVM-only | test/clj_surgeon/census_pool_test.clj | a16fa8b69823fce5… | **b843fadfd81772d8…** | **no — UNKNOWN** |
| **T5** | JVM-only | test/clj_surgeon/mcp_hot_verify_test.clj | 97e49995bb183430… | **4f0f3ff5386ee34e…** | **no — UNKNOWN** |
| T6 | JVM-only | test/clj_surgeon/mcp_formatter_test.clj | 72080596cc537997… | 72080596cc537997… | yes |

(`git apply --check` exits 0 for all six — the hunks still apply — but the frozen identity check
is the binding gate, and it refuses T4 and T5. The patches and targets were left untouched.)

This is a design consequence beyond bookkeeping. `--control` shares the same `row` lookup and the
same assertion, so **all six NATIVE T4 variance-floor controls are unknown too**. Even with B1
resolved, the JVM-only stratum would carry 3 runs per arm instead of the frozen nine and would
have **no variance floor at all**, so its primary statistic and its noise gate
(`median(NATIVE) − median(PROBE) > 2 × sd(native controls)`) are both undefined as frozen. The
JVM bound is the only column that separates Fable's row from Astra's (0.30 vs 0.45), so that
separation is unrecoverable without a re-cut of T4/T5 or a substitute JVM task — and the
preregistration forbids replacing a task, so this needs a decision, not a runner's judgement.
bb-portable is unaffected: three tasks, nine runs per arm, six T1 controls.

## Setup and amortized costs (retained, as the document requires)

| Item | Value |
|---|---:|
| Warm image startup wall (`make warm PORT=19066`, ready when `.clj-surgeon/probe.edn` appeared) | 13,258.28 ms |
| Admission probe, clj-surgeon.forms-test | 482 ms (in-image 148.10 ms) |
| Admission probe, clj-surgeon.analyze-test | 787 ms (in-image 511.69 ms) |
| Admission probe, clj-surgeon.edit-dsl-test | 700 ms (in-image 429.81 ms) |
| Admission probe, clj-surgeon.census-pool-test | 529 ms (in-image 261.95 ms) |
| Admission probe, clj-surgeon.mcp-hot-verify-test | 1,578 ms (in-image 1,285.47 ms) |
| Admission probe, clj-surgeon.mcp-formatter-test | 1,182 ms (in-image 899.59 ms) |
| Admission total | 5,258 ms |
| Admission verdicts | 6/6 `:state :probe-passed`, 0 failures, exit 0 |
| **Break-even invocation count** | **not computable** — it needs a measured per-invocation saving from matched cells, and there are none |

Warm image identity (`results/setup/probe.edn`, `results/setup/warm-image.json`): root
`/var/tmp/forge/probe-measure-fx/subject-probe`, generation `61b136bd-9419-4c60-8695-051f4bf6b075`,
fingerprint `3e5a5565a6814223974419f7b0f909a45078bcc15a43b8d319b23528cb7ca7f1`, port 19066,
java PID 1005716 (make PID 1004637), HEAD 759974c7, full argv in `results/setup/warm-argv.txt`.

### Indicative only — NOT a result, settles no bet

One NATIVE run was executed through the runner into a **separate** smoke output directory
(`results/apparatus/native-smoke-n1/`), purely to establish that the NATIVE arm is not also
blocked. It is not a preregistered cell and it is not in the matched order.

| Figure | Value |
|---|---:|
| NATIVE T1, complete request-to-verdict wall (n=1) | 4,401.499 ms |
| NATIVE T1, command-only wall | 4,386.459 ms |
| NATIVE T1 verdict | accepted — 24 tests / 95 assertions / 0 fail / 0 error / exit 0 |
| PROBE T1 namespace, admission probe wall (unpatched, outside the runner) | 482 ms |
| Naive ratio of those two numbers | 0.11 |
| Naive break-even against 13,258 ms startup | ≈ 3.4 invocations |

Why this is not a result: the 482 ms is an *unpatched admission probe outside the instrument*,
not a T0→T1 edit-to-verdict cell; 4,401 ms is one run, not a median of nine; there is no variance
floor, no matched pairing, no arm ordering, no first-attempt-success denominator and no refusal
telemetry. Quoting it as evidence for either bet would be exactly the "claim from incomplete
cells" the preregistration forbids.

## Per-cell table

| Cell class | Planned | Measured | Unknown | Reason |
|---|---:|---:|---:|---|
| Matched T1–T3 × {NATIVE,PROBE} × 3 | 18 | 0 | 18 | NATIVE not started (matched order requires the PROBE arm, blocked by B1); PROBE blocked by B1 |
| Matched T4–T6 × {NATIVE,PROBE} × 3 | 18 | 0 | 18 | T4,T5 blocked by B2 in both arms; T6 blocked by B1 on the PROBE side |
| NATIVE T1 controls × 6 | 6 | 0 | 6 | not started — controls are only meaningful against an arm comparison that cannot run |
| NATIVE T4 controls × 6 | 6 | 0 | 6 | blocked by B2 |
| Planted non-test N1,N2 × 2 arms | 4 | 0 | 4 | PROBE side blocked by B1; refusal bet unsettled |

No cell directory exists under `results/` — by design, since every abort fired before
`cell.mkdir()`. `results/subject.json` records the frozen subject, runner hash and manifest hash
from a successful `measure.py init`.

## Apparatus notes — every deviation from the document, verbatim

1. **The document's `make warm PORT=19066` was run, and it is the cause of B1.** The document's
   sentence is: *"In a separate owned terminal in the PROBE checkout: `make warm PORT=19066`"*.
   It was run exactly, in `subject-probe`, backgrounded under `setsid nohup` because this seat has
   no second terminal. The image started correctly. The resulting `?? .clj-surgeon/` is what
   `measure.py` line 56 refuses.
2. **`measure.py init` was run before the blocker was known**, from `subject-native`, which was
   clean. `results/subject.json` therefore exists and pins
   `subject_sha=759974c718889c52a05a1d0746c9c21d0f00dcdb`. It uses `open(...,'x')` and cannot be
   re-written; a resumed run must either reuse it (the subject SHA must still match) or use a
   fresh output directory.
3. **One NATIVE run was executed outside `results/` for apparatus diagnosis only**, in
   `/var/tmp/forge/probe-measure-fx/apparatus-smoke`, then moved to
   `results/apparatus/native-smoke-n1/`. The document does not authorise it as a cell and it is not
   reported as one. `subject-native` was restored with `git checkout -- .` afterwards and
   `git status --porcelain` is empty.
4. **`model` and `effort` recorded in `subject.json` are `claude-opus-5[1m]` and `default`.** The
   document requires them recorded before timing. `effort` is the runner operator's declared value
   and is not externally attested. Because the frozen patches are applied mechanically by the
   runner rather than authored by a model, neither value influenced any measured number — there
   are none.
5. **No stale-result negative control was run.** The document requires it untimed, but it is a
   control over *both* commands; with the PROBE command unrunnable it could only have exercised
   half the control, which would be a partial control reported as a whole one.
6. **Exactly one arm was ever active**, `TMPDIR` and `JAVA_TOOL_OPTIONS` were set verbatim as the
   document specifies, every JVM ran at `-Xmx1024m`/`-Xmx1g`, the warm image used port 19066 only,
   and the shared installed `~/bin/clj-surgeon` was never invoked — the document's shell function
   over the checkout was used for the admission probes.
7. **No `pkill`, no `pgrep -f` on the measured processes, no outer `timeout`.** The warm image was
   stopped by its exact PID (1005716). An exclusive `flock` on `/home/forge/tmp/suite-1.lock` was
   held for the whole run by a holder process this seat owned (flock PID 1000087, its `sleep`
   child PID 1000088 which actually inherited the descriptor), and both were terminated by exact
   PID at the end; the lock was verified free afterwards.
8. **`subject-probe` still contains the untracked `.clj-surgeon/` directory.** It was deliberately
   left in place so B1 reproduces without restarting an image; its bytes are also copied to
   `results/setup/probe.edn`. The image it points at is stopped, so that descriptor is stale and a
   resumption must run `make warm PORT=19066` again.
9. **Both worktrees are retained**, not removed: the measurement did not run, so they are needed to
   resume. Everything produced is under `results/`; the worktrees hold no unique evidence.

## What a resumption needs, in one line each

- **B1:** a decision on how the warm image's identity file may coexist with the clean-checkout
  assertion — amend `measure.py` line 56, ignore `.clj-surgeon/` in the subject, or pass an
  `:image-file` outside the checkout — each of which changes a frozen artifact and needs the
  bettors' sign-off, not a runner's.
- **B2:** a decision on T4/T5 — re-cut against trunk (changes frozen patch bytes and hashes), or
  accept a JVM-only stratum of T6 alone with no variance floor and a JVM bound that no longer
  separates the two rows.
- The warm image must be restarted after any resumption: its generation is single-use and the one
  recorded here is stopped.
