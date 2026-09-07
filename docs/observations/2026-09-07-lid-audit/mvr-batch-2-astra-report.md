# MVR LID batch 2 — G2 only

Branch: `astra/lid-batch-2`, base `b2261433ec613b4489bdd84135141ff2e784ea0a`.
Worktree: `/home/forge/src/mvr-lid3`.
Tracking: `marvin-voice-remote-nhz` (closed).
Commit: `3a2898f2a9d7c187ff6d1f17cbb8c2b59b1bfb25` (working tree clean; committed locally, not pushed). Author: environment identity `forge-anvil <forge-anvil@anvil>`.
Both requested co-author trailers are included. No push.
Evidence directory: `/var/tmp/forge/lid-batch/mvr-b2-evidence/`.

## Files changed

- `docs/intent/registry.edn`: two new active rows, one outcome each, exact requested EARS promises; OVER has five misreadings and five boundaries, CANCEL four misreadings and five boundaries. Both reference the shared protocol and matrix witnesses.
- `src/marvin_voice_remote/reducer/core.clj`: `;; INTENT:` immediately before `handle-over` and `handle-cancel`. The sticky-OVER mutation was removed; only these two comments remain changed in production code.
- `test/marvin_voice_remote/reducer/core_test.clj`: tags on the four existing manual command deftests; one new `manual-over-and-cancel-state-space-post-count-and-mode` deftest, tagged to both rows.
- `test/marvin_voice_remote/reducer/protocol_test.clj`: both tags on existing `manual-over-and-cancel-are-registered-and-rename-to-cmd`.

No production behavior fix was needed. No other ledger gap was implemented. Native, hosted UI, release configuration, `.mcp.json`, and `.codex/` were not changed. No live service ports were contacted; the full suite's existing wire test starts its own HTTP server with `:port 0`. Test temporary files and data were redirected under the requested temporary root. Beads created an untracked auto-import scratch export in `.beads/`; it was moved into the evidence directory and is not committed.

## The two registered rows

```clojure
{:id :TAPPED-OVER-JOINS-ONE-TURN-C01
   :status :active
   :see [:SPOKEN-SKIP-F2 :POL-P4]
   :ears "While a hands-free capture is live, when :cmd/over arrives, the reducer shall commit the live capture as a partial and post the joined turn exactly once when the completing transcript lands."
   :misreadings
   ["Post the partial immediately and the completing transcript as a second message — one utterance must reach the ledger as ONE turn."
    "Leave :over-requested? set after the join — it is one-shot; a sticky flag posts the NEXT turn early too."
    "Post nothing when there is only a stash and no live capture — a stash-only OVER posts immediately."
    "Post an empty turn on an empty session — an OVER with nothing to send is a named no-op, not a blank message."
    "Arm the button from client state — arming is the reducer's ruling (P4 disabled honesty)."]
   :boundaries
   ["live speech with or without a stash -> partial commit, zero posts at the tap, one joined post on the completing transcript, mode :hf"
    "no live capture with a nonblank stash -> one immediate post, mode :hf"
    "no live capture and no stash -> zero posts, named empty-over no-op, mode :hf"
    "after completion, the next ordinary partial -> no extra post; stash only the new turn"
    "silent listening window -> no speech commit; only an existing nonblank stash can post"]
   :tests [:manual-over-flushes-a-live-capture-and-the-next-transcript-posts-the-turn
           :manual-over-with-only-a-stash-posts-it-immediately
           :manual-over-on-an-empty-session-posts-nothing
           :manual-over-and-cancel-are-registered-and-rename-to-cmd
           :manual-over-and-cancel-state-space-post-count-and-mode]
   :rationale "Reducer command path c0b8cfe, audit G2: a tap has no transcript of its own, so handle-over commits live speech as a partial and handle-transcript consumes the one-shot request to post the joined turn. This is distinct from bridge4's force_end request path. Sites: reducer/core.clj handle-over and handle-transcript; ARCHITECTURE.md D2/P6."}

  {:id :TAPPED-CANCEL-DISCARDS-STAYS-HF-C02
   :status :active
   :see [:SPOKEN-SKIP-F2]
   :ears "While a hands-free capture or stash exists, when :cmd/cancel arrives, the reducer shall discard both and remain in hands-free mode."
   :misreadings
   ["Return the session to idle — CANCEL discards an utterance, it does not end the conversation."
    "Leave the stash behind because 'only the live capture was cancelled' — CANCEL is the destructive one; SKIP is the one that preserves the stash."
    "Post the discarded text anywhere (ledger, broadcast, archive as accepted)."
    "Let CANCEL and OVER race — CANCEL wins; precedence is CANCEL > SKIP > OVER."]
   :boundaries
   ["live capture with or without a stash -> discard the capture with no commit, empty stash, zero posts, mode :hf"
    "no live capture with a stash -> discard the stash, zero posts, mode :hf"
    "no live capture and no stash -> zero posts, mode :hf"
    "pending tapped OVER -> clear the request when CANCEL is handled; this is not a retraction of an already-posted turn"
    "next ordinary partial after CANCEL -> stash only the new text, zero posts, mode :hf"]
   :tests [:manual-cancel-drops-the-stash-discards-capture-and-stays-in-hf
           :manual-over-and-cancel-are-registered-and-rename-to-cmd
           :manual-over-and-cancel-state-space-post-count-and-mode]
   :rationale "Reducer command path c0b8cfe, audit G2: handle-cancel discards the current turn and clears any pending OVER while preserving the hands-free session; hf-stop is the separate command that exits. Sites: reducer/core.clj handle-cancel; ARCHITECTURE.md D2/P6."}
```

## Eight-cell witness

The cell inputs enumerate live speech capture present/absent, nonblank stash present/absent, and OVER/CANCEL. Expected counts, modes and posted text are hand-written table literals, not calculated from the command or reducer predicates. Each assertion runs under a `testing` string naming the exact cell.

| Cell | Posts at tap | Posts after completion | Posts after next ordinary partial | Mode at all three stages |
|---|---:|---:|---:|---|
| live=yes stash=yes OVER | 0 | 1 | 1 | :hf |
| live=yes stash=no OVER | 0 | 1 | 1 | :hf |
| live=no stash=yes OVER | 1 | 1 | 1 | :hf |
| live=no stash=no OVER | 0 | 0 | 0 | :hf |
| live=yes stash=yes CANCEL | 0 | 0 | 0 | :hf |
| live=yes stash=no CANCEL | 0 | 0 | 0 | :hf |
| live=no stash=yes CANCEL | 0 | 0 | 0 | :hf |
| live=no stash=no CANCEL | 0 | 0 | 0 | :hf |

The completion transcript is a partial (`:final? false`) with no OVER word. Live OVER joins `first part last part` or posts `last part`; stash-only OVER posts `first part`. All CANCEL cells post nothing. The next ordinary pause commits actual simulated speech and returns a new partial transcript; it must leave only `next ordinary partial` in the stash. The test also checks that HF issues the next capture and that the reducer harness reports no invariant violations. There are 11 assertions per cell, 88 total.

## Red first, then unplant and green

The first execution of the new witness was against this one-line narrowing in `handle-transcript`:

```diff
-        over-req' (if (or (and ends? over-req?) (:cancel? derived)) nil (:over-requested? state))]
+        over-req' (if (:cancel? derived) nil (:over-requested? state))]
```

This preserves the request after a successful OVER join, making it sticky. Complete planted diff: `planted-core.diff`. Command:

```sh
bin/kaocha --focus marvin-voice-remote.reducer.core-test/manual-over-and-cancel-state-space-post-count-and-mode --no-color
```

Red output from `red.log`:

```text
FAIL in marvin-voice-remote.reducer.core-test/manual-over-and-cancel-state-space-post-count-and-mode (core_test.clj:3190)
live=yes stash=yes OVER
no extra post from the next ordinary partial
Expected:
  1
Actual:
  -1 +2

FAIL in marvin-voice-remote.reducer.core-test/manual-over-and-cancel-state-space-post-count-and-mode (core_test.clj:3192)
live=yes stash=yes OVER
only the new turn is stashed
Expected:
  ["next ordinary partial"]
Actual:
  [-"next ordinary partial"]

FAIL in marvin-voice-remote.reducer.core-test/manual-over-and-cancel-state-space-post-count-and-mode (core_test.clj:3190)
live=yes stash=no OVER
no extra post from the next ordinary partial
Expected:
  1
Actual:
  -1 +2

FAIL in marvin-voice-remote.reducer.core-test/manual-over-and-cancel-state-space-post-count-and-mode (core_test.clj:3192)
live=yes stash=no OVER
only the new turn is stashed
Expected:
  ["next ordinary partial"]
Actual:
  [-"next ordinary partial"]
1 tests, 88 assertions, 4 failures.
EXIT_STATUS=4
```

Exactly two cells failed: `live=yes stash=yes OVER` and `live=yes stash=no OVER`. Each had two failures: the later partial raised the posted count from the expected 1 to 2, and its text was posted instead of stashed. The other six cells passed; all mode assertions passed even under this mutation.

Restored the original one-line expression with a native patch. No test expectation or fixture changed between red and green. The same command then produced (`green.log`):

```text
GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
Mode: :runtime  config: {:throw? true}
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
1 tests, 88 assertions, 0 failures.
EXIT_STATUS=0
```

## Full entry, in the requested order

Commands sourced `mvr-b2-evidence/env.sh`:

```sh
export TMPDIR=/var/tmp/forge/lid-batch/mvr-b2-evidence/tmp
export _JAVA_OPTIONS='-Djava.io.tmpdir=/var/tmp/forge/lid-batch/mvr-b2-evidence/tmp -Dmvr.data.dir=/var/tmp/forge/lid-batch/mvr-b2-evidence/data'

```

The JVM prints the inherited `JAVA_TOOL_OPTIONS` temporary directory too; `_JAVA_OPTIONS` takes precedence and points both JVM temp and MVR test persistence at the batch evidence directory.

### 1. make runtests-once

Exit 0. Tail below omits only the progress-dot line and ANSI color; full receipt: `make-runtests-once.log`. The Prolog oracle lines are retained:

```text
🔎 Checking committed Beads JSONL export...
✓ Beads export is parseable and contains unique issue IDs
director capture-control reconnect contract: PASS
% [1/4] director_control_..d_stream_reconnects ...... passed (0.003 sec)
% [2/4] director_control_..es_not_claim_a_slot ...... passed (0.000 sec)
% [3/4] director_control_..ield_counterexample ...... passed (0.000 sec)
% [4/4] director_control_..tes_have_one_action ...... passed (0.000 sec)
Running tests with fail-fast...
bin/kaocha --fail-fast
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
Picked up _JAVA_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/lid-batch/mvr-b2-evidence/tmp -Dmvr.data.dir=/var/tmp/forge/lid-batch/mvr-b2-evidence/data
GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
Mode: :runtime  config: {:throw? true}
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
580 tests, 7958 assertions, 0 failures.
EXIT_STATUS=0
```

The four Prolog cases are `active_closed_stream_reconnects`, `inactive_capture_does_not_claim_a_slot`, `old_non_null_handle_policy_has_the_field_counterexample`, and `all_stream_states_have_one_action` in `test/director_control_invariants.pl`. These are the existing director control lease oracle, not a new Prolog model of OVER/CANCEL.

### 2. bin/kaocha

Exit 0; **580 tests, 7958 assertions, zero failures**, exceeding the requested 579 minimum. Tail from `kaocha-full.log` (progress dots and ANSI color omitted):

```text
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
2026-09-07T17:27:59.419Z anvil-server INFO [marvin-voice-remote.channel:187] - :dictations-restored :count 0
2026-09-07T17:27:59.480Z anvil-server INFO [marvin-voice-remote.channel:1009] - :bridge3-sessions-restored :count 1
2026-09-07T17:27:59.484Z anvil-server INFO [marvin-voice-remote.channel:1038] - :bridge3-convos-restored :seats 0 :turns 0
580 tests, 7958 assertions, 0 failures.
EXIT_STATUS=0
```

The bidirectional intent contract runs in both full entries. An additional named receipt after the ordered gates makes its result explicit:

```sh
bin/kaocha --focus marvin-voice-remote.intent-contract-test --no-color
```

```text
2026-09-07T17:29:22.743Z anvil-server INFO [marvin-voice-remote.channel:187] - :dictations-restored :count 0
2026-09-07T17:29:22.804Z anvil-server INFO [marvin-voice-remote.channel:1009] - :bridge3-sessions-restored :count 1
2026-09-07T17:29:22.807Z anvil-server INFO [marvin-voice-remote.channel:1038] - :bridge3-convos-restored :seats 1 :turns 1
3 tests, 334 assertions, 0 failures.
EXIT_STATUS=0
```

### 3. ~/bin/clj-kondo --lint on all four changed files

The first invocation used `--cache-dir /var/tmp/forge/lid-batch/mvr-b2-evidence/kondo-cache` and crashed with a `NullPointerException` in `clj_kondo.impl.cache/skip_write_QMARK_` before publishing findings (exit 1; `clj-kondo-cache-error.log`). Fixed the invocation by disabling the optional cache, without changing source or suppressing lint checks:

```sh
~/bin/clj-kondo --lint docs/intent/registry.edn src/marvin_voice_remote/reducer/core.clj test/marvin_voice_remote/reducer/core_test.clj test/marvin_voice_remote/reducer/protocol_test.clj --cache false
```

Tail from `clj-kondo.log`:

```text
test/marvin_voice_remote/reducer/core_test.clj:1657:20: warning: Unresolved var: core/duration-str
test/marvin_voice_remote/reducer/core_test.clj:1664:20: warning: Unresolved var: core/size-str
test/marvin_voice_remote/reducer/core_test.clj:1861:25: warning: Unresolved var: core/turn-view
test/marvin_voice_remote/reducer/core_test.clj:1962:33: warning: Unresolved var: core/transport-view
test/marvin_voice_remote/reducer/core_test.clj:3080:5: warning: Redundant let expression.
test/marvin_voice_remote/reducer/core_test.clj:3274:20: warning: Unresolved var: core/silence-hallucination?
linting took 322ms, errors: 25, warnings: 13
EXIT_STATUS=3
```

Because lint was nonzero, linted pre-edit copies of all four files from `b226143` under `mvr-b2-evidence/baseline/`, with the same executable, working directory and `--cache false`. Baseline tail:

```text
/var/tmp/forge/lid-batch/mvr-b2-evidence/baseline/test/marvin_voice_remote/reducer/core_test.clj:1657:20: warning: Unresolved var: core/duration-str
/var/tmp/forge/lid-batch/mvr-b2-evidence/baseline/test/marvin_voice_remote/reducer/core_test.clj:1664:20: warning: Unresolved var: core/size-str
/var/tmp/forge/lid-batch/mvr-b2-evidence/baseline/test/marvin_voice_remote/reducer/core_test.clj:1861:25: warning: Unresolved var: core/turn-view
/var/tmp/forge/lid-batch/mvr-b2-evidence/baseline/test/marvin_voice_remote/reducer/core_test.clj:1962:33: warning: Unresolved var: core/transport-view
/var/tmp/forge/lid-batch/mvr-b2-evidence/baseline/test/marvin_voice_remote/reducer/core_test.clj:3078:5: warning: Redundant let expression.
/var/tmp/forge/lid-batch/mvr-b2-evidence/baseline/test/marvin_voice_remote/reducer/core_test.clj:3213:20: warning: Unresolved var: core/silence-hallucination?
linting took 275ms, errors: 25, warnings: 13
EXIT_STATUS=3
```

Comparison retains relative file, severity, message, and multiplicity, normalizing only the baseline directory prefix and source line/column shifts:

```text
Baseline b226143 and batch findings match after normalizing file prefix and line/column: True. Baseline 44; batch 44.
New findings: {}
Removed findings: {}

```

Both runs report **25 errors, 13 warnings, 6 informational findings**. Most are unresolved symbols/vars around existing Guardrails definitions; there are also pre-existing vector-call arity findings, an unused binding, a redundant let, and redundant boolean coercions. Raw lint is not green; this batch introduces no lint findings. `git diff --check` is clean.

## Doubts and limits

- The new matrix witnesses the pure reducer's posted utterance ledger (`:posted?`), joined text and mode. It does not by itself prove network enqueue, broadcast, archive, physical audio, or UI button arming. Existing tests and full-suite gates remain in place; this batch does not claim new physical evidence.
- “Live capture” in the matrix means registered speech; the no-live cells explicitly remove the capture and set the between-turn phase, matching the existing stash-only fixture. Silent open windows remain a distinct boundary, covered by the existing empty-session test and the documented handler behavior rather than a ninth matrix cell.
- The ledger's CANCEL > SKIP > OVER misreading is retained. This eight-cell product is sequential; it does not exhaust arbitrary concurrent delivery, transcript reordering, or retract an already-posted turn. The boundary explicitly limits cancellation of pending OVER to when CANCEL is handled.
- The red mutation demonstrates sensitivity to a sticky request in both live OVER cells. It does not prove sensitivity to every plausible future implementation mistake.
- The cache exception was worked around, not repaired inside clj-kondo. Baseline lint debt remains visible and unchanged. No repository test failures needed fixes after the mutation was removed.

All requested work for G2 is complete. G3/G4 and architecture expansion remain outside this batch's scope in the existing audit ledger.
