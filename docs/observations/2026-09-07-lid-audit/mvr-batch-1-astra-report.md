# MVR LID batch 1 — G5 and G1

Branch: `astra/lid-batch-1`; base: `d170f3d5edea6faa39396ea8b3418e29b2e2b4b1`.
Worktree: `/home/forge/src/mvr-lid2`. Commit: `b2261433ec613b4489bdd84135141ff2e784ea0a`. Author: `forge-anvil <forge-anvil@anvil>` (environment). Both requested co-author trailers are present. Working tree clean; branch is one local commit ahead of `origin/main`. No push.
Tracking: `marvin-voice-remote-960` (closed). Evidence files below are relative to `/var/tmp/forge/lid-batch/mvr-b1-evidence/`.

## Files changed

- `docs/intent/registry.edn`: appended one active stable intent, `BRIDGE4-TAPPED-OVER-ONE-PATH-G1`, with the ledger's EARS text verbatim, five misreadings, five boundaries, and the existing witness name.
- `src/marvin_voice_remote/channel.clj`: added intent tags at `force-end-param` and `ended = (or word-ended force-end?)`.
- `src/marvin_voice_remote/reducer/echo_guard.clj`: added `;; INTENT: SELF-ECHO-QUARANTINE-F5` immediately before `classify`. The requested path without `reducer/` does not exist; this is the ledger's actual classifier site.
- `test/marvin_voice_remote/channel_test.clj`: tagged `bridge4-keyword-check-honors-a-tapped-over-force-end` and added one sequential same-request replay case with `force_end=1` and the same `dedup_id`. Asserts exact first and cached terminal JSON, HTTP 200, exactly one seat message, exactly one conversation turn, and original queued text. A different retry STT stub makes accidental reprocessing observable. Temporary audio is deleted and the dedup cache reset in `finally`.

No runtime behavior changes remain. The intentional mutation was removed before acceptance checks. No other audit-ledger gaps were implemented; the bidirectional contract is unchanged.

## Registry row

```clojure
{:id :BRIDGE4-TAPPED-OVER-ONE-PATH-G1
   :status :active
   :see [:SPOKEN-SKIP-F2 :POL-P6]
   :ears "While a bridge4 hands-free utterance is open, when the client finalizes it with force_end=1 instead of a trailing end word, the server shall finalize it through the same dedup, archive, enqueue and broadcast path a spoken end word takes, bypassing only the last-word test."
   :misreadings
   ["Give a tapped OVER its own endpoint or its own enqueue call — a second send path is a second authority and will drift from the spoken one (dedup id reuse, archive verdict, bridge3 turn append)."
    "Let force_end skip the blank-transcript refusal — a tap on silence must still not post."
    "Treat a trailing CANCEL word without force_end as a send — the client discards it, and the server must keep returning ended false."
    "Strip the end-word sentinel only on the spoken path — a user who taps AND says 'over' must not see the sentinel in the posted text."
    "Treat force_end as client state — it is a request parameter on the SAME keyword-check request, so a lost response replays into the same dedup entry."]
   :boundaries
   ["force_end=1 with no end word -> ended true, raw transcript posted once"
    "force_end=1 with a trailing end word -> sentinel stripped"
    "force_end=1 on a blank/whitespace transcript -> ended false, nothing enqueued"
    "no force_end, trailing cancel word -> ended false, nothing enqueued"
    "repeat of the same dedup_id -> the cached terminal result, never a second message"]
   :tests [:bridge4-keyword-check-honors-a-tapped-over-force-end]
   :rationale "Incident marvin-voice-remote-3ug, 2026-09-02 (commit 6d4cecf): car-speaker self-echo drove an OVER/CANCEL button trial on the live driving surface; the tap was deliberately routed onto the existing keyword-check request so tap and end word cannot drift. Site: channel.clj force-end-param / handle-bridge4-keyword-check."}
```

The EARS outcome is one finalization path; dedup/archive/enqueue/broadcast name its stages, not independent outcomes. The CANCEL misreading was corrected to match the ledger's explicit `no force_end` boundary and existing witness; see doubts below.

## RED first: plant, run, unplant

Code inspection: `handle-bridge4-keyword-check` consults `bridge4-keyword-seen` before audio/archive/STT. A cache hit returns `{:ok true :ended true :transcript cached :dedup true}`. A miss can reach `(or word-ended force-end?)`, remember the result, enqueue, and append a conversation turn. Bypassing the cache for a tap therefore reprocesses and posts the replay.

Temporary mutation (only the bridge4 cache-hit condition):

```diff
-      cached
+      (and cached (not force-end?))
       (do (log/info :bridge4-keyword-dup :id dedup-id :seat seat)
```

The first execution of the new witness was against this planted defect:

```sh
bin/kaocha --focus marvin-voice-remote.channel-test/bridge4-keyword-check-honors-a-tapped-over-force-end
```

RED output (ANSI formatting and repeated captured application logs removed):

```text
FAIL in marvin-voice-remote.channel-test/bridge4-keyword-check-honors-a-tapped-over-force-end (channel_test.clj:1080)
replaying the same dedup_id with force_end=1 returns the cached terminal result
lost-response retry returns the original terminal receipt
Expected:
  {:dedup true, :ended true, :ok true, :transcript "ship this utterance exactly once"}
Actual:
  {:ended true,
   :ok true,
   :transcript -"ship this utterance exactly once" +"this must never replace the cached transcript",
   -:dedup true}

FAIL in marvin-voice-remote.channel-test/bridge4-keyword-check-honors-a-tapped-over-force-end (channel_test.clj:1083)
replaying the same dedup_id with force_end=1 returns the cached terminal result
Expected:
  1
Actual:
  -1 +2

FAIL in marvin-voice-remote.channel-test/bridge4-keyword-check-honors-a-tapped-over-force-end (channel_test.clj:1084)
replaying the same dedup_id with force_end=1 returns the cached terminal result
Expected:
  1
Actual:
  -1 +2
1 tests, 21 assertions, 3 failures.

EXIT_STATUS=3
```

The plant produced the intended three failures: changed terminal response, two seat messages, and two conversation turns. Restored the condition to `cached` using a native patch; no test expectations were changed after RED. The shared temporary `red.log` was overwritten by another repository while the report was being assembled. The plant/run/unplant was repeated in the dedicated evidence directory; SHA-256 confirmed the restored source was byte-identical to the already-tested source. The required acceptance sequence was repeated there too. Full planted source diff: `planted-channel.diff`; complete output: `red.log`.

## Acceptance checks, in requested order

Commands used the following environment so JVM temporary audio and test state stayed under the authorized temporary root (the `_JAVA_OPTIONS` setting overrides the runner alias's `target/test-data`):

```sh
TMPDIR=/var/tmp/forge/lid-batch/mvr-b1-evidence/tmp
_JAVA_OPTIONS='-Djava.io.tmpdir=/var/tmp/forge/lid-batch/mvr-b1-evidence/tmp -Dmvr.data.dir=/var/tmp/forge/lid-batch/mvr-b1-evidence/data'
```

1. `MAKEFLAGS=--trace make runtests-once` — exit 0. Make tracing exposes the prerequisite commands without changing them. Prolog and beads evidence pasted from `make-runtests-once.log`:

```text
Makefile:210: update target 'check-beads-export' due to: target is .PHONY
echo "🔎 Checking committed Beads JSONL export..."
🔎 Checking committed Beads JSONL export...
jq -e -s 'all(.[]; type == "object" and has("_type") and has("id")) and ((map(.id) | length) == (map(.id) | unique | length))' .beads/issues.jsonl >/dev/null
echo "✓ Beads export is parseable and contains unique issue IDs"
✓ Beads export is parseable and contains unique issue IDs
Makefile:105: update target 'director-control-contract' due to: target is .PHONY
node test/director_capture_control_test.mjs
director capture-control reconnect contract: PASS
swipl -q -s test/director_control_invariants.pl
% [1/4] director_control_..d_stream_reconnects ...... passed (0.003 sec)
% [2/4] director_control_..es_not_claim_a_slot ...... passed (0.000 sec)
% [3/4] director_control_..ield_counterexample ...... passed (0.000 sec)
% [4/4] director_control_..tes_have_one_action ...... passed (0.000 sec)
Makefile:100: update target 'runtests-once' due to: target is .PHONY
```

Make/Kaocha tail (progress dots omitted):

```text
579 tests, 7848 assertions, 0 failures.

EXIT_STATUS=0
```

2. `bin/kaocha` — full suite, exit 0; tail from `kaocha-full.log` (progress dots omitted):

```text
579 tests, 7848 assertions, 0 failures.

EXIT_STATUS=0
```

3. `~/bin/clj-kondo --lint docs/intent/registry.edn src/marvin_voice_remote/channel.clj src/marvin_voice_remote/reducer/echo_guard.clj test/marvin_voice_remote/channel_test.clj` — exit 2, zero errors, 12 warnings. Tail from `clj-kondo.log`:

```text
src/marvin_voice_remote/channel.clj:3438:11: info: Redundant nested call: str
src/marvin_voice_remote/channel.clj:3484:11: info: Redundant nested call: str
src/marvin_voice_remote/channel.clj:3501:11: info: Redundant nested call: str
test/marvin_voice_remote/channel_test.clj:108:19: warning: Unresolved namespace marvin-voice-remote.auth. Are you missing a require?
linting took 189ms, errors: 0, warnings: 12

EXIT_STATUS=2
```

Because lint was nonzero, linted copies of the same four files from `d170f3d` under `baseline/`, using the repository's same `.clj-kondo` config. Baseline tail:

```text
/var/tmp/forge/lid-batch/baseline/src/marvin_voice_remote/channel.clj:3499:11: info: Redundant nested call: str
/var/tmp/forge/lid-batch/baseline/test/marvin_voice_remote/channel_test.clj:108:19: warning: Unresolved namespace marvin-voice-remote.auth. Are you missing a require?
linting took 185ms, errors: 0, warnings: 12

EXIT_STATUS=2
```

```text
Baseline d170f3d and batch lint findings match exactly after removing file/line/column: 19 findings (12 warnings, 7 info), zero errors. Both exit 2.
```

No warning was introduced by this batch. The baseline warnings are unresolved fully-qualified namespaces, unused private persistence/watch installation vars, and one redundant `let`; informational findings are redundant nested `str` calls. They were not suppressed or swept into this scoped batch. Lint is not claimed fully green.

After the required sequence, a focused green run made the specific linked witnesses explicit:

```sh
bin/kaocha --focus marvin-voice-remote.channel-test/bridge4-keyword-check-honors-a-tapped-over-force-end --focus marvin-voice-remote.intent-contract-test
```

```text
4 tests, 333 assertions, 0 failures.

EXIT_STATUS=0
```

This includes all three `intent_contract_test` deftests: registry shape, bidirectional traceability, and existence of every claimed witness. The new replay assertions are inside an existing deftest, so total suite test count remains 579. `git diff --check` also passed.

## What broke / doubts

- Only the deliberate dedup-bypass plant broke behavioral tests; removing it restored green. No production fix was needed for sequential dedup replay.
- Raw lint is nonzero on the base commit too. Exact baseline comparison establishes no new lint findings; it does not make existing warnings disappear.
- The ledger's draft CANCEL misreading overclaimed server enforcement for `force_end=1` plus trailing CANCEL. Existing code allows any nonblank transcript through force-end; existing CANCEL witness omits force-end and explicitly says the client discards. The row therefore records that witnessed boundary instead. This batch does not establish precedence for contradictory tap/CANCEL inputs.
- The new witness covers a sequential replay within the current in-memory dedup cache. It does not claim simultaneous-request atomicity, cache-eviction replay, cross-process durability, or restart recovery.
- The Prolog oracle really ran and passed all four cases, including `old_non_null_handle_policy_has_the_field_counterexample`. It models Director stream ownership/reconnection, not tapped OVER; the G1 behavior is witnessed by the Clojure test. `bin/kaocha` alone does not invoke this Prolog target: `make runtests-once` does.
- No simulator or physical-device claim: this is registry/test/comment work with no shipped audio/UI behavior change.
- No forbidden-port service was deliberately contacted; the full-turn suite uses its own server on OS-assigned port 0. No nREPL/MCP server was used. `.mcp.json` and `.codex/` were untouched. No push, pull, deployment, or remote messaging was performed.
- LID traceability proves these promises have witnesses; it does not prove the promises are the right product policy.

Completed 2026-09-07 17:22 UTC, approximately 10 minutes into the 30-minute budget. Raw receipts are checksummed in `mvr-b1-evidence/SHA256SUMS`.
